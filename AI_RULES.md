# AI_RULES

이 파일은 변경 전에 알아두면 좋은 레포 전용 정보를 요약합니다.

## 프로젝트 기본
- 스택: Spring Boot (4.0.5), Java 17, Gradle.
- 기본 패키지: `com.shinhan.esg_be`.
- 엔트리 포인트: `EsgBeApplication` (`@EnableJpaAuditing` 사용).

## 런타임 설정
- `src/main/resources/application.yml`에서 `.env`를 불러오며 다음 값을 매핑:
    - PostgreSQL용 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- 서버 포트: 8080.
- 프로필:
    - `dev`: SQL 로그 활성화, Redis `localhost:6379`.
    - `prod`: SQL 로그 비활성화, Redis `esg-redis:6379`.

## 데이터/인프라
- JPA 엔티티 경로: `src/main/java/com/shinhan/esg_be/domain/**/entity`.
- 기본 DB는 PostgreSQL, Redis는 캐시/데이터 용도.

## 빌드/실행
- 빌드: `./gradlew build` (Windows: `gradlew.bat build`).
- 실행: `./gradlew bootRun` (Windows: `gradlew.bat bootRun`).
- 테스트: `./gradlew test`.

## API 참고
- 예시 컨트롤러: `src/main/java/com/shinhan/esg_be/init/TestController.java`에서 `/api/test` 제공.

---

# 기획 정리

## 회원가입/로그인

### 회원가입

#### 1. 목적
- 법적 신원 확보: 대안 신용평가 및 금융 혜택 제공을 위한 실명 기반 식별자(CI/DI) 확보.
- 부정 가입 방지: 1인 1계정 원칙으로 점수 조작 및 중복 수혜 방지.

#### 2. 가입 흐름
1. 가입 시작: 기본 필수 정보 입력.
2. 약관 동의: 개인신용정보 조회 및 제3자 제공 동의.
3. 통합 인증(필수): PASS/카카오/토스 중 선택하여 본인 확인.
4. 가입 완료: 통합 유저 정보 + 초기 ESG 점수(500점) 저장 후 대시보드 진입.

#### 3. 입력 데이터
- 아이디(중복확인)
- 비밀번호(해싱)
- 전화번호
- 생년월일
- 이름
- 이메일
- 법적 동의: 필수 약관 동의

포트원 연동 후 인증 성공 여부만 반환받음.

### 로그인
- 아이디/비밀번호 입력 후 로그인.
- JWT: 로그인 성공 시 토큰 발행.
    - 기본: 브라우저(LocalStorage 등) 저장.
    - 보안 강화 시: HttpOnly Cookie 저장.

## ESG 활동 정의 및 인증

### E 활동
**활동 선정 기준:** 탄소중립포인트 사이트에 정의된 활동 중 텍스트(결제내역/날짜/업체)로 자동 검증 가능한 활동만 선별.

#### E 활동 정의
- 텀블러 사용 인증, 공유 자전거 사용 인증, 전기차 대여 인증.
- 하루 1점, 월 최대 10점(월 10회 인증 가능).
- 3가지 E 활동은 하루에 각각 1회씩 인증 가능.

#### E 활동 인증
- 전자 영수증 캡처 업로드.
- 날짜가 명시된 전자 영수증만 인정.
- 인증 당일 영수증만 인정.

**AI 활용 후보**
- Azure Document Intelligence (Receipt 모델)
- CLOVA Document OCR (유료 Basic 기준 월 18,000원/기본 300건)
- Tesseract OCR (텍스트 추출 후 검증 로직 필요)

### S 활동

#### S 활동 정의
1) 가치가게: 친환경/일자리 창출 등 사회적 가치 기업 상품관 구매 활동.
2) 기부: 최소금액 3만원.
3) 봉사: 단체 공고/상품 등록 신청 → 플랫폼 선별 게시.

#### S 활동 인증
- 기부/가치가게: 결제 완료 시 인증 완료.
- 봉사: 공고 상세에서 신청 후, 단체가 참여 인증 시 완료.

**봉사 인증 방식**
- 봉사일 경과 시 자동 인증.
- 노쇼는 봉사 업체 확인 후 관리자가 패널티 부여.

### G 활동

