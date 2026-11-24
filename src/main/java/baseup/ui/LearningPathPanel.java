package baseup.ui;

import baseup.service.LearningService;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.BorderLayout;

import static baseup.ui.Theme.*;
import static baseup.ui.PageComponents.*;

/** 순차 학습 경로 화면. */
public final class LearningPathPanel extends JPanel {
    private final LearningService service;
    private final ScreenActions actions;

    public LearningPathPanel(LearningService service, ScreenActions actions) {
        super(new BorderLayout());
        this.service = service;
        this.actions = actions;
        setOpaque(false);
        add(build(), BorderLayout.CENTER);
    }

    private JComponent build() {
        JPanel p=page("YOUR LEARNING PATH","한 단계씩, 다음 베이스로.","새로운 상황 사이에 이전 판단이 다시 등장해요. 실수는 다음 도전을 준비하는 단서입니다.");
        if(service.courseComplete()){
            Theme.add(p,button(nextAction(service),true,actions::startNext));
            gap(p,20);
        }
        Theme.add(p,new LearningPathView(service, actions));
        return scroll(p);
    }
}
