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
    private final MajorPolicyRepository majorPolicyRepo;
    private final MajorRepository majorRepo;
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
                .majorPolicyName(club.getMajorPolicy() != null ? club.getMajorPolicy().getPolicyName() : null)
                .majorName(club.getMajor() != null ? club.getMajor().getName() : null)
                .build();
    }

    @Override
    public ClubResponse create(ClubCreateRequest req) {
        if (clubRepo.existsByName(req.name()))
            throw new ApiException(HttpStatus.CONFLICT, "Club name already exists");

        MajorPolicy policy = majorPolicyRepo.findById(req.majorPolicyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major policy not found"));
        Major major = majorRepo.findById(req.majorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Major not found"));

        Club club = Club.builder()
                .name(req.name())
                .description(req.description())
                .major(major)
                .majorPolicy(policy)
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

    @Override
    public void createFromOnlineApplication(ClubApplication app) {
        Club club = Club.builder()
                .name(app.getClubName())
                .description(app.getDescription())
                .createdBy(app.getProposer())
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
    }

    @Override
    public void createFromOfflineApplication(ClubApplication app, ClubApplicationOfflineRequest req) {
        Club club = Club.builder()
                .name(app.getClubName())
                .description(app.getDescription())
                .createdBy(app.getReviewedBy())
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
    }

    /** 🔹 Auto-create Leader & Vice-Leader when club is created */
    private void createDefaultAccounts(Club club) {
        Role leaderSystemRole = roleRepo.findByRoleName("CLUB_LEADER")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role CLUB_LEADER not found"));

        String slug = club.getName().trim().toLowerCase().replaceAll("\\s+", "");

        // 🔸 Chủ nhiệm (Leader)
        User leader = User.builder()
                .email("club_leader_" + slug + "@uniclub.edu.vn")
                .passwordHash(passwordEncoder.encode("123"))
                .fullName("Leader of " + club.getName())
                .studentCode("LEAD_" + slug)
                .status(UserStatusEnum.ACTIVE.name())
                .role(leaderSystemRole)
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
                .state(MembershipStateEnum.APPROVED)
                .staff(true)
                .build());

        // 🔸 Phó chủ nhiệm (Vice Leader)
        User vice = User.builder()
                .email("club_vice_" + slug + "@uniclub.edu.vn")
                .passwordHash(passwordEncoder.encode("123"))
                .fullName("Vice Leader of " + club.getName())
                .studentCode("VICE_" + slug)
                .status(UserStatusEnum.ACTIVE.name())
                .role(leaderSystemRole)
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
                .state(MembershipStateEnum.APPROVED)
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
