package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityAttendanceResponse {
    private long totalAttendances;
    private List<ClubAttendanceRankingResponse> clubRankings;
}
