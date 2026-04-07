package com.shinhan.esg_be.domain.environment.service.parser;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

// Azure 원본 JSON에서 필요한 값만 담아두는 중간 객체

@Getter
@Builder
public class ParsedEnvironmentData {

    // 이미지에서 추출된 텍스트를 줄 단위로 저장
    @Builder.Default
    private List<String> lines = new ArrayList<>();

    // 이미지 전체에서 읽어낸 순수 텍스트 전체
    private String rawText;

    // 텀블러 / 영수증
    private String merchantName;
    private String transactionDateText;
    private String transactionTimeText;
    private String totalAmountText;
    private String paymentMethod;

    // 공유자전거
    private String providerName;
    private String bikeQrId;
    private String completionText;
    private String rideTimeText;
    private String distanceText;
    private String finalAmountText;
    private String paymentDateTimeText;

    // 전기차
    private String vehicleModel;
    private String fuelType;
    private String carNumber;
    private String rentalPeriodText;
    private String rentalType;
    private String pickupLocation;
    private String returnLocation;
    private String prepaidAmountText;
    private String rentalFeeText;
    private String insuranceFeeText;
}