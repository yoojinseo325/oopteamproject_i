# 데이터 파일 규격 · 통합 V2.1

모든 파일은 UTF-8 TSV(열 구분은 실제 탭)입니다. 첫 행의 열 이름·순서는 정확히 유지합니다. 공백이 들어간 문장은 한 셀에 보관됩니다. CSV의 쉼표 구분과 혼동하지 마세요.

셀 안의 특수문자는 `\\`(역슬래시), `\t`(탭), `\n`(줄바꿈), `\r`로 저장합니다. `Tsv`가 읽기/쓰기를 담당합니다. UTF-8 BOM은 첫 행에서 허용합니다. 빈 행은 건너뛰지만 잘못된 데이터 행은 오류를 표시하고 실행을 멈춥니다.

## 팀이 수정하는 콘텐츠

### data/rules.tsv

`id, topic, title, summary, detail, source`

34개 규칙·개념을 담습니다. 같은 판단은 기존 ruleId를 재사용하세요. STEP은 data/steps.tsv에서 관리하며 Java의 고정 목록이나 단계 상한을 바꿀 필요가 없습니다. 같은 규칙·같은 문제 유형의 문항은 해당 단계가 열린 뒤 복습 후보가 됩니다.

### data/questions.tsv

`id, ruleId, title, level, situation, prompt, choices, answer, cluePrompt, clueTarget, hint, explanation, misconception, beforeScene, afterScene`

- `id`: 고유 문항 ID. 기존 학습 기록이 참조하므로 의미가 바뀐 문항에 재사용하지 않습니다.
- `ruleId`: rules.tsv의 규칙 ID.
- `choices`: `아웃|아웃 아님`처럼 `|`로 선택지 구분. 현재 콘텐츠는 2개 또는 4개 선택지를 사용합니다.
- `answer`: **0부터 시작**하는 정답 번호. 첫 선택지는 0, 두 번째는 1.
- `clueTarget`: `beforeScene`의 토큰 ID. 모든 단서는 장면 1에서 고릅니다.
- `beforeScene`, `afterScene`: scenes.tsv의 ID. “before”는 파일 이름상의 표현이며, 태그업처럼 장면 1이 야수의 첫 접촉 시점인 문제도 있습니다.
- `explanation`: 왜 정답인지, `misconception`: 틀리기 쉬운 생각을 교정하는 설명.

### data/image_questions.tsv · data/images/

`id, ruleId, title, level, situation, prompt, choices, answer, cluePrompt, clueTarget, hint, explanation, misconception, imageFile, imageGuide, clueChoices`

- V2 문항 40개를 통합 형식으로 옮긴 파일입니다. ID E01~U10과 선택지 순서는 원본을 유지하고 정답 번호만 1 기반에서 **0 기반**으로 바꿨습니다.
- `imageFile`: data/images/ 아래의 PNG 파일명. 영문·숫자·밑줄·하이픈만 사용합니다. 외부 경로·상위 폴더 접근을 허용하지 않습니다. 누락되거나 깨진 그림은 시작 시 오류로 표시합니다.
- `imageGuide`: 답을 알려주지 않는 그림 안내. 원본 V2 learningTerm/learningDescription은 질문 제목에 표시하지 않고 힌트와 제출 후 카드로 사용합니다.
- 기초형: cluePrompt, clueTarget, clueChoices를 빈 값으로 둡니다. 답을 제출하면 바로 해설로 이동합니다.
- 판정형: `clueChoices`에 `a:첫 번째 조건|b:두 번째 조건|c:세 번째 조건`을 넣고, clueTarget에 정답 ID를 넣습니다. 그림 좌표를 가짜 단서로 취급하지 않고 문장으로 근거를 선택합니다.
- 같은 개념이라도 기초형과 판정형의 복습 성공은 별도로 계산합니다.

### data/steps.tsv

`id, goal, questionIds`

- **행 순서가 화면의 STEP 번호**입니다. ID는 저장된 완료 기록의 영구 키이므로 행을 옮겨도 변경하지 않습니다.
- `questionIds`: `E01|E05`처럼 기본 문항을 나열합니다. 한 단계는 1~4문항을 허용하며 현재는 2문항씩입니다. 모든 문항을 전체 경로에 정확히 한 번 배정해야 합니다.
- 현재 ID 1~6은 V1의 원래 단계 내용을 유지합니다. 신규 ID 7~26을 사이에 배치했습니다. 기존 완료 기록에 새 문제의 완료를 잘못 부여하지 않습니다.
- `goal`은 시작 전 볼 수 있는 일반적인 목표입니다. 정답이나 미래 문항의 구체적인 내용을 적지 않습니다.
- 단계 사이 복습은 최대 2문항이며 핵심 문항과 같은 복습 기준이 겹치면 별도로 끼우지 않습니다.

### data/scenes.tsv

`id, caption, tokens`

`tokens`는 `토큰ID,화면이름,종류,x,y,playerId` 묶음을 `|`로 연결합니다.

```text
r1,1루 주자,runner,590,225,kia_na|batter,타자,runner,440,405,kia_kim
```

종류는 `runner`(공격), `fielder`(수비), `ball`(공), `fact`(카운트·조건 정보) 중 하나입니다. 각 장면 안의 토큰 ID는 중복할 수 없습니다. 토큰의 화면 이름에는 `,` 또는 `|`를 넣지 마세요.

