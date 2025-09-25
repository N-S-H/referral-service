package com.backbase.referral.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Entity for table user_referral_submission
 */
@Entity
@Table(name = "user_referral_submission",
        indexes = {
                @Index(name = "idx_submission_submitted_user_id", columnList = "submitted_user_id"),
                @Index(name = "idx_submission_submitted_referral_code", columnList = "submitted_referral_code"),
                @Index(name = "idx_submission_submission_date_time", columnList = "submission_date_time"),
                @Index(name = "idx_submission_submission_status", columnList = "submission_status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReferralSubmission {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "submitted_user_id", length = 150, nullable = false)
    private String submittedUserId;

    @Column(name = "submitted_referral_code", length = 255, nullable = false)
    private String submittedReferralCode;

    // Many submissions can reference a single referral
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "referral_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_user_referral_submission_referral_id"))
    @OnDelete(action = OnDeleteAction.CASCADE) // reflect onDelete="CASCADE"
    private UserReferral referral;

    @Column(name = "submission_source", length = 1000)
    private String submissionSource;

    @Column(name = "submission_date_time", nullable = false)
    private LocalDateTime submissionDateTime;

    @Column(name = "expiry_date_time")
    private LocalDateTime expiryDateTime;

    @Column(name = "submission_status", length = 50, nullable = false)
    private String submissionStatus;
}
