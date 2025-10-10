package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.LoginRequest;
import com.example.uniclub.dto.request.RegisterRequest;
import com.example.uniclub.dto.response.AuthResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.UserStatusEnum;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WalletRepository walletRepository;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ Đăng nhập
    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        var cud = (CustomUserDetails) auth.getPrincipal();
        var user = cud.getUser();

        String token = jwtUtil.generateToken(user.getEmail());
        String roleName = user.getRole().getRoleName();

        Long clubId = null;
        List<Long> clubIds = null;
        Boolean isClubStaff = false; // ✅ flag staff trong CLB

        if ("CLUB_LEADER".equals(roleName)) {
            clubId = clubRepository.findByLeader_UserId(user.getUserId())
                    .map(Club::getClubId)
                    .orElse(null);
        } else if ("MEMBER".equals(roleName)) {
            var memberships = membershipRepository.findAllByUser_UserId(user.getUserId());
            clubIds = memberships.stream()
                    .map(m -> m.getClub().getClubId())
                    .toList();
            // ✅ kiểm tra nếu có ít nhất 1 membership là staff
            isClubStaff = memberships.stream().anyMatch(Membership::isStaff);
        }

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(roleName)
                .clubId(clubId)
                .clubIds(clubIds)
                .staff(isClubStaff) // ✅ trả về true nếu là staff hỗ trợ CLB
                .build();
    }

    // ✅ Đăng ký
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }

        if (userRepository.existsByStudentCode(req.studentCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "Student code already exists");
        }

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .role(roleRepository.findByRoleName(req.roleName())
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid role name")))
                .status(UserStatusEnum.ACTIVE.name())
                .studentCode(req.studentCode())
                .majorName(req.majorName())
                .build();

        user = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.USER)
                .user(user)
                .balancePoints(0)
                .build();
        walletRepository.save(wallet);

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().getRoleName())
                .staff(false) // ✅ mặc định là member thường
                .build();
    }
}
