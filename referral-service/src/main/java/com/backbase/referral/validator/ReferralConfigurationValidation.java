package com.backbase.referral.validator;

import com.backbase.referral.config.AppConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
@RequiredArgsConstructor
public class ReferralConfigurationValidation {

    private final AppConfiguration appConfiguration;

    @PostConstruct
    public void validate() {
        if(appConfiguration.isReferralExpiryEnabled() && appConfiguration.getReferralExpiryInHours() <= 0 ) {
            throw new IllegalStateException("Referral expiry is enabled but expiry hours is not set correctly.");
        }
        if(appConfiguration.isSubmissionExpiryEnabled() && appConfiguration.getSubmissionExpiryInHours() <= 0 ) {
            throw new IllegalStateException("Submission expiry is enabled but expiry hours is not set correctly.");
        }

        if(appConfiguration.getReferralCodePatternType() == AppConfiguration.ReferralPatternEnum.REGEX) {
            if(StringUtils.isEmpty(appConfiguration.getReferralCodeRegexPattern())) {
                throw new IllegalStateException("Referral code pattern type is REGEX but regex pattern is not set.");
            }

            if(!isValidRegex(appConfiguration.getReferralCodeRegexPattern())) {
                throw new IllegalStateException("Referral code regex pattern is not a valid regex.");
            }
        }

        if(appConfiguration.getReferralCodeLengthWithoutPrefix() <= 0) {
            throw new IllegalStateException("Referral code length without prefix must be greater than zero.");
        }

        if(!appConfiguration.isReferralExpiryEnabled() && appConfiguration.isReGenerationUponExpiryEnabled()) {
            throw new IllegalStateException("Referral regeneration upon expiry is enabled but referral expiry is disabled.");
        }

        if(appConfiguration.getReferralCodeGenerationMaxAttempts() <= 0) {
            throw new IllegalStateException("Referral code generation max attempts must be greater than zero.");
        }
    }

    private boolean isValidRegex(String regex) {
        try {
            Pattern.compile(regex);
            return true;  // Compiled successfully → valid regex
        } catch (PatternSyntaxException e) {
            return false; // Invalid regex syntax
        }
    }

}
