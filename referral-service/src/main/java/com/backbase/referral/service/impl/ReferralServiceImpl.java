package com.backbase.referral.service.impl;

import com.backbase.referral.config.AppConfiguration;
import com.backbase.referral.entity.UserReferral;
import com.backbase.referral.entity.UserReferralSubmission;
import com.backbase.referral.exceptions.ReferralCodeSubmissionException;
import com.backbase.referral.mapper.ReferralEntityMapper;
import com.backbase.referral.repository.UserReferralDAO;
import com.backbase.referral.repository.UserReferralRepository;
import com.backbase.referral.repository.UserReferralSubmissionDAO;
import com.backbase.referral.repository.UserReferralSubmissionRepository;
import com.backbase.referral.service.ReferralService;
import com.backbase.referral.service.api.v1.model.ReferralCodeSubmissionRequestBody;
import com.backbase.referral.service.api.v1.model.ReferralGenerationResponseBody;
import com.backbase.referral.service.api.v1.model.ReferralSearchResponseBody;
import com.backbase.referral.service.api.v1.model.ReferralSearchResponseEntry;
import com.backbase.referral.service.api.v1.model.ReferralSubmissionSearchResponseBody;
import com.backbase.referral.service.api.v1.model.ReferralSubmissionSearchResponseEntry;
import com.backbase.referral.utils.Constants;
import com.backbase.referral.utils.ErrorCodes;
import com.backbase.referral.utils.ReferralUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    private final AppConfiguration appConfiguration;
    private final ReferralEntityMapper referralEntityMapper;
    private final UserReferralRepository userReferralRepository;
    private final UserReferralSubmissionRepository userReferralSubmissionRepository;
    private final UserReferralDAO userReferralDAO;
    private final UserReferralSubmissionDAO userReferralSubmissionDAO;
    private final ReferralUtils referralUtils;

    @Override
    public ReferralGenerationResponseBody generateReferralCode(String userId) {
        List<UserReferral> userReferralList = userReferralRepository.findAllByUserId(userId);
        int totalReferralCount = Optional.ofNullable(userReferralList).orElse(Collections.emptyList()).size();
        List<UserReferral> activeReferrals = Optional.ofNullable(userReferralList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(userReferral -> userReferral.getReferralStatus().equals(Constants.ACTIVE))
                .filter(userReferral -> !appConfiguration.isReferralExpiryEnabled() ||
                        userReferral.getCreatedDateTime().plusHours(appConfiguration.getReferralExpiryInHours()).isBefore(LocalDateTime.now()))
                .toList();
        int activeReferralCount = activeReferrals.size();
        int expiredReferralCount = totalReferralCount - activeReferralCount;
        if(activeReferralCount > 0) {
            return referralEntityMapper.toReferralGenerationResponseBody(activeReferrals.getFirst());
        } else if((expiredReferralCount > 0 && appConfiguration.isReGenerationUponExpiryEnabled()) || (totalReferralCount == 0)) {
            String referralCode = referralUtils.generateReferralCode(appConfiguration.getReferralCodePatternType(),
                    appConfiguration.getReferralCodeRegexPattern(), appConfiguration.getReferralCodePrefix(),
                    appConfiguration.getReferralCodeLengthWithoutPrefix(), appConfiguration.getReferralCodeGenerationMaxAttempts());
            String referralQrString = Optional.of(appConfiguration.isReferralCodeQREnabled())
                    .filter(enabled -> enabled)
                    .map(enabled -> referralUtils.generateReferralQRString(referralCode))
                    .orElse(null);
            UserReferral newUserReferral = referralEntityMapper.toUserReferralEntity(UUID.randomUUID().toString(), userId, referralCode, referralQrString);
            UserReferral savedReferral = userReferralRepository.save(newUserReferral);
            return referralEntityMapper.toReferralGenerationResponseBody(savedReferral);
        } else {
            return referralEntityMapper.toReferralGenerationResponseBody(userReferralList.getFirst());
        }
    }

    @Override
    public Void referralCodeSubmission(String userId, ReferralCodeSubmissionRequestBody referralCodeSubmissionRequestBody) {
        List<UserReferralSubmission> userReferralSubmissionList
                = userReferralSubmissionRepository.findAllBySubmittedUserId(userId);
        List<UserReferralSubmission> activeSubmissions = Optional.ofNullable(userReferralSubmissionList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(userReferralSubmission -> userReferralSubmission.getSubmissionStatus().equals(Constants.ACTIVE))
                .filter(userReferralSubmission -> !appConfiguration.isSubmissionExpiryEnabled() ||
                        userReferralSubmission.getSubmissionDateTime().plusHours(appConfiguration.getSubmissionExpiryInHours()).isBefore(LocalDateTime.now()))
                .filter(userReferralSubmission -> Objects.isNull(userReferralSubmission.getReferral()) || !appConfiguration.isSubmissionExpiryUponReferralExpiryEnabled() || userReferralSubmission.getReferral()
                        .getReferralStatus().equals(Constants.ACTIVE))
                .toList();
        int activeSubmissionCount = activeSubmissions.size();
        if(activeSubmissionCount >= appConfiguration.getUserActiveSubmissionLimit()) {
            throw new ReferralCodeSubmissionException()
                    .withKey(ErrorCodes.MAX_ACTIVE_SUBMISSION_LIMIT_EXCEEDED)
                    .withMessage(ErrorCodes.MAX_ACTIVE_SUBMISSION_LIMIT_EXCEEDED_MESSAGE);
        }

        Optional<UserReferral> userReferralOptional = userReferralRepository.findByReferralCode(referralCodeSubmissionRequestBody.getReferralCode());
        if(!appConfiguration.isUnknownReferralCodeSubmissionEnabled() && (userReferralOptional.isEmpty() || userReferralOptional.get()
                .getReferralStatus().equals(Constants.EXPIRED))) {
            throw new ReferralCodeSubmissionException()
                    .withKey(ErrorCodes.INVALID_REFERRAL_CODE)
                    .withMessage(ErrorCodes.INVALID_REFERRAL_CODE_MESSAGE);
        }
        if(!appConfiguration.isSelfReferralEnabled() && userReferralOptional.isPresent() && userReferralOptional.get().getUserId().equals(userId)) {
            throw new ReferralCodeSubmissionException()
                    .withKey(ErrorCodes.SELF_REFERRAL_NOT_ALLOWED)
                    .withMessage(ErrorCodes.SELF_REFERRAL_NOT_ALLOWED_MESSAGE);
        }

        boolean duplicateReferralCode = Optional.ofNullable(userReferralSubmissionList).orElse(Collections.emptyList())
                .stream()
                .anyMatch(userReferralSubmission -> userReferralSubmission.getSubmittedReferralCode().equals(referralCodeSubmissionRequestBody.getReferralCode()));

        if(duplicateReferralCode) {
            throw new ReferralCodeSubmissionException()
                    .withKey(ErrorCodes.DUPLICATE_REFERRAL_CODE_SUBMISSION)
                    .withMessage(ErrorCodes.DUPLICATE_REFERRAL_CODE_SUBMISSION_MESSAGE);
        }

        UserReferralSubmission newUserReferralSubmission = referralEntityMapper.toUserReferralSubmissionEntity(UUID.randomUUID().toString(), userId, referralCodeSubmissionRequestBody, userReferralOptional.orElse(null));
        userReferralSubmissionRepository.save(newUserReferralSubmission);
        return null;
    }

    @Override
    public ReferralSearchResponseBody searchReferralCode(String userId, String referralCode, Boolean activeReferralCodeOnly, String startDate, String endDate,  Integer from, Integer size) {
        List<UserReferral> userReferralList = userReferralDAO.referralCodeDetails(userId, referralCode, activeReferralCodeOnly, startDate, endDate, from, size);
        int totalCount = userReferralDAO.referralCodeDetailsCount(userId, referralCode, activeReferralCodeOnly, startDate, endDate);
        List<ReferralSearchResponseEntry> referralSearchResponseEntries = Optional.ofNullable(userReferralList)
                .orElse(Collections.emptyList())
                .stream()
                .map(referralEntityMapper::toReferralSearchResponseEntry)
                .toList();
        return new ReferralSearchResponseBody()
                .referrals(referralSearchResponseEntries)
                .totalResults(totalCount);
    }

    @Override
    public ReferralSubmissionSearchResponseBody searchReferralCodeSubmissions(String userId, String referralCode, String referralUserId, Boolean activeSubmissionsOnly, Boolean activeReferralCodeOnly, String startDate, String endDate,  Integer from, Integer size) {
        List<UserReferralSubmission> userReferralSubmissionList = userReferralSubmissionDAO.referralSubmissionDetails(userId, referralCode, referralUserId, activeSubmissionsOnly, activeReferralCodeOnly, startDate, endDate, from, size);
        int totalCount = userReferralSubmissionDAO.referralSubmissionDetailsCount(userId, referralCode, referralUserId, activeSubmissionsOnly, activeReferralCodeOnly, startDate, endDate);
        List<ReferralSubmissionSearchResponseEntry> referralSubmissionSearchResponseEntries = Optional.ofNullable(userReferralSubmissionList)
                .orElse(Collections.emptyList())
                .stream()
                .map(referralEntityMapper::toReferralSubmissionSearchResponseEntry)
                .toList();
        return new ReferralSubmissionSearchResponseBody()
                .submissions(referralSubmissionSearchResponseEntries)
                .totalResults(totalCount);
    }

    @Scheduled(cron = "${backbase.referral.referralStatusRefreshCronExpression}")
    public void referralStatusRefresh() {
        if(!appConfiguration.isReferralExpiryEnabled()) return;
        int expirationInHours = appConfiguration.getReferralExpiryInHours();
        List<UserReferral> expiringReferrals = userReferralRepository.findAllByReferralStatusAndCreatedDateTimeBefore(Constants.ACTIVE, LocalDateTime.now().minusHours(expirationInHours));
        expiringReferrals.forEach(userReferral -> {
                    userReferral.setReferralStatus(Constants.EXPIRED);
                    userReferral.setExpiryDateTime(LocalDateTime.now());
                    log.info("Referral code [{}] for userId [{}] has been marked as EXPIRED", userReferral.getReferralCode(), userReferral.getUserId());
                });
        userReferralRepository.saveAll(expiringReferrals);

        if(!appConfiguration.isSubmissionExpiryUponReferralExpiryEnabled()) return;
        List<UserReferralSubmission> expiredSubmissionsList = expiringReferrals.stream()
                .map(userReferralSubmissionRepository::findAllByReferral)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(userReferralSubmission -> userReferralSubmission.getSubmissionStatus().equals(Constants.ACTIVE))
                .map(userReferralSubmission -> {
                    userReferralSubmission.setSubmissionStatus(Constants.EXPIRED);
                    userReferralSubmission.setExpiryDateTime(LocalDateTime.now());
                  return userReferralSubmission;
                }).peek(userReferralSubmission -> {
                    log.info("REFERRAL: Referral submission id [{}] for userId [{}] has been marked as EXPIRED because of referral code expiry",
                            userReferralSubmission.getId(), userReferralSubmission.getSubmittedUserId());
                }).toList();
        userReferralSubmissionRepository.saveAll(expiredSubmissionsList);
    }

    @Scheduled(cron = "${backbase.referral.submissionStatusRefreshCronExpression}")
    public void submissionStatusRefresh() {
        if(!appConfiguration.isSubmissionExpiryEnabled()) return;
        int expirationInHours = appConfiguration.getSubmissionExpiryInHours();
        List<UserReferralSubmission> expiringSubmissions = userReferralSubmissionRepository.findAllBySubmissionStatusAndSubmissionDateTimeBefore(Constants.ACTIVE, LocalDateTime.now().minusHours(expirationInHours));
        expiringSubmissions.forEach(userReferralSubmission -> {
            userReferralSubmission.setSubmissionStatus(Constants.EXPIRED);
            userReferralSubmission.setExpiryDateTime(LocalDateTime.now());
            log.info("REFERRAL: Referral submission id [{}] for userId [{}] has been marked as EXPIRED", userReferralSubmission.getId(), userReferralSubmission.getSubmittedUserId());
        });
        userReferralSubmissionRepository.saveAll(expiringSubmissions);
    }
}
