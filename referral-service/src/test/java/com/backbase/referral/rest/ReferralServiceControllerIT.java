package com.backbase.referral.rest;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.test.http.TestRestTemplateConfiguration;
import com.backbase.referral.AbstractTestContainersSetup;
import com.backbase.referral.TestUtils;
import com.backbase.referral.client.api.v1.model.ReferralCodeSubmissionRequestBody;
import com.backbase.referral.client.api.v1.model.ReferralGenerationResponseBody;
import com.backbase.referral.client.api.v1.model.ReferralSearchResponseBody;
import com.backbase.referral.client.api.v1.model.ReferralSubmissionSearchResponseBody;
import com.backbase.referral.config.AppConfiguration;
import com.backbase.referral.entity.UserReferral;
import com.backbase.referral.entity.UserReferralSubmission;
import com.backbase.referral.repository.UserReferralRepository;
import com.backbase.referral.repository.UserReferralSubmissionRepository;
import com.backbase.referral.utils.Constants;
import com.backbase.referral.utils.ErrorCodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("it")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReferralServiceControllerIT extends AbstractTestContainersSetup {

    @Autowired
    MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    AppConfiguration appConfiguration;

    @Autowired
    UserReferralRepository userReferralRepository;

    @Autowired
    UserReferralSubmissionRepository userReferralSubmissionRepository;

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
    @Order(1)
    void test_generateReferralUponExpired() throws Exception {
        UserReferral userReferral = TestUtils.getUserReferrals().getFirst();
        userReferral.setCreatedDateTime(LocalDateTime.now().minusDays(30));
        userReferral.setReferralStatus(Constants.EXPIRED);
        userReferralRepository.save(userReferral);

        appConfiguration.setReferralExpiryEnabled(true);
        appConfiguration.setReferralExpiryInHours(300);
        appConfiguration.setReGenerationUponExpiryEnabled(true);

        MvcResult result1 = mvc.perform(MockMvcRequestBuilders.get("/service-api/v1/referral/generation")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .queryParam("userId", "user-1"))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String responseBody1 = result1.getResponse().getContentAsString();
        ReferralGenerationResponseBody userReferral1 = objectMapper.readValue(responseBody1, ReferralGenerationResponseBody.class);

        Assertions.assertNotEquals(userReferral1.getReferralCode(), userReferral.getReferralCode());
        Assertions.assertEquals(userReferral1.getUserId(), userReferral.getUserId());
        Assertions.assertEquals(userReferralRepository.count(), 2);
        Assertions.assertEquals(userReferral1.getReferralStatus(), Constants.ACTIVE);


        appConfiguration.setReGenerationUponExpiryEnabled(false);
        appConfiguration.setReferralExpiryInHours(0);
        appConfiguration.setReferralExpiryEnabled(false);
    }

    @Test
    @Order(2)
    void test_getExpiredReferralCode() throws Exception {
        UserReferral userReferral = TestUtils.getUserReferrals().getFirst();
        userReferral.setCreatedDateTime(LocalDateTime.now().minusDays(30));
        userReferral.setReferralStatus(Constants.EXPIRED);
        userReferralRepository.save(userReferral);

        MvcResult result1 = mvc.perform(MockMvcRequestBuilders.get("/service-api/v1/referral/generation")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .queryParam("userId", "user-1"))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String responseBody1 = result1.getResponse().getContentAsString();
        ReferralGenerationResponseBody userReferral1 = objectMapper.readValue(responseBody1, ReferralGenerationResponseBody.class);

        Assertions.assertEquals(userReferralRepository.count(), 1);
        Assertions.assertEquals(userReferral1.getReferralStatus(), Constants.EXPIRED);
    }

    @Test
    @Order(3)
    void test_selfReferralSubmissionEnabled() throws Exception {
        UserReferral userReferral = TestUtils.getUserReferrals().getFirst();
        userReferralRepository.save(userReferral);

        appConfiguration.setSelfReferralEnabled(true);

        mvc.perform(MockMvcRequestBuilders.post("/service-api/v1/referral/submission")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .param("userId", "user-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ReferralCodeSubmissionRequestBody()
                                        .referralCode(userReferral.getReferralCode())
                                        .source("MOBILE")
                        )))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        appConfiguration.setSelfReferralEnabled(false);
    }

    @Test
    @Order(4)
    void test_selfReferralSubmissionDisabledAsDefault() throws Exception {
        UserReferral userReferral = TestUtils.getUserReferrals().getFirst();
        userReferralRepository.save(userReferral);

        MvcResult errorResult = mvc.perform(MockMvcRequestBuilders.post("/service-api/v1/referral/submission")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .param("userId", "user-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ReferralCodeSubmissionRequestBody()
                                        .referralCode(userReferral.getReferralCode())
                                        .source("MOBILE")
                        )))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andReturn();

        String errorResponseBody = errorResult.getResponse().getContentAsString();
        BadRequestException badRequestException = objectMapper.readValue(errorResponseBody, BadRequestException.class);
        Assertions.assertEquals(ErrorCodes.SELF_REFERRAL_NOT_ALLOWED, badRequestException.getKey());
    }

    @Test
    @Order(5)
    void test_unknownReferralSubmissionDisabled() throws Exception {
        UserReferral userReferral = TestUtils.getUserReferrals().getFirst();
        userReferralRepository.save(userReferral);

        appConfiguration.setUnknownReferralCodeSubmissionEnabled(false);

        MvcResult errorResult = mvc.perform(MockMvcRequestBuilders.post("/service-api/v1/referral/submission")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .param("userId", "user-2")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ReferralCodeSubmissionRequestBody()
                                        .referralCode("SOME_RANDOM_CODE")
                                        .source("MOBILE")
                        )))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andReturn();

        String errorResponseBody = errorResult.getResponse().getContentAsString();
        BadRequestException badRequestException = objectMapper.readValue(errorResponseBody, BadRequestException.class);
        Assertions.assertEquals(ErrorCodes.INVALID_REFERRAL_CODE, badRequestException.getKey());

        appConfiguration.setUnknownReferralCodeSubmissionEnabled(true);
    }

    @Test
    @Order(6)
    void test_noDuplicateSubmissions() throws Exception {
        String code = "RANDOM_CODE";
        appConfiguration.setUserActiveSubmissionLimit(2);
        mvc.perform(MockMvcRequestBuilders.post("/service-api/v1/referral/submission")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .param("userId", "user-2")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ReferralCodeSubmissionRequestBody()
                                        .referralCode(code)
                                        .source("MOBILE")
                        )))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MvcResult errorResult = mvc.perform(MockMvcRequestBuilders.post("/service-api/v1/referral/submission")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .param("userId", "user-2")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ReferralCodeSubmissionRequestBody()
                                        .referralCode(code)
                                        .source("MOBILE")
                        )))
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andReturn();

        String errorResponseBody = errorResult.getResponse().getContentAsString();
        BadRequestException badRequestException = objectMapper.readValue(errorResponseBody, BadRequestException.class);
        Assertions.assertEquals(ErrorCodes.DUPLICATE_REFERRAL_CODE_SUBMISSION, badRequestException.getKey());

        appConfiguration.setUserActiveSubmissionLimit(1);
    }

    @Test
    @Order(7)
    void test_searchReferralCodeDefault() throws Exception {
        List<UserReferral> userReferrals = TestUtils.getUserReferrals();
        userReferralRepository.saveAll(userReferrals);

        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/service-api/v1/referral/search")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        var searchResponse = objectMapper.readValue(responseBody, ReferralSearchResponseBody.class);
        Assertions.assertEquals(userReferrals.size(), searchResponse.getTotalResults());
    }

    @Test
    @Order(8)
    void test_searchReferralCodeDefaultWithPagination() throws Exception {
        List<UserReferral> userReferrals = TestUtils.getUserReferrals();
        userReferralRepository.saveAll(userReferrals);

        MvcResult result1 = mvc.perform(MockMvcRequestBuilders.get("/service-api/v1/referral/search")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .param("from", "1")
                        .param("size", "2"))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String responseBody1 = result1.getResponse().getContentAsString();
        ReferralSearchResponseBody searchResponse1 = objectMapper.readValue(responseBody1, ReferralSearchResponseBody.class);
        Assertions.assertEquals(2, searchResponse1.getReferrals().size());
        Assertions.assertEquals(5, searchResponse1.getTotalResults());
    }

    @Test
    @Order(9)
    void test_searchReferralSubmissionsWithReferralData() throws Exception {
        List<UserReferral> userReferrals = TestUtils.getUserReferrals();
        userReferralRepository.saveAll(List.of(userReferrals.get(3), userReferrals.get(4)));
        List<UserReferralSubmission> userReferralSubmissionList = TestUtils.getUserReferralSubmissions();
        userReferralSubmissionList.get(3).setReferral(userReferrals.get(4));
        userReferralSubmissionList.get(4).setReferral(userReferrals.get(3));
        userReferralSubmissionRepository.saveAll(userReferralSubmissionList);

        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(2);

        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/service-api/v1/referral/submission/search")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .param("activeSubmissionsOnly", "true")
                        .param("activeReferralCodeOnly", "true")
                        .param("referralUserId", userReferrals.get(3).getUserId())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("from", "0")
                        .param("size", "10")
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        var searchResponse = objectMapper.readValue(responseBody, ReferralSubmissionSearchResponseBody.class);
        Assertions.assertEquals(1, searchResponse.getTotalResults());
        Assertions.assertEquals(userReferralSubmissionList.get(4).getSubmittedUserId(), searchResponse.getSubmissions().getFirst().getSubmittedUserId());
        Assertions.assertEquals(userReferralSubmissionList.get(4).getSubmittedReferralCode(), searchResponse.getSubmissions().getFirst().getSubmittedReferralCode());
        Assertions.assertEquals(userReferrals.get(3).getUserId(), searchResponse.getSubmissions().getFirst().getReferredUserId());
    }
}
