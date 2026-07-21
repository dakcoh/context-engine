# commerce-context-mcp 설치 가이드

`commerce-context-mcp`는 AI 코딩 도구에 한국 커머스와 Java/Spring 백엔드 설계 지식을 연결하는 실행 패키지입니다. 사용자가 JAR 파일을 직접 내려받거나 버전별 경로를 관리하지 않아도 됩니다.

## 준비물

| 항목 | 최소 버전 | 확인 명령 |
|---|---:|---|
| Node.js | 20 | `node -v` |
| Java | 17 | `java -version` |
| MCP 클라이언트 | 도구별 최신 안정 버전 | 해당 앱의 MCP 설정 확인 |

Java 17 배포판은 Amazon Corretto를 권장합니다. 화면에서 `Version 17`, `Vendor Amazon Corretto`를 선택하면 충분합니다.

## 연결 방법

Claude Code에서는 다음 명령을 실행합니다.

```bash
claude mcp add commerce-context -- npx -y commerce-context-mcp
```

Cursor와 Claude Desktop처럼 JSON 설정을 사용하는 클라이언트에는 다음 내용을 추가합니다.

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

Windows의 대표적인 설정 위치는 다음과 같습니다.

| 클라이언트 | 설정 파일 |
|---|---|
| Cursor | `C:\Users\<사용자이름>\.cursor\mcp.json` |
| Claude Desktop | `C:\Users\<사용자이름>\AppData\Roaming\Claude\claude_desktop_config.json` |

저장 후 클라이언트를 완전히 종료했다가 다시 실행하세요.

## 정상 설치 확인

```bash
npx -y commerce-context-mcp doctor
```

`java: ok`이면 실행 조건을 충족한 것입니다. `cached jar: missing`은 아직 서버를 실행하거나 내려받지 않았다는 뜻이므로 오류가 아닙니다.

JAR 다운로드만 미리 확인하려면 다음 명령을 사용합니다.

```bash
npx -y commerce-context-mcp download
```

실행기는 다음 순서로 동작합니다.

1. Java 17 이상인지 확인합니다.
2. npm 패키지 버전과 같은 GitHub Release JAR와 체크섬을 HTTPS로 받습니다.
3. SHA-256 값이 다르면 실행하지 않고 실패합니다.
4. 검증된 파일을 `~/.commerce-context-mcp`에 캐시합니다.
5. Windows에서도 한국어가 깨지지 않도록 UTF-8로 고정한 STDIO MCP 서버를 시작합니다.

동시에 여러 클라이언트가 처음 실행돼도 잠금 파일과 원자적 파일 교체로 불완전한 캐시가 노출되지 않도록 처리합니다.

서버는 네트워크 포트를 열지 않으며 다음 4개 Tool만 제공합니다.

- `search_knowledge`
- `get_rule`
- `get_checklist`
- `review_commerce_design`

답변의 `matchConfidence`, `evidenceLevel`, `owner`, `verifiedBy`, `lastReviewedAt`, `status`를 함께 확인하세요. `matchConfidence`는 검색 일치도이지 사실성 점수가 아닙니다. `project-guidance`는 프로젝트 관리 지침이며 외부 검증 자료가 아닙니다. `needs-review`는 최신 공식 자료나 전문가 확인이 더 필요하다는 뜻입니다.

## 0.0.5에서 0.1.0으로 업그레이드

기존 `npx -y commerce-context-mcp` 설정은 그대로 사용할 수 있지만 Node.js 20 이상이 필요합니다. 이전 Tool 이름을 직접 호출하는 프롬프트나 자동화는 `search_knowledge`, `get_rule`, `get_checklist`, `review_commerce_design` 중 하나로 변경하세요. 검색 결과의 `confidence` 필드는 의미를 명확히 하기 위해 `matchConfidence`로 변경되었습니다.

## 첫 질문 예시

- “주문 결제 API의 상태 전이와 멱등성을 검토해줘.”
- “결제 웹훅에서 성공 응답 전에 무엇을 저장해야 해?”
- “재고 차감에 분산 락이 정말 필요한지 현재 DB 구조를 기준으로 비교해줘.”
- “쿠폰 선착순 발급 Lua 스크립트가 재고와 사용자 중복을 한 번에 보장하는지 봐줘.”

## 자주 발생하는 문제

### `Java 17 or newer is required`

Java 17 이상을 설치한 뒤 새 터미널에서 `java -version`을 다시 실행하세요. IDE에서만 JDK를 설정하면 외부 MCP 클라이언트가 찾지 못할 수 있으므로 운영체제의 `PATH`에도 Java가 있어야 합니다.

### 직접 실행했더니 끝나지 않습니다

명령이 MCP 클라이언트의 입력을 기다리는 중이므로 정상입니다. 직접 실행하는 대신 클라이언트 설정의 `command`로 등록하세요.

### JAR 다운로드가 실패합니다

GitHub 접속이 가능한지 확인하고 다시 실행하세요. 체크섬 파일이 없거나 값이 다르면 안전을 위해 실행을 계속하지 않습니다. 프록시 환경이라면 GitHub와 GitHub Release 자산 도메인의 HTTPS 접근이 필요합니다.

### 캐시를 새로 받고 싶습니다

클라이언트를 종료한 뒤 사용자 홈의 `.commerce-context-mcp` 폴더에서 해당 버전 JAR와 `.sha256` 파일만 제거하고 `download`를 다시 실행하세요. 다른 폴더는 삭제할 필요가 없습니다.

## 제공 명령

```text
commerce-context-mcp --help       도움말
commerce-context-mcp --version    npm 패키지 버전
commerce-context-mcp doctor       Java와 로컬 캐시 상태 확인
commerce-context-mcp download     검증된 JAR만 미리 다운로드
commerce-context-mcp              STDIO MCP 서버 시작
```

## English

Install Node.js 20+ and Java 17+, then configure your MCP client to run `npx -y commerce-context-mcp`. The launcher downloads the matching release JAR over HTTPS, verifies its SHA-256 checksum, caches it under `~/.commerce-context-mcp`, and starts the server over stdio. See the [project README](https://github.com/dakcoh/commerce-context-mcp) for capabilities and development instructions.
