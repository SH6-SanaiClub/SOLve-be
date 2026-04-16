package com.shinhan.esg_be.domain.admin.service;

import com.shinhan.esg_be.domain.admin.dto.response.AdminDashboardResponse;
import com.shinhan.esg_be.domain.bank.entity.enums.LoanStatus;
import com.shinhan.esg_be.domain.bank.entity.enums.SavingStatus;
import com.shinhan.esg_be.domain.bank.repository.UserLoanRepository;
import com.shinhan.esg_be.domain.bank.repository.UserSavingRepository;
import com.shinhan.esg_be.domain.social.repository.UserDonationRepository;
import com.shinhan.esg_be.domain.social.repository.UserEcoProductRepository;
import com.shinhan.esg_be.domain.user.entity.enums.Grade;
import com.shinhan.esg_be.domain.user.repository.UserRepository;
import com.shinhan.esg_be.domain.volunteer.repository.UserVolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final UserDonationRepository userDonationRepository;
    private final UserVolunteerRepository userVolunteerRepository;
    private final UserEcoProductRepository userEcoProductRepository;
    private final UserLoanRepository userLoanRepository;
    private final UserSavingRepository userSavingRepository;
    private final Clock clock;

    public AdminDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextMonthStart = monthStart.plusMonths(1);

        return new AdminDashboardResponse(
                userRepository.count(),
                userRepository.countCreatedAtBetween(todayStart, tomorrowStart),
                userRepository.countCreatedAtBetween(weekStart, tomorrowStart),
                userRepository.countByIsActiveTrue(),
                buildGradeDistribution(),
                userDonationRepository.countCreatedAtBetween(monthStart, nextMonthStart),
                userDonationRepository.sumPaymentAmountCreatedAtBetween(monthStart, nextMonthStart),
                userVolunteerRepository.countCreatedAtBetween(monthStart, nextMonthStart),
                userEcoProductRepository.countCreatedAtBetween(monthStart, nextMonthStart),
                userLoanRepository.countByStatus(LoanStatus.ACTIVE),
                userLoanRepository.sumPrincipalAmountByStatus(LoanStatus.ACTIVE),
                userSavingRepository.countByStatus(SavingStatus.ACTIVE)
        );
    }

    private Map<String, Long> buildGradeDistribution() {
        Map<String, Long> gradeDistribution = new LinkedHashMap<>();
        for (Grade grade : Grade.values()) {
            gradeDistribution.put(grade.name(), 0L);
        }

        userRepository.countGroupByCurrentGrade()
                .forEach(result -> gradeDistribution.put(result.getCurrentGrade().name(), result.getCount()));

        return gradeDistribution;
    }
}
