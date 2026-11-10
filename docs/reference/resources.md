# 참고 자료 정리

과제 안내문에 나온 링크와 원본 소스 정보를 정리한 문서.

## 1. 원본 소스코드

| 항목 | 내용 |
|---|---|
| 배포 파일 | [`HolubLife.jar`](https://holub.com/software/life/HolubLife.jar) |
| 버전 | 1.02 (2005-07-16 배포, jar 안 `README.TXT` 기준) |
| 구성 | 실행 가능한 `.class`, 책에 실린 `.java` 소스(알려진 버그 수정본) |
| 원본 컴파일 환경 | Java 1.4 대상, 1.4.2 / Java 5에서 테스트됨 (manifest: `Created-By: 1.5.0-rc`) |
| 진입점 | `com.holub.life.Life` |
| 저장소 위치 | `src/main/java/com/holub/**` — 커밋 `ef7d962`에 원본 그대로 반입 (줄바꿈만 CRLF→LF) |

```bash
# 원본 jar 직접 실행
java -jar HolubLife.jar
```

- jar 안에 `docs/` 폴더가 있지만 비어 있음 (javadoc 미포함).
- JDK 25에서 그대로 컴파일됨. raw type·deprecated API 관련 **경고만** 나고 오류는 없음.
- 라이선스: 소스 주석에 `@include /etc/license.txt`라고만 되어 있고, 라이선스 파일은 jar에 없음. 저장소를 공개하기 전에 확인할 것.

### 버전 이력 (원본 README.TXT)

| 버전 | 변경 |
|---|---|
| 1.01 | `Neighborhood.java`의 "stuck glider" 버그 수정 (Alexey Marinichev) |
| 1.02 | 실행 중 메뉴를 열면 게임 화면이 메뉴를 덮어쓰는 Swing 버그 회피 — 메뉴가 열려 있는 동안 `Clock`이 보드 갱신을 멈추도록 수정 |

## 2. 과제 안내문의 링크

| 링크 | 내용 | 과제에서 쓸 곳 |
|---|---|---|
| [holub.com/software/life](https://holub.com/software/life/index.html) | jar 배포 페이지 + Life 소개. 글라이더 같은 움직임은 따로 프로그래밍한 게 아니라 셀 하나의 규칙에서 생기는 **창발 행동**이고, 이것이 OO 사고의 핵심이라는 Holub의 관점이 적혀 있음 | **베이스 코드**, 설계 철학 |
| [bitstorm.org/gameoflife/standalone](https://bitstorm.org/gameoflife/standalone/) | Edwin Martin의 Game of Life. 지금은 [playgameoflife.com](https://playgameoflife.com/)으로 리다이렉트됨. Stephen Silver의 Life Lexicon(패턴 사전)을 내장하고 규칙을 설명함 | 패턴 라이브러리, 패턴 선택 UI 등 **기능 확장 아이디어** |
| [math.com …/life.html](http://www.math.com/students/wonders/life/life.html) | Conway's Life 입문 설명. 규칙(살아있는 셀은 이웃 2~3이면 생존, 죽은 셀은 이웃 3이면 탄생), 역사, 대표 패턴(글라이더, 오실레이터 등) | **규칙 이해**, **테스트 케이스 근거** |
| [golly.sourceforge.net](http://golly.sourceforge.net/) | 오픈소스 고급 Life 시뮬레이터. HashLife 등 여러 알고리즘, B/S 표기 커스텀 규칙(HighLife 등), 무한/유한/토러스 경계, RLE 등 패턴 파일 포맷, 대형 패턴 모음, Lua/Python 스크립팅 | **확장 기능 레퍼런스** |

> math.com과 Golly는 봇 차단 때문에 직접 열어보지 못했음. 이 두 곳은 이미 알려진 내용을 기준으로 정리했으니 직접 확인할 것.

### 테스트 근거로 쓸 수 있는 고전 패턴

| 패턴 | 성질 | 검증 방법 |
|---|---|---|
| Block (2×2) | still life — 변하지 않음 | N세대 후에도 같은 상태 |
| Blinker (1×3) | 주기 2 oscillator | 1세대 후 가로↔세로, 2세대 후 원래 상태 |
| Glider | 4세대마다 대각선으로 1칸 이동 | 4세대 후 같은 모양이 (+1, +1) 위치에 있음 |

## 3. 링크 → 설계패턴 확장 아이디어

| 아이디어 | 출처 | 패턴 후보 |
|---|---|---|
| 규칙 교체 (B3/S23 → HighLife B36/S23 등) | Golly | Strategy |
| 경계 조건 (고정 벽 / 토러스) | Golly | Strategy, Template Method |
| RLE, Life 1.06 등 파일 포맷 입출력 | Golly | Adapter, Factory Method |
| 패턴 라이브러리 (글라이더, Gun 삽입) | bitstorm (Life Lexicon) | Prototype + Command (Undo 포함) |
| 세대 통계, 히스토리 표시 | — | 기존 `Clock`의 Observer 확장 |
| 진화 알고리즘 교체 (기본 vs 최적화) | Golly (HashLife) | Strategy / Bridge — JUnit으로 두 구현의 결과가 같은지 검증 |

## 4. 도서

- Allen Holub, *Holub on Patterns: Learning Design Patterns by Looking at Code* (Apress, 2004)
  - **3장(Life 예제)**: `Cell`/`Neighborhood` Composite, `Clock` Singleton·Observer, Memento 기반 저장/불러오기, `MenuSite` Facade 등 원본 설계 의도가 자세히 설명되어 있음. 원본 패턴 분석 문서를 쓸 때 참고.
