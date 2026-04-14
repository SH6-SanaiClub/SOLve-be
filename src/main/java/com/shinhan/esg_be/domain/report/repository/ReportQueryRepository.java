package com.shinhan.esg_be.domain.report.repository;

import com.shinhan.esg_be.domain.volunteer.entity.enums.VolunteerStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReportQueryRepository {

    private final EntityManager entityManager;

    public long countApprovedEnvironmentActivities(Long userId, LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("""
                        select count(ua)
                        from UserEnvironmentActivity ua
                        where ua.user.userId = :userId
                          and ua.isApproved = true
                          and ua.createdAt >= :from
                          and ua.createdAt < :to
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long countCompletedVolunteerActivities(Long userId, LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("""
                        select count(uv)
                        from UserVolunteer uv
                        where uv.user.userId = :userId
                          and uv.status = :completedStatus
                          and uv.volunteer.activityDate >= :from
                          and uv.volunteer.activityDate < :to
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("completedStatus", VolunteerStatus.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public int sumCompletedVolunteerHours(Long userId, LocalDateTime from, LocalDateTime to) {
        Long result = entityManager.createQuery("""
                        select coalesce(sum(uv.volunteerHour), 0L)
                        from UserVolunteer uv
                        where uv.user.userId = :userId
                          and uv.status = :completedStatus
                          and uv.volunteer.activityDate >= :from
                          and uv.volunteer.activityDate < :to
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("completedStatus", VolunteerStatus.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        return result == null ? 0 : Math.toIntExact(result);
    }

    public long countDonations(Long userId, LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("""
                        select count(ud)
                        from UserDonation ud
                        where ud.user.userId = :userId
                          and ud.createdAt >= :from
                          and ud.createdAt < :to
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long countEcoProductPurchases(Long userId, LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("""
                        select count(uep)
                        from UserEcoProduct uep
                        where uep.user.userId = :userId
                          and uep.createdAt >= :from
                          and uep.createdAt < :to
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public long countQuizActivities(Long userId, LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("""
                        select count(uq)
                        from UserQuiz uq
                        where uq.user.userId = :userId
                          and uq.createdAt >= :from
                          and uq.createdAt < :to
                        """, Long.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public LocalDate findFirstActivityDate(Long userId) {
        List<LocalDateTime> candidates = new ArrayList<>();

        addIfNotNull(candidates, entityManager.createQuery("""
                        select min(ua.createdAt)
                        from UserEnvironmentActivity ua
                        where ua.user.userId = :userId
                          and ua.isApproved = true
                        """, LocalDateTime.class)
                .setParameter("userId", userId)
                .getSingleResult());

        addIfNotNull(candidates, entityManager.createQuery("""
                        select min(uv.volunteer.activityDate)
                        from UserVolunteer uv
                        where uv.user.userId = :userId
                          and uv.status = :completedStatus
                        """, LocalDateTime.class)
                .setParameter("userId", userId)
                .setParameter("completedStatus", VolunteerStatus.COMPLETED)
                .getSingleResult());

        addIfNotNull(candidates, entityManager.createQuery("""
                        select min(ud.createdAt)
                        from UserDonation ud
                        where ud.user.userId = :userId
                        """, LocalDateTime.class)
                .setParameter("userId", userId)
                .getSingleResult());

        addIfNotNull(candidates, entityManager.createQuery("""
                        select min(uep.createdAt)
                        from UserEcoProduct uep
                        where uep.user.userId = :userId
                        """, LocalDateTime.class)
                .setParameter("userId", userId)
                .getSingleResult());

        addIfNotNull(candidates, entityManager.createQuery("""
                        select min(uq.createdAt)
                        from UserQuiz uq
                        where uq.user.userId = :userId
                        """, LocalDateTime.class)
                .setParameter("userId", userId)
                .getSingleResult());

        return candidates.stream()
                .min(LocalDateTime::compareTo)
                .map(LocalDateTime::toLocalDate)
                .orElse(null);
    }

    private void addIfNotNull(List<LocalDateTime> candidates, LocalDateTime value) {
        if (value != null) {
            candidates.add(value);
        }
    }
}
