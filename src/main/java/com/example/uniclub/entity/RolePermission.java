package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
class RolePermissionId implements Serializable {
    private Long roleId;
    private Long permissionId;
}

@Entity
@Table(name = "role_permissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    @ManyToOne @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne @MapsId("permissionId")
    @JoinColumn(name = "permission_id")
    private Permission permission;
}