#### G 활동 정의
- 출석체크 겸 금융/윤리 관련 퀴즈.
- 하루 1회 가능.

#### G 활동 인증
- 퀴즈 참여 시 G 1점 획득.

## ESG 신용 점수 변환 로직

### 1. 목적
- ESG 활동 데이터를 기반으로 대안 신용평가 점수 산출.

### 2. 점수 체계
- 최소 0점, 최대 1000점.
- 기본 점수: E 50점, S 250점, G 200점 (총 500점).

### 3. 데이터 인정 기준
- 최근 1년 이내 활동만 반영.

### 4. 항목별 점수 구조
- 비중: E 10%, S 50%, G 40%(G 활동 20% + 대출 상환 20%).
- 최대 점수: E 100점, S 500점, G 400점(G 활동 200 + 대출 상환 200).

### 5. 점수 산정 로직
- 기부: S +20점
- 봉사: S +5점
- 상품 구매: S +10점
- 퀴즈 참여: G +1점
- 사진 인증: E +1점
- 대출 상환: G +100점

### 6. 월별 점수 획득 제한
- E: 월 최대 5점
- S: 월 최대 25점
- G(대출 상환 제외): 월 최대 10점
- 월 총 획득 최대 40점
- 매월 1일 00시 초기화

### 7. 연속 만점 보너스
- E 3개월 연속 최대 달성: +10점
- S 3개월 연속 최대 달성: +50점
- G 3개월 연속 최대 달성: +20점
- 만점 미달 시 연속 달성 실패

### 8. 패널티 정책
- 노쇼/어뷰징 적발 시: E -5점, S -25점, G -20점 + 대출 불가 전환.
- 기본 점수도 50점 감소.
- 1개월 무활동 시: E -2점, S -4점, G -4점.
- 점수는 기본 점수 이하로 하락하지 않음.

## ESG 등급

등급 네이밍: 자연/성장 테마.

| 등급명 | 코드 | 점수 구간 |
| --- | --- | --- |
| 씨앗 | Seed | 0~599 |
| 새싹 | Sprout | 600~699 |
| 나무 | Tree | 700~799 |
| 숲 | Forest | 800~899 |
| 지구 | Earth | 900~1000 |

## 포인트 적립 로직

ESG 포인트 적립 가이드 (1P = 1원)

### 1. 일반 활동 적립 (단일 수행)
- 봉사활동 완료(S): 1회 완료 시 정액 1,000P
- 기부 수행(S): 결제 금액의 3% 포인트 페이백
- 친환경 상품 구매(S): 결제 금액의 1% 포인트 페이백
- ESG 사진 인증(E): 1회당 30P
- ESG 퀴즈 참여(G): 정답 20P, 오답 10P(1일 1회 제한)

### 2. 지속 활동 보너스 (추가 적립)
- 봉사 5번 달성 시 1,000P 추가
- 7일 연속 사진 인증 시 300P 추가
- 한 달 연속 퀴즈 참여 시 1,000P 추가

### 3. 운영 및 패널티 원칙
- 페이백 포인트는 소수 첫째 자리에서 반올림.
- 한도: 누적/월별 상한선 없음(신용점수와 별개).
- 어뷰징 적발 시 부당 적립분 전액 환수(마이너스 처리), 정상 포인트는 유지.

## 포인트샵

### 기능 소개
- ESG 활동 포인트로 취업 준비/자기계발 지원 서비스 이용.

### 기능 목적
- 초기: 취업·역량 강화에 직접 도움이 되는 서비스 제공 → 활동 참여 유도.
- 장기: 금융 신뢰 축적 및 혜택으로 연결.

### 포인트샵 상품 종류
- 자격증: 오픽 등 시험 응시료 상품권
- 교육: 인프런 등 교육 플랫폼 강의 상품권
- 기타: 이력서 사진 촬영/면접 의상 대여 상품권

## 대출/적금 금융상품

### 대출 상품
**상품명:** ESG 소액대출

**상품 개요:**
- 금융 이력이 부족한 2030 씬파일러에게 공정한 금융 기회 제공.
- ACSS(대안 신용평가 시스템) 기반 우량 고객 발굴.

**점수별 조건**
- 700점 이상: 한도 100만원, 연 8.5%
- 800점 이상: 한도 200만원, 연 7.0%
- 900점 이상: 한도 300만원, 연 6.0%
- 기존 대출 상환 전 추가 대출 불가

