package com.example.uniclub.dto.request;

import lombok.Data;

@Data
public class CreateClubAccountsRequest {
    private String clubCode;       // viết tắt CLB (VD: ftit)
    private String leaderName;
    private String viceLeaderName;
}
