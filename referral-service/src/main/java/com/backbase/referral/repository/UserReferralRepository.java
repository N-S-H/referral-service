package com.backbase.referral.repository;

import com.backbase.referral.entity.UserReferral;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserReferralRepository extends JpaRepository<UserReferral, String> {
    List<UserReferral> findAllByUserId(String userId);

    Optional<UserReferral> findByReferralCode(@NotNull String referralCode);

    boolean existsByReferralCode(String referralCode);

    List<UserReferral> findAllByReferralStatusAndCreatedDateTimeBefore(String active, LocalDateTime localDateTime);
}
