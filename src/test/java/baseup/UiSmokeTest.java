package baseup;

import baseup.model.*;
import baseup.repository.Catalog;
import baseup.service.*;
import baseup.ui.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** 실제 Swing 버튼을 눌러 혼합 문제를 풀고 숨겨진 창의 화면을 렌더링한다. */
public final class UiSmokeTest {
    private static MainFrame frame;
    private static LearningService service;
    private static int checks;
    private static final Path images=Path.of("build/qa/screenshots");
    @FunctionalInterface interface Work { void run() throws Exception; }
    private static void edt(Work work) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{work.run();}catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);checks++;}
    private static void checkTextLayout() throws Exception {
        frame.validate();
        for(Component c:components()) if(c instanceof JTextArea area && area.getWidth()>0 && area.getHeight()>0 && !area.getText().isBlank()) {
            var end=area.modelToView2D(area.getDocument().getLength());
            check(end!=null && end.getY()+end.getHeight()<=area.getHeight()+2,"Text clipped: "+area.getText());
        }
    }
    private static List<Component> descendants(Container parent) {
        List<Component> result=new ArrayList<>();
        for(Component c:parent.getComponents()){result.add(c);if(c instanceof Container child)result.addAll(descendants(child));}
        return result;
    }
    private static List<Component> components() {return descendants(frame.getContentPane());}
    private static void click(String text) {clickButton(components().stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b->text.equals(b.getText())).findFirst().orElseThrow(()->new AssertionError("Missing button: "+text)));}
    private static void named(String name) {clickButton(components().stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b->name.equals(b.getName())).findFirst().orElseThrow(()->new AssertionError("Missing button: "+name)));}
    private static void clickButton(JButton b) {check(b.isEnabled(),"Button disabled: "+b.getText());b.doClick();}
    private static Component byName(String name) {return components().stream().filter(c->name.equals(c.getName())).findFirst().orElse(null);}
    /** 버튼이 아닌 카드·칩(마우스 클릭으로 동작)을 누른다. */
    private static void clickLabel(String name) {
        Component c=byName(name);check(c!=null,"Missing clickable: "+name);
        for(var l:c.getMouseListeners()) l.mouseClicked(new java.awt.event.MouseEvent(c,java.awt.event.MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,5,5,1,false));
    }
    private static List<String> cards() {return components().stream().map(Component::getName).filter(n->n!=null&&n.startsWith("rule-card-")).toList();}
    private static java.util.stream.Stream<String> labels() {return components().stream().filter(JLabel.class::isInstance).map(c->((JLabel)c).getText()).filter(Objects::nonNull);}
    private static void page(String id) throws Exception {
        edt(()->{Method m=MainFrame.class.getDeclaredMethod("showPage",String.class);m.setAccessible(true);m.invoke(frame,id);});
    }
    private static QuizSession active() throws Exception {Field field=MainFrame.class.getDeclaredField("session");field.setAccessible(true);return (QuizSession)field.get(frame);}
    private static void show(QuizSession session) throws Exception {
        edt(()->{Method m=MainFrame.class.getDeclaredMethod("showQuiz",QuizSession.class,String.class);m.setAccessible(true);m.invoke(frame,session,"통합 학습");});
    }
    private static void snapshot(String name) throws Exception {
        edt(()->{});
        edt(()->frame.validate());   // 첫 배치: 글 상자가 실제 폭을 알고 다시 배치를 예약한다
        edt(()->{});                 // 예약된 다시 배치 요청을 처리
        edt(()->{
            // 숨긴 창에서는 Swing이 다시 배치할 화면(스크롤 영역)을 자동 처리하지 않으므로, 보이는 창처럼 직접 마무리한다.
            for(int pass=0;pass<3;pass++){frame.validate();for(Component c:components()) if(c instanceof JScrollPane sp&&!sp.isValid()) sp.validate();}
            Container content=frame.getContentPane();
            BufferedImage image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();content.printAll(g);g.dispose();ImageIO.write(image,"png",images.resolve(name+".png").toFile());checks++;
        });
    }
    private static void answer(QuizSession session) throws Exception {
        Question q=session.current();
        if(service.catalog().isShortAnswer(q)) edt(()->{
            JTextField typed=components().stream().filter(JTextField.class::isInstance).map(JTextField.class::cast).filter(f->"short-answer".equals(f.getName())).findFirst().orElseThrow(()->new AssertionError("Missing short-answer field "+q.id()));
            check(components().stream().filter(JButton.class::isInstance).noneMatch(x->"choice-0".equals(x.getName())),"Short answer hides choices "+q.id());
            typed.setText(" "+service.catalog().shortAnswers(q.id()).get(service.catalog().shortAnswers(q.id()).size()-1).toUpperCase()+" ");
        });
        else edt(()->named("choice-"+q.answer()));
        if(q.requiresClue()) {
            edt(()->click("판정 선택 완료 · 단서 찾기   →"));
            edt(()->named("clue-"+q.clueTarget()));edt(()->click("판정과 단서 제출하기   →"));
        } else edt(()->click("답 제출하기   →"));
        check(session.stage()==QuizSession.Stage.FEEDBACK,"Feedback reached");
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("baseup.autoplay","off");Files.createDirectories(images);Theme.install();
        service=new LearningService(new Catalog(Path.of("data")),Files.createTempDirectory(Path.of("build"),"ui-test-"));
        try {
            edt(()->{frame=new MainFrame(service);frame.setSize(1360,940);frame.addNotify();frame.validate();});snapshot("01-home");
            edt(()->{
                // 홈에서 게임 쪽으로 내려간 뒤 자동 스크롤 요청이 와도 위치가 유지되어야 한다(화면이 맨 위로 튀는 문제).
                JViewport home=components().stream().filter(JViewport.class::isInstance).map(JViewport.class::cast)
                        .filter(v->v.getView()!=null&&v.getView().getPreferredSize().height>v.getExtentSize().height+400).findFirst().orElseThrow();
                home.setViewPosition(new Point(0,400));
                ((JComponent)home.getView()).scrollRectToVisible(new Rectangle(0,0,10,10));
                check(home.getViewPosition().y==400,"Home keeps scroll position during play");
                home.setViewPosition(new Point(0,0));
            });
            edt(()->{
                check(components().stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b->b.getName()!=null&&b.getName().startsWith("lesson-")).count()==3,"home shows 3 next lessons");
                check(components().stream().filter(JLabel.class::isInstance).map(JLabel.class::cast).noneMatch(l->l.getText().contains("포스")||l.getText().contains("태그업")),"Future content hidden");
                click("STEP 1 시작하기   →");
            });snapshot("02-quiz");
            QuizSession first=active();
            while(first.stage()!=QuizSession.Stage.COMPLETE){answer(first);edt(()->click(first.index()+1==first.size()?"미션 결과 보기   →":"다음 상황으로   →"));}
            snapshot("05-result");check(service.attempts().size()==2&&service.solvedCount()==2&&service.completedSteps()==1,"Unified persistence");
            // 결과 화면의 "학습 경로 보기" → 퀴즈 허브의 시즌 코스(STEP 지도)
            edt(()->click("학습 경로 보기"));snapshot("06-missions");
            edt(()->{
                check(byName("season-map")!=null,"Season map shown");
                check(byName("quiz-random-situation")!=null&&byName("quiz-quick-general")!=null&&byName("quiz-review")!=null,"Quiz hub actions");
                check(!((JButton)byName("season-step-7")).isEnabled(),"Situation STEP 7 still locked");
                named("season-tab-general");
                check(((JButton)byName("season-step-2")).isEnabled()&&!((JButton)byName("season-step-3")).isEnabled(),"Next general STEP opens in order");
            });snapshot("06b-season-general");
            page("quiz");snapshot("06c-quiz-hub");
            // 규칙 학습 = 규칙 사전: 카드 그리드 + 검색 + 분류
            page("library");snapshot("07-library");
            edt(()->{
                JTextField search=(JTextField)byName("rule-search");
                check(cards().size()==service.catalog().rules().size(),"Every rule has a card");
                search.setText("수비 인원");check(!cards().isEmpty()&&cards().get(0).equals("rule-card-defense"),"Rule searchable by title");
                search.setText("보크");check(cards().contains("rule-card-balk"),"Undiscovered rule visible in rule book");
                search.setText("no-result-12345");check(cards().isEmpty(),"Empty search");
                search.setText("");
                clickLabel("category-record");check(!cards().isEmpty()&&cards().stream().allMatch(id->service.catalog().rule(id.substring(10)).category()==RuleCategory.RECORD),"Category chip filters");
                clickLabel("category-all");check(cards().size()==service.catalog().rules().size(),"All chip resets");
                clickLabel("rule-card-balk");
                check(byName("rule-hero")!=null&&byName("rule-detail")!=null,"Rule with its own detail shows it");
            });snapshot("07b-rule-detail");
            page("library");
            edt(()->{
                clickLabel("rule-card-defense");
                check(byName("rule-detail")==null,"Detail equal to summary is not repeated");
                int at=service.catalog().rules().stream().map(RuleConcept::id).toList().indexOf("defense")+1;
                check(labels().anyMatch(t->t.startsWith(at+" / "+service.catalog().rules().size())),"Book page number");
                named("rule-save");
            });check(service.bookmarked("defense"),"Rule bookmark persisted");
            int defenseAt=service.catalog().rules().stream().map(RuleConcept::id).toList().indexOf("defense")+1;
            edt(()->{named("page-next");check(labels().anyMatch(t->t.startsWith((defenseAt+1)+" / ")),"Next page button flips the rule book");
                named("page-prev");check(labels().anyMatch(t->t.startsWith(defenseAt+" / ")),"Previous page button flips back");});
            service.addNote("defense","투수와 포수도 수비 인원에 포함된다.");page("notes");snapshot("10-notes");
            for(Question q:service.catalog().questions()) {
                QuizSession session=service.start(x->List.of(q));show(session);
                edt(()->{
                    checkTextLayout();
                    check(components().stream().anyMatch(q.hasImage()?ImageView.class::isInstance:FieldPanel.class::isInstance),"Correct renderer "+q.id());
                    if(!q.hasImage()) {
                        String team=service.catalog().matchup(q.id()).offense().name();
                        check(components().stream().filter(JLabel.class::isInstance).map(JLabel.class::cast).anyMatch(l->team.equals(l.getText())),"KBO badge preserved");
                        String firstCaption = "<html>" + Theme.html(service.catalog().scene(q.beforeScene()).caption()) + "</html>";
                        check(components().stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                                .anyMatch(l -> firstCaption.equals(l.getText())), "Scene 1 is shown first: " + q.id());
                    }
                });
                if(List.of("Q01","U08","H02","E09").contains(q.id()))snapshot("integrated-"+q.id());
                if(q.requiresClue()) {
                    edt(()->named("choice-"+q.answer()));edt(()->click("판정 선택 완료 · 단서 찾기   →"));
                    if(List.of("Q01","U08").contains(q.id()))snapshot("clue-"+q.id());
                    edt(() -> {
                        Component visual = components().stream()
                                .filter(q.hasImage() ? ImageView.class::isInstance : FieldPanel.class::isInstance).findFirst().orElseThrow();
                        for (Question.Clue clue : service.catalog().clues(q)) {
                            named("clue-" + clue.id());
                            check(clue.id().equals(session.clue()), "Clue selection changes: " + q.id());
                            check(components().contains(visual), "Clue selection preserves visual and focus targets: " + q.id());
                            check(components().stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast)
                                    .anyMatch(a -> ("선택한 단서: " + clue.text()).equals(a.getText())), "Selected clue description updates");
                            checkTextLayout();
                        }
                    });
                    edt(()->named("clue-"+q.clueTarget()));edt(()->click("판정과 단서 제출하기   →"));
                }else answer(session);
                check(session.results().get(0).independent(),"Correct UI scoring "+q.id());
                edt(UiSmokeTest::checkTextLayout);
                if(List.of("Q01","U08","E01").contains(q.id()))snapshot("feedback-"+q.id());
                edt(()->click("미션 결과 보기   →"));
            }
            page("records");snapshot("08-records");
            edt(()->{
                JTable table=components().stream().filter(JTable.class::isInstance).map(JTable.class::cast).findFirst().orElseThrow();
                boolean na=false;for(int row=0;row<table.getRowCount();row++)na|="해당 없음".equals(table.getValueAt(row,3));
                check(na,"Concept records do not invent clue success");
            });
            page("mypage");snapshot("09-mypage");
            edt(()->{
                check(components().stream().anyMatch(c->"accuracy-dashboard".equals(c.getName())),"Mypage shows accuracy dashboard");
                check(components().stream().filter(JLabel.class::isInstance).map(c->((JLabel)c).getText()).anyMatch("심화"::equals),"Mypage shows level accuracy");
            });
            page("home");edt(()->frame.setSize(1100,760));snapshot("11-home-small");page("library");snapshot("11b-library-small");edt(()->clickLabel(cards().get(0)));snapshot("11d-rule-small");page("quiz");snapshot("11c-quiz-small");
            for(String id:List.of("E01","Q01","U08","H02")) {
                QuizSession session=service.start(x->List.of(service.catalog().question(id).orElseThrow()));show(session);snapshot("small-"+id);
                edt(()->{
                    checkTextLayout();
                    for(Component c:components()) if(c instanceof JScrollPane pane && pane.getViewport().getView() instanceof Scrollable)
                        check(pane.getViewport().getView().getWidth()<=pane.getViewport().getExtentSize().width,"No horizontal overflow");
                    JButton submit=components().stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                            .filter(b->b.getText().equals("답 제출하기   →")||b.getText().equals("판정 선택 완료 · 단서 찾기   →")).findFirst().orElseThrow();
                    Rectangle bounds=SwingUtilities.convertRectangle(submit.getParent(),submit.getBounds(),frame.getContentPane());
                    check(bounds.y>=0&&bounds.y+bounds.height<=frame.getContentPane().getHeight(),"Submit stays visible in small window");
                });
                answer(session);snapshot("small-feedback-"+id);edt(UiSmokeTest::checkTextLayout);
            }
            System.out.println("PASS: "+checks+" Swing integration checks. Screenshots: "+images.toAbsolutePath());
        }finally{edt(()->{if(frame!=null)frame.dispose();});}
    }
}
