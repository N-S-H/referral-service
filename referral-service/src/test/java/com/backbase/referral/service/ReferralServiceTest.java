package com.backbase.referral.service;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.referral.AbstractTestContainersSetup;
import com.backbase.referral.TestUtils;
import com.backbase.referral.config.AppConfiguration;
import com.backbase.referral.entity.UserReferral;
import com.backbase.referral.entity.UserReferralSubmission;
import com.backbase.referral.repository.UserReferralRepository;
import com.backbase.referral.repository.UserReferralSubmissionRepository;
import com.backbase.referral.service.impl.ReferralServiceImpl;
import com.backbase.referral.utils.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.backbase.referral.TestUtils.mockSecurityContext;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@ActiveProfiles("it")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReferralServiceTest extends AbstractTestContainersSetup  {

    @Autowired
    MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    UserReferralRepository userReferralRepository;

    @Autowired
    UserReferralSubmissionRepository userReferralSubmissionRepository;

    @Autowired
    AppConfiguration appConfiguration;

    @Autowired
    ReferralServiceImpl referralService;

    @BeforeEach
    public void setSecurityContextUtil() {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
    }


    @AfterEach
    public void clearDB() {
        userReferralSubmissionRepository.deleteAll();
        userReferralRepository.deleteAll();
    }

    @Test
    public void referralStatusRefreshExpiryDisabled() {
        Assertions.assertDoesNotThrow( () -> {
            referralService.referralStatusRefresh();
        });
    }

    @Test
    public void referralStatusRefreshButNoSubmissionExpiry() {
        appConfiguration.setReferralExpiryEnabled(true);
        appConfiguration.setReferralExpiryInHours(10);
        List<UserReferral> userReferralList = TestUtils.getUserReferrals();
        userReferralList.get(3).setCreatedDateTime(LocalDateTime.now().minusHours(11));
        userReferralList.get(4).setCreatedDateTime(LocalDateTime.now().minusDays(325));
        userReferralRepository.saveAll(userReferralList);
        referralService.referralStatusRefresh();
        List<UserReferral> expiredList = userReferralRepository.findAll().stream().filter(ref -> ref.getReferralStatus().equals(Constants.EXPIRED))
                .toList();
        Assertions.assertEquals(2, expiredList.size());
        Assertions.assertTrue(expiredList.getFirst().getExpiryDateTime().isAfter(LocalDateTime.now().minusMinutes(5)));
        appConfiguration.setReferralExpiryEnabled(false);
        appConfiguration.setReferralExpiryInHours(0);
    }

    @Test
    public void referralStatusRefreshAlongWithSubmissions() {
        appConfiguration.setReferralExpiryEnabled(true);
        appConfiguration.setReferralExpiryInHours(10);
        appConfiguration.setSubmissionExpiryEnabled(true);
        appConfiguration.setSubmissionExpiryInHours(3);
        appConfiguration.setSubmissionExpiryUponReferralExpiryEnabled(true);
        List<UserReferral> userReferralList = TestUtils.getUserReferrals();
        userReferralList.get(3).setCreatedDateTime(LocalDateTime.now().minusHours(11));
        userReferralList.get(4).setCreatedDateTime(LocalDateTime.now().minusDays(325));


        List<UserReferralSubmission> userReferralSubmissionList = TestUtils.getUserReferralSubmissions();
        userReferralSubmissionList.get(3).setReferral(userReferralList.get(4));
        userReferralSubmissionList.get(3).setSubmissionDateTime(LocalDateTime.now().minusHours(6));

        userReferralSubmissionList.get(4).setReferral(userReferralList.get(3));
        userReferralSubmissionList.get(4).setSubmissionDateTime(LocalDateTime.now().minusHours(4));

        userReferralRepository.saveAll(userReferralList);
        userReferralSubmissionRepository.saveAll(userReferralSubmissionList);

        referralService.referralStatusRefresh();
        List<UserReferral> expiredList = userReferralRepository.findAll().stream().filter(ref -> ref.getReferralStatus().equals(Constants.EXPIRED))
                .toList();
        Assertions.assertEquals(2, expiredList.size());
        Assertions.assertTrue(expiredList.getFirst().getExpiryDateTime().isAfter(LocalDateTime.now().minusMinutes(5)));


        List<UserReferralSubmission> expiredSubmissionList = userReferralSubmissionRepository.findAll().stream().filter(ref -> ref.getSubmissionStatus().equals(Constants.EXPIRED))
                .toList();
        Assertions.assertEquals(2, expiredSubmissionList.size());
        Assertions.assertTrue(expiredSubmissionList.getLast().getExpiryDateTime().isAfter(LocalDateTime.now().minusMinutes(5)));

        appConfiguration.setReferralExpiryEnabled(false);
        appConfiguration.setReferralExpiryInHours(0);
        appConfiguration.setSubmissionExpiryEnabled(false);
        appConfiguration.setSubmissionExpiryInHours(0);
        appConfiguration.setSubmissionExpiryUponReferralExpiryEnabled(false);
    }

    @Test
    public void submissionStatusRefreshDisabled() {
        Assertions.assertDoesNotThrow( () -> {
            referralService.submissionStatusRefresh();
        });
    }

    @Test
    public void submissionStatusRefresh() {
        appConfiguration.setSubmissionExpiryEnabled(true);
        appConfiguration.setSubmissionExpiryInHours(3);
        List<UserReferralSubmission> userReferralSubmissionList = TestUtils.getUserReferralSubmissions();
        userReferralSubmissionList.get(1).setSubmissionDateTime(LocalDateTime.now().minusHours(6));
        userReferralSubmissionList.get(2).setSubmissionDateTime(LocalDateTime.now().minusHours(4));
        userReferralSubmissionRepository.saveAll(userReferralSubmissionList);

        referralService.submissionStatusRefresh();

        List<UserReferralSubmission> submissionList = userReferralSubmissionRepository.findAll();
        Assertions.assertEquals(2, submissionList.stream().filter(ref -> ref.getSubmissionStatus().equals(Constants.EXPIRED)).count());
        Assertions.assertEquals(3, submissionList.stream().filter(ref -> ref.getSubmissionStatus().equals(Constants.ACTIVE)).count());

        Assertions.assertTrue(submissionList.stream().filter(ref -> ref.getSubmissionStatus().equals(Constants.EXPIRED)).toList().getLast().getExpiryDateTime().isAfter(LocalDateTime.now().minusMinutes(5)));

        appConfiguration.setSubmissionExpiryEnabled(false);
        appConfiguration.setSubmissionExpiryInHours(0);
    }

}
