# Bakery Production Planning API

REST API для управления производством пекарни: ингредиенты, рецептуры,
производственные партии и заявки на выпечку.

**Стек:** Java 21, Spring Boot, Spring Data JPA, Hibernate, PostgreSQL, Maven.

---

## Предметная область

Приложение моделирует производственный процесс пекарни:

1. **Пользователь (User)** — заказчик, который оформляет заявки на выпечку.
2. **Ингредиент (Ingredient)** — сырьё (мука, сахар и т.д.) с единицей измерения и остатком на складе.
3. **Рецептура (Recipe)** — состав изделия: список ингредиентов с количеством на единицу продукции.
4. **Производственная партия (ProductionBatch)** — запущенное производство: рецептура, количество изделий, статус.
5. **Заявка на выпечку (BakingRequest)** — заказ от пользователя: рецептура, количество, статус.

### Связи между сущностями

- `BakingRequest.user_id` → `users.id` (заявка принадлежит пользователю)
- `BakingRequest.recipe_id` → `recipes.id` (заявка на конкретную рецептуру)
- `BakingRequest.batch_id` → `production_batches.id` (заявка может быть привязана к партии)
- `ProductionBatch.recipe_id` → `recipes.id` (партия производится по рецептуре)
- `RecipeItem.recipe_id` → `recipes.id` (элемент состава рецептуры)
- `RecipeItem.ingredient_id` → `ingredients.id` (элемент ссылается на ингредиент)

---

## Бизнес-операции

Реализованы 5 операций, отражающих специфику производства пекарни. Каждая операция изменяет состояние
нескольких сущностей и зависит от текущего состояния данных.

### 1. Запуск партии со списанием ингредиентов
`POST /api/business/start-batch/{batchId}`

Партия из статуса `PLANNED` переходит в `IN_PROGRESS`. Со склада списываются ингредиенты
по рецептуре (количество на единицу × количество изделий). Если ингредиентов не хватает —
операция отклоняется, данные не изменяются (транзакция откатывается).

**Затрагивает:** ProductionBatch, Recipe, Ingredient.

### 2. Завершение партии
`POST /api/business/complete-batch/{batchId}`

Партия из статуса `IN_PROGRESS` переходит в `DONE`. Завершить можно только запущенную партию.

### 3. Превращение заявки в производственную партию
`POST /api/business/request-to-batch/{requestId}`

Заявка из статуса `NEW` превращается в новую производственную партию. Заявка связывается
с партией и переходит в статус `IN_PROGRESS`.

**Затрагивает:** BakingRequest, ProductionBatch.

### 4. Массовое планирование партии
`POST /api/business/plan-batch` (тело: `{ "recipeId": 1, "quantity": 20 }`)

Создаётся новая партия указанного объёма, **если** ингредиентов хватает. Иначе — отказ.

**Затрагивает:** Recipe, Ingredient, ProductionBatch.

### 5. Закрытие партии с обновлением связанных заявок
`POST /api/business/close-batch/{batchId}`

Партия должна быть в статусе `DONE`. Все заявки, привязанные к этой партии,
переходят в статус `READY`.

**Затрагивает:** ProductionBatch, BakingRequest.

---

## REST API

Для каждой сущности реализованы 5 CRUD-операций:

| Метод | URL | Действие |
|---|---|---|
| GET | `/api/{resource}` | Список всех |
| GET | `/api/{resource}/{id}` | Один по id |
| POST | `/api/{resource}` | Создать (201 Created) |
| PATCH | `/api/{resource}/{id}` | Обновить (200 OK) |
| DELETE | `/api/{resource}/{id}` | Удалить (204 No Content) |

Ресурсы: `users`, `ingredients`, `recipes`, `production-batches`, `baking-requests`.

**Ограничения целостности при удалении:** нельзя удалить сущность, на которую ссылаются
другие (например, ингредиент, используемый в рецептуре). Возвращается ошибка 400.

---

## Стек и архитектура

- **Java 21**, **Spring Boot**, **Maven**.
- **Spring Web MVC** — REST-контроллеры.
- **Spring Data JPA + Hibernate** — слой работы с БД.
- **PostgreSQL** — реляционная СУБД.
- **dotenv-java** — для чтения секретов из `.env`-файла.

**Архитектура (слои):**

```
Controller → Service → Repository → PostgreSQL
```

- **Controller** — приём HTTP-запросов, формирование ответов.
- **Service** — бизнес-логика, проверки, транзакции.
- **Repository** — доступ к данным (Spring Data JPA).
- **BusinessService** — 5 бизнес-операций.

---

## Запуск проекта

### Требования
- Java 21
- Maven
- PostgreSQL 14+
- Настроенный файл `.env` в корне проекта (см. `.env.example`)

### Настройка БД

```sql
CREATE DATABASE bakery;
CREATE USER bakery_user WITH PASSWORD 'bakery_pass';
GRANT CONNECT ON DATABASE bakery TO bakery_user;
\c bakery
GRANT USAGE ON SCHEMA public TO bakery_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO bakery_user;
GRANT CREATE ON SCHEMA public TO bakery_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO bakery_user;
```

### Конфигурация `.env`

Создайте файл `.env` в корне проекта:

```
DB_URL=jdbc:postgresql://localhost:5432/bakery
DB_USERNAME=bakery_user
DB_PASSWORD=bakery_pass
```

### Запуск

```bash
./mvnw spring-boot:run
```

Сервер стартует на `http://localhost:8080`.

Таблицы создаются автоматически Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

---

## Тестирование

Файл `requests.http` в корне проекта содержит 30 запросов:
- CRUD для всех 5 сущностей (25 запросов).
- 5 бизнес-операций.

**Запуск в IntelliJ IDEA:** откройте `requests.http` и кликните ▷ слева от нужного запроса.

**Рекомендуемая последовательность для проверки:**
1. Создать пользователя.
2. Создать 2 ингредиента.
3. Создать рецептуру с этими ингредиентами.
4. Создать партию и заявку.
5. Выполнить бизнес-операции (`start-batch`, `request-to-batch`, `close-batch`).
6. Проверить, что данные в БД изменились согласованно.

---

## Структура проекта

```
src/main/java/com/example/student/
├── StudentApplication.java
├── business/                 ← бизнес-операции
│   ├── BusinessController.java
│   └── BusinessService.java
├── controller/               ← CRUD-контроллеры
├── model/                    ← JPA-сущности
├── repository/               ← JpaRepository-интерфейсы
└── service/                  ← бизнес-логика CRUD
```