### 적금 상품 (납입 30만원, 만기 1년 고정)

1) 그린 스텝업 적금
- 금리: 기본 2.0% + 우대 최대 3.0% = 최고 5.0%
- 우대: 가입 시점 ESG 점수 기준, 50점 상승마다 다음 달 1.0% 추가

2) 지구 수호대 적금 (E)
- 금리: 기본 3.0% + 우대 최대 2.0% = 최고 5.0%
- 우대: 매월 E 목표 달성 시 다음 달 추가 금리

3) 따뜻한 동행 적금 (S)
- 금리: 기본 3.0% + 우대 최대 4.0% = 최고 7.0%
- 우대: 기부/사회적 소비 결제 기록이 매월 유지될 때 0.5%씩 제공

4) 바른 금융 스마트 적금 (G)
- 금리: 기본 3.0% + 우대 최대 2.5% = 최고 5.5%
- 우대: 매월 G 퀴즈 목표 달성 시 0.1% 추가(만기 유지 시 +0.3%),
  만기까지 연체/패널티 0건 유지 시 추가 1.0%

5) ESG 마스터 적금 (청년 지원금 연동)
- 금리: 기본 4.0% + 만기 900점 유지 우대 6.0% = 최고 10.0%
- 우대: 가입 월부터 만기 월까지 매월 1일 기준 900점 이상 유지
- 페널티: 만기 전 899점 이하로 1회라도 하락 시 10% 혜택 소멸,
  기본 4.0%만 적용

## AI 추천 로직 및 챗봇

### 설문 및 사용자 유형 분류

#### 사용자 유형 4가지

| 유형 | 이름 | 관심 영역 | 초기 추천 전략 |
| --- | --- | --- | --- |
| 유형 1 | 그린 실천가 | E 중심(환경) | E 활동 우선(텀블러 인증, 전자영수증 등) |
| 유형 2 | 소셜 서포터 | S 중심(사회) | 기부 캠페인, 사회적 기업 상품관 우선 |
| 유형 3 | 금융 빌더 | G 중심(자기관리) | 적금 상품, 일일 퀴즈, 출석 챌린지 우선 |
| 유형 4 | 올라운더 | 균형형/미정 | E/S/G 고르게, 난이도 낮은 활동부터 안내 |

#### 유형별 챗봇 톤 예시
- 그린 실천가: "요즘 텀블러 사용하고 계시죠? 이번 주 인증하면 Sprout 등급까지 15점 남았어요!"
- 소셜 서포터: "이번 달 인기 기부 캠페인이 있어요. 참여하면 S 점수 +10점이에요!"
- 금융 빌더: "5일 연속 출석 중이에요! 7일 달성하면 보너스 점수가 붙어요."
- 올라운더: "아직 어떤 활동이 맞을지 모르시죠? 가장 쉬운 것부터 시작해볼게요."

### 설문 질문
- 회원가입 직후 또는 첫 로그인 시 3문항(1분 이내).

Q1. 평소 환경 관련 활동을 하고 있거나, 관심이 있나요?
- (1) 적극적으로 하고 있다 → E 가중치 +2
- (2) 관심은 있지만 실천은 못 하고 있다 → E 가중치 +1
- (3) 별로 관심 없다 → E 가중치 +0

Q2. 기부나 봉사, 사회적 가치 소비에 관심이 있나요?
- (1) 정기적으로 하고 있다 → S 가중치 +2
- (2) 기회가 되면 하고 싶다 → S 가중치 +1
- (3) 별로 관심 없다 → S 가중치 +0

Q3. 금융 목표를 세우고 달성하는 걸 좋아하나요?
- (1) 적금/저축 목표를 세우고 실천 중이다 → G 가중치 +2
- (2) 목표를 세우고 싶지만 아직 못 했다 → G 가중치 +1
- (3) 별로 관심 없다 → G 가중치 +0

**유형 분류 로직**
- E/S/G 가중치 중 가장 높은 카테고리로 분류.
- 동점이면 올라운더.
- 이후 실제 행동 패턴 기반으로 자동 업데이트.

### AI 기능 정리 (우선순위)

