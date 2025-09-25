package com.backbase.referral.repository;

import com.backbase.referral.entity.UserReferral;
import com.backbase.referral.entity.UserReferralSubmission;
import com.backbase.referral.utils.Constants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
@Slf4j
@RequiredArgsConstructor
public class UserReferralSubmissionDAO {

    @PersistenceContext
    private EntityManager entityManager;


    public List<UserReferralSubmission> referralSubmissionDetails(String userId, String referralCode, String referralUserId, Boolean activeSubmissionsOnly, Boolean activeReferralCodeOnly, String startDate, String endDate, Integer from, Integer size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        Pageable pageable =  PageRequest.of(from, size);
        CriteriaQuery<UserReferralSubmission> cq = cb.createQuery(UserReferralSubmission.class);
        Root<UserReferralSubmission> root = cq.from(UserReferralSubmission.class);

        List<Predicate> predicates = getPredicates(cb,root,userId,referralCode,referralUserId,activeSubmissionsOnly,activeReferralCodeOnly,startDate,endDate);
        cq.where(predicates.toArray(new Predicate[0]));

        cq.orderBy(cb.desc(root.get("submissionDateTime")));

        return entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }



    public int referralSubmissionDetailsCount(String userId, String referralCode, String referralUserId, Boolean activeSubmissionsOnly, Boolean activeReferralCodeOnly, String startDate, String endDate) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<UserReferralSubmission> root = cq.from(UserReferralSubmission.class);
        cq.select(cb.count(root));

        List<Predicate> predicates = getPredicates(cb,root,userId,referralCode,referralUserId,activeSubmissionsOnly,activeReferralCodeOnly,startDate,endDate);
        cq.where(predicates.toArray(new Predicate[0]));

        Long count = entityManager.createQuery(cq).getSingleResult();
        return count != null ? count.intValue() : 0;
    }

    private List<Predicate> getPredicates(CriteriaBuilder cb, Root<UserReferralSubmission> root, String userId, String referralCode, String referralUserId, Boolean activeSubmissionsOnly, Boolean activeReferralCodeOnly, String startDate, String endDate) {
        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.isNotEmpty(userId)) {
            predicates.add(cb.equal(root.get("submittedUserId"), userId));
        }
        if (StringUtils.isNotEmpty(referralCode)) {
            predicates.add(cb.equal(root.get("submittedReferralCode"), referralCode));
        }

        if(StringUtils.isNotEmpty(referralUserId)) {
            predicates.add(cb.equal(root.join("referral", JoinType.INNER).get("userId"), referralUserId));
        }

        if(Objects.nonNull(activeReferralCodeOnly) && activeReferralCodeOnly) {
            predicates.add(cb.equal(root.join("referral", JoinType.INNER).get("referralStatus"), "ACTIVE"));
        }

        if(Objects.nonNull(activeSubmissionsOnly) && activeSubmissionsOnly) {
            predicates.add(cb.equal(root.get("submissionStatus"), "ACTIVE"));
        }

        if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
            LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            predicates.add(cb.between(root.get("submissionDateTime"), startDateTime, endDateTime));
        }
        return predicates;
    }
}
