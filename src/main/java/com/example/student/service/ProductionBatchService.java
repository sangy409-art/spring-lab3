package com.example.student.service;

import com.example.student.model.ProductionBatch;
import com.example.student.model.Recipe;
import com.example.student.repository.BakingRequestRepository;
import com.example.student.repository.ProductionBatchRepository;
import com.example.student.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductionBatchService {
    private final ProductionBatchRepository repository;
    private final RecipeRepository recipeRepository;
    private final BakingRequestRepository requestRepository;

    public ProductionBatchService(ProductionBatchRepository repository,
                                  RecipeRepository recipeRepository,
                                  BakingRequestRepository requestRepository) {
        this.repository = repository;
        this.recipeRepository = recipeRepository;
        this.requestRepository = requestRepository;
    }

    public List<ProductionBatch> getAll() { return repository.findAll(); }

    public ProductionBatch getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Партия не найдена: " + id));
    }

    public ProductionBatch create(ProductionBatch batch) {
        if (batch.getRecipe() == null || batch.getRecipe().getId() == null
                || !recipeRepository.existsById(batch.getRecipe().getId())) {
            throw new RuntimeException("Рецептура не найдена");
        }
        Recipe recipe = recipeRepository.findById(batch.getRecipe().getId()).get();
        batch.setRecipe(recipe);
        batch.setId(null);
        if (batch.getStatus() == null) batch.setStatus("PLANNED");
        return repository.save(batch);
    }

    public ProductionBatch update(Long id, ProductionBatch patch) {
        ProductionBatch existing = getById(id);
        if (patch.getQuantity() != 0) existing.setQuantity(patch.getQuantity());
        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Партия не найдена: " + id);
        }
        boolean usedInRequest = requestRepository.findAll().stream()
                .anyMatch(r -> r.getBatch() != null && r.getBatch().getId().equals(id));
        if (usedInRequest) {
            throw new RuntimeException(
                    "Нельзя удалить партию id=" + id + ": на неё ссылаются заявки на выпечку");
        }
        repository.deleteById(id);
    }
}