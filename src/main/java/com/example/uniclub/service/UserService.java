package com.example.uniclub.service;

import com.example.uniclub.entity.User;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public List<User> findAll() {
        return repo.findAll();
    }

    public User findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public User create(User u) {
        if (repo.existsByEmail(u.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        return repo.save(u);
    }

    public User update(Long id, User req) {
        User u = findById(id);
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        return repo.save(u);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
