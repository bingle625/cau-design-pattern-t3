# Life Game 기능 확장 및 설계 개선

> 중앙대학교 **설계패턴(Design Patterns) 2026 · 01분반** 텀프로젝트
> [Holub Life Game](https://holub.com/software/life/index.html)에 설계패턴을 적용하여 **기능을 확장**하고 **설계를 개선**한다.

| 항목 | 내용 |
|---|---|
| 팀 번호 | `TBD` |
| 구현 언어 | Java |
| 테스트 | JUnit |
| 배점 | 학기 총점의 25% |
| 최종 마감 | **2026-11-29 (일) 23:00** — 자정 아님 |
| 발표 | 2026-12-02 (화) 15:00 ~ 18:00 수업시간 |

---

## 목차

1. [팀원](#1-팀원)
2. [프로젝트 개요](#2-프로젝트-개요)
3. [원본 Holub Life Game 분석](#3-원본-holub-life-game-분석)
4. [기능 확장 및 설계 개선 계획](#4-기능-확장-및-설계-개선-계획)
5. [빌드 및 실행](#5-빌드-및-실행)
6. [테스트](#6-테스트)
7. [디렉터리 구조](#7-디렉터리-구조)
8. [협업 규칙](#8-협업-규칙)
9. [일정](#9-일정)
10. [제출물 체크리스트](#10-제출물-체크리스트)
11. [참고 자료](#11-참고-자료)

---

## 1. 팀원

| 학번 | 이름 | GitHub | 담당 |
|---|---|---|---|
| `TBD` | `TBD` | `@TBD` | `TBD` |
| `TBD` | `TBD` | `@TBD` | `TBD` |
| `TBD` | `TBD` | `@TBD` | `TBD` |
| `TBD` | `TBD` | `@TBD` | `TBD` |
| `TBD` | `TBD` | `@TBD` | `TBD` |
| `TBD` | `TBD` | `@TBD` | `TBD` |

> 팀원별 기여는 커밋 히스토리, PR, Issue로 증빙한다. 프로젝트 문서의 "GitHub 활용 요약"에 그대로 쓰이므로 **각자 본인 계정으로 커밋**할 것.

---

## 2. 프로젝트 개요

### 목표

- 수업에서 다룬 **설계패턴 및 객체지향 설계 원칙**을 적극 활용해 Holub Life Game을 확장·개선한다.
- **기존 기능은 그대로 동작**해야 한다. (회귀 금지)
- 기능 확장과 설계 개선이 잘 되었음을 **JUnit 테스트로 입증**한다.
- 설계 결정 과정(고민한 문제, 대안, 장단점, 최종 선택 이유)을 **문서화**한다.

### 평가 대상

발표물 · 발표력 · Q&A · 프로젝트 문서 · 소스코드 및 테스트코드

### 핵심 원칙 (문서 작성 시 유의)

- 문서에서 **가장 중요한 부분은 각 기능 확장·설계 개선 내용**이다.
  - 확장 기능 / 설계 개선 요약
  - 기존 설계·코드와의 비교 및 상세 변경 내용
  - 도입한 패턴·원칙의 **적용 이유와 과정**
  - 주요 설계 결정 시 고려한 **대안들과 장단점, 최종안 선택 이유**
- 실제로 고민하지 않은 내용을 생성형 AI 등으로 **나열하거나, 글과 그림이 맞지 않으면 감점**된다.
  → 설계 결정은 그때그때 [`docs/design-decisions/`](#7-디렉터리-구조)에 기록해 두고 문서에 반영한다.

---

## 3. 원본 Holub Life Game 분석

> 원본: [`HolubLife.jar`](https://holub.com/software/life/HolubLife.jar) v1.02 (2005-07-16). `src/main/java/com/holub/**`에 원본 그대로 반입함 (커밋 `ef7d962`).
> **동작 원리, 다이어그램, 패턴, 개선 포인트 상세 분석: [`docs/holub-life-overview.md`](docs/holub-life-overview.md)**
> 출처, 링크, 아이디어 정리는 [`docs/reference/resources.md`](docs/reference/resources.md) 참고.

### 주요 패키지

| 패키지 | 클래스 | 역할 |
|---|---|---|
| `com.holub.life` | `Life`, `Universe`, `Cell`, `Neighborhood`, `Resident`, `Clock`, `Direction`, `Storable` | 게임 모델과 실행. `Life`가 진입점(`JFrame`) |
| `com.holub.ui` | `MenuSite`, `Colors` | 메뉴 관리, 색상 상수 |
| `com.holub.tools` | `Publisher`, `Log` | Observer 구독자 관리, 로깅 |
| `com.holub.io` | `Files` | 파일 선택 다이얼로그 |
| `com.holub.asynch` | `ConditionVariable` | 동기화 유틸리티 |

### 원본에 이미 적용된 패턴

> 코드에서 위치만 확인한 상태. 세부 분석은 진행 중.

| 패턴 | 적용 위치 |
|---|---|
| Composite | `Cell` 인터페이스 ← `Neighborhood`(복합) / `Resident`(단말) |
| Singleton | `Clock.instance()`, `Universe.instance()` |
| Observer | `Clock.addClockListener()` + `Clock.Listener`, `tools.Publisher`로 구독자에게 알림 |
| Memento | `Cell.Memento` (← `Storable`), `Neighborhood.NeighborhoodState` |
| Null Object | `Cell.DUMMY` — 항상 죽어 있는 셀. 경계 밖 이웃으로 쓰임 |
| Facade | `MenuSite` — 메뉴 추가/삭제를 감쌈 |

### 원본 기능 (회귀 테스트 대상)

| 메뉴 / 조작 | 기능 | 구현 위치 |
|---|---|---|
| 마우스 클릭 | 셀 생/사 토글 | `Universe`, `Cell.userClicked` |
| Grid → Clear | 보드 초기화 | `Universe` |
| Grid → Load / Store | `.life` 파일 불러오기 / 저장 | `Universe`, `Storable` |
| Grid → Exit | 종료 | `Universe` |
| Go → Halt | 정지 | `Clock` |
| Go → Tick (Single Step) | 한 세대 진행 | `Clock` |
| Go → Agonizing / Slow / Medium / Fast | 실행 속도 선택 | `Clock` |

### 설계상 문제점 (개선 후보)

> 코드 분석하면서 채운다. 예: UI와 모델 결합도, Singleton으로 인한 테스트 어려움, 하드코딩된 규칙 등.

- `TBD`

---

## 4. 기능 확장 및 설계 개선 계획

> 확장 유스케이스는 문서 요구사항에 따라 **이름에 `E`를 붙여** 명명한다. (예: `E1. 규칙 변경`)

| ID | 기능 / 개선 | 적용 패턴·원칙 (후보) | 담당 | 상태 |
|---|---|---|---|---|
| E1 | `TBD` | `TBD` | `TBD` | ⬜ 계획 |
| E2 | `TBD` | `TBD` | `TBD` | ⬜ 계획 |
| E3 | `TBD` | `TBD` | `TBD` | ⬜ 계획 |
| D1 | (설계 개선) `TBD` | `TBD` | `TBD` | ⬜ 계획 |

<details>
<summary>아이디어 풀 (팀 논의용, 확정 아님)</summary>

| 아이디어 | 패턴 후보 |
|---|---|
| 생존 규칙 교체 (B3/S23, HighLife B36/S23, Seeds 등) | Strategy |
| 실행 취소 / 다시 실행 (Undo/Redo) | Command, Memento |
| 패턴 라이브러리 배치 (Glider, Blinker, Gosper Gun 등) | Prototype, Command |
| 다양한 파일 포맷 지원 (RLE, Life 1.06, Plaintext `.cells`) | Adapter, Factory Method |
| 시뮬레이션 상태 관리 (Running / Paused / Stepping) | State |
| 세대 통계, 히스토리 패널 (세대 수, 살아있는 셀 수) | Observer (기존 `Clock` 확장) |
| 셀 렌더링 테마 / 나이별 색상 | Decorator, Strategy |
| 보드 경계 방식 (토러스 / 고정 벽) | Strategy, Template Method |
| 진화 알고리즘 교체 (기본 vs 최적화) | Strategy, Bridge — 두 구현의 결과가 같은지 JUnit으로 검증 |
| 모델-UI 분리로 독립 테스트 가능하게 | MVC, Dependency Injection, DIP |

출처별 정리는 [`docs/reference/resources.md`](docs/reference/resources.md#3-링크--설계패턴-확장-아이디어) 참고.

</details>

---

## 5. 빌드 및 실행

Gradle Wrapper를 쓰므로 **Gradle을 따로 설치할 필요 없음.** JDK만 있으면 된다.

| 항목 | 버전 |
|---|---|
| JDK | **17 이상** (17, 25에서 확인). 바이트코드는 Java 17 대상으로 컴파일 |
| Gradle | 9.5.1 (Wrapper가 자동으로 내려받음) |
| JUnit | Jupiter 5.13.4 |

```bash
./gradlew run       # 게임 실행
./gradlew test      # 테스트 실행 (headless, 화면 없이)
./gradlew build     # 컴파일 + 테스트 + jar 생성

java -jar build/libs/holub-life-1.02.jar   # build 후 jar로 실행
```

> Windows에서는 `./gradlew` 대신 `gradlew.bat`.
> 컴파일할 때 나오는 deprecation / unchecked 경고는 원본 코드(Java 1.4) 때문이고 정상이다.

- 테스트 결과 리포트: `build/reports/tests/test/index.html` (문서용 스크린샷 찍을 때 사용)
- IntelliJ: 프로젝트 폴더를 열면 Gradle 프로젝트로 자동 인식된다.

---

## 6. 테스트

- 모든 테스트는 JUnit으로 작성한다.
- 테스트 케이스마다 **목적**을 주석 또는 `@DisplayName`으로 명시한다. (문서에 그대로 옮겨야 함)
- **기존 기능 회귀 테스트**와 **확장 기능 테스트**를 구분한다.
- 가능하면 **모델을 UI와 분리**해 독립적으로 테스트한다. (가산 요소)
- 테스트 실행 결과는 스크린샷으로 남긴다. → `docs/screenshots/`

| 구분 | 대상 | 테스트 클래스 |
|---|---|---|
| 회귀 | 원본 기능 | `TBD` |
| 확장 | E1 ~ En | `TBD` |

---

## 7. 디렉터리 구조

```text
.
├── README.md
├── build.gradle.kts        # Gradle 빌드 설정
├── settings.gradle.kts
├── gradlew, gradlew.bat    # Gradle Wrapper 실행 스크립트
├── gradle/wrapper/         # Wrapper 설정 (커밋 대상)
├── src/
│   ├── main/java/          # 원본 + 확장 소스
│   └── test/java/          # JUnit 테스트
├── docs/
│   ├── holub-life-overview.md  # 원본 Holub Life 분석 (Before 기준)
│   ├── design-decisions/   # 설계 결정 기록 (문제·대안·장단점·결정)
│   ├── uml/                # 클래스·시퀀스·상태·유스케이스 다이어그램
│   ├── screenshots/        # 테스트 결과, 실행 화면, GitHub 히스토리
│   └── reference/          # 과제 안내문 등
└── submission/             # 최종 제출용 산출물 (README.txt, PDF, 영상 링크 등)
```

---

## 8. 협업 규칙

개발 과정이 **커밋 히스토리로 드러나야** 평가에 반영되므로, 마감 직전 몰아서 커밋하지 않고 꾸준히 작업한다.

### 브랜치

| 브랜치 | 용도 |
|---|---|
| `main` | 항상 빌드·테스트 통과 상태 유지 |
| `feature/E{n}-{설명}` | 기능 확장 (예: `feature/E1-rule-strategy`) |
| `refactor/D{n}-{설명}` | 설계 개선 |
| `docs/{설명}` | 문서 작업 |

### Git hooks 설정

클론한 뒤 한 번 실행한다. Git Bash를 사용하는 Windows에서도 같은 명령을 쓴다.

```bash
bash scripts/setup-hooks.sh
```

- **pre-commit**: 스테이징한 파일의 공백 오류와 충돌 마커를 검사한다. 소스나 Gradle 설정이 바뀌면 **스테이징한 파일만 임시 폴더에 복사해 JUnit을 실행**한다. 아직 스테이징하지 않은 작업은 테스트에 섞이지 않는다.
- **commit-msg**: 아래 커밋 메시지 형식을 검사한다. 선택적으로 `test(life): 설명`처럼 범위를 붙일 수 있다. Git의 merge/revert 및 fixup/squash 메시지는 허용한다.
- 문서만 수정하면 JUnit은 생략한다. 첫 테스트 시 Gradle과 의존성을 내려받을 수 있으므로 JDK 17 이상과 네트워크가 필요하다.
- 터미널에서는 색상으로 결과를 표시하고 IDE·로그에서는 일반 텍스트를 사용한다. `NO_COLOR=1`로 색상을 끌 수 있다.
- 훅은 로컬 검사이므로 팀원마다 설치해야 한다. 해제하려면 `git config --local --unset core.hooksPath`를 실행한다.
- 테스트만 직접 실행하려면 `bash scripts/run-gradle.sh test`를 사용한다. 명시한 `JAVA_HOME`을 우선 사용한다. PATH에 Java가 없거나 17 미만이면 macOS에 등록된 JDK와 Homebrew JDK를 순서대로 찾는다. `JAVA_HOME`이 잘못되었거나 호환 JDK를 찾지 못하면 수정 방법을 안내한다.

### 커밋 메시지 형식

```text
<type>: <요약>

type: feat | fix | refactor | test | docs | chore
예) feat: E1 생존 규칙을 Strategy 패턴으로 분리
```

### PR

- `main` 직접 push 금지, PR로 병합
- 최소 1명 리뷰 후 병합
- PR 본문에 **적용 패턴과 선택 이유**를 간단히 적는다 → 문서 작성 시 재활용

### 설계 결정 기록

중요한 설계 결정을 할 때마다 `docs/design-decisions/NNN-제목.md`에 아래 형식으로 남긴다.

```markdown
# NNN. 제목
- 문제: 어떤 설계 문제를 풀려 했는가
- 대안: A / B / C 와 각각의 장단점
- 결정: 무엇을, 왜 선택했는가
- 결과: 변경된 클래스, 관련 다이어그램, 관련 테스트
```

---

## 9. 일정

| 날짜 | 마일스톤 | 상태 |
|---|---|---|
| 10-27 (화) | Mid Due — 팀 구성 완료, GitHub 주소 eclass 공지 | ✅ 팀 구성 완료 / ⬜ GitHub 주소 공지 |
| 11-?? | 원본 코드 분석 및 확장 기능 확정 | ⬜ |
| 11-?? | 기능 구현 및 테스트 | ⬜ |
| 11-?? | 프로젝트 문서·UML 작성 | ⬜ |
| 11-?? | 비디오 촬영 및 발표 자료 제작 | ⬜ |
| **11-29 (일) 23:00** | **Final Due — eclass "TermProject" 과제 업로드** | ⬜ |
| 12-02 (화) 15:00 | 발표 및 Q&A | ⬜ |

> GitHub 주소 공지 방법: eclass 설계패턴 01분반 → 사용자 및 그룹 → Term Project 탭 → 팀 번호의 `⋮` → 그룹 홈페이지 방문 → `+공지사항`에 주소 작성

---

## 10. 제출물 체크리스트

**파일명:** `팀번호_팀원이름1_팀원이름2_팀원이름3_팀원이름4_팀원이름5_팀원이름6.zip` (zip 1개)

- [ ] **`README.txt`** — 제출된 모든 산출물의 목록·요약 + GitHub 주소
  *(이 `README.md`와 별개. 제출용 텍스트 파일로 따로 작성)*
- [ ] **소스코드 / 실행 파일(`.class`) / JUnit 테스트 코드 / 데이터**
- [ ] **프로젝트 문서 (PDF, 50페이지 이내)**
  - [ ] 표지: 팀원 학번, 이름
  - [ ] 프로젝트 내용 요약
  - [ ] 유스케이스 다이어그램 (확장 기능은 이름에 `E`)
  - [ ] 각 기능 확장·설계 개선 내용 **(가장 중요)**
    - [ ] 요약
    - [ ] 상세 변경 내용, 기존 설계/코드와 비교
    - [ ] 패턴·원칙 적용 이유 및 과정
    - [ ] 설계 대안과 장단점, 최종안 선택 이유
    - [ ] UML (클래스 / 시퀀스 / 상태 등)
  - [ ] 테스트 수행 내역
    - [ ] 각 테스트 케이스 목적
    - [ ] 기존 기능 정상 동작 증명
    - [ ] 추가 기능별 정상 동작 증명
    - [ ] JUnit 테스트 코드
    - [ ] 모델-UI 분리 테스트 (권장)
    - [ ] 테스트 결과 스크린샷
  - [ ] GitHub 활용 요약 **(1~3페이지)**
    - [ ] 저장소 주소
    - [ ] Progress history 스크린샷
    - [ ] 팀원별 기여 자료
- [ ] **비디오 클립 (30분 이내)**
  - [ ] 설계·구현·테스트 설명, 적용 패턴과 설계 고민
  - [ ] 기존 코드 대비 변경점 중심
  - [ ] 테스트 케이스 목적 설명
  - [ ] 기존 기능 시연
  - [ ] **모든** 추가 기능 시연
- [ ] **발표물 (PDF, 10분 내외, 분량 제한 없음)** — 발표 시 eclass 제출 파일만 사용 가능

---

## 11. 참고 자료

> 링크별 내용과 활용 방안: [`docs/reference/resources.md`](docs/reference/resources.md)

- 과제 안내문: `DP Term Project 2026 Life Game - updated-4.pdf` (10-13 업데이트판, 저장소에는 올리지 않음)
- Holub Life Game: <https://holub.com/software/life/index.html> — 원본 [`HolubLife.jar`](https://holub.com/software/life/HolubLife.jar)
- Allen Holub, *Holub on Patterns: Learning Design Patterns by Looking at Code* (Apress, 2004)
- Bitstorm Game of Life: <https://bitstorm.org/gameoflife/standalone/>
- Math.com — Life: <http://www.math.com/students/wonders/life/life.html>
- Golly: <http://golly.sourceforge.net/>
