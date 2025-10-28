package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.BulkAttendanceRequest;
import com.example.uniclub.dto.request.ClubAttendanceSessionRequest;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.AttendanceStatusEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubAttendanceServiceImpl implements ClubAttendanceService {

    private final ClubRepository clubRepo;
    private final MembershipRepository membershipRepo;
    private final ClubAttendanceSessionRepository sessionRepo;
    private final ClubAttendanceRecordRepository recordRepo;

    // =========================================================
    // 1. Get today's attendance (auto-create session if missing)
    // =========================================================
    @Override
    public Map<String, Object> getTodayAttendance(Long clubId) {
        LocalDate today = LocalDate.now();
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        ClubAttendanceSession session = sessionRepo.findByClub_ClubIdAndDate(clubId, today)
                .orElseGet(() -> sessionRepo.save(ClubAttendanceSession.builder()
                        .club(club)
                        .date(today)
                        .createdAt(LocalDateTime.now())
                        .isLocked(false)
                        .build()));

        List<ClubAttendanceRecord> records = recordRepo.findBySession_Id(session.getId());
        List<Map<String, Object>> list = new ArrayList<>();

        for (Membership m : membershipRepo.findByClub_ClubIdAndState(clubId, MembershipStateEnum.ACTIVE)) {
            ClubAttendanceRecord record = records.stream()
                    .filter(r -> r.getMembership().getMembershipId().equals(m.getMembershipId()))
                    .findFirst().orElse(null);

            Map<String, Object> map = new HashMap<>();
            map.put("memberId", m.getMembershipId());
            map.put("studentCode", m.getUser().getStudentCode());
            map.put("fullName", m.getUser().getFullName());
            map.put("status", record != null ? record.getStatus() : AttendanceStatusEnum.ABSENT);
            map.put("note", record != null ? record.getNote() : null);
            list.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("date", session.getDate());
        response.put("isLocked", session.isLocked());
        response.put("records", list);
        return response;
    }

    // =========================================================
    // 2. Get club attendance history by date
    // =========================================================
    @Override
    public Map<String, Object> getAttendanceHistory(Long clubId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        ClubAttendanceSession session = sessionRepo.findByClub_ClubIdAndDate(clubId, date)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No attendance data found for " + dateStr));

        List<ClubAttendanceRecord> records = recordRepo.findBySession_Id(session.getId());
        List<Map<String, Object>> list = new ArrayList<>();

        for (ClubAttendanceRecord r : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("memberId", r.getMembership().getMembershipId());
            map.put("fullName", r.getMembership().getUser().getFullName());
            map.put("studentCode", r.getMembership().getUser().getStudentCode());
            map.put("status", r.getStatus());
            map.put("note", r.getNote());
            list.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("date", session.getDate());
        response.put("isLocked", session.isLocked());
        response.put("records", list);
        return response;
    }

    // =========================================================
    // 3. Mark attendance for one member
    // =========================================================
    @Override
    @Transactional
    public void markAttendance(Long sessionId, Long membershipId, AttendanceStatusEnum status, String note) {
        ClubAttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getDate().isBefore(LocalDate.now())) {
            session.setLocked(true);
            sessionRepo.save(session);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Session locked after midnight.");
        }

        if (session.isLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Session already locked.");
        }

        ClubAttendanceRecord record = recordRepo
                .findBySession_IdAndMembership_MembershipId(sessionId, membershipId)
                .orElse(ClubAttendanceRecord.builder()
                        .session(session)
                        .membership(membershipRepo.getReferenceById(membershipId))
                        .build());

        record.setStatus(status);
        record.setNote(note);
        record.setUpdatedAt(LocalDateTime.now());
        recordRepo.save(record);
    }

    // =========================================================
    // 4. Mark all members with the same status
    // =========================================================
    @Override
    @Transactional
    public void markAll(Long sessionId, AttendanceStatusEnum status) {
        ClubAttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getDate().isBefore(LocalDate.now())) {
            session.setLocked(true);
            sessionRepo.save(session);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Session locked after midnight.");
        }

        List<Membership> members = membershipRepo.findByClub_ClubIdAndState(
                session.getClub().getClubId(), MembershipStateEnum.ACTIVE);

        for (Membership m : members) {
            ClubAttendanceRecord record = recordRepo
                    .findBySession_IdAndMembership_MembershipId(sessionId, m.getMembershipId())
                    .orElse(ClubAttendanceRecord.builder()
                            .session(session)
                            .membership(m)
                            .build());
            record.setStatus(status);
            record.setUpdatedAt(LocalDateTime.now());
            recordRepo.save(record);
        }
    }

    // =========================================================
    // 4b. ✅ Mark multiple members at once (bulk update)
    // =========================================================
    @Override
    @Transactional
    public Map<String, Object> markBulk(Long sessionId, BulkAttendanceRequest request, CustomUserDetails user) {
        ClubAttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Session not found."));

        // 1️⃣ Auto-lock after midnight
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(session.getDate().atTime(0, 0).plusDays(1))) {
            session.setLocked(true);
            sessionRepo.save(session);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Session locked after midnight. Cannot modify anymore.");
        }

        // 2️⃣ Check permission
        String role = user.getRoleName();
        if (!"CLUB_LEADER".equals(role) && !"VICE_LEADER".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only Leader or Vice Leader can update attendance.");
        }

        // 3️⃣ Reject if session locked
        if (session.isLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Session is already locked.");
        }

        // 4️⃣ Update each record
        int updated = 0;
        for (var recordReq : request.getRecords()) {
            ClubAttendanceRecord record = recordRepo
                    .findBySession_IdAndMembership_MembershipId(sessionId, recordReq.getMembershipId())
                    .orElse(ClubAttendanceRecord.builder()
                            .session(session)
                            .membership(membershipRepo.getReferenceById(recordReq.getMembershipId()))
                            .build());

            record.setStatus(AttendanceStatusEnum.valueOf(recordReq.getStatus()));
            record.setNote(recordReq.getNote());
            record.setUpdatedAt(LocalDateTime.now());
            recordRepo.save(record);
            updated++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Attendance updated successfully");
        result.put("updatedCount", updated);
        result.put("sessionId", session.getId());
        result.put("date", session.getDate());
        return result;
    }

    // =========================================================
    // 5. Auto-lock sessions at midnight (scheduled)
    // =========================================================
    @Scheduled(cron = "0 0 0 * * *")
    public void lockYesterdaySessions() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<ClubAttendanceSession> sessions = sessionRepo.findByDateAndIsLockedFalse(yesterday);
        sessions.forEach(s -> s.setLocked(true));
        sessionRepo.saveAll(sessions);
    }

    // =========================================================
    // 6. Get member attendance history
    // =========================================================
    @Override
    public Map<String, Object> getMemberAttendanceHistory(Long membershipId) {
        List<ClubAttendanceRecord> records = recordRepo.findByMembership_MembershipId(membershipId);
        if (records.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No attendance history found for this member");
        }

        List<Map<String, Object>> history = new ArrayList<>();
        for (ClubAttendanceRecord r : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", r.getSession().getDate());
            map.put("clubName", r.getSession().getClub().getName());
            map.put("status", r.getStatus());
            map.put("note", r.getNote());
            history.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("membershipId", membershipId);
        response.put("clubName", records.get(0).getSession().getClub().getName());
        response.put("attendanceHistory", history);
        return response;
    }

    // =========================================================
    // 7. University-wide attendance overview
    // =========================================================
    @Override
    public Map<String, Object> getUniversityAttendanceOverview(String dateStr) {
        LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : LocalDate.now();
        List<ClubAttendanceSession> sessions = sessionRepo.findByDateAndIsLockedTrue(date);

        List<Map<String, Object>> clubStats = new ArrayList<>();
        for (ClubAttendanceSession s : sessions) {
            List<ClubAttendanceRecord> records = recordRepo.findBySession_Id(s.getId());
            long present = records.stream()
                    .filter(r -> r.getStatus() == AttendanceStatusEnum.PRESENT)
                    .count();
            long total = records.size();
            double rate = total == 0 ? 0 : (double) present / total;

            Map<String, Object> map = new HashMap<>();
            map.put("clubName", s.getClub().getName());
            map.put("totalMembers", total);
            map.put("present", present);
            map.put("absent", total - present);
            map.put("attendanceRate", rate);
            clubStats.add(map);
        }

        double avgRate = clubStats.isEmpty()
                ? 0
                : clubStats.stream()
                .mapToDouble(c -> (double) c.get("attendanceRate"))
                .average()
                .orElse(0);

        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("clubs", clubStats);
        result.put("averageAttendance", avgRate);
        return result;
    }

    // =========================================================
    // 8. Create attendance session manually
    // =========================================================
    @Override
    @Transactional
    public Map<String, Object> createSession(Long clubId, ClubAttendanceSessionRequest req) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        if (sessionRepo.findByClub_ClubIdAndDate(clubId, req.getDate()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A session already exists on this date.");
        }

        if (req.getDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot create session for a past date.");
        }

        ClubAttendanceSession session = sessionRepo.save(ClubAttendanceSession.builder()
                .club(club)
                .date(req.getDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .note(req.getNote())
                .isLocked(false)
                .createdAt(LocalDateTime.now())
                .build());

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Session created successfully");
        result.put("sessionId", session.getId());
        result.put("clubId", club.getClubId());
        result.put("date", session.getDate());
        result.put("startTime", session.getStartTime());
        result.put("endTime", session.getEndTime());
        result.put("note", session.getNote());
        return result;
    }
}
