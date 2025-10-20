package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.ClubApplicationOfflineRequest;
import com.example.uniclub.dto.request.ClubCreateRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClubServiceImpl implements ClubService {

    private final ClubRepository clubRepo;
    private final WalletRepository walletRepo;
    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final PasswordEncoder passwordEncoder;

    // ✅ Convert entity → DTO (tìm tên chủ nhiệm)
    private ClubResponse toResponse(Club club) {
        String leaderName = membershipRepo
                .findByClub_ClubIdAndClubRole(club.getClubId(), ClubRoleEnum.LEADER)
                .stream()
                .findFirst()
                .map(m -> m.getUser().getFullName())
                .orElse(null);

        return ClubResponse.builder()
                .id(club.getClubId())
                .name(club.getName())
                .description(club.getDescription())
                .leaderName(leaderName)
                .majorName(club.getMajorName())  // ✅ chỉ lưu tên ngành
                .build();
    }

    // 🟢 1. Tạo CLB mới thủ công (Admin / Staff)
    @Override
    public ClubResponse create(ClubCreateRequest req) {
        if (clubRepo.existsByName(req.name()))
            throw new ApiException(HttpStatus.CONFLICT, "Club name already exists");

        Club club = Club.builder()
                .name(req.name())
                .description(req.description())
                .majorName(req.majorName()) // ✅ chỉ cần String
                .vision(req.vision())
                .createdBy(null) // có thể set staff hiện tại nếu cần
                .build();

        Wallet clubWallet = Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.CLUB)
                .balancePoints(0)
                .club(club)
                .build();

        walletRepo.save(clubWallet);
        club.setWallet(clubWallet);

        Club saved = clubRepo.save(club);
        createDefaultAccounts(saved);
        return toResponse(saved);
    }

    // 🟢 2. Tạo CLB từ đơn Online
    @Override
    public void createFromOnlineApplication(ClubApplication app) {
        Club club = Club.builder()
                .name(app.getClubName())
                .description(app.getDescription())
                .majorName(app.getMajor().getName())// ✅ String
                .vision(app.getVision())
                .createdBy(app.getReviewedBy())
                .build();

        Wallet wallet = Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.CLUB)
                .balancePoints(0)
                .club(club)
                .build();

        walletRepo.save(wallet);
        club.setWallet(wallet);
        Club saved = clubRepo.save(club);
        createDefaultAccounts(saved);
    }

    // 🟢 3. Tạo CLB từ đơn Offline (Staff nhập)
    @Override
    public void createFromOfflineApplication(ClubApplication app, ClubApplicationOfflineRequest req) {
        Club club = Club.builder()
                .name(app.getClubName())
                .description(app.getDescription())
                .majorName(app.getMajor().getName())
                .vision(app.getVision())
                .createdBy(app.getReviewedBy())
                .build();

        Wallet wallet = Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.CLUB)
                .balancePoints(0)
                .club(club)
                .build();

        walletRepo.save(wallet);
        club.setWallet(wallet);
        Club saved = clubRepo.save(club);
        createDefaultAccounts(saved);
    }

    // 🔹 Tự động tạo tài khoản Leader & Vice Leader
    private void createDefaultAccounts(Club club) {
        Role leaderRole = roleRepo.findByRoleName("CLUB_LEADER")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role CLUB_LEADER not found"));

        String slug = club.getName().trim().toLowerCase().replaceAll("\\s+", "");

        // Chủ nhiệm
        User leader = User.builder()
                .email("leader_" + slug + "@uniclub.edu.vn")
                .passwordHash(passwordEncoder.encode("123"))
                .fullName("Leader of " + club.getName())
                .studentCode("LEAD_" + slug.toUpperCase())
                .status(UserStatusEnum.ACTIVE.name())
                .role(leaderRole)
                .build();

        Wallet leaderWallet = Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.USER)
                .user(leader)
                .balancePoints(0)
                .build();

        leader.setWallet(leaderWallet);
        userRepo.save(leader);
        walletRepo.save(leaderWallet);

        membershipRepo.save(Membership.builder()
                .club(club)
                .user(leader)
                .clubRole(ClubRoleEnum.LEADER)
                .state(MembershipStateEnum.ACTIVE)
                .staff(true)
                .build());

        // Phó chủ nhiệm
        User vice = User.builder()
                .email("vice_" + slug + "@uniclub.edu.vn")
                .passwordHash(passwordEncoder.encode("123"))
                .fullName("Vice Leader of " + club.getName())
                .studentCode("VICE_" + slug.toUpperCase())
                .status(UserStatusEnum.ACTIVE.name())
                .role(leaderRole)
                .build();

        Wallet viceWallet = Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.USER)
                .user(vice)
                .balancePoints(0)
                .build();

        vice.setWallet(viceWallet);
        userRepo.save(vice);
        walletRepo.save(viceWallet);

        membershipRepo.save(Membership.builder()
                .club(club)
                .user(vice)
                .clubRole(ClubRoleEnum.VICE_LEADER)
                .state(MembershipStateEnum.ACTIVE)
                .staff(true)
                .build());
    }

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
}
