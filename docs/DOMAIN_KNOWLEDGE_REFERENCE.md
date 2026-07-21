# 지식 카탈로그

Commerce Context MCP에는 6개 도메인, 70개 규칙이 포함되어 있습니다. 이 문서는 어떤 지식을 찾을 수 있는지 보여 주는 안내서이며, 실제 응답의 단일 원본은 [`src/main/resources/knowledge`](../src/main/resources/knowledge)의 YAML입니다.

## 읽는 방법

규칙 ID는 MCP의 `get_rule` 도구와 `commerce://rules/{ruleId}` Resource에서 사용합니다. 먼저 자연어로 `search_knowledge`를 호출하고, 선택된 ID의 전체 내용을 조회하는 흐름을 권장합니다.

각 규칙은 다음 메타데이터를 가질 수 있습니다.

| 항목 | 의미 |
|---|---|
| `severity` | 누락됐을 때의 일반적인 영향도. 실제 서비스 상황에 따라 달라질 수 있음 |
| `applicability` | 적용 조건, 적용 제외 조건, 전제와 관할 범위 |
| `status: active` | 카탈로그에서 일반 설계 지침으로 사용 중. 외부 검증 완료를 뜻하지 않음 |
| `status: needs-review` | 고위험이거나 출처·검토일·적용 조건을 추가 확인해야 함 |
| `status: deprecated` | 새 규칙 또는 다른 접근으로 교체 중 |
| `evidenceLevel: external-source` | URL을 가진 명시적 외부 근거가 있음 |
| `evidenceLevel: project-guidance` | 프로젝트가 관리하는 지침이며 독립 외부 근거는 아님 |
| `evidenceLevel: unsourced` | 근거가 없어 사용 전 보완 필요 |
| `owner` | 규칙을 유지할 책임이 있는 팀 또는 역할 |
| `verifiedBy` | 내용을 실제로 검토한 주체. 확인되지 않았다면 기록하지 않음 |
| `lastReviewedAt` | 실제 기록된 검토일. 자동 생성하지 않음 |
| `sourceVersion`, `accessedAt` | 외부 자료의 버전과 실제 확인일 |
| `changeNote` | 규칙을 변경한 이유 |

> `severity`가 높다는 이유만으로 특정 구현을 기계적으로 적용해서는 안 됩니다. 적용 조건과 제외 조건을 먼저 확인하고 현재 트래픽, 저장소, PG 계약, 장애 복구 방식에 맞춰 선택하세요.

현재 기본 YAML 지침은 별도 외부 evidence가 첨부되지 않은 경우 `project-guidance`로 노출됩니다. High·Critical 규칙은 applicability, evidence, verified-by, last-reviewed-at이 모두 명시되지 않으면 자동으로 `needs-review`가 됩니다. 이는 규칙을 숨기는 대신 불확실성을 사용자에게 전달하기 위한 정책입니다. 검색의 `matchConfidence`는 질문과 규칙의 일치도이며 근거 수준과는 별개입니다.

## 재고 `inventory` — 8개

| 규칙 ID | 주제 |
|---|---|
| `stock-reservation` | 재고 예약·확정·복구 단계 분리 |
| `available-stock-query` | 실재고·예약 재고를 반영한 가용 재고 조회 |
| `concurrency-optimistic` | 충돌이 낮은 구간의 낙관 락 |
| `concurrency-pessimistic` | 충돌이 높은 구간의 비관 락과 교착상태 주의 |
| `concurrency-distributed` | 분산 락의 적용 조건, DB 안전망과 fencing |
| `idempotency` | 중복 요청 방어와 결과 재사용 |
| `saga-pattern` | 주문·결제·재고 사이의 보상 흐름 |
| `ai-pitfalls-checklist` | 재고 구현 누락 점검표 |

핵심 방향: “MSA면 분산 락”처럼 구조만 보고 기술을 고르지 않습니다. DB 조건부 갱신과 유일 제약을 먼저 비교하고, 여러 인스턴스의 외부 작업까지 직렬화해야 할 때 분산 락을 검토합니다.

