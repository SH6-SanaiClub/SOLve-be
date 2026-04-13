package com.shinhan.esg_be.domain.activitystatus.repository;

import com.shinhan.esg_be.domain.score.entity.ExpiredScoreHistory;
import com.shinhan.esg_be.domain.score.entity.ValidScoreHistory;
import com.shinhan.esg_be.global.common.enums.ScoreCategory;
import com.shinhan.esg_be.global.common.enums.ScoreReason;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ActivityStatusQueryRepository {

    private final EntityManager entityManager;

    public List<ValidScoreHistory> findHistoriesInPeriod(
            Long userId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return entityManager.createQuery("""
                        select v
                        from ValidScoreHistory v
                        where v.user.userId = :userId
                          and v.createdAt >= :from
                          and v.createdAt < :to
                        order by v.createdAt desc, v.scoreId desc
                        """, ValidScoreHistory.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public List<ValidScoreHistory> findHistoriesByReasonsInPeriod(
            Long userId,
            LocalDateTime from,
            LocalDateTime to,
            Collection<ScoreReason> reasons
    ) {
        return entityManager.createQuery("""
                        select v
                        from ValidScoreHistory v
                        where v.user.userId = :userId
                          and v.createdAt >= :from
                          and v.createdAt < :to
                          and v.reason in :reasons
                        order by v.createdAt desc, v.scoreId desc
                        """, ValidScoreHistory.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("reasons", reasons)
                .getResultList();
    }

    public List<ExpiredScoreHistory> findExpiredHistoriesByValidUntilPeriod(
            Long userId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return entityManager.createQuery("""
                        select e
                        from ExpiredScoreHistory e
                        where e.user.userId = :userId
                          and e.validUntil >= :from
                          and e.validUntil < :to
                        order by e.validUntil desc, e.scoreId desc
                        """, ExpiredScoreHistory.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public List<ValidScoreHistory> findLogHistories(
            Long userId,
            LocalDateTime from,
            Collection<ScoreReason> reasons,
            Collection<ScoreCategory> categories,
            LocalDateTime cursorOccurredAt,
            Long cursorScoreId,
            int limit
    ) {
        StringBuilder jpql = new StringBuilder("""
                select v
                from ValidScoreHistory v
                where v.user.userId = :userId
                  and v.createdAt >= :from
                  and v.reason in :reasons
                """);

        if (categories != null && !categories.isEmpty()) {
            jpql.append("""
                      and v.category in :categories
                    """);
        }

        if (cursorOccurredAt != null && cursorScoreId != null) {
            jpql.append("""
                      and (v.createdAt < :cursorOccurredAt
                           or (v.createdAt = :cursorOccurredAt and v.scoreId < :cursorScoreId))
                    """);
        }

        jpql.append("""
                  order by v.createdAt desc, v.scoreId desc
                """);

        TypedQuery<ValidScoreHistory> query = entityManager.createQuery(jpql.toString(), ValidScoreHistory.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("reasons", reasons)
                .setMaxResults(limit);

        if (categories != null && !categories.isEmpty()) {
            query.setParameter("categories", categories);
        }

        if (cursorOccurredAt != null && cursorScoreId != null) {
            query.setParameter("cursorOccurredAt", cursorOccurredAt);
            query.setParameter("cursorScoreId", cursorScoreId);
        }

        return query.getResultList();
    }

    public long countLogHistories(
            Long userId,
            LocalDateTime from,
            Collection<ScoreReason> reasons,
            Collection<ScoreCategory> categories
    ) {
        StringBuilder jpql = new StringBuilder("""
                select count(v)
                from ValidScoreHistory v
                where v.user.userId = :userId
                  and v.createdAt >= :from
                  and v.reason in :reasons
                """);

        if (categories != null && !categories.isEmpty()) {
            jpql.append("""
                      and v.category in :categories
                    """);
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("reasons", reasons);

        if (categories != null && !categories.isEmpty()) {
            query.setParameter("categories", categories);
        }

        return query.getSingleResult();
    }
}
