package com.example.student.service;

import com.example.student.model.User;
import com.example.student.repository.BakingRequestRepository;
import com.example.student.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository repository;
    private final BakingRequestRepository requestRepository;

    public UserService(UserRepository repository,
                       BakingRequestRepository requestRepository) {
        this.repository = repository;
        this.requestRepository = requestRepository;
    }

    public List<User> getAll() { return repository.findAll(); }

    public User getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + id));
    }

    public User create(User user) {
        user.setId(null);
        return repository.save(user);
    }

    public User update(Long id, User patch) {
        User existing = getById(id);
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Пользователь не найден: " + id);
        }
        boolean hasRequests = requestRepository.findAll().stream()
                .anyMatch(r -> r.getUser() != null && r.getUser().getId().equals(id));
        if (hasRequests) {
            throw new RuntimeException(
                    "Нельзя удалить пользователя id=" + id + ": у него есть заявки на выпечку");
        }
        repository.deleteById(id);
    }
}