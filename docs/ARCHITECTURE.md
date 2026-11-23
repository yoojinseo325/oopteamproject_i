# 구조와 객체 간 협력

이 문서는 팀원들이 클래스 담당자를 정하고 기존 코드를 이해한 후 확장하기 위한 안내입니다. 단순한 입문 프로젝트를 유지하기 위해 DI 프레임워크, ORM, 웹 서버를 사용하지 않습니다. 모든 Java 코드는 표준 JDK 17 API만 사용합니다.

## 실행 흐름

```mermaid
flowchart LR
  App --> MainFrame
  MainFrame --> Pages[HomePanel / LearningPathPanel / RuleLibraryPanel / RecordsPanel / NotesPanel]
  MainFrame --> MissionResultPanel
  MainFrame --> LearningDialogs
  MainFrame --> QuizPanel
  Pages --> ScreenActions
  QuizPanel --> QuestionVisual
  QuestionVisual --> ImageQuestionPanel
  QuestionVisual --> SceneQuestionPanel
  SceneQuestionPanel --> FieldPanel
  ImageQuestionPanel --> ImageView
  QuizPanel --> QuizAnswerPanel
  QuizPanel --> QuizFeedbackPanel
  MainFrame --> LearningService
  QuizPanel --> LearningService
  LearningService --> Catalog
  LearningService --> ReviewPolicy
  LearningService --> LearningProgress
  LearningService --> LearningStatistics
  LearningService --> UserRepositories
  LearningService --> Manager
  LearningService --> Repository
  FileRepository -.구현.-> Repository
  FileRepository --> TSV[UTF-8 TSV 파일]
```

1. `App`이 데이터 경로와 기록 폴더를 결정하고 파일 잠금을 획득합니다.
2. `Catalog`가 규칙·문제·장면을 파일에서 읽어 `Manager`에 보관하고 연결 ID를 검증합니다.
3. `LearningService`가 사용자와 풀이 기록, 북마크, 메모를 복원합니다.
4. Swing 이벤트 스레드에서 `MainFrame`이 만들어집니다.
5. 버튼은 서비스를 호출하고, 결과를 받아 화면만 갱신합니다.

## 클래스 책임과 공개 메서드

