package com.backbase.referral.mapper;

import com.backbase.referral.client.api.v1.model.ReferralGenerationResponseBody;
import com.backbase.referral.client.api.v1.model.ReferralSearchResponseBody;
import com.backbase.referral.client.api.v1.model.ReferralSubmissionSearchResponseBody;
import com.backbase.referral.service.api.v1.model.ReferralCodeSubmissionRequestBody;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReferralModelMapper {


    ReferralGenerationResponseBody mapToGenerateReferralCodeResponse(com.backbase.referral.service.api.v1.model.ReferralGenerationResponseBody referralGenerationResponseBody);

    ReferralCodeSubmissionRequestBody mapToSubmissionRequestBody(com.backbase.referral.client.api.v1.model.ReferralCodeSubmissionRequestBody referralCodeSubmissionRequestBody);

    ReferralSearchResponseBody mapToSerachReferralCodeResponse(com.backbase.referral.service.api.v1.model.ReferralSearchResponseBody referralSearchResponseBody);

    ReferralSubmissionSearchResponseBody mapToSearchReferralCodeSubmissionResponse(com.backbase.referral.service.api.v1.model.ReferralSubmissionSearchResponseBody referralSubmissionSearchResponseBody);
}
