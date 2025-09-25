package com.backbase.referral;

import com.backbase.buildingblocks.backend.security.auth.config.SecurityContextUtil;
import com.backbase.buildingblocks.jwt.internal.token.InternalJwt;
import com.backbase.buildingblocks.jwt.internal.token.InternalJwtClaimsSet;
import com.backbase.referral.entity.UserReferral;
import com.backbase.referral.entity.UserReferralSubmission;
import com.backbase.referral.utils.Constants;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

    public static List<UserReferral> getUserReferrals() {
        UserReferral userReferral1 = new UserReferral(UUID.randomUUID().toString(), "user-1", "REFUSER1",
                "", LocalDateTime.now(),null, Constants.ACTIVE, new ArrayList<>());
        UserReferral userReferral2 = new UserReferral(UUID.randomUUID().toString(), "user-2", "REFUSER2",
                "", LocalDateTime.now(),null, Constants.ACTIVE, new ArrayList<>());
        UserReferral userReferral3 = new UserReferral(UUID.randomUUID().toString(), "user-3", "REFUSER3",
                "", LocalDateTime.now(),null, Constants.ACTIVE, new ArrayList<>());
        UserReferral userReferral4 = new UserReferral(UUID.randomUUID().toString(), "user-4", "REFUSER4",
                "", LocalDateTime.now(),null, Constants.ACTIVE, new ArrayList<>());
        UserReferral userReferral5 = new UserReferral(UUID.randomUUID().toString(), "user-5", "REFUSER5",
                "", LocalDateTime.now(),null, Constants.ACTIVE, new ArrayList<>());
        return new ArrayList<>(List.of(userReferral1, userReferral2, userReferral3, userReferral4, userReferral5));
    }

    public static List<UserReferralSubmission> getUserReferralSubmissions() {
        UserReferralSubmission submission1 = new UserReferralSubmission(UUID.randomUUID().toString(), "user-1", "REFSUBUSER1",
                null, "", LocalDateTime.now(),null, Constants.ACTIVE);
        UserReferralSubmission submission2 = new UserReferralSubmission(UUID.randomUUID().toString(), "user-2", "REFSUBUSER2",
                null, "", LocalDateTime.now(),null, Constants.ACTIVE);
        UserReferralSubmission submission3 = new UserReferralSubmission(UUID.randomUUID().toString(), "user-3", "REFSUBUSER3",
                null, "", LocalDateTime.now(),null, Constants.ACTIVE);
        UserReferralSubmission submission4 = new UserReferralSubmission(UUID.randomUUID().toString(), "user-4", "REFUSER5",
                null, "", LocalDateTime.now(),null, Constants.ACTIVE);
        UserReferralSubmission submission5 = new UserReferralSubmission(UUID.randomUUID().toString(), "user-5", "REFUSER4",
                null, "", LocalDateTime.now(),null, Constants.ACTIVE);
        return new ArrayList<>(List.of(submission1, submission2, submission3, submission4, submission5));
    }

    public static void mockSecurityContext(SecurityContextUtil securityContextUtil) {
        Map<String,Object> claimsMap = new HashMap<>();
        InternalJwtClaimsSet internalJwtClaimsSet = mock(InternalJwtClaimsSet.class);
        when(internalJwtClaimsSet.getClaims()).thenReturn(claimsMap);
        when(securityContextUtil.getInternalId()).thenReturn(Optional.of("user-6"));
        InternalJwt internalJwt = mock(InternalJwt.class);
        when(internalJwt.getClaimsSet()).thenReturn(internalJwtClaimsSet);
        when(securityContextUtil.getOriginatingUserJwt()).thenReturn(Optional.of(internalJwt));
    }
}
