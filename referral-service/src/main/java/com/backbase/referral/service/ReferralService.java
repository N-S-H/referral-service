package com.backbase.referral.service;

import com.backbase.referral.service.api.v1.model.ReferralCodeSubmissionRequestBody;
import com.backbase.referral.service.api.v1.model.ReferralGenerationResponseBody;
import com.backbase.referral.service.api.v1.model.ReferralSearchResponseBody;
import com.backbase.referral.service.api.v1.model.ReferralSubmissionSearchResponseBody;

public interface ReferralService {

    ReferralGenerationResponseBody generateReferralCode(String userId);

    Void referralCodeSubmission(String userId, ReferralCodeSubmissionRequestBody referralCodeSubmissionRequestBody);

    ReferralSearchResponseBody searchReferralCode(String userId, String referralCode, Boolean activeReferralCodeOnly, String startDate, String endDate, Integer from, Integer size);

    ReferralSubmissionSearchResponseBody searchReferralCodeSubmissions(String userId, String referralCode, String referralUserId, Boolean activeSubmissionsOnly, Boolean activeReferralCodeOnly, String startDate, String endDate,  Integer from, Integer size);
}
