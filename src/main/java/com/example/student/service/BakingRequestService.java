package com.example.student.service;

import com.example.student.model.BakingRequest;
import com.example.student.model.ProductionBatch;
import com.example.student.model.Recipe;
import com.example.student.model.User;
import com.example.student.repository.BakingRequestRepository;
import com.example.student.repository.ProductionBatchRepository;
import com.example.student.repository.RecipeRepository;
import com.example.student.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BakingRequestService {
    private final BakingRequestRepository repository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final ProductionBatchRepository batchRepository;

    public BakingRequestService(BakingRequestRepository repository,
                                RecipeRepository recipeRepository,
                                UserRepository userRepository,
                                ProductionBatchRepository batchRepository) {
        this.repository = repository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
    }

    public List<BakingRequest> getAll() { return repository.findAll(); }

    public BakingRequest getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена: " + id));
    }

    public BakingRequest create(BakingRequest request) {
        if (request.getUser() == null || request.getUser().getId() == null
                || !userRepository.existsById(request.getUser().getId())) {
            throw new RuntimeException("Пользователь не найден");
        }
        if (request.getRecipe() == null || request.getRecipe().getId() == null
                || !recipeRepository.existsById(request.getRecipe().getId())) {
            throw new RuntimeException("Рецептура не найдена");
        }
        User user = userRepository.findById(request.getUser().getId()).get();
        Recipe recipe = recipeRepository.findById(request.getRecipe().getId()).get();
        request.setUser(user);
        request.setRecipe(recipe);
        request.setId(null);
        if (request.getStatus() == null) request.setStatus("NEW");
        return repository.save(request);
    }

    public BakingRequest update(Long id, BakingRequest patch) {
        BakingRequest existing = getById(id);
        if (patch.getQuantity() != 0) existing.setQuantity(patch.getQuantity());
        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        if (patch.getBatch() != null && patch.getBatch().getId() != null) {
            ProductionBatch batch = batchRepository.findById(patch.getBatch().getId())
                    .orElseThrow(() -> new RuntimeException("Партия не найдена"));
            existing.setBatch(batch);
        }
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Заявка не найдена: " + id);
        }
        repository.deleteById(id);
    }
}