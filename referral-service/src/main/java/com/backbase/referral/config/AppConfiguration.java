package com.backbase.referral.config;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@ConfigurationProperties(prefix = "backbase.referral")
@Getter
@Setter
public class AppConfiguration {
    private int userActiveSubmissionLimit;
    private boolean referralExpiryEnabled;
    private int referralExpiryInHours;
    private String referralStatusRefreshCronExpression;
    private boolean reGenerationUponExpiryEnabled;
    private boolean submissionExpiryEnabled;
    private int submissionExpiryInHours;
    private String submissionStatusRefreshCronExpression;
    private boolean submissionExpiryUponReferralExpiryEnabled;
    private boolean unknownReferralCodeSubmissionEnabled;
    private boolean selfReferralEnabled;
    private int referralCodeLengthWithoutPrefix;
    private String referralCodePrefix;
    private ReferralPatternEnum referralCodePatternType;
    private String referralCodeRegexPattern;
    private boolean referralCodeQREnabled;
    private int referralCodeGenerationMaxAttempts;

    public enum ReferralPatternEnum {
          ALPHANUMERIC,
          ALPHABETIC,
          NUMERIC,
          REGEX
    }

}
