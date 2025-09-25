package com.backbase.referral.rest;

import com.backbase.referral.service.ReferralService;
import com.backbase.referral.service.api.v1.ReferralApi;
import com.backbase.referral.service.api.v1.model.ReferralCodeSubmissionRequestBody;
import com.backbase.referral.service.api.v1.model.ReferralGenerationResponseBody;
import com.backbase.referral.service.api.v1.model.ReferralSearchResponseBody;
import com.backbase.referral.service.api.v1.model.ReferralSubmissionSearchResponseBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RestController
public class ReferralServiceController implements ReferralApi {

    private final ReferralService referralService;

    @Override
    public ResponseEntity<ReferralGenerationResponseBody> generateReferralCode(String userId) {
        return ResponseEntity.ok(referralService.generateReferralCode(userId));
    }

    @Override
    public ResponseEntity<Void> referralCodeSubmission(String userId, ReferralCodeSubmissionRequestBody referralCodeSubmissionRequestBody) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(referralService.referralCodeSubmission(userId, referralCodeSubmissionRequestBody));
    }

    @Override
    public ResponseEntity<ReferralSearchResponseBody> searchReferralCode(String userId, String referralCode, Boolean activeReferralCodeOnly, String startDate, String endDate, Integer from, Integer size) {
        return ResponseEntity.ok(referralService.searchReferralCode(userId, referralCode, activeReferralCodeOnly, startDate, endDate, from, size));
    }

    @Override
    public ResponseEntity<ReferralSubmissionSearchResponseBody> searchReferralCodeSubmissions(String userId, String referralCode, String referralUserId, Boolean activeSubmissionsOnly, Boolean activeReferralCodeOnly, String startDate, String endDate, Integer from, Integer size) {
        return ResponseEntity.ok(referralService.searchReferralCodeSubmissions(userId, referralCode, referralUserId, activeSubmissionsOnly, activeReferralCodeOnly, startDate, endDate, from, size));
    }
}
