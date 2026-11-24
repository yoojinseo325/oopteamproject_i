package baseup.ui;

import baseup.model.Question;
import baseup.model.QuizSession;
import baseup.model.Scene;
import baseup.repository.Catalog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import static baseup.ui.Theme.*;
import static baseup.ui.QuizText.copy;

/** 장면 전환, KBO 선수 도식, 단서 강조를 담당한다. */
final class SceneQuestionPanel extends QuestionVisual {
    private final Catalog catalog;
    private final QuizSession session;
    private final Consumer<String> onClue;
    private final Map<String, ActionButton> clueButtons = new LinkedHashMap<>();
    private final Consumer<String> message;
    private boolean before = true;
    private FieldPanel field;
    private JLabel caption;
    private ActionButton first;
    private ActionButton second;

    SceneQuestionPanel(Catalog catalog, QuizSession session, Consumer<String> onClue, Consumer<String> message) {
        this.catalog = catalog;
        this.session = session;
        this.onClue = onClue;
        this.message = message;
        setLayout(new BorderLayout());
        setOpaque(false);
        add(build(session.current()), BorderLayout.CENTER);
    }

    private JPanel build(Question q) {
        RoundedPanel card=card();
        card.setBorder(new EmptyBorder(20,20,20,20));
        card.setLayout(new BorderLayout(0,12));
        JPanel top=column();
        Theme.add(top,label("FIELD VIEW   /   장면을 관찰하세요",11,GREEN,true));
        gap(top,14);
        JPanel toggle=new JPanel(new GridLayout(1,2,8,0));
        toggle.setOpaque(false);
        first=button("장면 1 · 상황 시작",false,()->setScene(true));
        second=button("장면 2 · 판정 순간",false,()->setScene(false));
        first.setName("before");
        second.setName("after");
        toggle.add(first);
        toggle.add(second);
        Theme.add(top,toggle);
        card.add(top,BorderLayout.NORTH);
        if(session.stage()==QuizSession.Stage.CLUE)before=true;
        Scene scene=catalog.scene(before?q.beforeScene():q.afterScene());
        field=new FieldPanel(scene);
        field.setPreferredSize(new Dimension(500,350));
        first.mark(before);
        second.mark(!before);
        if(session.stage()==QuizSession.Stage.CLUE)field.onSelect(onClue);
        if(session.stage()==QuizSession.Stage.FEEDBACK) {
            before=true;
            field.scene(catalog.scene(q.beforeScene()));
            field.selected(q.clueTarget());
            first.mark(true);
            second.mark(false);
        }
        JPanel middle=new JPanel(new BorderLayout(0,10));
        middle.setOpaque(false);
        middle.add(field,BorderLayout.CENTER);
        caption=label("<html>"+html(catalog.scene(before?q.beforeScene():q.afterScene()).caption())+"</html>",12,MUTED,false);
        caption.setPreferredSize(new Dimension(300,45));
        middle.add(caption,BorderLayout.SOUTH);
        card.add(middle,BorderLayout.CENTER);
        JPanel bottom=column();
        Theme.add(bottom,label("R 공격 · D 수비  /  선수 색상 = 소속 구단  /  ! 상황 정보",11,MUTED,false));
        gap(bottom,15);
        if(session.stage()==QuizSession.Stage.CLUE) {
            Theme.add(bottom,label("그림 속 대상을 클릭하거나 아래에서 선택하세요.",12,GREEN,true));
            gap(bottom,9);
            JPanel options=new JPanel(new GridLayout(0,2,7,7));
            options.setOpaque(false);
            for(Scene.Token token:catalog.scene(q.beforeScene()).tokens()) {
                String copy=token.player()==null?html(token.label()):html(token.player().displayName())+"<br>"+html(token.label());
                ActionButton b=button("<html><center>"+copy+"</center></html>",false,()->onClue.accept(token.id()));
                b.setFont(font(11,true));
                b.setBorder(new EmptyBorder(8,5,8,5));
                b.setName("clue-"+token.id());
                b.getAccessibleContext().setAccessibleName(token.displayLabel());
                if(token.player()!=null)b.teamColor(new Color(token.player().team().color()));
                b.mark(session.clue().equals(token.id()));
                options.add(b);
                clueButtons.put(token.id(),b);
            }
            Theme.add(bottom,options);
            field.selected(session.clue());
        }
        else {
            RoundedPanel note=new RoundedPanel(PALE,12);
            note.setLayout(new BorderLayout());
            note.setBorder(new EmptyBorder(14,14,14,14));
            JTextArea copy=text(session.stage()==QuizSession.Stage.FEEDBACK?"노란 테두리의 단서가 판정을 바꿉니다. 장면을 바꾸며 조건을 비교해 보세요.":"장면 1과 장면 2를 번갈아 보세요. 그림은 상황을 단순화한 도식이며, 공의 보유·접촉 시점은 오른쪽 설명을 함께 확인해요.",12,MUTED);
            copy.setRows(3);
            note.add(copy);
            Theme.add(bottom,note);
        }
        card.add(bottom,BorderLayout.SOUTH);
        return card;
    }

    private void setScene(boolean showBefore) {
        if(session.stage()==QuizSession.Stage.CLUE && !showBefore) {
            message.accept("단서는 장면 1에서 찾아주세요. 판정 순간은 단서 제출 후 다시 볼 수 있어요.");
            return;
        }
        before=showBefore;
        Question q=session.current();
        Scene scene=catalog.scene(before?q.beforeScene():q.afterScene());
        field.scene(scene);
        if(before)field.selected(session.stage()==QuizSession.Stage.FEEDBACK?q.clueTarget():session.clue());
        first.mark(before);
        second.mark(!before);
        caption.setText("<html>"+html(scene.caption())+"</html>");
    }

    @Override void selectClue(String id) {
        clueButtons.forEach((key, button) -> button.mark(key.equals(id)));
        field.selected(id);
    }
}
