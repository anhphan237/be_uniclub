package com.example.uniclub.controller;

import com.example.uniclub.entity.User;
import com.example.uniclub.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<User> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public User one(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@RequestBody User req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
