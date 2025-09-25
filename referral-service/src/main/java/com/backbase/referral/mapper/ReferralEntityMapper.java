package com.backbase.referral.mapper;

import com.backbase.referral.entity.UserReferral;
import com.backbase.referral.entity.UserReferralSubmission;
import com.backbase.referral.service.api.v1.model.ReferralCodeSubmissionRequestBody;
import com.backbase.referral.service.api.v1.model.ReferralGenerationResponseBody;
import com.backbase.referral.service.api.v1.model.ReferralSearchResponseEntry;
import com.backbase.referral.service.api.v1.model.ReferralSubmissionSearchResponseEntry;
import com.backbase.referral.utils.Constants;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReferralEntityMapper {
    
    ReferralGenerationResponseBody toReferralGenerationResponseBody(UserReferral userReferral);


    @Mapping(target = "createdDateTime", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "referralStatus", constant = Constants.ACTIVE)
    UserReferral toUserReferralEntity(String id,String userId, String referralCode, String referralQrString);

    @Mapping(target = "submittedReferralCode", source = "referralCodeSubmissionRequestBody.referralCode")
    @Mapping(target = "submissionSource", source = "referralCodeSubmissionRequestBody.source")
    @Mapping(target = "submissionStatus", constant = Constants.ACTIVE)
    @Mapping(target = "submissionDateTime", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "expiryDateTime", ignore = true)
    UserReferralSubmission toUserReferralSubmissionEntity(String id, String submittedUserId, ReferralCodeSubmissionRequestBody referralCodeSubmissionRequestBody, UserReferral referral);

    @Mapping(target = "totalSubmissions", expression = "java(userReferral.getSubmissions() != null ? userReferral.getSubmissions().size() : 0)")
    ReferralSearchResponseEntry toReferralSearchResponseEntry(UserReferral userReferral);


    @Mapping(target = "referredUserId", expression = "java(userReferralSubmission.getReferral() != null ? userReferralSubmission.getReferral().getUserId() : null)")
    @Mapping(target = "referralCodeStatus", expression = "java(userReferralSubmission.getReferral() != null ? userReferralSubmission.getReferral().getReferralStatus() : null)")
    ReferralSubmissionSearchResponseEntry toReferralSubmissionSearchResponseEntry(UserReferralSubmission userReferralSubmission);
}
