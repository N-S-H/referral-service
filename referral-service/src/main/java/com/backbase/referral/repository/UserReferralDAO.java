package com.backbase.referral.repository;

import com.backbase.referral.entity.UserReferral;
import com.backbase.referral.utils.Constants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
public class UserReferralDAO {

    @PersistenceContext
    private EntityManager entityManager;

    public List<UserReferral> referralCodeDetails(String userId, String referralCode, Boolean activeReferralCodeOnly, String startDate, String endDate,  Integer from, Integer size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        Pageable pageable =  PageRequest.of(from, size);
        CriteriaQuery<UserReferral> cq = cb.createQuery(UserReferral.class);
        Root<UserReferral> root = cq.from(UserReferral.class);

        List<Predicate> predicates = getPredicates(cb,root,userId,referralCode,activeReferralCodeOnly,startDate,endDate);
        cq.where(predicates.toArray(new Predicate[0]));

        cq.orderBy(cb.desc(root.get("createdDateTime")));

        return entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }

    public int referralCodeDetailsCount(String userId, String referralCode, Boolean activeReferralCodeOnly, String startDate, String endDate) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<UserReferral> root = cq.from(UserReferral.class);
        cq.select(cb.count(root));

        List<Predicate> predicates = getPredicates(cb,root,userId,referralCode,activeReferralCodeOnly,startDate,endDate);
        cq.where(predicates.toArray(new Predicate[0]));

        Long count = entityManager.createQuery(cq).getSingleResult();
        return count != null ? count.intValue() : 0;
    }

    private List<Predicate> getPredicates(CriteriaBuilder cb, Root<UserReferral> root, String userId, String referralCode, Boolean activeReferralCodeOnly, String startDate, String endDate) {
        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.isNotEmpty(userId)) {
            predicates.add(cb.equal(root.get("userId"), userId));
        }
        if (StringUtils.isNotEmpty(referralCode)) {
            predicates.add(cb.equal(root.get("referralCode"), referralCode));
        }
        if (Objects.nonNull(activeReferralCodeOnly) && activeReferralCodeOnly) {
            predicates.add(cb.equal(root.get("referralStatus"), Constants.ACTIVE));
        }

        if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
            LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            predicates.add(cb.between(root.get("createdDateTime"), startDateTime, endDateTime));
        }
        return predicates;
    }
}
