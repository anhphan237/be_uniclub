package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.UserCreateRequest;
import com.example.uniclub.dto.request.UserUpdateRequest;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserCreateRequest req){
        return ResponseEntity.ok(ApiResponse.ok(userService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody UserUpdateRequest req){
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.ok(userService.get(id)));
    }

    @GetMapping
    public ResponseEntity<?> list(Pageable pageable){
        return ResponseEntity.ok(userService.list(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }
}
