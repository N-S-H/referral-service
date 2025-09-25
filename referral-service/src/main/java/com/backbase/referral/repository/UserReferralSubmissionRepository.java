package com.backbase.referral.repository;

import com.backbase.referral.entity.UserReferral;
import com.backbase.referral.entity.UserReferralSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserReferralSubmissionRepository extends JpaRepository<UserReferralSubmission, String> {

    List<UserReferralSubmission> findAllByReferral(UserReferral referral);

    List<UserReferralSubmission> findAllBySubmittedUserId(String submittedUserId);

    List<UserReferralSubmission> findAllBySubmissionStatusAndSubmissionDateTimeBefore(String active, LocalDateTime localDateTime);
}