## 결제 `payment` — 7개

| 규칙 ID | 주제 |
|---|---|
| `webhook-handling` | 웹훅 서명 검증, 내구성 있는 인수, 비동기 후처리 |
| `payment-status-machine` | PENDING·UNCERTAIN·PAID 등 상태 전이와 가드 |
| `duplicate-payment-guard` | 상태·멱등성 키·PG 거래 ID를 통한 중복 결제 방어 |
| `network-cancellation` | 응답 불명확 상황의 조회와 상태 확정 |
| `partial-refund` | 주문 라인별 할인 배분 스냅샷과 반올림 정책 |
| `payment-idempotency` | 같은 논리 시도의 키 재사용과 새 시도의 구분 |
| `payment-ai-pitfalls-checklist` | 결제 구현 누락 점검표 |

핵심 방향: 웹훅 성공 응답은 “받자마자 무조건 200”이 아닙니다. 출처와 서명을 확인하고 inbox 또는 queue에 내구성 있게 저장한 뒤, 사용하는 PG의 공식 계약에 맞는 상태 코드와 제한시간으로 응답해야 합니다.

## 정산 `settlement` — 9개

| 규칙 ID | 주제 |
|---|---|
| `settlement-timing` | 구매 확정과 정산 기준일 |
| `settlement-deduction` | 수수료·반품·분담금 등 공제 항목 |
| `settlement-cycle` | 정산 배치, 페이징, 멱등성과 재시작 |
| `settlement-integrity` | 금액·건수·중복·누락·타임존 대사 |
| `settlement-statement` | 확정 시점의 요율과 금액 스냅샷 |
| `settlement-tax` | 부가세·원천징수·면세 구분 시 확인할 경계 |
| `settlement-hold` | 분쟁·클레임 정산 보류와 해제 이력 |
| `settlement-payout` | 정산 확정과 실제 이체의 분리·멱등성 |
| `settlement-ai-pitfalls-checklist` | 정산 구현 누락 점검표 |

`settlement-tax`는 `needs-review` 규칙입니다. 사업자 유형, 거래 구조, 과세 구분, 증빙 주체와 적용 시점에 따라 달라지므로 현재 법령·국세청 자료·계약과 세무 전문가 검토가 필요합니다. 고정 세율이나 보존 기간을 이 프로젝트의 문장만으로 결정하지 마세요.

## 쿠폰·프로모션 `coupon` — 6개

| 규칙 ID | 주제 |
|---|---|
| `coupon-validation` | 기간·대상·최소 금액·중복·상태 검증 |
| `coupon-discount-calculation` | 정률·정액·상한·라인 배분과 반올림 |
| `coupon-issuance` | 선착순 발급의 원자적 재고·사용자 중복 검사 |
| `coupon-issuance-compensation` | Redis 선점, DB 확정, 보상과 재처리 |
| `promotion-rules` | 규칙 우선순위와 조합 가능성 |
| `coupon-ai-pitfalls-checklist` | 쿠폰·프로모션 구현 누락 점검표 |

핵심 방향: Redis의 `DECR`, `SETNX`, `SADD`를 별도 명령으로 이어 붙이면 중간 실패가 원자성을 깨뜨릴 수 있습니다. 쿠폰 수량 확인·차감과 사용자 중복 검사를 하나의 Lua script에서 처리하고 DB의 유일 제약을 최종 안전망으로 둡니다.

## 커머스 공통 `commerce` — 20개

