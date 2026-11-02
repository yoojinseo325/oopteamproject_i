package baseup;

import baseup.model.*;
import baseup.repository.Catalog;
import baseup.service.*;
import baseup.ui.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** 실제 Swing 컴포넌트의 입력을 연결해 보고 화면을 PNG로 렌더링한다. 사용자 기록은 사용하지 않는다. */
public final class UiSmokeTest {
    private static MainFrame frame;
    private static LearningService service;
    private static int checks;
    private static final Path images=Path.of("build/qa/screenshots");
    @FunctionalInterface interface Work { void run() throws Exception; }
    private static void edt(Work work) throws Exception {
        SwingUtilities.invokeAndWait(()->{try{work.run();}catch(Exception e){throw new RuntimeException(e);}});
    }
    private static List<Component> descendants(Container parent) {
        List<Component> result=new ArrayList<>();
        for(Component c:parent.getComponents()){result.add(c);if(c instanceof Container child)result.addAll(descendants(child));}
        return result;
    }
    private static void click(String text) {
        JButton button=descendants(frame.getContentPane()).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(b->b.getText()!=null && b.getText().equals(text)).findFirst().orElseThrow(()->new AssertionError("Missing button: "+text));
        if(!button.isEnabled())throw new AssertionError("Disabled button: "+text);
        button.doClick();checks++;
    }
    private static void clickClue(String id) {
        JButton button=descendants(frame.getContentPane()).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(b->("clue-"+id).equals(b.getName())).findFirst().orElseThrow();
        button.doClick();checks++;
    }
    private static void page(String id) throws Exception {
        edt(()->{Method m=MainFrame.class.getDeclaredMethod("showPage",String.class);m.setAccessible(true);m.invoke(frame,id);});
    }
    private static void snapshot(String name) throws Exception {
        edt(()->{}); // drain queued layout/scroll callbacks
        edt(()->{
            frame.validate();
            Container content=frame.getContentPane();BufferedImage image=new BufferedImage(content.getWidth(),content.getHeight(),BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();content.printAll(g);g.dispose();ImageIO.write(image,"png",images.resolve(name+".png").toFile());checks++;
        });
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(images);
        Theme.install();
        service=new LearningService(new Catalog(Path.of("data")),Files.createTempDirectory(Path.of("build"),"ui-test-"));
        try {
            // 테스트는 숨겨진 Swing 창에서 실행해 사용자의 키보드/마우스와 섞이지 않게 한다.
            edt(()->{frame=new MainFrame(service);frame.setSize(1360,940);frame.addNotify();frame.validate();});
            snapshot("01-home");
            edt(()->{
                long locked=descendants(frame.getContentPane()).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                        .filter(b->"잠김".equals(b.getText())&&!b.isEnabled()).count();
                if(locked!=5)throw new AssertionError("Five future steps must be locked");checks++;
                for(Component c:descendants(frame.getContentPane())) {
                    if(c instanceof JLabel l && (l.getText().contains("포스")||l.getText().contains("태그업")))
                        throw new AssertionError("Home reveals future content");
                }checks++;
                click("STEP 1 시작하기   →");
            });snapshot("02-quiz");
            Field active=MainFrame.class.getDeclaredField("session");active.setAccessible(true);
            QuizSession current=(QuizSession)active.get(frame);
            for(int i=0;i<current.size();i++) {
                Question q=current.current();
                edt(()->click((q.answer()+1)+"   "+q.choices().get(q.answer())));
                edt(()->click("판정 선택 완료 · 단서 찾기   →"));if(i==0)snapshot("03-clue");
                String clue=service.catalog().scene(q.beforeScene()).tokens().stream().filter(t->t.id().equals(q.clueTarget())).findFirst().orElseThrow().label();
                edt(()->clickClue(q.clueTarget()));edt(()->click("판정과 단서 제출하기   →"));if(i==0)snapshot("04-feedback");
                boolean last=i==current.size()-1;
                edt(()->click(last?"미션 결과 보기   →":"다음 상황으로   →"));
            }
            snapshot("05-result");
            if(service.attempts().size()!=2||service.solvedCount()!=2||service.completedSteps()!=1)throw new AssertionError("Step submissions/completion did not persist");checks++;
            edt(()->click("학습 경로 보기"));snapshot("06-missions");
            page("library");snapshot("07-library");
            edt(()->{
                JTextField search=descendants(frame.getContentPane()).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast).findFirst().orElseThrow();
                search.setText("포스");
                JList<?> list=descendants(frame.getContentPane()).stream().filter(JList.class::isInstance).map(JList.class::cast).findFirst().orElseThrow();
                if(list.getModel().getSize()<1||list.getModel().getSize()>=6)throw new AssertionError("Search not filtering");checks++;
                search.setText("no-result-12345");if(list.getModel().getSize()!=0)throw new AssertionError("Empty result state");checks++;
                search.setText("");click("북마크에 추가");
            });
            if(!service.bookmarked("force"))throw new AssertionError("Bookmark not persisted");checks++;
            page("records");snapshot("08-records");
            service.addNote("force","타자주자 때문에 다음 베이스로 밀려나는지 먼저 확인하기.\n1루가 비었다면 같은 판정이 아닐 수 있다.");
            page("notes");snapshot("10-notes");
            // 모든 장면/단계가 실제 컴포넌트로 구성되는지 확인한다.
            for(Question q:service.catalog().questions()) {
                edt(()->{
                    QuizSession s=service.start(x->List.of(q));
                    Method show=MainFrame.class.getDeclaredMethod("showQuiz",QuizSession.class,String.class);show.setAccessible(true);show.invoke(frame,s,"KBO 상황");
                    if(descendants(frame.getContentPane()).stream().noneMatch(FieldPanel.class::isInstance))throw new AssertionError("Missing scene");checks++;
                    String team=service.catalog().matchup(q.id()).offense().name();
                    if(descendants(frame.getContentPane()).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast).noneMatch(l->team.equals(l.getText())))
                        throw new AssertionError("Missing team matchup");checks++;
                });
                snapshot("kbo-"+q.id());
            }
            page("home");edt(()->frame.setSize(1100,760));snapshot("11-home-small");
            edt(()->click("STEP 2 시작하기   →"));snapshot("12-quiz-small");
            edt(()->{
                for(Component c:descendants(frame.getContentPane())) {
                    if(c instanceof JScrollPane pane && pane.getViewport().getView() instanceof Scrollable) {
                        if(pane.getViewport().getView().getWidth()>pane.getViewport().getExtentSize().width)
                            throw new AssertionError("Body exceeds narrow viewport");
                    }
                }
                checks++;
            });
            // 다음 단계의 실수가 새 단계 중간에 다른 조건으로 돌아오는 UI 흐름.
            QuizSession second=(QuizSession)active.get(frame);
            while(second.stage()!=QuizSession.Stage.COMPLETE) {
                Question q=second.current();int choice=q.ruleId().equals("force")?1-q.answer():q.answer();
                edt(()->click((choice+1)+"   "+q.choices().get(choice)));
                edt(()->click("판정 선택 완료 · 단서 찾기   →"));
                String clue=service.catalog().scene(q.beforeScene()).tokens().stream().filter(t->t.id().equals(q.clueTarget())).findFirst().orElseThrow().label();
                edt(()->clickClue(q.clueTarget()));edt(()->click("판정과 단서 제출하기   →"));
                edt(()->click(second.index()+1==second.size()?"미션 결과 보기   →":"다음 상황으로   →"));
            }
            edt(()->click("STEP 3 시작하기   →"));
            QuizSession third=(QuizSession)active.get(frame);
            if(third.size()!=3)throw new AssertionError("Expected one embedded review");checks++;
            Question opening=third.current();
            edt(()->click((opening.answer()+1)+"   "+opening.choices().get(opening.answer())));
            edt(()->click("판정 선택 완료 · 단서 찾기   →"));
            String clue=service.catalog().scene(opening.beforeScene()).tokens().stream().filter(t->t.id().equals(opening.clueTarget())).findFirst().orElseThrow().label();
            edt(()->clickClue(opening.clueTarget()));edt(()->click("판정과 단서 제출하기   →"));edt(()->click("다음 상황으로   →"));
            if(!third.current().id().equals("Q01"))throw new AssertionError("Review must use a different situation in the middle");checks++;
            snapshot("13-embedded-practice");
            System.out.println("PASS: "+checks+" Swing checks. Screenshots: "+images.toAbsolutePath());
        }finally{edt(()->{if(frame!=null)frame.dispose();});}
    }
}
