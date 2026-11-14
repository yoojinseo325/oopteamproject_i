# 엔진부 보고서 및 팀원 변경 비교

확인일: 2026-11-15 (한국 시간)

## 비교 기준

- 로컬: main의 ecd9bd0 및 현재 작업 폴더에 존재하는 미추적 파일.
- 원격 main: af7a4a2. ecd9bd0 이후 커밋은 2개이지만 최종 추적 파일 내용은 ecd9bd0과 동일하다.
- 원격 수정본: restore-baseup-source의 7a1266be7afb43c2857946ea968b2b0827f5b4f1.
- 첨부 보고서: [2026-11-14 팀원 검토 초안](C:/Users/seong/Downloads/BASEUP_중간보고서_팀원검토용_restore_초안.pdf), 2~4쪽.

원격 정보를 fetch하여 비교했다. 원격 소스와 데이터를 로컬 작업 파일에 병합하거나 덮어쓰지는 않았다. 아래 실행 결과는 현재 로컬 코드의 결과이며 원격 브랜치 실행 결과가 아니다.

## 팀원이 올린 변경

| 시각 (2026-11-12, 한국 시간) | 작성자·브랜치 | 변경 |
|---|---|---|
| 00:27 | ywmh00 · main | ImageView.java 추가 후 다음 커밋에서 삭제. 최종적으로 원래 main과 파일 내용이 같다. |
| 00:31~00:32 | ywmh00 · fix/missing-resource | ImageView.java, image_questions.tsv, steps.tsv 추가. PNG는 포함되지 않았다. |
| 21:04 | Jinseo Yoo · restore-baseup-source | 위 3개 파일과 PNG 40개를 포함해 총 43개 파일 추가. 3개 텍스트 파일은 fix/missing-resource와 동일하다. |

