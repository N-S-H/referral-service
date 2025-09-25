package com.backbase.referral.utils;

import com.backbase.referral.config.AppConfiguration;
import com.backbase.referral.exceptions.ReferralCodeGenerationException;
import com.backbase.referral.repository.UserReferralRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReferralUtilsTest {

    @Mock
    UserReferralRepository userReferralRepository;

    @InjectMocks
    ReferralUtils referralUtils;

    @Test
    void test_generateReferralCodeUtil() {
        String code1 = referralUtils.generateReferralCode(AppConfiguration.ReferralPatternEnum.ALPHABETIC, null,
                "REF", 6, 3);
        Assertions.assertEquals(9, code1.length());
        Assertions.assertTrue(code1.startsWith("REF"));
        Assertions.assertTrue(code1.substring(3).chars().allMatch(Character::isAlphabetic));

        String code2 = referralUtils.generateReferralCode(AppConfiguration.ReferralPatternEnum.NUMERIC, null,
                "P", 6, 3);
        Assertions.assertEquals(7, code2.length());
        Assertions.assertTrue(code2.startsWith("P"));
        Assertions.assertTrue(code2.substring(1).chars().allMatch(Character::isDigit));

        String code3 = referralUtils.generateReferralCode(AppConfiguration.ReferralPatternEnum.ALPHANUMERIC, null,
                "", 6, 3);
        Assertions.assertEquals(6, code3.length());
        Assertions.assertTrue(code3.chars().allMatch(cp -> Character.isDigit(cp) || Character.isAlphabetic(cp)));


        String code4 = referralUtils.generateReferralCode(AppConfiguration.ReferralPatternEnum.REGEX, "[A-K0-2]+",
                "AG", 4, 3);
        Assertions.assertEquals(6, code4.length());
        Assertions.assertTrue(code4.startsWith("AG"));
        Assertions.assertTrue(code4.chars().allMatch(cp -> (cp >= 'A' && cp <= 'K') || (cp >= '0' && cp <= '2')));
    }

    @Test
    void test_generateReferralCodeUtil_maxAttemptsExceeded() {
        when(userReferralRepository.existsByReferralCode(anyString())).thenReturn(true);
        Assertions.assertThrows(ReferralCodeGenerationException.class, () ->
        referralUtils.generateReferralCode(AppConfiguration.ReferralPatternEnum.ALPHABETIC, null,
                "REF", 6, 2));
    }

    @Test
    void test_generateReferralQRString_invalid() {
        Assertions.assertThrows(ReferralCodeGenerationException.class, () -> {
           referralUtils.generateReferralQRString(null);
        });
    }
}
