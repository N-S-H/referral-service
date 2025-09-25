package com.backbase.referral.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorCodes {

    public static final String MAX_ACTIVE_SUBMISSION_LIMIT_EXCEEDED = "backbase.referral.maxActiveSubmissionLimitExceeded";
    public static final String MAX_ACTIVE_SUBMISSION_LIMIT_EXCEEDED_MESSAGE = "You have exceeded the maximum limit of active referral code submissions allowed.";
    public static final String INVALID_REFERRAL_CODE = "backbase.referral.invalidReferralCode";
    public static final String INVALID_REFERRAL_CODE_MESSAGE = "The referral code you entered is invalid. Please check and try again.";
    public static final String SELF_REFERRAL_NOT_ALLOWED = "backbase.referral.selfReferralNotAllowed";
    public static final String SELF_REFERRAL_NOT_ALLOWED_MESSAGE = "You cannot use your own referral code.";
    public static final String DUPLICATE_REFERRAL_CODE_SUBMISSION = "backbase.referral.duplicateReferralCodeSubmission";
    public static final String DUPLICATE_REFERRAL_CODE_SUBMISSION_MESSAGE = "You have already submitted this referral code.";
    public static final String REFERRAL_QR_CODE_GENERATION_FAILED = "backbase.referral.qrCodeGenerationFailed";
    public static final String REFERRAL_QR_CODE_GENERATION_FAILED_MESSAGE = "Failed to generate QR code. Please try again later.";
    public static final String MAX_REFERRAL_CODE_GENERATION_ATTEMPTS_EXCEEDED = "backbase.referral.maxReferralCodeGenerationAttemptsExceeded";
    public static final String MAX_REFERRAL_CODE_GENERATION_ATTEMPTS_EXCEEDED_MESSAGE = "You have exceeded the maximum number of referral code generation attempts. Please try again later.";
}
