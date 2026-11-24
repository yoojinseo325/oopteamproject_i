package baseup.ui;

import baseup.model.RuleConcept;
import baseup.model.UserProfile;
import baseup.service.LearningService;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.BorderLayout;
import java.util.List;
import static baseup.ui.Theme.*;
import static baseup.ui.PageComponents.*;

/** 프로필 선택과 메모 입력 대화상자. 창 탐색은 ScreenActions에 위임한다. */
final class LearningDialogs {
    private final Component owner;
    private final LearningService service;
    private final ScreenActions actions;

    LearningDialogs(Component owner, LearningService service, ScreenActions actions) {
        this.owner = owner;
        this.service = service;
        this.actions = actions;
    }

    public void editNote(RuleConcept initial) {
        // 백과사전에서는 아직 문제로 만나지 않은 규칙에도 메모할 수 있다.
        List<RuleConcept> discovered=service.catalog().rules().stream().filter(r->service.discovered(r.id())||r.equals(initial)).toList();
        if(discovered.isEmpty()){
            actions.message("먼저 첫 단계에 도전해 규칙을 발견해 보세요.");
            return;
        }
        JComboBox<RuleConcept> rule=new JComboBox<>(discovered.toArray(RuleConcept[]::new));
        if(initial!=null)rule.setSelectedItem(initial);
        JTextArea body=new JTextArea(7,36);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setFont(font(14,false));
        JPanel form=new JPanel(new BorderLayout(0,12));
        form.add(rule,BorderLayout.NORTH);
        form.add(new JScrollPane(body));
        form.add(label("최대 500자 · 파일에 저장되어 다음 실행에도 남아요.",12,MUTED,false),BorderLayout.SOUTH);
        while(JOptionPane.showConfirmDialog(owner,form,"내 말로 메모하기",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                service.addNote(((RuleConcept)rule.getSelectedItem()).id(),body.getText());
                actions.message("메모를 저장했습니다.");
                actions.showPage("notes");
                return;
            }
            catch(Exception e){
                actions.error(e);
            }
        }
    }

    public void selectProfile() {
        JComboBox<UserProfile> people=new JComboBox<>(service.profiles().toArray(UserProfile[]::new));
        people.setSelectedItem(service.user());
        Object[] options={
            "선택한 사용자로 전환","새 사용자 추가","취소"
        };
        int answer=JOptionPane.showOptionDialog(owner,people,"학습할 사용자",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,options,options[0]);
        if(answer==0){
            service.selectUser(((UserProfile)people.getSelectedItem()).id());
            actions.message("사용자를 전환했습니다.");
        }
        else if(answer==1){
            String nickname=JOptionPane.showInputDialog(owner,"닉네임을 입력하세요. (1~16자)");
            if(nickname!=null)try{
                service.addProfile(nickname);
                actions.message("새 사용자를 저장했습니다.");
            }
            catch(Exception e){
                actions.error(e);
            }
        }
        actions.showPage("home");
    }
}