| 영역 | 클래스 | 역할 / 주요 메서드 |
|---|---|---|
| 데이터 | `RuleConcept` | 규칙 개념과 출처 |
| 데이터 | `Question` | 상황, 선택지, 정답, 근거, 해설 / `isAnswerCorrect`, `isClueCorrect` |
| 데이터 | `Scene`, `Scene.Token` | 장면 설명과 선수·공·조건 토큰의 좌표, 선수 연결 |
| 데이터 | `Team`, `Player`, `Matchup` | 구단 색상, 선수 소속, 문제별 공격/수비 배정 |
| 데이터 | `UserProfile`, `Attempt` | 사용자와 제출 결과 / `understood`, `independent` |
| 데이터 | `Bookmark`, `StudyNote` | 사용자별 북마크와 개인 메모 |
| 진도 | `LearningStep`, `StepCompletion` | 파일에서 읽은 단계 목표·영구 ID·문항 배치와 사용자별 완료 기록 |
| 상태 | `QuizSession` | 문제 순서, 학습 단계 번호, 입력, 힌트 / `choose`, `beginClue`, `chooseClue`, `next` |
| 리스트 | `Manager<T>` | `all`, `find`, `search`, `add`, `remove` |
| 저장 | `Repository<T>` | 매체와 무관한 `load`, `save` 계약 |
| 저장 | `FileRepository<T>` | TSV 해독 함수 주입, BufferedReader/Writer, 임시 파일 교체 |
| 저장 구성 | `UserRepositories` | 5개 사용자 저장소의 계약 묶음, `files(Path)`에서 기존 TSV 파일명·헤더·변환 정의 |
| 콘텐츠 | `Catalog` | 파일 로딩, 스키마·ID·정답/단서 연결 검사, 규칙 검색 |
| 처리 | `LearningService` | UI의 진입점, 사용자 선택, 입력 검증, 저장 성공 후 상태 변경, 전용 정책 객체 호출 |
| 복습 | `ReviewPolicy` | 오답·힌트 우선순위, 1/3/7일 간격, 같은 규칙·유형의 대체 문항 선택 |
| 진도 | `LearningProgress` | 한 사용자의 완료 단계, 화면 순번과 영구 ID, 다음 단계 해제 판단 |
| 성취 | `LearningStatistics` | 최신 제출 기준 독립 해결, 규칙별 성취, 문항별 최고 XP와 칭호 |
| 이력 조회 | `LearningHistory` | 저장 순서를 유지하는 사용자 이력 스냅샷, 문항/규칙별 조회; 서비스 패키지 내부 전용 |
| 출제 조회 | `QuestionSource` | 출제에 필요한 조회 계약. 사용자 전환·채점·저장 변경 메서드는 노출하지 않음 |
| 출제 | `QuestionSelector` | `select(QuestionSource)` 계약 |
| 출제 | `StepSelector` | 단계 잠금 검사, 새 상황 사이에 규칙별 재도전 삽입 |
| 출제 | `TopicSelector` | 기존 주제별 출제 API (기본 UI에서는 미사용) |
| 출제 | `ReviewSelector` | 기존 문항별 복습 API 유지 (기본 UI에서는 미사용) |
| 창 | `MainFrame` | 공통 탐색·헤더, CardLayout, 진행 중인 미션 수명 관리 |
| 탐색 계약 | `ScreenActions` | 페이지가 창 구현을 모르고 시작·이동·메모 입력·알림을 요청하는 인터페이스 |
| 페이지 | `HomePanel`, `LearningPathPanel`, `RuleLibraryPanel`, `RecordsPanel`, `NotesPanel`, `MissionResultPanel` | 각 화면의 구성과 입력 처리 |
| 공통 화면 | `StatisticsPanel`, `LearningPathView`, `PageComponents` | 성취 요약·단계 목록·제목/문단 표현 재사용 |
| 대화상자 | `LearningDialogs` | 프로필 선택/추가, 메모 입력 |
| 퀴즈 조립 | `QuizPanel` | 단계에 맞는 패널 조립, 제출·다음 문제 서비스 호출 |
| 퀴즈 표현 | `QuestionVisual`, `ImageQuestionPanel`, `SceneQuestionPanel` | 그림/도식의 다형적 표현과 단서 선택 표시 |
| 퀴즈 입력/해설 | `QuizAnswerPanel`, `QuizFeedbackPanel`, `QuizText` | 답 선택·힌트·근거 안내, 저장된 결과 해설, 긴 본문 높이 계산 |
| 이미지 재사용 | `QuestionImages` | 현재 문항의 이미지 하나만 보관하여 단계 변경·확대 때 재사용 |
| 그림 | `ImageView` | V2 PNG 비율 유지·확대 표시 |
| 그림 | `FieldPanel` | 정답을 모르는 독립적인 야구장 렌더러, 단서 클릭 알림 |
| 스타일 | `Theme` | 색·글꼴·공통 컴포넌트 |

데이터의 `record`는 Java 17의 불변 데이터 클래스입니다. private final 필드와 접근자, 생성자, equals/hashCode 등을 자동 제공합니다. 수업에서 일반 클래스를 요구한다면 동일한 메서드 계약을 유지하면서 일반 클래스로 바꿔도 됩니다.

## 한 번의 제출

```text
JUDGE
  기초형: choose(choice) → LearningService.submit(session) → FEEDBACK
  판정형: choose(choice) → beginClue()
CLUE
  chooseClue(tokenId) → LearningService.submit(session)
  1. 미션 소유 사용자·단계·단서 검증
  2. Question이 판정과 근거를 각각 검사
  3. Attempt를 만들어 파일 저장
  4. 저장 성공 시 Manager에 추가, session.recordSaved()
FEEDBACK
  LearningService.advance(session)
  → 마지막 문항이면 steps.tsv에 완료 기록 저장 후 session.next()
  → 다음 문항 JUDGE 또는 마지막 COMPLETE
```

