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
    private static void page(String id) throws Exception {
        edt(()->{Method m=MainFrame.class.getDeclaredMethod("showPage",String.class);m.setAccessible(true);m.invoke(frame,id);});
    }
    private static QuizSession active() throws Exception {Field field=MainFrame.class.getDeclaredField("session");field.setAccessible(true);return (QuizSession)field.get(frame);}
    private static void show(QuizSession session) throws Exception {
        edt(()->{Method m=MainFrame.class.getDeclaredMethod("showQuiz",QuizSession.class,String.class);m.setAccessible(true);m.invoke(frame,session,"통합 학습");});
    }
    private static void snapshot(String name) throws Exception {
        edt(()->{});
        edt(()->{
            frame.validate();Container content=frame.getContentPane();
            BufferedImage image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();content.printAll(g);g.dispose();ImageIO.write(image,"png",images.resolve(name+".png").toFile());checks++;
        });
    }
    private static void answer(QuizSession session) throws Exception {
        Question q=session.current();edt(()->named("choice-"+q.answer()));
        if(q.requiresClue()) {
            edt(()->click("판정 선택 완료 · 단서 찾기   →"));
            edt(()->named("clue-"+q.clueTarget()));edt(()->click("판정과 단서 제출하기   →"));
        } else edt(()->click("답 제출하기   →"));
        check(session.stage()==QuizSession.Stage.FEEDBACK,"Feedback reached");
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(images);Theme.install();
        service=new LearningService(new Catalog(Path.of("data")),Files.createTempDirectory(Path.of("build"),"ui-test-"));
        try {
            edt(()->{frame=new MainFrame(service);frame.setSize(1360,940);frame.addNotify();frame.validate();});snapshot("01-home");
            edt(()->{
                check(components().stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b->"잠김".equals(b.getText())&&!b.isEnabled()).count()==25,"25 future steps locked");
                check(components().stream().filter(JLabel.class::isInstance).map(JLabel.class::cast).noneMatch(l->l.getText().contains("포스")||l.getText().contains("태그업")),"Future content hidden");
                click("STEP 1 시작하기   →");
            });snapshot("02-quiz");
            QuizSession first=active();
            while(first.stage()!=QuizSession.Stage.COMPLETE){answer(first);edt(()->click(first.index()+1==first.size()?"미션 결과 보기   →":"다음 상황으로   →"));}
            snapshot("05-result");check(service.attempts().size()==2&&service.solvedCount()==2&&service.completedSteps()==1,"Unified persistence");
            edt(()->click("학습 경로 보기"));snapshot("06-missions");page("library");snapshot("07-library");
            edt(()->{
                JTextField search=components().stream().filter(JTextField.class::isInstance).map(JTextField.class::cast).findFirst().orElseThrow();
                search.setText("수비 인원");JList<?> list=components().stream().filter(JList.class::isInstance).map(JList.class::cast).findFirst().orElseThrow();
                check(list.getModel().getSize()==1,"Discovered image rule searchable");
                search.setText("no-result-12345");check(list.getModel().getSize()==0,"Empty search");search.setText("");click("북마크에 추가");
            });check(service.bookmarked("defense"),"Image rule bookmark persisted");
            service.addNote("defense","투수와 포수도 수비 인원에 포함된다.");page("notes");snapshot("10-notes");
            for(Question q:service.catalog().questions()) {
                QuizSession session=service.start(x->List.of(q));show(session);
                edt(()->{
                    checkTextLayout();
                    check(components().stream().anyMatch(q.hasImage()?ImageView.class::isInstance:FieldPanel.class::isInstance),"Correct renderer "+q.id());
                    if(!q.hasImage()) {
                        String team=service.catalog().matchup(q.id()).offense().name();
                        check(components().stream().filter(JLabel.class::isInstance).map(JLabel.class::cast).anyMatch(l->team.equals(l.getText())),"KBO badge preserved");
                    }
                });
                if(List.of("Q01","U08","H02").contains(q.id()))snapshot("integrated-"+q.id());
                if(q.requiresClue()) {
                    edt(()->named("choice-"+q.answer()));edt(()->click("판정 선택 완료 · 단서 찾기   →"));
                    if(List.of("Q01","U08").contains(q.id()))snapshot("clue-"+q.id());
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
            page("home");edt(()->frame.setSize(1100,760));snapshot("11-home-small");
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
