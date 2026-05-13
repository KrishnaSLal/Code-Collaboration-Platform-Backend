package com.app.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String mobileNumber;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'USER'")
    @Builder.Default
    private String role = "USER";

    private String resetOtp;

    private LocalDateTime resetOtpExpiry;

    private Boolean resetOtpVerified;
}