저장 실패 시 4번을 수행하지 않아 중복/유실된 상태로 다음 문제에 넘어가지 않습니다. 중복 제출과 단계 밖 호출은 `QuizSession` 및 서비스에서 거부합니다. 서비스 테스트에는 실패 후 같은 선택을 다시 저장하는 검사가 있습니다.

## 객체지향을 설명할 때

- **캡슐화:** 화면이 파일이나 내부 ArrayList를 직접 변경하지 않습니다. Manager는 불변 복사본을 반환합니다.
- **인터페이스와 다형성:** `QuestionSelector` 하나로 StepSelector, TopicSelector, ReviewSelector의 다른 출제 방식을 실행합니다.
- **의존성 분리:** `LearningService(Catalog, UserRepositories, Clock)`에 저장소 계약을 주입합니다. 기존 `Path` 생성자는 편의를 위한 파일 저장소 조립 진입점입니다. 출제는 `QuestionSource`, 페이지 탐색은 `ScreenActions` 계약에 의존합니다.
- **책임 분리:** FieldPanel은 그리기, Question은 정답 판별, QuizSession은 단계, LearningService는 저장과 흐름 조정, ReviewPolicy는 복습 정책, LearningStatistics는 성취 계산, FileRepository는 파일을 맡습니다.
- **상속:** MainFrame extends JFrame, QuizPanel/FieldPanel extends JPanel, 공통 패널/버튼의 그리기 메서드 오버라이딩을 실제로 사용합니다.
- **이벤트 처리:** ActionListener, MouseAdapter, DocumentListener, WindowAdapter를 용도에 맞게 사용합니다.

## 확장할 때 지킬 계약

1. UI 패키지 밖에는 Swing import를 추가하지 않습니다.
2. 기록 변경은 서비스 메서드를 통해 처리합니다.
3. 기존 ID를 다른 규칙/문제로 재사용하지 않습니다. 판정 조건이 달라지면 새 문제 ID를 사용합니다.
4. 필드를 추가하면 TSV 헤더, 로더·저장 함수, 문서, 테스트를 함께 바꿉니다. 기존 사용자 기록의 마이그레이션도 정합니다.
5. 미션 상태를 바꾸는 기능을 추가하면 단계별 허용 동작을 먼저 합의합니다.
6. 프로그램 종료 시에는 이미 제출된 문항만 보존됩니다. 이어하기를 추가하려면 별도 세션 저장 모델이 필요합니다.

2026-11-15에 페이지와 학습 정책을 전용 클래스로 분리했습니다. 화면 담당자는 각 Panel에서 작업하고, 복습 담당자는 ReviewPolicy, 진도 담당자는 LearningProgress, 점수 담당자는 LearningStatistics를 수정합니다. 서비스에 모든 규칙을 다시 넣거나 화면에서 저장소를 직접 수정하지 않습니다.

`Manager`는 `baseup.common`으로 이동했습니다. repository가 service를 참조하던 의존 관계가 제거되어 `ui → service → repository`, `service/repository → common → model` 방향을 유지합니다. 화면의 읽기 전용 콘텐츠 조회는 Catalog를 사용합니다. model/common/repository에는 UI 의존성이 없습니다.

진도·통계·복습 객체는 생성 시점의 사용자 기록을 복사한 스냅샷입니다. 저장하거나 사용자를 바꾼 뒤에는 서비스에서 새 스냅샷을 얻습니다. 이전 스냅샷이 다른 사용자의 데이터로 바뀌지 않습니다.

## 퀴즈 화면 갱신

