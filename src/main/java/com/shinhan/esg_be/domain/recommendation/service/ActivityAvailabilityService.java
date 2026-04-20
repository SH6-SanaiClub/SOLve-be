package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.quiz.repository.UserQuizRepository;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.recommendation.dto.UserFeatureDto;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import com.shinhan.esg_be.domain.environment.repository.UserEnvironmentActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityAvailabilityService {

    public static final String BLOCKED_MONTHLY_LIMIT_REACHED = "MONTHLY_LIMIT_REACHED";
    public static final String BLOCKED_PHOTO_ALREADY_ATTEMPTED_TODAY = "PHOTO_ALREADY_ATTEMPTED_TODAY";
    public static final String BLOCKED_DONATION_ALREADY_PARTICIPATED_TODAY = "DONATION_ALREADY_PARTICIPATED_TODAY";
    public static final String BLOCKED_VOLUNTEER_ALREADY_APPLIED = "VOLUNTEER_ALREADY_APPLIED";
    public static final String BLOCKED_QUIZ_ALREADY_PARTICIPATED_TODAY = "QUIZ_ALREADY_PARTICIPATED_TODAY";
    public static final String BLOCKED_DONATION_INACTIVE = "DONATION_INACTIVE";
    public static final String BLOCKED_DONATION_CLOSED = "DONATION_CLOSED";
    public static final String BLOCKED_VOLUNTEER_INACTIVE = "VOLUNTEER_INACTIVE";
    public static final String BLOCKED_VOLUNTEER_CLOSED = "VOLUNTEER_CLOSED";
    public static final String BLOCKED_PURCHASE_INACTIVE = "PURCHASE_INACTIVE";
    public static final String BLOCKED_PURCHASE_CLOSED = "PURCHASE_CLOSED";

    private final UserEnvironmentActivityRepository userEnvironmentActivityRepository;
    private final UserDonationRepository userDonationRepository;
    private final UserVolunteerRepository userVolunteerRepository;
    private final UserQuizRepository userQuizRepository;
    private final RecommendationPolicyService recommendationPolicyService;

    public ActivityAvailabilityStatus evaluate(ActivityCandidateDto candidate, UserFeatureDto feature) {
        return evaluate(candidate, feature, true);
    }

    public ActivityAvailabilityStatus evaluate(
            ActivityCandidateDto candidate,
            UserFeatureDto feature,
            boolean enforceMonthlyLimit
    ) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        boolean monthlyLimitReached = isMonthlyLimitReached(candidate, feature);
        ActivityAvailabilityStatus status = switch (candidate.getActivityType()) {
            case "PHOTO" -> evaluatePhoto(candidate, feature.getUserId(), startOfDay, monthlyLimitReached);
            case "DONATION" -> evaluateDonation(candidate, feature.getUserId(), today, startOfDay, monthlyLimitReached);
            case "VOLUNTEER" -> evaluateVolunteer(candidate, feature.getUserId(), today, monthlyLimitReached);
            case "PURCHASE" -> evaluatePurchase(candidate, today, monthlyLimitReached);
            case "QUIZ" -> evaluateQuiz(feature.getUserId(), startOfDay, monthlyLimitReached);
            default -> new ActivityAvailabilityStatus(false, false, monthlyLimitReached, "UNKNOWN_ACTIVITY_TYPE");
        };
        if (enforceMonthlyLimit && monthlyLimitReached) {
            return new ActivityAvailabilityStatus(
                    false,
                    status.alreadyParticipatedToday(),
                    true,
                    BLOCKED_MONTHLY_LIMIT_REACHED
            );
        }
        return status;
    }

    private boolean isMonthlyLimitReached(ActivityCandidateDto candidate, UserFeatureDto feature) {
        int monthlyMax = recommendationPolicyService.getMonthlyMaxScore(candidate.getScoreCategory());
        if (monthlyMax <= 0) {
            return false;
        }

        int currentScore = switch (candidate.getScoreCategory()) {
            case "E" -> feature.getMonthlyEScore();
            case "S" -> feature.getMonthlySScore();
            case "G" -> feature.getMonthlyGScore();
            default -> 0;
        };
        return currentScore >= monthlyMax;
    }

    private ActivityAvailabilityStatus evaluatePhoto(
            ActivityCandidateDto candidate,
            Long userId,
            LocalDateTime startOfDay,
            boolean monthlyLimitReached
    ) {
        boolean attemptedToday = userEnvironmentActivityRepository
                .countTodayAttempts(userId, candidate.getReferenceId(), startOfDay) > 0;
        return new ActivityAvailabilityStatus(
                !attemptedToday,
                attemptedToday,
                monthlyLimitReached,
                attemptedToday ? BLOCKED_PHOTO_ALREADY_ATTEMPTED_TODAY : null
        );
    }

    private ActivityAvailabilityStatus evaluateDonation(
            ActivityCandidateDto candidate,
            Long userId,
            LocalDate today,
            LocalDateTime startOfDay,
            boolean monthlyLimitReached
    ) {
        if (!candidate.isActive()) {
            return new ActivityAvailabilityStatus(false, false, monthlyLimitReached, BLOCKED_DONATION_INACTIVE);
        }
        if (candidate.getDeadlineDate() != null && candidate.getDeadlineDate().isBefore(today)) {
            return new ActivityAvailabilityStatus(false, false, monthlyLimitReached, BLOCKED_DONATION_CLOSED);
        }

        boolean donatedToday = userDonationRepository
                .countTodayByDonation(userId, candidate.getReferenceId(), startOfDay) > 0;
        return new ActivityAvailabilityStatus(
                !donatedToday,
                donatedToday,
                monthlyLimitReached,
                donatedToday ? BLOCKED_DONATION_ALREADY_PARTICIPATED_TODAY : null
        );
    }

    private ActivityAvailabilityStatus evaluateVolunteer(
            ActivityCandidateDto candidate,
            Long userId,
            LocalDate today,
            boolean monthlyLimitReached
    ) {
        if (!candidate.isActive()) {
            return new ActivityAvailabilityStatus(false, false, monthlyLimitReached, BLOCKED_VOLUNTEER_INACTIVE);
        }
        if (candidate.getDeadlineDate() != null && !candidate.getDeadlineDate().isAfter(today)) {
            return new ActivityAvailabilityStatus(false, false, monthlyLimitReached, BLOCKED_VOLUNTEER_CLOSED);
        }

        boolean alreadyApplied = userVolunteerRepository
                .existsByUser_UserIdAndVolunteer_VolunteerId(userId, candidate.getReferenceId());
        return new ActivityAvailabilityStatus(
                !alreadyApplied,
                false,
                monthlyLimitReached,
                alreadyApplied ? BLOCKED_VOLUNTEER_ALREADY_APPLIED : null
        );
    }

    private ActivityAvailabilityStatus evaluatePurchase(
            ActivityCandidateDto candidate,
            LocalDate today,
            boolean monthlyLimitReached
    ) {
        if (!candidate.isActive()) {
            return new ActivityAvailabilityStatus(false, false, monthlyLimitReached, BLOCKED_PURCHASE_INACTIVE);
        }
        if (candidate.getDeadlineDate() != null && candidate.getDeadlineDate().isBefore(today)) {
            return new ActivityAvailabilityStatus(false, false, monthlyLimitReached, BLOCKED_PURCHASE_CLOSED);
        }
        return new ActivityAvailabilityStatus(true, false, monthlyLimitReached, null);
    }

    private ActivityAvailabilityStatus evaluateQuiz(
            Long userId,
            LocalDateTime startOfDay,
            boolean monthlyLimitReached
    ) {
        boolean participatedToday = userQuizRepository.countToday(userId, startOfDay) > 0;
        return new ActivityAvailabilityStatus(
                !participatedToday,
                participatedToday,
                monthlyLimitReached,
                participatedToday ? BLOCKED_QUIZ_ALREADY_PARTICIPATED_TODAY : null
        );
    }
}
