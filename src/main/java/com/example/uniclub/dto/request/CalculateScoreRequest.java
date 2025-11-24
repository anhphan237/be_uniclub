package com.example.uniclub.dto.request;

import lombok.Data;

@Data
public class CalculateScoreRequest {
    private int attendanceBaseScore;
    private int staffBaseScore;
}
