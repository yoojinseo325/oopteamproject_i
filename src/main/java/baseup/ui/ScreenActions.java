package baseup.ui;

import baseup.model.RuleConcept;

/** 개별 화면은 창 구현을 알지 않고 필요한 탐색/알림만 요청한다. */
public interface ScreenActions {
    void startNext();
    void startStep(int number);
    void showPage(String page);
    void editNote(RuleConcept initial);
    void message(String text);
    void error(Exception error);
}
