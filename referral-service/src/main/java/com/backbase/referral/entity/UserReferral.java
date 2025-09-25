package com.backbase.referral.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity for table user_referral
 */
@Entity
@Table(name = "user_referral",
        indexes = {
                @Index(name = "idx_referral_referral_code", columnList = "referral_code"),
                @Index(name = "idx_referral_user_id", columnList = "user_id"),
                @Index(name = "idx_referral_created_date_time", columnList = "created_date_time"),
                @Index(name = "idx_referral_status", columnList = "referral_status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReferral {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", length = 150, nullable = false)
    private String userId;

    @Column(name = "referral_code", length = 255, nullable = false, unique = true)
    private String referralCode;

    @Column(name = "referral_qr_string", length = 3072)
    private String referralQrString;

    @Column(name = "created_date_time", nullable = false)
    private LocalDateTime createdDateTime;

    @Column(name = "expiry_date_time")
    private LocalDateTime expiryDateTime;

    @Column(name = "referral_status", length = 50, nullable = false)
    private String referralStatus;

    // Optional bi-directional mapping (not required). Helpful if you want to navigate submissions from referral.
    @OneToMany(mappedBy = "referral", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserReferralSubmission> submissions = new ArrayList<>();


}
