package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.StaffPerformanceRequest;
import com.example.uniclub.dto.response.StaffPerformanceMonthlySummaryResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.EventStaffStateEnum;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.EventStaffRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.StaffPerformanceRepository;
import com.example.uniclub.service.ActivityEngineService;
import com.example.uniclub.service.StaffPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffPerformanceServiceImpl implements StaffPerformanceService {

    private final MembershipRepository membershipRepo;
    private final EventRepository eventRepo;
    private final EventStaffRepository eventStaffRepo;
    private final StaffPerformanceRepository staffPerformanceRepo;
    private final ActivityEngineService activityEngineService;

    // ============================================================================
    // 1) CREATE STAFF EVALUATION
    // ============================================================================
    @Override
    @Transactional
    public StaffPerformance createStaffPerformance(Long clubId,
                                                   StaffPerformanceRequest request,
                                                   User createdBy) {

        // 1) Evaluator must belong to club
        Membership evaluator = membershipRepo
                .findByUser_UserIdAndClub_ClubId(createdBy.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You are not a member of this club."));

        if (!(evaluator.getClubRole() == ClubRoleEnum.LEADER ||
                evaluator.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only leader or vice-leader can evaluate staff.");
        }

        // 2) Staff membership
        Membership membership = membershipRepo.findById(request.membershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Membership not found."));

        if (!membership.getClub().getClubId().equals(clubId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Membership does not belong to this club.");
        }

        // 3) Event validation
        Event event = eventRepo.findById(request.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Event not found."));

        if (!event.getHostClub().getClubId().equals(clubId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This event does not belong to the club.");
        }

        if (event.getStatus() != EventStatusEnum.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot evaluate staff until the event is COMPLETED.");
        }

        // 4) Check staff participation
        EventStaff es = eventStaffRepo
                .findByEvent_EventIdAndMembership_MembershipId(event.getEventId(), membership.getMembershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "This member did not participate as staff."));

        if (es.getState() == EventStaffStateEnum.REMOVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This member was removed from the event.");
        }

        // 5) Prevent duplicate evaluation
        if (staffPerformanceRepo.existsByMembership_MembershipIdAndEvent_EventId(
                request.membershipId(), request.eventId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This staff has already been evaluated for this event.");
        }

        // 6) Save evaluation
        StaffPerformance perf = StaffPerformance.builder()
                .eventStaff(es)
                .membership(membership)
                .event(event)
                .performance(request.performance())
                .note(request.note())
                .build();

        perf = staffPerformanceRepo.save(perf);

        // ================================================================
        // 🔥 FIX QUAN TRỌNG: Lấy ngày chốt điểm theo EventDay hoặc endDate
        // ================================================================
        LocalDate endDate;

        if (event.getDays() != null && !event.getDays().isEmpty()) {
            EventDay last = event.getDays().stream()
                    .max(Comparator.comparing(EventDay::getDate))
                    .orElseThrow();
            endDate = last.getDate();
        } else {
            endDate = event.getEndDate();  // fallback
        }

        // Trigger monthly activity calculation
        activityEngineService.recalculateForMembership(
                membership.getMembershipId(),
                endDate.getYear(),
                endDate.getMonthValue()
        );

        return perf;
    }

    // ============================================================================
    // 2) LIST EVALUATIONS BY EVENT
    // ============================================================================

    @Override
    public List<StaffPerformance> getEvaluationsByEvent(Long eventId) {
        return staffPerformanceRepo.findByEvent_EventId(eventId);
    }


    // ============================================================================
    // 3) MONTHLY SUMMARY FOR CLUB
    // ============================================================================

    @Override
    public StaffPerformanceMonthlySummaryResponse getClubStaffMonthlySummary(
            Long clubId, int year, int month) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<StaffPerformance> list =
                staffPerformanceRepo.findByMembership_Club_ClubIdAndEvent_StartDateGreaterThanEqualAndEvent_EndDateLessThanEqual(
                        clubId,
                        start,
                        end
                );

        long excellent = list.stream().filter(p -> p.getPerformance().isExcellent()).count();
        long good = list.stream().filter(p -> p.getPerformance().isGood()).count();
        long average = list.stream().filter(p -> p.getPerformance().isAverage()).count();
        long poor = list.stream().filter(p -> p.getPerformance().isPoor()).count();

        return StaffPerformanceMonthlySummaryResponse.from(
                clubId,
                year,
                month,
                excellent,
                good,
                average,
                poor
        );
    }

}
