package com.example.uniclub.dto.response;

import com.example.uniclub.entity.MajorPolicy;
import com.example.uniclub.entity.MultiplierPolicy;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPolicyResponse {
    private Long id;
    private String name;         // map từ policyName
    private String description;
    private String type;         // MAJOR / MULTIPLIER
    private boolean active;

    // ===============================================
    // 🧩 Mapping cho MajorPolicy
    // ===============================================
    public static AdminPolicyResponse fromMajor(MajorPolicy mp) {
        return AdminPolicyResponse.builder()
                .id(mp.getId())
                .name(mp.getPolicyName())    // ✅ đồng bộ field
                .description(mp.getDescription())
                .type("MAJOR")
                .active(mp.isActive())
                .build();
    }

    // ===============================================
    // 🧩 Mapping cho MultiplierPolicy
    // ===============================================
    public static AdminPolicyResponse fromMultiplier(MultiplierPolicy mp) {
        return AdminPolicyResponse.builder()
                .id(mp.getId())
                .name(mp.getPolicyName())
                .description(mp.getPolicyDescription())
                .type("MULTIPLIER")
                .active(mp.isActive())
                .build();
    }

    // ===============================================
    // 🧩 Convert sang Entity (khi admin tạo/sửa)
    // ===============================================
    public MajorPolicy toMajorEntity() {
        return MajorPolicy.builder()
                .id(id)
                .policyName(name)
                .description(description)
                .active(active)
                .build();
    }

    public MultiplierPolicy toMultiplierEntity() {
        return MultiplierPolicy.builder()
                .id(id)
                .policyName(name)
                .policyDescription(description)
                .active(active)
                .build();
    }
}