#### 1순위(필수)
| 기능 | 엔진 | LLM 역할 | 설명 |
| --- | --- | --- | --- |
| ESG 활동 추천 | 가중 점수 기반 랭킹 | 추천 이유 자연어 설명 | 대시보드 추천 카드 2~3개, 클릭 시 활동 페이지 이동 |
| 금융 상품 추천 | 등급/상품 상태 기반 규칙 | 혜택 설명/가입 타이밍 조언 | 사용자 상태 기반 추천 |
| AI 챗봇 | 의도 분류 + 추천 엔진 활용 | 자연어 대화 | 바텀 시트/플로팅 버튼으로 접근 |

#### 2순위(필수)
| 기능 | 엔진 | LLM 역할 | 설명 |
| --- | --- | --- | --- |
| ESG 활동 보고서 | 활동 데이터 집계 | 활동 요약 문단 생성 | PDF 다운로드, 취업 포트폴리오 활용 |

### 메인 대시보드 + 챗봇 구조

**메인 대시보드(로그인 직후)**
- 상단: 현재 ESG 등급/점수, 다음 등급까지 남은 점수
- 중단: AI 추천 활동 카드 2~3개(클릭 시 활동 페이지 이동)
- 하단: 금융 상품 추천 카드 1개 + 점수 변화 요약

**AI 챗봇(바텀 시트/플로팅 버튼)**
- 대시보드 추천 카드와 같은 추천 데이터 공유
- 추천/점수 조회/금융 상담 통합 대응

**핵심**
- 백엔드에서 추천 결과를 1회 생성 → 대시보드/챗봇 공통 사용

## 마이페이지

### 1. 프로필 및 계정 관리
- 기본 정보 조회
- 비밀번호 변경: 현재 비밀번호 확인 → 새 비밀번호 입력 → 확인
- 금융 정보: 마이데이터 연동 영역

### 2. ESG 점수 및 등급
- 현재 ESG 등급/점수: Sprout(32점) 형태로 상단 노출
- 등급 변화 이력: 월별 등급 변화 타임라인(Seed → Sprout → Tree)
- 점수 획득 내역: 최근 N건 점수 변동 로그(날짜, 활동명, +N점)

### 3. ESG 활동 내역
- 전체 활동 로그: 날짜순 정렬, 카테고리(E/S/G) 필터
- 카테고리별 통계: 이번 달 E/S/G 활동 건수

### 4. 금융 상품 관리
- 가입 적금: 상품명/가입일/현재 금리/납입 현황/만기일/예상 수익
- 가입 대출: 상품명/대출 금액/현재 금리/상환 스케줄/남은 원금/다음 상환일
- 금리 갱신 정보: "다음 금리 갱신까지 23일 / 현재 예상 등급: Tree → Forest(금리 -0.6%p 예상)"
- 미가입 시: 추천 상품 카드 노출(AI 추천 연동)

### 5. 포인트 관리 (2순위)
- 현재 보유 포인트
- 포인트 적립/사용 내역(날짜, 사유, +/- 포인트)
- 포인트 사용처 바로가기(ESG 상품관/리워드 상품관)

### 6. ESG 활동 보고서 (2순위)
- 보고서 생성: 기간 선택(1개월/3개월/전체) → PDF 생성 → 다운로드
- 과거 생성 보고서 목록

## 관리자

### 1. 대시보드(관리자 메인)
- 전체 사용자 수/신규 가입자 수(일별/주별)
- 활동 현황 요약: 오늘 활동 건수, 카테고리별 비율
- ESG 등급 분포: 등급별 인원 수
- 인증 대기 건수: OCR 자동 인증 실패로 수동 확인 필요 건수

### 2. 사용자 관리
- 사용자 목록: 검색(이름/이메일), 필터(유저 유형/ESG 등급), 페이지네이션
- 사용자 상세: 프로필, 점수/등급, 활동 이력, 금융 상품, 포인트 잔액
- 사용자 상태 관리: 계정 활성/비활성

### 3. ESG 활동 관리
- 활동 마스터 CRUD: 활동명/카테고리/기본 점수/포인트/난이도/비용/유형/일일 한도/활성 여부
- 인증 관리: OCR 실패 건 수동 승인/반려(인증 사진 + OCR 텍스트 확인)

