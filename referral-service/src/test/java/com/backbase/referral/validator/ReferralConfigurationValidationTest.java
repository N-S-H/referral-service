package com.backbase.referral.validator;

import com.backbase.referral.AbstractTestContainersSetup;
import com.backbase.referral.config.AppConfiguration;
import com.backbase.referral.repository.UserReferralRepository;
import com.backbase.referral.repository.UserReferralSubmissionRepository;
import com.backbase.referral.service.impl.ReferralServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("it")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReferralConfigurationValidationTest extends AbstractTestContainersSetup  {

    @Autowired
    MockMvc mvc;

    @Autowired
    AppConfiguration appConfiguration;

    @Autowired
    ReferralConfigurationValidation configurationValidation;

    @Test
    void test_validationFailures() {
        Assertions.assertDoesNotThrow(() -> configurationValidation.validate());

        appConfiguration.setReferralExpiryEnabled(true);
        appConfiguration.setReferralExpiryInHours(0);

        Assertions.assertThrows(IllegalStateException.class, () -> configurationValidation.validate());

        appConfiguration.setReferralExpiryEnabled(false);

        appConfiguration.setSubmissionExpiryEnabled(true);
        appConfiguration.setSubmissionExpiryInHours(-1);
        Assertions.assertThrows(IllegalStateException.class, () -> configurationValidation.validate());

        appConfiguration.setSubmissionExpiryEnabled(false);
        appConfiguration.setSubmissionExpiryInHours(0);

        appConfiguration.setReferralCodePatternType(AppConfiguration.ReferralPatternEnum.REGEX);
        appConfiguration.setReferralCodeRegexPattern(null);
        Assertions.assertThrows(IllegalStateException.class, () -> configurationValidation.validate());

        appConfiguration.setReferralCodeRegexPattern("[A-Z");
        Assertions.assertThrows(IllegalStateException.class, () -> configurationValidation.validate());

        appConfiguration.setReferralCodePatternType(AppConfiguration.ReferralPatternEnum.ALPHABETIC);
        appConfiguration.setReferralCodeRegexPattern(null);
        appConfiguration.setReferralCodeLengthWithoutPrefix(0);

        Assertions.assertThrows(IllegalStateException.class, () -> configurationValidation.validate());

        appConfiguration.setReferralCodeLengthWithoutPrefix(6);
        appConfiguration.setReGenerationUponExpiryEnabled(true);

        Assertions.assertThrows(IllegalStateException.class, () -> configurationValidation.validate());

        appConfiguration.setReGenerationUponExpiryEnabled(false);

        appConfiguration.setReferralCodeGenerationMaxAttempts(-100);
        Assertions.assertThrows(IllegalStateException.class, () -> configurationValidation.validate());

        appConfiguration.setReferralCodeGenerationMaxAttempts(100);

        Assertions.assertDoesNotThrow(() -> configurationValidation.validate());

    }
}
