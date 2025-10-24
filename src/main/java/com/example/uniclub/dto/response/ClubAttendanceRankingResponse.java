package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ClubAttendanceRankingResponse {
    private int rank;
    private long clubId;
    private String clubName;
    private long attendanceCount;
}