### 4. ESG 상품관 관리
- 상품 CRUD: 상품명/카테고리/가격(포인트)/이미지/설명/재고/활성 여부
- 주문/구매 내역 조회
- 실제 배송/정산은 불필요(모의 결제)

### 5. 기부 캠페인 관리
- 캠페인 CRUD: 캠페인명/설명/목표 금액/현재 모금액/시작일/마감일/이미지/활성 여부
- 참여 현황: 참여자 수/총 모금액/참여자 목록

### 6. 봉사 캠페인 관리
- 캠페인 CRUD: 봉사명/설명/모집 인원/현재 신청 인원/활동 일시/장소/마감일/활성 여부
- 참여 관리: 신청자 목록, 참여 상태(신청/참여완료)
- 종료일 기준 자동 참여완료이므로 수동 승인/거절 불필요

### 7. 리워드 상품관 관리 (2순위)
- 리워드 CRUD: 리워드명/필요 포인트/이미지/설명/수량/활성 여부
- 교환 내역 조회

### 8. 금융 상품 관리
- 상품 CRUD: 상품명/유형(적금/대출)/기본 금리/등급별 우대금리 테이블/최소 금액/기간/활성 여부
- 가입 현황: 상품별 가입자 수/총 가입 금액
- 이자 계산/상환 로직은 백엔드 서비스 레이어에서 처리

### 9. AI/퀴즈 관리 (3순위)
- 퀴즈 목록 조회: 날짜별 자동 생성 퀴즈 확인(문제/선택지/정답)
- 퀴즈 수동 등록/수정: AI 생성 퀴즈 부적절 시 수정/신규 등록
- 퀴즈 참여 통계: 일별 참여율/정답률

---

# 코드 컨벤션

## 클래스/DTO 네이밍
```
Controller  : 도메인 + Controller        → UserController
Service     : 도메인 + Service           → UserService
Repository  : 도메인 + Repository        → UserRepository
Entity      : 단수형                      → User
Request DTO : 도메인 + 행위 + Request    → UserCreateRequest
Response DTO: 도메인 + Response          → UserResponse
Exception   : 원인 + Exception           → UserNotFoundException
Enum        : PascalCase                 → UserStatus
```

- Enum 값: UPPER_SNAKE_CASE
- DTO는 행위 기반으로 명확히 (`Create`, `Update`)

## 메서드
```
getXxx()  → 반드시 존재, 없으면 Exception
findXxx() → Optional 반환 가능

CRUD:
getUser(id)       → 단건 조회
findUserByEmail() → 선택 조회
getUsers()        → 목록 조회
createUser(req)   → 생성
updateUser(id,req)→ 수정
deleteUser(id)    → 삭제
```

## Controller
- RESTful URL: GET/POST/PUT/PATCH/DELETE
- 반환: `ResponseEntity<T>`
- Entity 직접 반환 금지 → DTO 사용
- 비즈니스 로직 X → Service 호출
- 요청 검증: `@Valid`

예시:
```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid UserCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req));
    }
}
```

## Service
- 인터페이스는 필요 시만 사용
- 트랜잭션:
    - 클래스: `@Transactional(readOnly = true)`
    - 쓰기 메서드: `@Transactional`

## Repository
- `JpaRepository` 상속
- 복잡 쿼리: QueryDSL / `@Query`
- N+1 방지: `@EntityGraph` 또는 fetch join
- 페이징: `Pageable`

## Entity
- Enum: `@Enumerated(EnumType.STRING)`
- Setter 금지 → 도메인 메서드 사용
- 정적 팩토리 메서드 권장
- BaseEntity 상속: 생성/수정 시간 기록

예시:
```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String email;
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    public static User create(String name, String email) {
        User u = new User();
        u.name = name;
        u.email = email;
        u.status = UserStatus.ACTIVE;
        return u;
    }
}
```

## 예외 처리 / Optional
- CustomException + ErrorCode Enum 사용
- Optional은 반환용만 사용, `orElseThrow()` 활용

## 변수 / 상수 / 로그
```
변수/필드 : camelCase
상수      : UPPER_SNAKE_CASE
메서드    : camelCase 동사 시작
클래스    : PascalCase

로그      : @Slf4j + {} 파라미터, 레벨 DEBUG/INFO/WARN/ERROR
System.out.println 금지
```

