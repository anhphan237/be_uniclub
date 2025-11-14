package com.example.uniclub.service;

import com.example.uniclub.dto.request.LocationCreateRequest;
import com.example.uniclub.dto.request.LocationUpdateRequest;
import com.example.uniclub.dto.response.LocationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LocationService {


    LocationResponse create(LocationCreateRequest req);


    LocationResponse get(Long id);


    Page<LocationResponse> list(Pageable pageable);

    LocationResponse update(Long id, LocationUpdateRequest req);

    void delete(Long id);
}
