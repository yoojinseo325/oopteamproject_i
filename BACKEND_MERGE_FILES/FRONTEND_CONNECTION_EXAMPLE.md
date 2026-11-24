# 프론트 담당자에게 전달할 코드

`QuizPanel.java`에서 다음 분기만 맞추면 됩니다:

```java
QuizQuestion q = session.getCurrentQuestion();
if (q.isMultipleChoice()) {
    // q.getOptions(): 길이 4인 List<String>
    // 사용자가 클릭한 1~4 번호를 제출
    boolean correct = session.submitChoice(selectedNumber);
} else {
    boolean correct = session.submit(inputField.getText());
}
// q.hasScenario()라면 ScenarioManager.getMedia(q)로 동영상 경로 전달
```

백엔드의 `QuizResult`가 정답/오답과 실제 입력/선택한 보기를 저장하고,
`StatisticsManager`가 사용자 폴더별 대시보드 값을 계산합니다.

전체 Swing 예제는 별도의 실행형 프로젝트 ZIP에서 확인할 수 있습니다.
