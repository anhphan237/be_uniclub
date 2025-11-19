package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.ClubCreateRequest;
import com.example.uniclub.dto.request.ClubRenameRequest;
import com.example.uniclub.dto.request.ClubUpdateRequest;
import com.example.uniclub.dto.response.ClubResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ClubServiceImpl implements ClubService {

    private final ClubRepository clubRepo;
    private final WalletRepository walletRepo;
    private final MajorRepository majorRepo;
    private final MembershipRepository membershipRepo;
    private final UserRepository userRepo;

    private ClubResponse toResponse(Club club) {
        var leaderMembership = membershipRepo
                .findByClub_ClubIdAndClubRole(club.getClubId(), ClubRoleEnum.LEADER)
                .stream()
                .findFirst()
                .orElse(null);

        Long leaderId = leaderMembership != null ? leaderMembership.getUser().getUserId() : null;
        String leaderName = leaderMembership != null ? leaderMembership.getUser().getFullName() : null;

        return ClubResponse.builder()
                .id(club.getClubId())
                .name(club.getName())
                .description(club.getDescription())
                .leaderId(leaderId)
                .leaderName(leaderName)
                .majorId(club.getMajor().getId())
                .majorName(club.getMajor().getName())
                .memberCount(club.getMemberCount() != null ? club.getMemberCount().longValue() : 0L)
                .build();
    }

    // 🟢 1. Tạo CLB thủ công (Admin / Staff)
    @Override
    public ClubResponse create(ClubCreateRequest req) {
        if (clubRepo.existsByName(req.name()))
            throw new ApiException(HttpStatus.CONFLICT, "Club name already exists");

        Major major = majorRepo.findById(req.majorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major not found"));

        Club club = Club.builder()
                .name(req.name())
                .description(req.description())
                .vision(req.vision())
                .major(major)
                .createdBy(null)
                .memberCount(0)
                .activityStatus(ClubActivityStatusEnum.ACTIVE)
                .build();

        Wallet wallet = Wallet.builder()
                .club(club)
                .ownerType(WalletOwnerTypeEnum.CLUB)
                .balancePoints(0L)
                .build();

        walletRepo.save(wallet);
        club.setWallet(wallet);
        Club saved = clubRepo.save(club);
        return toResponse(saved);
    }

    // 🟣 Các hàm CRUD cơ bản
    @Override
    public ClubResponse get(Long id) {
        return clubRepo.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
    }

    @Override
    public Page<ClubResponse> list(Pageable pageable) {
        return clubRepo.findAll(pageable).map(this::toResponse);
    }

    @Override
    public void delete(Long id) {
        if (!clubRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Club not found");
        clubRepo.deleteById(id);
    }

    @Override
    public Club saveClub(Club club) {
        return clubRepo.save(club);
    }

    // 🟢 Cập nhật member_count thực tế
    @Transactional
    public void updateMemberCount(Long clubId) {
        int total = (int) membershipRepo.countByClub_ClubIdAndState(clubId, MembershipStateEnum.ACTIVE);

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        club.setMemberCount(total);
        clubRepo.saveAndFlush(club);

        System.out.println("Updated member_count for club " + clubId + " = " + total);
    }



    @Override
    public Page<ClubResponse> getAvailableForApply(Long userId, String keyword, Pageable pageable) {
        // Lấy danh sách clubId mà user đã tham gia hoặc đang chờ duyệt
        List<Long> excluded = membershipRepo.findJoinedOrPendingClubIds(
                userId,
                List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.PENDING, MembershipStateEnum.APPROVED)
        );

        if (excluded.isEmpty()) excluded = Collections.singletonList(-1L); // tránh lỗi "NOT IN ()"

        return clubRepo.findAvailableForApply(excluded, keyword, pageable)
                .map(this::toResponse);
    }
    @Override
    @Transactional
    public ClubResponse renameClub(Long clubId, ClubRenameRequest req, Long requesterId) {
        var club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        var user = userRepo.findById(requesterId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        var roleName = user.getRole().getRoleName(); // ADMIN / UNIVERSITY_STAFF / STUDENT
        boolean canRename = false;

        // ✅ 1. Kiểm tra quyền hệ thống (ADMIN hoặc UNIVERSITY_STAFF)
        if ("ADMIN".equalsIgnoreCase(roleName) || "UNIVERSITY_STAFF".equalsIgnoreCase(roleName)) {
            canRename = true;
        } else {
            // ✅ 2. Kiểm tra nếu user là LEADER của CLB
            var membership = membershipRepo.findByUser_UserIdAndClub_ClubId(requesterId, clubId)
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

            if (membership.getClubRole() == ClubRoleEnum.LEADER) {
                canRename = true;
            }
        }

        if (!canRename) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only ADMIN, UNIVERSITY_STAFF or LEADER can rename this club.");
        }

        // ✅ 3. Kiểm tra trùng tên
        if (clubRepo.existsByNameIgnoreCase(req.getNewName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Club name already exists.");
        }

        // ✅ 4. Cập nhật tên + thời gian chỉnh sửa
        club.setName(req.getNewName());
        club.setUpdatedAt(LocalDateTime.now()); // Nếu Club có field updatedAt
        clubRepo.save(club);

        // ✅ 5. Trả về dữ liệu sau khi cập nhật
        return ClubResponse.fromEntity(club);
    }

    @Override
    @Transactional
    public ClubResponse updateClub(Long clubId, ClubUpdateRequest req, Long requesterId) {

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        User requester = userRepo.findById(requesterId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String roleName = requester.getRole().getRoleName();

        boolean isAdmin = roleName.equals("ADMIN");
        boolean isStaff = roleName.equals("UNIVERSITY_STAFF");

        boolean isLeader = membershipRepo.findByUser_UserIdAndClub_ClubId(requesterId, clubId)
                .map(m -> m.getClubRole() == ClubRoleEnum.LEADER)
                .orElse(false);

        // 🚨 Kiểm tra quyền
        if (!(isAdmin || isStaff || isLeader)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only ADMIN, UNIVERSITY_STAFF or CLUB_LEADER can update this club.");
        }

        // ========= 🎯 LEADER chỉ được chỉnh 3 trường =========
        if (isLeader && !(isAdmin || isStaff)) {
            if (req.getName() != null) club.setName(req.getName());
            if (req.getDescription() != null) club.setDescription(req.getDescription());
            if (req.getVision() != null) club.setVision(req.getVision());

            if (req.getMajorId() != null) {
                Major major = majorRepo.findById(req.getMajorId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major not found"));
                club.setMajor(major);
            }

            // leader không được đổi leader, multiplier, status
            club.setUpdatedAt(LocalDateTime.now());
            clubRepo.save(club);
            return ClubResponse.fromEntity(club);
        }

        // ========= 🎯 ADMIN / STAFF được chỉnh full =========
        if (req.getName() != null) club.setName(req.getName());
        if (req.getDescription() != null) club.setDescription(req.getDescription());
        if (req.getVision() != null) club.setVision(req.getVision());

        if (req.getMajorId() != null) {
            Major m = majorRepo.findById(req.getMajorId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major not found"));
            club.setMajor(m);
        }

        if (req.getClubMultiplier() != null) {
            club.setClubMultiplier(req.getClubMultiplier());
        }

        if (req.getActivityStatus() != null) {
            try {
                club.setActivityStatus(
                        ClubActivityStatusEnum.valueOf(req.getActivityStatus().toUpperCase())
                );
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid activity status");
            }
        }

        // ========= 🧑‍🤝‍🧑 Đổi Leader (Admin/Staff) =========
        if (req.getNewLeaderId() != null) {
            Membership leaderMem = membershipRepo
                    .findByUser_UserIdAndClub_ClubId(req.getNewLeaderId(), clubId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                            "User is not a member of this club"));

            if (leaderMem.getState() != MembershipStateEnum.ACTIVE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Leader must be ACTIVE member");
            }

            leaderMem.setClubRole(ClubRoleEnum.LEADER);
            membershipRepo.save(leaderMem);

            club.setLeader(leaderMem.getUser());
        }

        club.setUpdatedAt(LocalDateTime.now());
        clubRepo.save(club);

        return ClubResponse.fromEntity(club);
    }


}
