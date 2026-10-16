package com.example.student.service;

import com.example.student.model.Ingredient;
import com.example.student.repository.IngredientRepository;
import com.example.student.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngredientService {
    private final IngredientRepository repository;
    private final RecipeRepository recipeRepository;

    public IngredientService(IngredientRepository repository,
                             RecipeRepository recipeRepository) {
        this.repository = repository;
        this.recipeRepository = recipeRepository;
    }

    public List<Ingredient> getAll() { return repository.findAll(); }

    public Ingredient getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ингредиент не найден: " + id));
    }

    public Ingredient create(Ingredient ingredient) {
        ingredient.setId(null);
        return repository.save(ingredient);
    }

    public Ingredient update(Long id, Ingredient patch) {
        Ingredient existing = getById(id);
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getUnit() != null) existing.setUnit(patch.getUnit());
        if (patch.getStock() != 0) existing.setStock(patch.getStock());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Ингредиент не найден: " + id);
        }
        boolean usedInRecipe = recipeRepository.findAll().stream()
                .flatMap(r -> r.getItems().stream())
                .anyMatch(item -> item.getIngredient() != null
                        && item.getIngredient().getId().equals(id));
        if (usedInRecipe) {
            throw new RuntimeException(
                    "Нельзя удалить ингредиент id=" + id + ": он используется в рецептурах");
        }
        repository.deleteById(id);
    }
}