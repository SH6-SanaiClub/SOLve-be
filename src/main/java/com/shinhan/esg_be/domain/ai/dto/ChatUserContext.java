package com.shinhan.esg_be.domain.ai.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserContext {
    private String name;
    private int totalScore;
    private String grade;
    private int eScore;
    private int sScore;
    private int gScore;
    private long point;
    private int monthlyEScore;
    private int monthlySScore;
    private int monthlyGScore;
    private int nextGradeScore;
    private List<String> activeSavings;
    private boolean hasActiveLoan;
}
