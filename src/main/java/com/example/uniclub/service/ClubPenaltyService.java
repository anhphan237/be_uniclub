package com.example.uniclub.service;

import com.example.uniclub.dto.request.CreateClubPenaltyRequest;
import com.example.uniclub.entity.ClubPenalty;
import com.example.uniclub.entity.User;

public interface ClubPenaltyService {

    ClubPenalty createPenalty(Long clubId,
                              CreateClubPenaltyRequest request,
                              User createdBy);
}
