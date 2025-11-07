package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.LocationCreateRequest;
import com.example.uniclub.dto.response.LocationResponse;
import com.example.uniclub.entity.Location;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.LocationRepository;
import com.example.uniclub.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepo;

    private LocationResponse toResp(Location l) {
        return LocationResponse.builder()
                .id(l.getLocationId())
                .name(l.getName())
                .address(l.getAddress())
                .capacity(l.getCapacity())
                .build();
    }

    @Override
    public LocationResponse create(LocationCreateRequest req) {
        if (locationRepo.existsByName(req.name()))
            throw new ApiException(HttpStatus.CONFLICT, "Location already exists.");
        Location l = Location.builder()
                .name(req.name())
                .address(req.address())
                .capacity(req.capacity())
                .build();
        return toResp(locationRepo.save(l));
    }

    @Override
    public LocationResponse get(Long id) {
        return locationRepo.findById(id)
                .map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Location not found."));
    }

    @Override
    public Page<LocationResponse> list(Pageable pageable) {
        return locationRepo.findAll(pageable).map(this::toResp);
    }

    @Override
    public void delete(Long id) {
        if (!locationRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Location not found.");
        locationRepo.deleteById(id);
    }
}