근거: [ImageView 추가](https://github.com/yoojinseo325/oopteamproject_i/commit/79961f947028c49e38cdfd368b6241cdd5830028), [main에서 삭제](https://github.com/yoojinseo325/oopteamproject_i/commit/af7a4a2b958f908d7fb896c9851bb60a4e59813a), [수정 브랜치의 코드 추가](https://github.com/yoojinseo325/oopteamproject_i/commit/6ea6ef6b63a1481da84d44500b0140698e6436e2), [데이터 추가](https://github.com/yoojinseo325/oopteamproject_i/commit/42329d69bce288af4ca5127cead80f9a65ab7d2f), [복구 브랜치 업로드](https://github.com/yoojinseo325/oopteamproject_i/commit/7a1266be7afb43c2857946ea968b2b0827f5b4f1).

Git에서는 신규 파일로 표시되지만 로컬에는 이미 미추적 상태로 존재한다. 따라서 실제 사용 중인 로컬 파일과 별도로 내용을 비교했다. PNG 40개는 로컬과 바이트 단위로 같으며, 다른 파일은 아래 3개다.

### 1. 그림 문제 데이터

- 문제 수 40개, 문제 ID, 규칙 연결, 답 선택지, 답 정답 번호, 정답 해설, 그림 파일명은 유지됐다.
- 40문항 모두 제목을 바꿨다. 예: E02의 `한 차례씩 주고받기`가 `이닝`으로 변경됐다.
- 난이도 표기를 입문→기초, 기본→중간, 응용→판정으로 바꿨다. 심화는 유지됐다.
- E/M/H의 공통 상황 안내를 비웠고, U 10문항의 질문란에는 상황 설명 전체를 다시 넣었다.
- 40문항의 힌트를 공통 문장인 `장면과 문제의 조건을 하나씩 확인하세요.`로 바꿨다.
- 40문항의 오개념 설명란에 `문제에서 제시된 조건을 생략하지 않고 판단하세요.`를 넣었다.
- 이미지 안내 문장을 각 그림의 구체적인 장면 설명으로 바꿨다.
- U 10문항의 근거 선택지를 다시 작성했고, 그중 7문항의 정답 근거 ID가 달라졌다. 답 선택지의 정답 번호 변경과는 구분해야 한다.

### 2. STEP 데이터

- 총 26 STEP, 각 기본 문항 2개, 전체 52문항은 유지됐다.
- 목표 문구 26개가 모두 바뀌었다.
- 영구 STEP ID 기준 23개 단계의 문제 묶음이 바뀌었고 일부 ID의 표시 순서도 이동했다.
- 기존에는 기본 판단과 조건 변화 문제를 다음 단계에서 만났지만, 수정본은 같은 규칙의 문항을 같은 단계로 묶는 구성이 늘었다.
- 예: 영구 ID 1의 Q01/Q03이 Q01/Q02로, ID 2의 Q02/Q04가 Q03/Q04로 바뀌었다. 화면 순서상 STEP 7과 8에 해당한다.
- U01~U10 판정 문제는 화면 STEP 22~26에 모였다.

### 3. ImageView

- 이미지 경로가 null이거나 일반 파일이 아닌 경우를 먼저 검사한다.
- 기본 선호 크기가 600×400에서 730×320으로 변경됐다. 실제 표시 크기는 부모 레이아웃의 영향도 받는다.
- 그림 주변에 약 10px씩 여백을 두고, 그림 크기가 0이 되지 않게 보정한다.
- 이미지 확대·축소 보간을 bicubic에서 bilinear로 바꿨다.
- Graphics2D 정리를 finally에서 수행한다.
- 기존 최소 크기 지정과 접근성 설명 설정은 빠졌다. title 필드를 추가했지만 현재 그리기에는 사용하지 않는다.

model, service, repository의 Java 코드는 변경되지 않았다. 채점, XP 산정, 자동 복습 알고리즘, 사용자 기록 저장 로직은 그대로다. 콘텐츠 변경으로 실제 출제 순서와 화면에 보이는 내용이 달라진다.

## 반영 전에 확인할 부분

1. **제목의 정답 노출:** E02는 질문이 이닝의 이름을 묻는데 제목에 `이닝`이 표시된다. E04의 `볼넷`, E05의 `포수` 등도 정답을 먼저 알려줄 수 있다. 학습 내용과 정답을 문제를 풀며 발견하도록 설계한 기존 방향과 맞는지 확인해야 한다.
2. **기존 진도의 의미 변경:** 완료 기록은 화면 순번이 아닌 영구 STEP ID에 연결된다. 같은 ID의 문제 묶음을 바꾸면 이전 완료 기록이 새 문제 묶음의 완료로 해석된다. 기존 사용자에게 적용하려면 새 ID를 부여하거나 완료 기록의 이전 정책을 정해야 한다.
3. **긴 문제의 중복 표시:** U 10문항은 situation과 prompt가 같은 긴 문장이다. 현재 화면은 두 항목을 모두 표시하므로 상황을 반복해서 읽게 된다.
4. **원격 테스트 소스 누락:** restore 브랜치에는 ContentIntegrationTest.java가 없지만 test.ps1은 해당 클래스를 실행하도록 되어 있다. 보고서의 지적은 이 원격 브랜치 기준으로 맞다. 로컬에는 파일이 있지만 아직 Git에 추가되지 않았다.
5. **브랜치별 완성도 차이:** 원격 main에는 ImageView, 그림 문제·STEP 데이터, PNG 40개가 없다. fix 브랜치에는 PNG가 없다. restore 브랜치에는 이 자료들이 있지만 위 테스트 클래스는 없다. main만 내려받은 상태와 현재 로컬의 상태를 동일하게 보아서는 안 된다.

## 보고서와 실제 엔진의 일치 여부

핵심 구조와 기능 목록은 대체로 일치한다. 34개 개념, 52문항, 26 STEP, TSV 파일 처리, 사용자별 기록, 상태 관리, 자동 복습에 대한 설명은 코드로 확인된다. 다음 표현을 보완하면 더 정확하다.

| 보고서 항목 | 실제 구현에 맞춘 설명 |
|---|---|
| 이미지 문제의 유형 | E/M/H/U는 콘텐츠 분류이며, 실제 입력 흐름은 답 선택형 30문항과 답·근거형 22문항으로 나뉜다. |
| XP 기록 | 풀이 기록을 저장하고, 문항별 최고 성취 점수를 합산해 XP를 계산한다. XP 숫자를 별도 파일에 저장하지 않는다. |
| TC-01·02의 제출 | 답 선택형은 답만 선택해 제출한다. 판정형은 답과 유효한 근거를 모두 골라야 제출할 수 있다. |
| TC-04 중복 제출 | 같은 세션에서 이미 제출한 문항을 다시 제출하는 것을 막는다. 새 도전에서 같은 문항을 다시 푸는 것은 허용한다. |
| TXT 기반 저장 | 실제 형식과 확장자는 UTF-8 TSV(.tsv)다. |
| 테스트 클래스 없음 | 원격 restore 브랜치에서는 맞다. 현재 로컬에는 존재하지만 미추적 상태다. |

## 보고서에 사용할 수 있는 엔진부 구현 설명

### ① 설계 구조와 데이터 관리

BASEUP의 엔진부는 model, service, repository로 역할을 분리하였다. model은 문제와 규칙, 제출 결과 및 퀴즈 상태를 표현하고, service는 출제·채점·진도·복습을 처리하며, repository는 텍스트 파일의 읽기와 쓰기를 담당한다. Manager<T>는 제네릭과 ArrayList를 이용해 객체를 관리하고, Repository<T>와 QuestionSelector 인터페이스를 통해 저장 방식과 출제 방식을 분리하였다. Swing 화면은 ui 패키지에서 구성한다.

Catalog는 8개 콘텐츠 TSV 파일을 읽어 34개 규칙·개념과 52문항, 26단계를 연결한다. 파일 헤더, ID 중복, 정답 번호, 규칙·장면·선수 연결, 이미지 파일, 단계별 문항 배정을 검사하며 잘못된 콘텐츠가 있으면 명시적으로 오류를 알린다.

### ② 문제 진행과 채점

Question은 문항에 지정된 정답 번호와 정답 근거 ID를 이용해 입력을 비교한다. 답 선택형 30문항은 답을 제출한 뒤 해설로 이동하고, 판정형 22문항은 답 선택과 근거 선택을 거쳐 두 결과를 각각 채점한다. QuizSession은 JUDGE, CLUE, FEEDBACK, COMPLETE 상태를 관리하여 입력 순서 오류와 동일 세션의 중복 제출을 방지한다.

Attempt에는 사용자와 문항 ID, 선택한 답과 근거, 각각의 정오답, 힌트 사용 여부, 풀이 시간과 제출 시각을 기록한다. 필요한 답과 근거를 모두 맞히면 이해한 문제로, 여기에 힌트까지 사용하지 않았다면 독립 해결로 분류한다. 답 선택형의 근거는 해당 없음으로 처리한다.

### ③ 단계 진행과 자동 복습

학습 경로는 data/steps.tsv에서 읽는다. 완료한 단계와 첫 미완료 단계를 시작할 수 있으며, 마지막 문항의 해설을 확인하고 완료 처리를 하면 다음 단계가 열린다. 정답률 자체는 단계 해제 조건이 아니다. 화면의 단계 번호와 저장용 영구 ID를 구분해 진도를 관리한다.

StepSelector는 현재 단계의 기본 문항 사이에 복습 문항을 최대 2개 배치한다. 자동 복습은 규칙과 문제 유형별 이력을 기준으로 오답·잘못된 근거·힌트 사용 기록을 우선 처리하며, 가능한 경우 직전과 다른 문항을 선택한다. 독립 해결이 이어지면 1일, 3일, 7일 간격으로 복습한다. 아직 열리지 않았고 풀이 이력도 없는 문항은 복습 후보에서 제외한다.

### ④ XP와 사용자 기록

XP는 저장된 풀이 이력에서 문항별 최고 성취 점수를 계산해 합산한다. 판정형은 답만 정답이면 10점, 필요한 답과 근거가 모두 정답이면 20점, 힌트 없이 해결하면 30점이다. 답 선택형은 정답이면 20점, 힌트 없이 해결하면 30점이다. 같은 문제를 여러 번 풀어도 최고 성취를 초과해 점수가 중복 누적되지 않는다.

프로필, 제출 기록, 단계 완료, 북마크, 메모는 사용자 ID로 구분하여 userdata 아래 TSV 파일에 저장하고 재실행 시 복원한다. 미완료 미션의 중간 위치는 저장하지 않으므로 재시작하면 해당 단계를 처음부터 진행하며 이미 제출한 결과는 유지한다.

### ⑤ 파일 처리와 예외 대응

FileRepository는 UTF-8로 파일을 처리하고 Tsv는 탭·줄바꿈·역슬래시를 변환한다. 저장할 때 임시 파일을 완성한 뒤 기존 파일을 교체하며, 파일 시스템이 지원하면 원자적 교체를 사용한다. LearningService는 저장 성공 후에 메모리와 퀴즈 상태를 갱신하므로 저장 실패 시 같은 입력으로 다시 제출할 수 있다. App은 동일한 기록 폴더를 여러 실행 창에서 동시에 사용하지 못하도록 파일 잠금을 사용한다.

## 이번 실행 검증 결과

2026-11-15에 현재 로컬 코드로 빌드 및 테스트를 실행했다. 테스트는 임시 사용자 기록을 사용했다.

| 검사 | 결과 |
|---|---|
| Java 빌드 | 통과 |
| ServiceTest | 161개 검사 통과 |
| StepFlowTest | 첫 실행과 개별 재실행 모두 실패 |
| KboContentTest | 241개 검사 통과 |
| ContentIntegrationTest | 12개 검사 통과 |
| UiSmokeTest | 914개 검사 통과 |

StepFlowTest는 FileRepository.save의 임시 파일→attempts.tsv 교체 과정에서 AccessDeniedException이 발생했다. 문제 데이터의 정답 실패와는 구분되며, 파일 접근 거부의 원인은 이번 확인만으로 확정하지 않았다. 따라서 현재 상태를 전체 테스트 통과로 보고서에 적으면 안 된다. 보고서에는 단계 흐름 검사 중 기록 파일 교체 실패가 재현되어 추가 점검 중이라고 적는 것이 정확하다.

실행 로그: [전체 실행](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/build/engine-review-2026-11-15.log), [단계 재검사](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/build/engine-review-2026-11-15-StepFlowTest.log), [KBO 검사](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/build/engine-review-2026-11-15-KboContentTest.log), [콘텐츠 검사](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/build/engine-review-2026-11-15-ContentIntegrationTest.log), [화면 검사](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/build/engine-review-2026-11-15-UiSmokeTest.log).

주요 로컬 근거: [Catalog](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/src/main/java/baseup/repository/Catalog.java), [LearningService](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/src/main/java/baseup/service/LearningService.java), [StepSelector](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/src/main/java/baseup/service/StepSelector.java), [QuizSession](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/src/main/java/baseup/model/QuizSession.java), [Question](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/src/main/java/baseup/model/Question.java), [Attempt](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/src/main/java/baseup/model/Attempt.java), [FileRepository](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/src/main/java/baseup/repository/FileRepository.java), [Manager](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/src/main/java/baseup/service/Manager.java), [App](C:/JavaStudy/ObjectOrientedProgramming/BaseUp/src/main/java/baseup/App.java).
