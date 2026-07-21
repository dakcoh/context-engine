# Commerce Context MCP 기여 가이드

버그 수정, 지식 규칙 개선과 문서 기여를 환영합니다. 변경 전에는 [README](README.md), [서비스 구조](docs/ARCHITECTURE.md), [지식 카탈로그](docs/DOMAIN_KNOWLEDGE_REFERENCE.md)를 먼저 확인해 주세요.

## 개발 환경

| 구성 요소 | 기준 |
|---|---|
| Java | 17 이상, Amazon Corretto 17 권장 |
| Node.js | 20 이상 |
| Transport | STDIO 전용 |
| 지식 저장 | JAR 내부 YAML, 실행 시 인메모리 조회 |

HTTP 서버, 외부 DB, 캐시 또는 외부 LLM은 실행에 필요하지 않습니다.

## 설계 원칙

- 의존 방향 `adapter → application → core`를 유지합니다.
- `core`에 Spring, MCP, Jackson 또는 파일 I/O 의존성을 추가하지 않습니다.
- 새 기능은 기존 4개 Tool의 입력이나 응답 확장으로 먼저 해결합니다.
- 정적 규칙마다 Tool을 추가하지 않습니다.
- Tomcat, Spring Web, Actuator를 추가하지 않습니다.
- `review_commerce_design` 결과를 확정 결함으로 표현하지 않습니다.
- 검색 변경은 전체 평가와 실제 한국어 MCP 호출로 확인합니다.

## 지식 YAML 작성

지식의 단일 원본은 `src/main/resources/knowledge/*.yml`입니다. 새 domain 파일은 자동으로 발견되며 별도 Java repository나 mapper가 필요하지 않습니다.

```yaml
schema-version: 1
domain: payment
governance:
  owner: commerce-context-maintainers
rules:
    - id: stable-rule-id
      category: webhook
      title: "규칙 제목"
      summary: "적용 조건과 핵심 판단을 담은 한 문장"
      severity: high
      applicability:
        applies-when: ["적용 조건"]
        does-not-apply-when: ["적용하지 않는 조건"]
        assumptions: ["전제"]
        jurisdiction: KR
      guidance: ["실패와 복구를 포함한 구현 지침"]
      checklist: ["검토 질문"]
      tags: [payment, webhook]
```

`governance.owner`는 domain 규칙을 지속해서 관리할 팀이나 역할입니다. 개인 이름보다 유지 가능한 팀 식별자를 권장합니다.

### 실제 검토 정보를 추가하는 경우

검토를 수행한 규칙에만 `review`를 작성합니다.

```yaml
review:
  last-reviewed-at: 2026-07-21
  verified-by: payment-platform-team
  change-note: "서명 검증과 중복 수신 방어 조건을 명확화"
```

- 검토하지 않은 날짜나 검토자를 자동 생성하지 않습니다.
- `verified-by`에는 실제 검토자 또는 팀만 기록합니다.
- `change-note`는 변경 이유를 한 줄로 설명합니다.
- 만료일과 재검토 주기는 관리하지 않습니다.

### 외부 근거를 추가하는 경우

자료를 직접 확인한 경우에만 `evidence`를 추가합니다.

```yaml
evidence:
  - title: "확인한 공식 문서 제목"
    url: "https://official.example/document"
    type: official-documentation
    source-version: "API v2"
    published-at: 2026-01-15
    reviewed-at: 2026-07-21
    accessed-at: 2026-07-21
    note: "확인한 범위와 적용 조건"
```

- 버전이 없는 웹 문서는 `source-version`을 생략합니다.
- 블로그, 검색 결과 요약 또는 생성형 AI 답변을 공식 근거처럼 등록하지 않습니다.
- 특정 PG의 상태 코드와 timeout을 모든 PG의 공통 규칙으로 단정하지 않습니다.
- 세율, 증빙 발행 시점과 보존 기간은 기준일 없이 고정하지 않습니다.
- 프로젝트 문서와 YAML 경로는 `project-guidance`이며 외부 근거가 아닙니다.

High·Critical 규칙은 applicability, 명시적 evidence, 실제 `verified-by`와 `last-reviewed-at`이 모두 있어야 `active`가 될 수 있습니다. 하나라도 부족하면 loader가 `needs-review`로 분류합니다. YAML에 `status: active`를 적어도 이 검증을 우회할 수 없습니다.

## 로컬 검증

Windows PowerShell 예시입니다.

```powershell
$env:JAVA_HOME="C:\Users\<사용자>\.jdks\corretto-17.0.19"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

.\gradlew.bat clean validateKnowledge bootJar --no-daemon

cd npm
npm.cmd test
node test\mcp-smoke.js ..\build\libs\context-engine-0.1.0-SNAPSHOT.jar
$env:npm_config_cache="..\.npm-cache"
npm.cmd pack --dry-run
```

`validateKnowledge`는 YAML 계약, 100개 검색 평가, 20개 설계 검토, 10개 체크리스트, 15개 신뢰 정책과 MCP 계약을 검증합니다.

## 변경별 확인 항목

| 변경 | 필수 확인 |
|---|---|
| core model | 불변성, 필수 값, 신뢰 메타데이터 |
| YAML | schema v1, owner, ID, related ID, 실제 검토 정보 |
| 검색 | Top-1·Top-3·no-answer 평가와 결과 수 제한 |
| 리뷰·체크리스트 | 기대 규칙, owner, evidence level |
| MCP | Tool 4개, Resource·Prompt, 실제 JSON-RPC 호출 |
| npm | Java 검사, checksum, cache와 package 내용 |
| 의존성 | JAR에 web server와 Actuator가 없는지 확인 |
| 문서 | 공개 계약, 버전, Tool 이름과 로컬 링크 확인 |

## 릴리스 확인표

- [ ] Corretto 17에서 `clean validateKnowledge bootJar` 통과
- [ ] 자동 평가 145건 통과
- [ ] Tool 4개, Resource 3종, Prompt 2종 유지
- [ ] 한국어 검색과 no-answer MCP smoke test 통과
- [ ] High·Critical 미검증 규칙이 `needs-review`로 표시됨
- [ ] 검토하지 않은 날짜, 검토자와 외부 근거가 없음
- [ ] JAR에 Tomcat, Spring Web, Actuator가 없음
- [ ] npm 테스트와 `pack --dry-run` 통과
- [ ] `npm/package.json` 버전과 `vX.Y.Z` 릴리스 태그가 일치
- [ ] Release JAR와 SHA-256 checksum 일치
- [ ] README와 문서가 현재 구현과 일치

## Pull Request

하나의 Pull Request에는 가능한 한 하나의 목적만 포함해 주세요. 설명에는 변경 이유, 영향받는 규칙 또는 공개 계약, 실행한 검증을 적습니다. 법률·세무·결제·보안 지식 변경에는 확인한 공식 근거와 적용 범위도 함께 남겨 주세요.
