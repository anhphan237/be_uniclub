package com.example.uniclub.service;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.CardRequest;
import com.example.uniclub.dto.response.CardResponse;
import java.util.List;

public interface CardService {

    ApiResponse<CardResponse> saveOrUpdate(Long clubId, CardRequest req);

    CardResponse getByClubId(Long clubId);

    CardResponse getById(Long id);

    List<CardResponse> getAll();

    ApiResponse<String> delete(Long id);
}