가상 캔버스는 **800×500**이고 화면 크기에 맞게 같은 비율로 축소됩니다. 좌표는 x 30~770, y 35~470 범위입니다. 홈 (400,425), 1루 (600,265), 2루 (400,112), 3루 (200,265)입니다. 도형·선수 위 설명은 한 토큰에 너무 길게 넣지 말고 caption과 문제 상황에 나누어 쓰세요.

단서를 그림에서 선택할 때와 아래 버튼에서 선택할 때 동일한 ID를 서비스에 전달합니다. 그림만 보고 판단하기 어려운 보유·태그·첫 접촉 조건은 반드시 situation/caption에 명시합니다.

### data/teams.tsv · players.tsv · matchups.tsv

- teams.tsv: `id, name, shortName, color, accent`. color/accent는 #RRGGBB 형식입니다.
- players.tsv: `id, name, teamId`. teamId는 teams.tsv를 참조합니다.
- matchups.tsv: `id, offense, defense`. id는 문항 ID이며 공격·수비 구단은 서로 달라야 합니다.
- Scene.Token의 마지막 playerId는 players.tsv를 참조합니다. runner/fielder에는 반드시 배정하고 ball/fact에는 비워 둡니다. 토큰의 기존 5항목 형식도 읽지만 선수 마커에는 playerId가 필요합니다.
- 양 장면의 선수 소속이 문항의 공격/수비와 일치해야 합니다. 구단이나 선수 연결이 틀리면 실행을 중단하고 오류를 표시합니다.
- 이름은 표시용이며 채점은 기존 questionId/clueTarget으로 처리합니다. 개인정보 저장 파일의 형식은 바꾸지 않습니다.
- [선수 배정·구단 색상 출처](KBO_CONTENT.md)를 확인한 뒤 데이터를 갱신하세요.

## 프로그램이 저장하는 userdata/

| 파일 | 열 |
|---|---|
| profiles.tsv | id, nickname |
| attempts.tsv | id, userId, questionId, choice, clue, answerCorrect, clueCorrect, hintUsed, seconds, at |
| bookmarks.tsv | id, userId, ruleId |
| notes.tsv | id, userId, ruleId, text |
| steps.tsv | id, userId, step, at |
| .lock | 실행 중 파일 잠금을 위한 파일, 학습 정보 아님 |

ID는 UUID이며 북마크 ID는 `userId:ruleId`, 단계 완료 ID는 `userId:step`입니다. step은 data/steps.tsv의 영구 ID인 양의 정수이며 화면의 순번과 다릅니다. 완료 기록을 모두 집계하고 다음 시작 버튼은 첫 미완료 단계로 이동합니다. steps.tsv가 없으면 기존 풀이 기록을 유지한 채 새 경로를 STEP 1부터 시작합니다. Boolean 값은 `true`/`false`, 시각은 ISO-8601 UTC(Instant)입니다. 화면에서 풀이 시각은 컴퓨터의 시간대로 표시합니다. 풀이 시간에는 장면 관찰과 힌트 확인 시간이 포함됩니다.

기초형 제출은 clue=`@not-required`, clueCorrect=`false`입니다. understood는 기초형에서는 답만, 판정형에서는 답과 단서를 함께 검사합니다. 과거 V1 파일의 열과 값은 바뀌지 않습니다.

시도 기록은 성공적으로 저장한 순서대로 쌓입니다. 최신 결과는 마지막 제출로 찾습니다. 과거 문항 ID를 콘텐츠에서 삭제한 경우 기록 표에서는 “삭제된 문항”으로 표시하고 현재 진도/복습에서는 제외합니다. **규칙은 북마크·메모도 참조하므로 기존 규칙 ID를 삭제하기 전에 마이그레이션해야 합니다.** 기존 Q 문항·규칙 ID와 영구 STEP ID를 유지해 V1 기록을 그대로 읽습니다. V2의 익명 results.txt는 사용자와 문항별 근거 정보가 없어 자동 합산하지 않습니다. 현재 작업 폴더에는 해당 결과 파일이 없습니다.

파일은 저장할 때 같은 폴더에 임시 파일을 완성하고 기존 파일을 교체합니다. 가능한 경우 원자적 교체를 쓰고, 지원하지 않는 파일 시스템에서는 일반 교체로 대체합니다. 한 기록 폴더에 두 앱이 동시에 쓰지 않도록 App이 잠금 파일을 사용합니다.

## 새 문항 추가 순서

1. rules.tsv에서 학습 목표 규칙을 선택합니다.
2. 다른 문항의 정답을 단순히 외워 맞힐 수 없도록 중요한 조건을 바꿉니다.
3. scenes.tsv에 장면 두 개와 고유 ID를 추가합니다.
4. questions.tsv에 새 ID로 문제를 추가하고 `clueTarget`이 장면 1에 있는지 확인합니다.
5. data/steps.tsv에 새 문항을 정확히 한 번 배정합니다. 새 단계는 사용한 적 없는 ID를 부여합니다.
6. 카운트, 주자 배치, 공 보유, 포구, 태그, 어필 같은 필요 조건을 빠뜨리지 않았는지 검토합니다.
7. 기본 데이터 검증 뒤 실제 화면에서 문장 길이·토큰 겹침·정답/단서를 확인합니다.

서비스 테스트의 Q01~Q12 정답 목록은 기존 V1 콘텐츠 검수 기준입니다. 통합 테스트는 52문항·26단계의 연결 및 전체 화면 풀이를 검증합니다. 문항을 정식 확장한 뒤에는 검수 결과에 맞게 테스트 기대값도 수정하세요. 실행 시 Catalog의 형식 검증은 문항 수와 무관하게 적용됩니다.
