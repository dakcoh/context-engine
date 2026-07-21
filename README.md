# Commerce Context MCP

[![npm version](https://img.shields.io/npm/v/commerce-context-mcp)](https://www.npmjs.com/package/commerce-context-mcp)
[![CI](https://github.com/dakcoh/commerce-context-mcp/actions/workflows/ci.yml/badge.svg)](https://github.com/dakcoh/commerce-context-mcp/actions/workflows/ci.yml)
[![license](https://img.shields.io/npm/l/commerce-context-mcp)](LICENSE)

한국 커머스 백엔드에서 자주 놓치는 설계 기준을 AI 코딩 도구가 찾아보고 점검할 수 있게 해 주는 MCP 서버입니다. 재고, 결제, 정산, 쿠폰뿐 아니라 주문·배송·회원·보안과 Java/Spring 구현 지식을 함께 제공합니다.

> 이 프로젝트의 답변은 프로젝트가 관리하는 설계 지침입니다. 외부 근거가 확인된 사실과 동일하지 않습니다. PG사 계약, 세금·정산 규정, 개인정보·전자금융 관련 요구사항은 적용 시점의 공식 문서와 전문가 검토로 확인하세요.

## 5분 안에 연결하기

필요한 환경은 Node.js 20 이상과 Java 17 이상입니다. Java는 Amazon Corretto 17을 권장하지만 다른 Java 17 배포판도 사용할 수 있습니다.

```powershell
node -v
java -version
npx -y commerce-context-mcp doctor
```

Claude Code에서는 다음 한 줄로 등록합니다.

```bash
claude mcp add commerce-context -- npx -y commerce-context-mcp
```

Cursor나 Claude Desktop 등 JSON 설정을 사용하는 클라이언트에는 다음 서버를 추가하고 프로그램을 다시 시작합니다.

```json
{
  "mcpServers": {
    "commerce-context": {
      "command": "npx",
      "args": ["-y", "commerce-context-mcp"]
    }
  }
}
```

처음 실행할 때 현재 npm 버전과 일치하는 서버 JAR를 내려받고 SHA-256 체크섬을 확인한 뒤 사용자 캐시에 보관합니다. JAR를 직접 관리할 필요는 없습니다. 자세한 설치 위치와 문제 해결은 [설치 가이드](npm/README.md)를 참고하세요.

## 이렇게 사용합니다

연결 후 평소처럼 AI에게 질문하거나 코드 검토를 요청하면 됩니다.

- “결제 웹훅 구현을 검토하고 서명, 중복 수신, 장애 복구 누락을 찾아줘.”
- “재고 예약 API에서 오버셀링을 막을 방법을 현재 구조에 맞춰 비교해줘.”
- “부분 환불 금액을 계산할 때 쿠폰 분담액과 반올림을 어떻게 보존해야 해?”
- “선착순 쿠폰 발급의 Redis 연산이 원자적인지 점검해줘.”
- “이 DDL과 API 설계에서 정산 대사와 재처리 위험을 검토해줘.”

서버는 검색, 단일 규칙 조회, 체크리스트 생성, 설계 검토의 네 가지 Tool만 제공합니다. 정적 규칙마다 별도 Tool을 만들지 않아 AI가 고를 대상과 전달되는 schema 크기를 줄였습니다.

응답에는 검색 일치도를 뜻하는 `matchConfidence`와 함께 `evidenceLevel`, `owner`, 실제 `verifiedBy`, `lastReviewedAt`, `status`가 포함됩니다. 외부 근거가 없는 규칙은 `project-guidance`, 기록되지 않은 검토 정보는 `not-recorded`로 표시합니다. High·Critical 규칙에 적용 조건·명시적 근거·검토자·검토일이 부족하면 자동으로 `needs-review`가 됩니다.

MCP 클라이언트가 Resource와 Prompt를 지원한다면 규칙·도메인 카탈로그를 직접 읽거나 결제 웹훅/API 검토 프롬프트를 사용할 수도 있습니다.

### 0.0.5에서 0.1.0으로 업그레이드

MCP 클라이언트 설정과 `npx` 명령은 바뀌지 않습니다. 0.1.0은 Tool과 응답 계약을 정리한 호환성 변경 릴리스입니다. Node.js 최소 버전은 20이며, 이전의 도메인별 Tool 이름을 프롬프트나 자동화에서 직접 호출했다면 다음 통합 Tool로 변경해야 합니다.

| 이전 사용 방식 | 변경 후 |
|---|---|
| `search_*_knowledge`, `search_all_knowledge` | `search_knowledge`와 선택 `domain` |
| `get_*_guide`, `get_*_context` | `search_knowledge` 후 `get_rule` |
| `get_*_checklist` | `get_checklist` |

도메인별 37개 Tool은 동일 지식을 중복 노출하고 AI 선택 비용을 키워 제거했습니다.

검색 결과의 기존 `confidence` 필드는 의미를 명확히 하기 위해 `matchConfidence`로 변경했습니다. 이는 질문과 규칙의 검색 일치도이며 내용의 사실성 점수가 아닙니다.

## 지식 범위

| 영역 | 포함하는 내용 |
|---|---|
| 재고 | 예약·확정·복구, 가용 재고, 낙관·비관·분산 동시성, 멱등성, Saga |
| 결제 | 상태 머신, PG 웹훅, 중복 결제, 망취소, 부분 환불, 멱등성 키 |
| 정산 | 기준 시점, 공제, 배치, 대사, 명세서, 보류, 지급, 세무 확인 항목 |
| 쿠폰·프로모션 | 유효성, 할인 배분, 선착순 발급, 원자적 Redis 처리, 보상 |
| 커머스 | 상품·가격·주문·배송·반품·채널·회원·보안·포인트·구독·운영 |
| Java/Spring | 트랜잭션, JPA, 이벤트·Outbox, 캐시, 배치, 보안, 복원력, 배포 |

지식의 단일 원본은 [`src/main/resources/knowledge`](src/main/resources/knowledge) 아래 YAML 70개 규칙입니다. `active`는 카탈로그에서 사용 중이라는 뜻이지 외부 검증 완료를 뜻하지 않습니다. 신뢰 수준과 사용 범위는 [지식 카탈로그](docs/DOMAIN_KNOWLEDGE_REFERENCE.md)를 확인하세요.

## 가볍게 동작하는 이유

- STDIO 전용으로 실행하며 HTTP 포트를 열지 않습니다.
- Tomcat, Spring Web, Actuator, DB, Redis, 외부 LLM이 없습니다.
- 서버 시작 시 YAML을 한 번 읽고 메모리에서 결정론적으로 검색합니다.
- Tool은 4개이고 결과·체크리스트는 최대 10개로 제한됩니다.
- 현재 검증 빌드의 fat JAR는 약 23MiB입니다.

현재 품질 기준은 자동 평가 145건입니다. 70개 규칙 전체를 검색하며, 검색 Top-1 77.8%, Top-3 100%, 무관한 질문의 결과 없음 100%를 기록했습니다. 낮은 관련도는 억지로 반환하지 않고 질문 구체화를 안내하며, 결과마다 `matchConfidence`와 `queryCoverage`를 제공합니다.

## 개발하기

```powershell
$env:JAVA_HOME="C:\Users\<사용자>\.jdks\corretto-17.0.19"
.\gradlew.bat validateKnowledge --no-daemon
.\gradlew.bat bootJar --no-daemon

cd npm
npm.cmd test
node test\mcp-smoke.js ..\build\libs\context-engine-0.1.0-SNAPSHOT.jar
npm.cmd pack --dry-run
```

직접 실행할 때도 STDIO를 사용합니다.

```powershell
java -Dfile.encoding=UTF-8 -jar build\libs\context-engine-0.1.0-SNAPSHOT.jar
```

## 문서 안내

- [설치 및 문제 해결](npm/README.md)
- [서비스 구조](docs/ARCHITECTURE.md)
- [지식 카탈로그와 작성 원칙](docs/DOMAIN_KNOWLEDGE_REFERENCE.md)
- [기여 및 릴리스 가이드](CONTRIBUTING.md)

## English summary

Commerce Context MCP is a lightweight, stdio-only server with four focused tools for Korean ecommerce and Java/Spring design guidance. Responses expose whether a rule has external evidence, project-only guidance, and a recorded review date. Install Node.js 20+ and Java 17+, then configure your MCP client to run `npx -y commerce-context-mcp`. High-risk decisions must still be checked against current official requirements.

## License

[MIT](LICENSE)
