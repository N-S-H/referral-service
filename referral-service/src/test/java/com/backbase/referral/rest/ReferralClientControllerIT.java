package com.backbase.referral.rest;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.test.http.TestRestTemplateConfiguration;
import com.backbase.referral.AbstractTestContainersSetup;
import com.backbase.referral.TestUtils;
import com.backbase.referral.client.api.v1.model.ReferralCodeSubmissionRequestBody;
import com.backbase.referral.client.api.v1.model.ReferralGenerationResponseBody;
import com.backbase.referral.client.api.v1.model.ReferralSearchResponseBody;
import com.backbase.referral.client.api.v1.model.ReferralSubmissionSearchResponse;
import com.backbase.referral.client.api.v1.model.ReferralSubmissionSearchResponseBody;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;

import static com.backbase.referral.Fixture.resourceJson;
import static com.backbase.referral.TestUtils.mockSecurityContext;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("it")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReferralClientControllerIT extends AbstractTestContainersSetup  {

    @Autowired
    MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    SecurityContextUtil securityContextUtil;

    @Autowired
    UserReferralRepository userReferralRepository;

    @Autowired
    UserReferralSubmissionRepository userReferralSubmissionRepository;

    @BeforeEach
    public void setSecurityContextUtil() {
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        mockSecurityContext(securityContextUtil);
    }


    @AfterEach
    public void clearDB() {
        userReferralSubmissionRepository.deleteAll();
        userReferralRepository.deleteAll();
    }

    @Test
    @Order(1)
    void test_generateReferralCodeOnePerUser() throws Exception {
        MvcResult result1 = mvc.perform(MockMvcRequestBuilders.get("/client-api/v1/referral/generation")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String responseBody1 = result1.getResponse().getContentAsString();
        ReferralGenerationResponseBody userReferral1 = objectMapper.readValue(responseBody1, ReferralGenerationResponseBody.class);

        MvcResult result2 = mvc.perform(MockMvcRequestBuilders.get("/client-api/v1/referral/generation")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String responseBody2 = result2.getResponse().getContentAsString();
        ReferralGenerationResponseBody userReferral2 = objectMapper.readValue(responseBody2, ReferralGenerationResponseBody.class);

        Assertions.assertEquals(userReferral1.getReferralCode(), userReferral2.getReferralCode());
        Assertions.assertEquals(userReferral1.getUserId(), userReferral2.getUserId());
        Assertions.assertEquals(userReferral1.getReferralQrString(), userReferral2.getReferralQrString());
        Assertions.assertEquals(userReferral1.getReferralStatus(), Constants.ACTIVE);
    }

    @Test
    @Order(2)
    void test_maxSubmissionsExceeded() throws Exception {
        UserReferral userReferral = TestUtils.getUserReferrals().getFirst();
        userReferralRepository.save(userReferral);
        mvc.perform(MockMvcRequestBuilders.post("/client-api/v1/referral/submission")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ReferralCodeSubmissionRequestBody()
                                        .referralCode("RANDOMCODE")
                                        .source("MOBILE")
                        )))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MvcResult errorResult = mvc.perform(MockMvcRequestBuilders.post("/client-api/v1/referral/submission")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
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
        Assertions.assertEquals(ErrorCodes.MAX_ACTIVE_SUBMISSION_LIMIT_EXCEEDED, badRequestException.getKey());
    }

    @Test
    @Order(3)
    void test_searchReferralCode() throws Exception {
        List<UserReferral> userReferrals = TestUtils.getUserReferrals();
        userReferralRepository.saveAll(userReferrals);

        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(2);

        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/client-api/v1/referral/search")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .param("userId", userReferrals.get(0).getUserId())
                        .param("referralCode", userReferrals.get(0).getReferralCode())
                        .param("activeReferralCodeOnly", "true")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("from", "0")
                        .param("size", "10")
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        var searchResponse = objectMapper.readValue(responseBody, ReferralSearchResponseBody.class);
        Assertions.assertEquals(1, searchResponse.getTotalResults());
        Assertions.assertEquals(userReferrals.get(0).getUserId(), searchResponse.getReferrals().getFirst().getUserId());
        Assertions.assertEquals(userReferrals.get(0).getReferralCode(), searchResponse.getReferrals().getFirst().getReferralCode());
    }


    @Test
    @Order(4)
    void test_searchReferralSubmissions() throws Exception {
        List<UserReferral> userReferrals = TestUtils.getUserReferrals();
        userReferralRepository.saveAll(userReferrals);
        List<UserReferralSubmission> userReferralSubmissionList = TestUtils.getUserReferralSubmissions();
        userReferralSubmissionRepository.saveAll(userReferralSubmissionList);

        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(2);

        MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/client-api/v1/referral/submission/search")
                        .header("Authorization", TestRestTemplateConfiguration.TEST_SERVICE_BEARER)
                        .param("userId", userReferralSubmissionList.get(0).getSubmittedUserId())
                        .param("referralCode", userReferralSubmissionList.get(0).getSubmittedReferralCode())
                        .param("activeSubmissionsOnly", "true")
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
        Assertions.assertEquals(userReferralSubmissionList.get(0).getSubmittedUserId(), searchResponse.getSubmissions().getFirst().getSubmittedUserId());
        Assertions.assertEquals(userReferralSubmissionList.get(0).getSubmittedReferralCode(), searchResponse.getSubmissions().getFirst().getSubmittedReferralCode());
        Assertions.assertNull(searchResponse.getSubmissions().getFirst().getReferredUserId());
    }

}
