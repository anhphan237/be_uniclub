package com.example.uniclub.service;

import com.example.uniclub.entity.Major;
import java.util.List;

public interface MajorService {
    List<Major> getAll();
    Major create(Major major);
    void delete(Long id);
}