예시:
```java
log.info("User created: {}", userId);
log.error("Failed to create user: {}", email, e);
```

---

# DB 테이블 구조 요약

아래는 제공된 SQL을 기준으로 정리한 테이블/컬럼 요약이다.

## 사용자/인증
- `user`: 사용자 기본 정보(로그인/연락처/CI-DI), ESG 점수(E/S/G), 등급, 포인트, 활동일, 활성/연동 여부.
- `survey`: 설문 문항(질문/3개 선택지).
- `quiz`: 퀴즈(문항/선택지 JSON/정답/활성 여부).
- `user_quiz`: 사용자 퀴즈 참여 로그(정답 여부/답변/일시).

## 활동/인증
- `activity`: ESG 활동 마스터.
- `user_activity`: 활동 인증 로그(승인 여부, OCR 텍스트, 관리자 코멘트).
- `volunteer`: 봉사 공고/모집 정보.
- `user_volunteer`: 봉사 신청/이력(상태: APPLIED/COMPLETED/NOSHOW, 봉사 시간).

## S 활동(기부/상품)
- `donation`: 기부 캠페인(목표/현재 모금, 기간, 활성).
- `user_donation`: 사용자 기부 로그.
- `eco_product`: 친환경 상품.
- `user_eco_product`: 사용자 구매 로그.

## 포인트/리워드
- `item`: 포인트 교환 상품.
- `user_point`: 포인트 변동 로그(카테고리, 증감, 잔액).
- `payment`: 결제 정보(결제 수단/상태/카드/영수증 URL).

## 금융
- `financial_product`: 금융 상품(대출/적금, 금리, 기간).
- `user_loan`: 사용자 대출(원금/금리/상태/상환일).
- `loan_history`: 대출 상환 이력.
- `user_saving`: 사용자 적금(납입/만기/상태/점수).
- `saving_history`: 적금 납입 이력.
- `saving_prime_history`: 우대 금리 적용 이력.

## 점수/정책/통계
- `activity_reward_policy`: 활동별 점수/포인트 정책.
- `esg_score_policy`: ESG 점수 정책(기본 점수/월 최대/연속 보너스).
- `penalty_policy`: 패널티 정책(감점/대출 제한).
- `valid_score_history`: 유효 점수 이력(1년 유효).
- `expired_score_history`: 만료 점수 이력.
- `user_monthly_stat`: 월별 점수/연속 최대 달성 통계.

## 주요 Enum/제약 요약
- `user.user_type`: GREEN/SOCIAL/FINANCE/ALL-ROUNDER
- `user.current_grade`: SEED/SPROUT/TREE/FOREST/EARTH
- `user_loan.status`, `user_saving.status`: ACTIVE/COMPLETE
- `user_volunteer.status`: APPLIED/COMPLETED/NOSHOW
- `activity_reward_policy.score_category`: E/S/G
- `valid_score_history.category`: E/S/G-ACTIVITY/G-REPAYMENT
- `valid_score_history.reason`: DONATION/VOLUNTEER/PURCHASE/QUIZ/PHOTO/ABUSE/NO_ACTIVITY/CONSECUTIVE_BONUS/LOAN_REPAY

---

# API 명세 요약

표 형식으로 제공한 명세를 읽기 쉬운 목록으로 정리했다.

## 인증/계정
- AUTH-01: ID 중복 확인 `GET /api/v1/auth/check-id`
    - Req: `{ loginId }` / Res: `{ isAvailable }` / Status: 200, 409
    - 비고: 가입 폼 실시간 중복 체크(Unique 여부 반환)
- AUTH-02: 회원가입 `POST /api/v1/auth/signup`
    - Req: `{ loginId, password, ... }` / Res: `{ userId, message }` / Status: 201, 400
    - 비고: BCrypt 암호화, 필수 약관 동의 저장
- AUTH-03: 본인인증 완료 `POST /api/v1/auth/verify-identity`
    - Req: `{ impUid, merchantUid }` / Res: `{ verified, initialScore }` / Status: 200, 401
    - 비고: 포트원 연동, 성공 시 초기 500점 부여
- AUTH-04: 로그인 `POST /api/v1/auth/login`
    - Req: `{ loginId, password }` / Res: `{ accessToken, user }` / Status: 200, 401
    - 비고: JWT 발급 및 쿠키 저장, Redis 세션 생성