- 문항 시작 시 도식은 장면 1부터 표시합니다. 단서 단계에서도 장면 1의 대상을 선택합니다.
- JUDGE → CLUE → FEEDBACK 또는 다음 문항으로 바뀔 때 QuizPanel이 영역을 조립합니다.
- 답/단서를 고를 때는 선택 표시·안내 문구·제출 버튼만 갱신합니다. 이미지/도식과 기존 버튼을 버리지 않으므로 포커스와 스크롤을 유지합니다.
- 현재 그림은 QuestionImages가 한 장만 보관합니다. 확대와 해설에서도 같은 이미지를 사용하며, 다른 그림 문항으로 바뀌면 교체합니다. 앱 전체 이미지의 무제한 전역 캐시는 만들지 않습니다.
- 힌트를 열어도 사용 중인 장면이나 단서를 초기화하지 않습니다.

## 검증과 호환성

- 8개 콘텐츠 TSV와 5개 사용자 TSV의 파일명·헤더·ID 의미를 바꾸지 않았습니다. 기존 52문항·26단계·XP·복습 정책을 유지합니다.
- RepositoryContractTest는 파일 대신 메모리 저장소를 주입하여 제출/단계 완료/프로필/메모/북마크 저장 실패와 재시도, 사용자 격리를 검증합니다.
- UiSmokeTest는 모든 문항을 실제 Swing 버튼으로 풀고, 장면 1 우선 표시와 단서 변경 후 안내·화면 유지도 검사합니다.
- build.ps1은 프로젝트의 build/classes만 비운 뒤 컴파일합니다. 패키지를 이동한 이전 클래스가 배포 JAR에 남지 않습니다.

## 단계와 자동 재도전

- 필수 문항은 data/steps.tsv 행 순서로 읽습니다. 그림 기초 → KBO 도식 판정 → 그림 응용 → 기록 읽기 순으로 26 STEP을 구성했습니다. 원래 V1의 6단계는 영구 ID와 문제 묶음을 유지합니다.
- `startStep`은 완료한 단계와 바로 다음 단계만 허용합니다. `StepSelector`는 새 문항 1개 → 필요한 이전 규칙 최대 2개 → 남은 새 문항 순서로 배치합니다. 필수 문항과 같은 규칙·유형은 추가하지 않습니다.
- 재도전 대상은 규칙·유형별 최신 제출로 결정합니다. 아직 잠긴 단계의 문항은 후보에서 제외합니다. 미독립 해결을 먼저, 오래된 제출을 그다음 기준으로 선택합니다. 직전과 다른 문항을 우선하되, 2회 연속 미독립 해결이면 기본 상황을 우선합니다.
- 연속 독립 해결 1/2/3회 이상에 따라 1/3/7일 간격을 사용합니다. 간격은 제품 설계값입니다.
- 완료 저장 실패 시 FEEDBACK 상태를 유지하여 결과 보기 버튼으로 다시 시도합니다. 정답률은 단계 해제 조건이 아닙니다.
- 이전 문항별 `reviewQuestions` API는 기존 호출 호환과 회귀 검사를 위해 남기며, 현재 UI의 자동 재출제는 `adaptiveQuestions`를 사용합니다.
- 기존 attempts/bookmarks/notes 파일은 바꾸지 않습니다. steps.tsv가 없는 기존 사용자의 새 학습 경로는 STEP 1에서 시작합니다.

## V1.2 구단/선수 표시

Catalog가 teams → players → matchups 및 scenes를 연결합니다. Team의 색상은 RGB 정수로 보관하여 model에 Swing/AWT 의존성을 추가하지 않습니다. FieldPanel과 QuizPanel은 같은 Team을 사용해 선수 마커, 이름표, 배지, 단서 버튼을 그립니다. 가까운 선수는 위치를 유지하고 이름표만 양옆으로 배치합니다. KboContentTest에서 소속 연결, 실제 픽셀의 팀색, 선수 클릭과 오류 데이터 거부를 검사합니다.

## 통합 버전의 호환 경계

`Question.hasImage()`가 렌더러를, `requiresClue()`가 입력 단계를 결정합니다. 저장·채점·프로필은 동일한 서비스를 사용합니다. `LearningStep.number`는 화면 순번, `key`는 userdata/steps.tsv의 기존 step 값입니다. 자세한 원본 출처와 변환은 [INTEGRATION.md](INTEGRATION.md)를 참고하세요.
