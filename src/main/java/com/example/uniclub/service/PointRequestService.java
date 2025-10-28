package com.example.uniclub.service;

import com.example.uniclub.dto.request.PointRequestCreateRequest;
import com.example.uniclub.dto.response.PointRequestResponse;
import com.example.uniclub.security.CustomUserDetails;

import java.util.List;

public interface PointRequestService {
    PointRequestResponse createRequest(CustomUserDetails principal, PointRequestCreateRequest req);
    List<PointRequestResponse> getPendingRequests();
    String reviewRequest(Long requestId, boolean approve, String note);
}
