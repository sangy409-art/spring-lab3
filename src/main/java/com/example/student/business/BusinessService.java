package com.example.student.business;

import com.example.student.model.*;
import com.example.student.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessService {

    private final ProductionBatchRepository batchRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final BakingRequestRepository requestRepository;

    public BusinessService(ProductionBatchRepository batchRepository,
                           RecipeRepository recipeRepository,
                           IngredientRepository ingredientRepository,
                           BakingRequestRepository requestRepository) {
        this.batchRepository = batchRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.requestRepository = requestRepository;
    }

    /**
     * Операция 1. Запуск партии со списанием ингредиентов.
     *
     * Условия:
     *  - Партия должна быть в статусе PLANNED.
     *  - Ингредиентов на складе должно хватать.
     *
     * @Transactional гарантирует: либо всё спишется, либо ничего (откат).
     */
    @Transactional
    public ProductionBatch startBatch(Long batchId) {
        // 1. Находим партию
        ProductionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Партия не найдена: " + batchId));

        // 2. Проверяем статус
        if (!"PLANNED".equals(batch.getStatus())) {
            throw new RuntimeException("Нельзя запустить партию в статусе: " + batch.getStatus());
        }

        // 3. Получаем рецептуру
        Recipe recipe = batch.getRecipe();
        if (recipe == null) {
            throw new RuntimeException("У партии нет рецептуры");
        }

        // 4. Проверяем, хватает ли ингредиентов
        for (RecipeItem item : recipe.getItems()) {
            Ingredient ingredient = item.getIngredient();
            double needed = item.getQuantity() * batch.getQuantity();
            if (ingredient.getStock() < needed) {
                throw new RuntimeException(
                        "Недостаточно ингредиента '" + ingredient.getName() + "': "
                                + "нужно " + needed + ", на складе " + ingredient.getStock());
            }
        }

        // 5. Списываем ингредиенты
        for (RecipeItem item : recipe.getItems()) {
            Ingredient ingredient = item.getIngredient();
            double needed = item.getQuantity() * batch.getQuantity();
            ingredient.setStock(ingredient.getStock() - needed);
            ingredientRepository.save(ingredient);
        }

        // 6. Меняем статус партии
        batch.setStatus("IN_PROGRESS");
        return batchRepository.save(batch);
    }

    /**
     * Операция 2. Завершение партии.
     * Партия PLANNED или IN_PROGRESS → DONE.
     */
    @Transactional
    public ProductionBatch completeBatch(Long batchId) {
        ProductionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Партия не найдена: " + batchId));

        if ("DONE".equals(batch.getStatus())) {
            throw new RuntimeException("Партия уже завершена");
        }
        if (!"IN_PROGRESS".equals(batch.getStatus())) {
            throw new RuntimeException("Нельзя завершить партию в статусе: " + batch.getStatus());
        }

        batch.setStatus("DONE");
        return batchRepository.save(batch);
    }

    /**
     * Операция 3. Превратить заявку в партию.
     * Заявка в статусе NEW → создаётся партия, заявка связывается, статус заявки IN_PROGRESS.
     */
    @Transactional
    public ProductionBatch requestToBatch(Long requestId) {
        BakingRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена: " + requestId));

        if (!"NEW".equals(request.getStatus())) {
            throw new RuntimeException("Нельзя превратить в партию заявку в статусе: " + request.getStatus());
        }

        Recipe recipe = request.getRecipe();
        if (recipe == null) {
            throw new RuntimeException("У заявки нет рецептуры");
        }

        // Создаём партию
        ProductionBatch batch = new ProductionBatch();
        batch.setRecipe(recipe);
        batch.setQuantity(request.getQuantity());
        batch.setStatus("PLANNED");
        batch = batchRepository.save(batch);

        // Связываем заявку с партией
        request.setBatch(batch);
        request.setStatus("IN_PROGRESS");
        requestRepository.save(request);

        return batch;
    }

    /**
     * Операция 4. Массовое планирование партий.
     * Проверяет, хватит ли ингредиентов на N партий по рецептуре.
     * Если хватает — создаёт партию и возвращает её.
     * Если нет — отказ.
     */
    @Transactional
    public ProductionBatch planBatch(Long recipeId, int quantity) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Рецептура не найдена: " + recipeId));

        // Проверяем, хватит ли ингредиентов
        for (RecipeItem item : recipe.getItems()) {
            Ingredient ingredient = item.getIngredient();
            double needed = item.getQuantity() * quantity;
            if (ingredient.getStock() < needed) {
                throw new RuntimeException(
                        "Недостаточно ингредиента '" + ingredient.getName() + "': "
                                + "нужно " + needed + ", на складе " + ingredient.getStock());
            }
        }

        ProductionBatch batch = new ProductionBatch();
        batch.setRecipe(recipe);
        batch.setQuantity(quantity);
        batch.setStatus("PLANNED");
        return batchRepository.save(batch);
    }

    /**
     * Операция 5. Закрытие партии с обновлением связанных заявок.
     * Партия → DONE, все связанные заявки → READY.
     */
    @Transactional
    public ProductionBatch closeBatch(Long batchId) {
        ProductionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Партия не найдена: " + batchId));

        if (!"DONE".equals(batch.getStatus())) {
            throw new RuntimeException("Нельзя закрыть партию в статусе: " + batch.getStatus());
        }

        // Обновляем все связанные заявки
        for (BakingRequest request : requestRepository.findAll()) {
            if (request.getBatch() != null && request.getBatch().getId().equals(batchId)) {
                request.setStatus("READY");
                requestRepository.save(request);
            }
        }

        return batch;
    }
}