| 규칙 ID | 주제 |
|---|---|
| `commerce-catalog-model` | Product·SKU·Offer 분리 |
| `commerce-pricing-money` | 금액 타입과 계산 근거 스냅샷 |
| `commerce-order-lifecycle` | 주문 상태 전이와 이력 |
| `commerce-inventory-availability` | 실재고·예약·판매 가능 수량 |
| `commerce-payment-ledger` | 주문 상태와 결제 거래 원장 분리 |
| `commerce-fulfillment-reverse-logistics` | 배송과 반품 역물류 |
| `commerce-promotion-allocation` | 혜택 계산·배부·사용 이력 |
| `commerce-channel-seller-distribution` | 원장과 채널별 노출 모델 |
| `commerce-settlement-reconciliation` | 거래 원장 기반 정산과 대사 |
| `commerce-operational-integrity` | 이벤트·배치·수동 조치 추적 |
| `commerce-customer-identity` | 회원·비회원·수신자 식별자 |
| `commerce-cart-checkout` | 임시 견적과 확정 주문 분리 |
| `commerce-search-discovery` | 검색 인덱스와 원장 분리 |
| `commerce-claim-cs` | 취소·교환·반품·환불 상태와 책임 |
| `commerce-security-privacy` | 최소 수집·권한 경계·감사 |
| `commerce-loyalty-point-ledger` | 포인트 잔액과 적립·사용 원장 |
| `commerce-membership-tier-benefits` | 등급 산정과 혜택 적용 시점 |
| `commerce-review-ugc-moderation` | 리뷰 권한·노출·신고 처리 |
| `commerce-subscription-recurring-order` | 반복 결제와 반복 주문 분리 |
| `commerce-ops-slo-incident` | 고객 영향 기반 SLO와 장애 대응 |

## Java/Spring `spring-commerce` — 20개

| 규칙 ID | 주제 |
|---|---|
| `spring-commerce-modular-monolith` | 모듈형 모놀리스 시작 구조 |
| `spring-commerce-transaction-boundary` | 짧고 명확한 트랜잭션 경계 |
| `spring-commerce-jpa-consistency` | 엔티티와 조회 모델 분리 |
| `spring-commerce-locking-idempotency` | 동시성·멱등성과 DB 제약 |
| `spring-commerce-validation-errors` | 입력 검증과 비즈니스 오류 분리 |
| `spring-commerce-events-outbox` | Outbox와 멱등 Consumer |
| `spring-commerce-cache-boundary` | 원장과 Redis 가속 계층 분리 |
| `spring-commerce-scheduler-batch` | 배치 중복 실행과 재시작 |
| `spring-commerce-testing-observability` | 불변식 테스트와 관측성 |
| `java-domain-modeling` | 의미 있는 값 타입과 도메인 모델 |
| `java-null-exception-boundary` | null과 예외 계약 |
| `java-collections-streams` | 대량 컬렉션과 Stream 부작용 |
| `spring-security-authz` | 인증·인가·소유권 검증 분리 |
| `spring-configuration-secrets` | 환경 설정과 비밀 값 분리 |
| `spring-schema-migration` | 무중단 스키마 변경 |
| `spring-api-pagination-idempotent-post` | 페이지네이션과 멱등 POST |
| `spring-database-index-isolation` | 인덱스·격리 수준과 정합성 |
| `java-concurrency-executor-completablefuture` | Executor와 비동기 경계 |
| `spring-resilience-retry-timeout` | timeout·retry·circuit breaker |
| `spring-deployment-health-rollout` | readiness·점진 배포·롤백 |

## 품질과 책임 범위

카탈로그는 일반적인 설계 실수를 줄이기 위한 검토 기준입니다. 다음 사항을 대신하지 않습니다.

- 사용하는 PG·결제대행·은행·물류사의 최신 공식 연동 문서
- 세무·회계·법률 전문가의 판단
- 실제 트래픽과 장애 목표를 반영한 부하·복구 테스트
- 개인정보 영향평가, 보안 감사와 침투 테스트
- 코드 전체 흐름, DB 제약, 배포 환경을 포함한 사람의 리뷰

내용이 단정적으로 보이거나 현재 계약과 다르면 issue를 만들 때 규칙 ID, 적용 상황, 확인한 공식 근거와 기준일을 함께 적어 주세요.
