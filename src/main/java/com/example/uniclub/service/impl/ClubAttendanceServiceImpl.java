package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.enums.AttendanceStatusEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
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

    // =========================
    // 1. Lấy điểm danh hôm nay
    // =========================
    @Override
    public Map<String, Object> getTodayAttendance(Long clubId) {
        LocalDate today = LocalDate.now();
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy CLB"));

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

            list.add(Map.of(
                    "memberId", m.getMembershipId(),
                    "studentCode", m.getUser().getStudentCode(),
                    "fullName", m.getUser().getFullName(),
                    "status", record != null ? record.getStatus() : AttendanceStatusEnum.ABSENT,
                    "note", record != null ? record.getNote() : null
            ));
        }

        return Map.of(
                "sessionId", session.getId(),
                "date", session.getDate(),
                "isLocked", session.isLocked(),
                "records", list
        );
    }

    // =========================
    // 2. Lịch sử điểm danh CLB
    // =========================
    @Override
    public Map<String, Object> getAttendanceHistory(Long clubId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        ClubAttendanceSession session = sessionRepo.findByClub_ClubIdAndDate(clubId, date)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không có dữ liệu điểm danh ngày " + dateStr));

        List<ClubAttendanceRecord> records = recordRepo.findBySession_Id(session.getId());
        List<Map<String, Object>> list = new ArrayList<>();

        for (ClubAttendanceRecord r : records) {
            list.add(Map.of(
                    "memberId", r.getMembership().getMembershipId(),
                    "fullName", r.getMembership().getUser().getFullName(),
                    "studentCode", r.getMembership().getUser().getStudentCode(),
                    "status", r.getStatus(),
                    "note", r.getNote()
            ));
        }

        return Map.of(
                "sessionId", session.getId(),
                "date", session.getDate(),
                "isLocked", session.isLocked(),
                "records", list
        );
    }

    // =========================
    // 3. Điểm danh từng người
    // =========================
    @Override
    @Transactional
    public void markAttendance(Long sessionId, Long membershipId, AttendanceStatusEnum status, String note) {
        ClubAttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi sinh hoạt"));

        if (session.getDate().isBefore(LocalDate.now())) {
            session.setLocked(true);
            sessionRepo.save(session);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Đã qua 12h đêm, không thể chỉnh sửa kết quả điểm danh.");
        }

        if (session.isLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Buổi sinh hoạt đã bị khóa.");
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

    // =========================
    // 4. Điểm danh hàng loạt
    // =========================
    @Override
    @Transactional
    public void markAll(Long sessionId, AttendanceStatusEnum status) {
        ClubAttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi sinh hoạt"));

        if (session.getDate().isBefore(LocalDate.now())) {
            session.setLocked(true);
            sessionRepo.save(session);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Đã qua 12h đêm, không thể chỉnh sửa kết quả điểm danh.");
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

    // =========================
    // 5. 🔒 Tự động khóa qua 12h đêm
    // =========================
    @Scheduled(cron = "0 0 0 * * *")
    public void lockYesterdaySessions() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<ClubAttendanceSession> sessions = sessionRepo.findByDateAndIsLockedFalse(yesterday);
        sessions.forEach(s -> s.setLocked(true));
        sessionRepo.saveAll(sessions);
    }

    // =========================
    // 6. 🆕 Lịch sử điểm danh cá nhân
    // =========================
    @Override
    public Map<String, Object> getMemberAttendanceHistory(Long membershipId) {
        List<ClubAttendanceRecord> records = recordRepo.findByMembership_MembershipId(membershipId);
        if (records.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không có lịch sử điểm danh cho thành viên này");
        }

        List<Map<String, Object>> history = records.stream()
                .sorted(Comparator.comparing(r -> r.getSession().getDate()))
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", r.getSession().getDate());
                    map.put("clubName", r.getSession().getClub().getName());
                    map.put("status", r.getStatus());
                    map.put("note", r.getNote());
                    return map;
                })
                .collect(Collectors.toList());


        return Map.of(
                "membershipId", membershipId,
                "clubName", records.get(0).getSession().getClub().getName(),
                "attendanceHistory", history
        );
    }

    // =========================
    // 7. 🆕 Tổng quan điểm danh toàn trường
    // =========================
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

            clubStats.add(Map.of(
                    "clubName", s.getClub().getName(),
                    "totalMembers", total,
                    "present", present,
                    "absent", total - present,
                    "attendanceRate", rate
            ));
        }

        double avgRate = clubStats.isEmpty()
                ? 0
                : clubStats.stream()
                .mapToDouble(c -> (double) c.get("attendanceRate"))
                .average()
                .orElse(0);

        return Map.of(
                "date", date,
                "clubs", clubStats,
                "averageAttendance", avgRate
        );
    }
}