- AUTH-05: 로그아웃 `POST /api/v1/auth/logout`
    - Req: `{}` / Res: `{ message }` / Status: 204
    - 비고: 토큰 무효화 및 쿠키 삭제

## 마이페이지
- MYP-01: 활동 이력 조회 `GET /api/v1/mypage/activities`
    - Req: `?category=E` / Res: `{ totalCount, activities: [] }` / Status: 200
    - 비고: 카테고리(E/S/G) 필터링 필수, 최신순 정렬(Pagination)
- MYP-02: 포인트 내역 조회 `GET /api/v1/mypage/points`
    - Req: `?type=all` / Res: `{ currentPoint, history: [] }` / Status: 200
    - 비고: 적립/차감 구분값 포함, 소멸 예정 포인트 계산 포함
- MYP-03: 활동 보고서 생성 `POST /api/v1/mypage/report`
    - Req: `{ year, month }` / Res: `{ reportUrl, summaryData }` / Status: 201, 422
    - 비고: 월별 활동 집계 → PDF 업로드 후 URL 반환
- MYP-04: 내 정보 수정 `POST /api/v1/mypage/profile`
    - Req: `{ nickname, phone }` / Res: `{ status, updatedAt }` / Status: 200, 400
    - 비고: 닉네임 중복 체크, 전화번호 유효성 검사
- MYP-05: 금융 상품 조회 `GET /api/v1/mypage/finance`
    - Req: `-` / Res: `{ loans: [], savings: [] }` / Status: 200
    - 비고: 가입 중인 상품만 필터링(금리/만기일 포함)
- MYP-06: 포인트 충전/전환 `POST /api/v1/mypage/points/exchange`
    - Req: `{ amount, source }` / Res: `{ balance, txId }` / Status: 200, 402
    - 비고: 외부 포인트 연동 시 트랜잭션 처리(오류 시 롤백)
- MYP-07: 등급 산정 정보 조회 `GET /api/v1/mypage/grade-info`
    - Req: `-` / Res: `{ currentGrade, criteria, nextPoint }` / Status: 200
    - 비고: 등급별 혜택 및 다음 등급까지 점수 Gap 계산

## 온보딩/AI
- ONB-01: 설문 응답 제출 `POST /api/v1/survey/submit`
    - Req: `{ answers[] }` / Res: `{ message }` / Status: 201
    - 비고: 가입 후 최초 1회, Raw 데이터 저장
- ONB-02: AI 맞춤 추천 `GET /api/v1/chat/recommend`
    - Req: `-` / Res: `{ recommendations[] }` / Status: 200
    - 비고: 점수/유형 기반 LLM 개인화 추천
- CHAT-01: AI 챗봇 상담 `POST /api/v1/chat/messages`
    - Req: `{ message }` / Res: `{ reply }` / Status: 200
    - 비고: 자연어 질의응답(SSE 가능)

## ESG 활동
- ESG-E01: 영수증 업로드 `POST /api/v1/esg/e/receipts`
    - Req: `multipart file` / Res: `{ receiptId }` / Status: 201, 429
    - 비고: S3 업로드, 1일 1회 제한
- ESG-E02: OCR 검증/지급 `POST /api/v1/esg/e/verify`
    - Req: `{ receiptId }` / Res: `{ verified, score, point }` / Status: 200, 400
    - 비고: OCR 비교 후 점수/포인트 지급
- ESG-S01: 기부/봉사/상품 목록 조회 `GET /api/v1/esg/s/list`
    - Req: `?category=all` / Res: `{ items: [{id, title, type...}] }` / Status: 200
    - 비고: 기부처/상품/봉사 공고 통합 조회
- ESG-S02: 기부 상세 조회 `GET /api/v1/esg/s/donations/{id}`
    - Req: `-` / Res: `{ id, title, currentAmount, goalAmount }` / Status: 200, 404
    - 비고: 기부처 상세 및 현황
- ESG-S03: 상품 목록 조회 `GET /api/v1/esg/s/products`
    - Req: `?page=1` / Res: `{ products: [{id, name, price...}] }` / Status: 200
    - 비고: 사회적 기업 상품(가치가게) 리스트
