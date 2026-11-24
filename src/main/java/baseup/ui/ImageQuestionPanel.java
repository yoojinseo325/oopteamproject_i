package baseup.ui;

import baseup.model.Question;
import baseup.model.QuizSession;
import baseup.repository.Catalog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.JDialog;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Dialog;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import static baseup.ui.Theme.*;
import static baseup.ui.QuizText.copy;

/** 삽화와 그림형 단서 선택만 표시한다. */
final class ImageQuestionPanel extends QuestionVisual {
    private final Catalog catalog;
    private final QuizSession session;
    private final Consumer<String> onClue;
    private final Map<String, ActionButton> clueButtons = new LinkedHashMap<>();
    private final QuestionImages images;
    private final Consumer<Exception> error;

    ImageQuestionPanel(Catalog catalog, QuizSession session, Consumer<String> onClue, QuestionImages images, Consumer<Exception> error) {
        this.catalog = catalog;
        this.session = session;
        this.onClue = onClue;
        this.images = images;
        this.error = error;
        setLayout(new BorderLayout());
        setOpaque(false);
        add(build(session.current()), BorderLayout.CENTER);
    }

    private JPanel build(Question q) {
        RoundedPanel card=card();
        card.setBorder(new EmptyBorder(18,18,18,18));
        card.setLayout(new BorderLayout(0,12));
        JPanel top=new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("PICTURE VIEW   /   장면을 관찰하세요",11,GREEN,true));
        ActionButton zoom=button("크게 보기",false,()->{
            try {
                JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),"장면 크게 보기",Dialog.ModalityType.APPLICATION_MODAL);
                dialog.add(new ImageView(images.load(catalog.imagePath(q)),q.title()));
                Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
                dialog.setSize(Math.min(1100,screen.width-80),Math.min(800,screen.height-80));
                dialog.setLocationRelativeTo(this);
                dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
            }
            catch(Exception e) {
                error.accept(e);
            }
        });
        zoom.setFont(font(11,false));
        top.add(zoom,BorderLayout.EAST);
        card.add(top,BorderLayout.NORTH);
        try {
            card.add(new ImageView(images.load(catalog.imagePath(q)),q.title()),BorderLayout.CENTER);
        }
        catch(Exception e) {
            card.add(label("그림을 불러오지 못했습니다.",14,RED,true),BorderLayout.CENTER);
        }
        JPanel bottom=column();
        Theme.add(bottom,copy(q.imageGuide(),12,MUTED,1));
        gap(bottom,12);
        if(session.stage()==QuizSession.Stage.CLUE) {
            Theme.add(bottom,label("그림과 설명을 보고, 결정적인 조건을 골라요.",12,GREEN,true));
            gap(bottom,10);
            for(Question.Clue clue:q.clueOptions()) {
                ActionButton b=button("<html>"+html(clue.text())+"</html>",false,()->onClue.accept(clue.id()));
                b.setName("clue-"+clue.id());
                b.setFont(font(12,true));
                b.setHorizontalAlignment(SwingConstants.LEFT);
                int h=Math.max(48,24+20*(int)Math.ceil(b.getFontMetrics(b.getFont()).stringWidth(clue.text())/300.0));
                b.setPreferredSize(new Dimension(300,h));
                b.setMinimumSize(new Dimension(0,h));
                b.setMaximumSize(new Dimension(Integer.MAX_VALUE,h));
                b.mark(clue.id().equals(session.clue()));
                clueButtons.put(clue.id(),b);
                Theme.add(bottom,b);
                gap(bottom,7);
            }
        }
        else if(session.stage()==QuizSession.Stage.FEEDBACK) {
            Theme.add(bottom,copy("발견한 규칙  ·  "+catalog.rule(q.ruleId()).title(),14,GREEN,1));
            gap(bottom,7);
            Theme.add(bottom,copy(catalog.rule(q.ruleId()).summary(),13,INK,2));
        }
        else Theme.add(bottom,copy("정답과 규칙 카드는 답을 제출하면 열려요. 막히면 코치의 힌트를 확인하세요.",12,MUTED,2));
        card.add(bottom,BorderLayout.SOUTH);
        return card;
    }

    @Override void selectClue(String id) {
        clueButtons.forEach((key, button) -> button.mark(key.equals(id)));
    }
}
