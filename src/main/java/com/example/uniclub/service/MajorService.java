package com.example.uniclub.service;

import com.example.uniclub.entity.Major;
import java.util.List;

public interface MajorService {

    List<Major> getAll();

    Major getById(Long id);

    Major getByMajorCode(String code);

    Major create(Major major);

    Major update(Long id, Major updatedMajor);

    void delete(Long id);
}