- ESG-S04: 상품 상세 조회 `GET /api/v1/esg/s/products/{id}`
    - Req: `-` / Res: `{ id, name, price, stock, description }` / Status: 200, 404
    - 비고: 상품 상세/재고
- ESG-S05: 봉사 목록 조회 `GET /api/v1/esg/s/volunteers`
    - Req: `?region=seoul` / Res: `{ volunteers: [{id, title, date...}] }` / Status: 200
    - 비고: 지역/기간별 필터링
- ESG-S06: 봉사활동 신청 `POST /api/v1/esg/s/volunteers/apply`
    - Req: `{ volunteerId }` / Res: `{ message, applicationId }` / Status: 201, 403, 409
    - 비고: 모집 인원 체크 후 신청
- ESG-S07: 활동 완료 결과 조회 `GET /api/v1/esg/s/result/{id}`
    - Req: `-` / Res: `{ type, rewardPoint, totalScore }` / Status: 200, 404
    - 비고: 구매/기부/봉사 완료 후 보상 확인
- ESG-G01: 금융 퀴즈 조회 `GET /api/v1/esg/g/quiz/today`
    - Req: `-` / Res: `{ question, ... }` / Status: 200, 429
    - 비고: LLM 생성 및 참여 여부 확인
- ESG-G02: 퀴즈 결과 제출 `POST /api/v1/esg/g/quiz/submit`
    - Req: `{ quizId, answer }` / Res: `{ correct, score, point }` / Status: 200
    - 비고: 정답 확인 및 보상 지급

## 금융/커머스
- FIN-01: 금융 상품 조회 `GET /api/v1/finance/list`
    - Req: `?type=loan` / Res: `{ products: [{id, name, rate...}] }` / Status: 200
    - 비고: ESG 등급 기반 우대금리 계산
- FIN-02: 대출 신청 `POST /api/v1/finance/loans/apply`
    - Req: `{ loanId, amount }` / Res: `{ status }` / Status: 201, 403
    - 비고: 대출 원장 생성
- FIN-03: 적금 가입 `POST /api/v1/finance/savings/apply`
    - Req: `{ productId }` / Res: `{ status }` / Status: 201
    - 비고: 가입 시점 점수 기록
- SHOP-01: 포인트 상품 조회 `GET /api/v1/shop/products`
    - Req: `-` / Res: `{ products[] }` / Status: 200
    - 비고: 포인트 샵 리스트
- SHOP-02: 상품 구매 `POST /api/v1/shop/orders`
    - Req: `{ productId, quantity }` / Res: `{ orderId }` / Status: 201, 402
    - 비고: 포인트 차감 및 재고 제어
- SHOP-03: 구매 내역 조회 `GET /api/v1/shop/history`
    - Req: `?page=1` / Res: `{ orders: [{orderId, name, code...}] }` / Status: 200
    - 비고: 구매한 기프티콘/교환권 번호 목록

## 관리자
- ADM-01: 통계 대시보드 `GET /api/v1/admin/stats`
    - Req: `-` / Res: `{ totalUsers, ... }` / Status: 200, 403
    - 비고: 전체 지표 통계
- ADM-02: OCR 수동 심사 `POST /api/v1/admin/receipts/review`
    - Req: `{ receiptId, approved }` / Res: `{ message }` / Status: 200
    - 비고: 승인/반려 처리
- ADM-03: 어뷰징 제재 `POST /api/v1/admin/users/ban`
    - Req: `{ userId, reason }` / Res: `{ message }` / Status: 200
    - 비고: 계정 정지 및 패널티
- ADM-04: 콘텐츠 관리 `POST /api/v1/admin/contents/create`
    - Req: `{ type, title }` / Res: `{ contentId }` / Status: 201
    - 비고: 상품/공고/퀴즈 등록

## 메인
- DASH-01: 대시보드 통합 조회 `GET /api/v1/main/dashboard`
    - Req: `-` / Res: `{ userName, userGrade, currentScore, progressRate, availablePoint, weeklyActivity[] }` / Status: 200, 401
    - 비고:
        - 현재 등급/점수/포인트 요약 반환
        - 이번 주(일~토) 활동 성공 여부 boolean 배열 반환
        - 다음 등급까지 남은 점수 및 게이지 % 계산
