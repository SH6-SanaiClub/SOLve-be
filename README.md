# esg-be

ESG 활동 기반 대안 신용 평가 플랫폼 백엔드입니다.

## Profiles

- 기본 프로필은 `dev` 입니다.
- 서버 배포 시에는 `SPRING_PROFILES_ACTIVE=prod` 를 명시해야 합니다.

## Runtime Notes

- `dev` 프로필은 DB 커넥션 풀을 작게 유지하고 scheduler/batch 를 기본 비활성화합니다.
- `prod` 프로필은 Redis 컨테이너 기준 설정을 사용하고 scheduler/batch 를 활성화합니다.
