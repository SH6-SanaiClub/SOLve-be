package com.shinhan.esg_be.domain.recommendation.service;

import com.shinhan.esg_be.domain.activity.entity.Activity;
import com.shinhan.esg_be.domain.activity.repository.ActivityRepository;
import com.shinhan.esg_be.domain.policy.entity.ActivityRewardPolicy;
import com.shinhan.esg_be.domain.policy.repository.ActivityRewardPolicyRepository;
import com.shinhan.esg_be.domain.quiz.repository.QuizRepository;
import com.shinhan.esg_be.domain.recommendation.dto.ActivityCandidateDto;
import com.shinhan.esg_be.domain.social.entity.Donation;
import com.shinhan.esg_be.domain.social.entity.EcoProduct;
import com.shinhan.esg_be.domain.social.repository.DonationRepository;
import com.shinhan.esg_be.domain.social.repository.EcoProductRepository;
import com.shinhan.esg_be.domain.volunteer.entity.Volunteer;
import com.shinhan.esg_be.domain.volunteer.repository.VolunteerRepository;
import com.shinhan.esg_be.global.common.enums.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityCandidateLoader {

    private final ActivityRepository activityRepository;
    private final DonationRepository donationRepository;
    private final VolunteerRepository volunteerRepository;
    private final EcoProductRepository ecoProductRepository;
    private final QuizRepository quizRepository;
    private final ActivityRewardPolicyRepository activityRewardPolicyRepository;

    private Map<ActivityType, ActivityRewardPolicy> loadPolicyMap() {
        return activityRewardPolicyRepository.findAll().stream()
                .collect(Collectors.toMap(
                        ActivityRewardPolicy::getActivityType,
                        p -> p,
                        (a, b) -> a
                ));
    }

    public List<ActivityCandidateDto> loadAll(LocalDateTime now) {
        Map<ActivityType, ActivityRewardPolicy> policyMap = loadPolicyMap();
        List<ActivityCandidateDto> candidates = new ArrayList<>();

        ActivityRewardPolicy photoPolicy = policyMap.get(ActivityType.PHOTO);
        if (photoPolicy != null) {
            List<Activity> activities = activityRepository.findAll();
            for (Activity a : activities) {
                candidates.add(ActivityCandidateDto.builder()
                        .activityType(ActivityType.PHOTO.name())
                        .referenceId(a.getActivityId())
                        .name(a.getName())
                        .scoreCategory("E")
                        .scoreValue(photoPolicy.getScoreValue())
                        .pointValue(photoPolicy.getPointValue() != null ? photoPolicy.getPointValue() : 0)
                        .pointRate(photoPolicy.getPointRate() != null ? photoPolicy.getPointRate().doubleValue() : 0.0)
                        .difficultyIndex(1.5)
                        .isActive(true)
                        .deadlineDate(null)
                        .todayCount(0)
                        .recent14DayCount(0)
                        .build());
            }
        }

        ActivityRewardPolicy donationPolicy = policyMap.get(ActivityType.DONATION);
        if (donationPolicy != null) {
            List<Donation> donations = donationRepository.findActiveDonations(now);
            for (Donation d : donations) {
                candidates.add(ActivityCandidateDto.builder()
                        .activityType(ActivityType.DONATION.name())
                        .referenceId(d.getDonationId())
                        .name(d.getName())
                        .scoreCategory("S")
                        .scoreValue(donationPolicy.getScoreValue())
                        .pointValue(donationPolicy.getPointValue() != null ? donationPolicy.getPointValue() : 0)
                        .pointRate(donationPolicy.getPointRate() != null ? donationPolicy.getPointRate().doubleValue() : 0.0)
                        .difficultyIndex(4.0)
                        .isActive(Boolean.TRUE.equals(d.getIsActive()))
                        .deadlineDate(d.getEndDate() != null ? d.getEndDate().toLocalDate() : null)
                        .todayCount(0)
                        .recent14DayCount(0)
                        .build());
            }
        }

        ActivityRewardPolicy volunteerPolicy = policyMap.get(ActivityType.VOLUNTEER);
        if (volunteerPolicy != null) {
            List<Volunteer> volunteers = volunteerRepository.findActiveVolunteers(now);
            for (Volunteer v : volunteers) {
                candidates.add(ActivityCandidateDto.builder()
                        .activityType(ActivityType.VOLUNTEER.name())
                        .referenceId(v.getVolunteerId())
                        .name(v.getName())
                        .scoreCategory("S")
                        .scoreValue(volunteerPolicy.getScoreValue())
                        .pointValue(volunteerPolicy.getPointValue() != null ? volunteerPolicy.getPointValue() : 0)
                        .pointRate(volunteerPolicy.getPointRate() != null ? volunteerPolicy.getPointRate().doubleValue() : 0.0)
                        .difficultyIndex(3.0)
                        .isActive(Boolean.TRUE.equals(v.getIsActive()))
                        .deadlineDate(v.getActivityDate() != null ? v.getActivityDate().toLocalDate() : null)
                        .todayCount(0)
                        .recent14DayCount(0)
                        .build());
            }
        }

        ActivityRewardPolicy purchasePolicy = policyMap.get(ActivityType.PURCHASE);
        if (purchasePolicy != null) {
            List<EcoProduct> products = ecoProductRepository.findByIsActiveTrue();
            for (EcoProduct p : products) {
                candidates.add(ActivityCandidateDto.builder()
                        .activityType(ActivityType.PURCHASE.name())
                        .referenceId(p.getProductId())
                        .name(p.getName())
                        .scoreCategory("S")
                        .scoreValue(purchasePolicy.getScoreValue())
                        .pointValue(purchasePolicy.getPointValue() != null ? purchasePolicy.getPointValue() : 0)
                        .pointRate(purchasePolicy.getPointRate() != null ? purchasePolicy.getPointRate().doubleValue() : 0.0)
                        .difficultyIndex(2.5)
                        .isActive(Boolean.TRUE.equals(p.getIsActive()))
                        .deadlineDate(null)
                        .todayCount(0)
                        .recent14DayCount(0)
                        .build());
            }
        }

        ActivityRewardPolicy quizPolicy = policyMap.get(ActivityType.QUIZ);
        if (quizPolicy != null) {
            quizRepository.findTodayActiveQuiz(now).ifPresent(q ->
                    candidates.add(ActivityCandidateDto.builder()
                            .activityType(ActivityType.QUIZ.name())
                            .referenceId(q.getQuizId())
                            .name("오늘의 금융 퀴즈")
                            .scoreCategory("G")
                            .scoreValue(quizPolicy.getScoreValue())
                            .pointValue(quizPolicy.getPointValue() != null ? quizPolicy.getPointValue() : 0)
                            .pointRate(0.0)
                            .difficultyIndex(1.0)
                            .isActive(Boolean.TRUE.equals(q.getIsActive()))
                            .deadlineDate(null)
                            .todayCount(0)
                            .recent14DayCount(0)
                            .build())
            );
        }

        return candidates;
    }
}
