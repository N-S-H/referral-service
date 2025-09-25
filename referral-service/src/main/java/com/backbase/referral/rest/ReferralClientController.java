package com.backbase.referral.rest;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.referral.client.api.v1.ReferralApi;
import com.backbase.referral.client.api.v1.model.ReferralCodeSubmissionRequestBody;
import com.backbase.referral.client.api.v1.model.ReferralGenerationResponseBody;
import com.backbase.referral.client.api.v1.model.ReferralSearchResponseBody;
import com.backbase.referral.client.api.v1.model.ReferralSubmissionSearchResponseBody;
import com.backbase.referral.mapper.ReferralModelMapper;
import com.backbase.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RestController
public class ReferralClientController implements ReferralApi {

    private final SecurityContextUtil securityContextUtil;
    private final ReferralModelMapper referralModelMapper;
    private final ReferralService referralService;

    @Override
    public ResponseEntity<ReferralGenerationResponseBody> generateReferralCode() {
        String userId = getUserIdFromContext();
        return ResponseEntity.ok(referralModelMapper.mapToGenerateReferralCodeResponse(referralService.generateReferralCode(userId)));
    }

    @Override
    public ResponseEntity<Void> referralCodeSubmission(ReferralCodeSubmissionRequestBody referralCodeSubmissionRequestBody) {
        String userId = getUserIdFromContext();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(referralService.referralCodeSubmission(userId, referralModelMapper.mapToSubmissionRequestBody(referralCodeSubmissionRequestBody)));
    }

    @Override
    public ResponseEntity<ReferralSearchResponseBody> searchReferralCode(String userId, String referralCode, Boolean activeReferralCodeOnly, String startDate, String endDate, Integer from, Integer size) {
        return ResponseEntity.ok(referralModelMapper.mapToSerachReferralCodeResponse(referralService.searchReferralCode(userId, referralCode, activeReferralCodeOnly, startDate, endDate, from, size)));
    }

    @Override
    public ResponseEntity<ReferralSubmissionSearchResponseBody> searchReferralCodeSubmissions(String userId, String referralCode, String referralUserId, Boolean activeSubmissionsOnly, Boolean activeReferralCodeOnly, String startDate, String endDate, Integer from, Integer size) {
        return ResponseEntity.ok(referralModelMapper.mapToSearchReferralCodeSubmissionResponse(referralService.searchReferralCodeSubmissions(userId, referralCode, referralUserId, activeSubmissionsOnly, activeReferralCodeOnly, startDate, endDate, from, size)));
    }

    private String getUserIdFromContext() {
        return securityContextUtil.getInternalId()
                .orElseThrow(() -> new IllegalStateException("User ID not found in security context"));
    }
}
