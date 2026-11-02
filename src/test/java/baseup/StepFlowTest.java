package baseup;

import baseup.model.*;
import baseup.repository.Catalog;
import baseup.service.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** 순차 해제, 자동 재출제, 저장 실패를 실제 임시 파일로 검증한다. */
public final class StepFlowTest {
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message); checks++;
    }
    @FunctionalInterface interface Work { void run() throws Exception; }
    private static void rejects(Work work, String message) throws Exception {
        try { work.run(); } catch (java.io.IOException | IllegalStateException | IllegalArgumentException e) { checks++; return; }
        throw new AssertionError(message);
    }
    private static void submit(LearningService service, QuizSession session, boolean correct, boolean hint) throws Exception {
        Question q = session.current(); session.choose(correct ? q.answer() : 1-q.answer());
        session.beginClue(); session.chooseClue(q.clueTarget()); if (hint) session.useHint(); service.submit(session);
    }
    private static void finish(LearningService service, QuizSession session) throws Exception {
        while (session.stage() != QuizSession.Stage.COMPLETE) { submit(service,session,true,false); service.advance(session); }
    }
    public static void main(String[] args) throws Exception {
        Catalog catalog = new Catalog(Path.of("data"));
        Path dir = Files.createTempDirectory(Path.of("build"),"step-test-");
        Instant now = Instant.parse("2030-01-01T00:00:00Z"); Clock clock = Clock.fixed(now,ZoneOffset.UTC);
        LearningService s = new LearningService(catalog,dir,clock);
        String user = s.user().id();
        check(s.completedSteps()==0 && s.stepUnlocked(1) && !s.stepUnlocked(2),"only first step unlocked");
        rejects(()->s.startStep(2),"locked step cannot start");
        rejects(()->s.startStep(0),"invalid step");
        check(!s.discovered("force"),"rules hidden before encounter");
        QuizSession first=s.startNext();
        check(first.size()==2 && first.current().id().equals("Q01"),"basic situations first");
        rejects(()->s.advance(first),"cannot skip question");
        submit(s,first,false,false);s.advance(first);
        check(s.completedSteps()==0 && s.discovered("force") && !s.discovered("foul"),"partial step does not unlock next");
        check(new LearningService(catalog,dir,clock).completedSteps()==0,"partial restart remains at same step");
        submit(s,first,true,true);
        check(s.completedSteps()==0,"feedback before completion");
        Files.createDirectory(dir.resolve("steps.tsv"));
        rejects(()->s.advance(first),"completion failure reported");
        check(first.stage()==QuizSession.Stage.FEEDBACK && s.completedSteps()==0,"failed completion does not unlock");
        Files.delete(dir.resolve("steps.tsv"));s.advance(first);
        check(s.completedSteps()==1,"mistakes and hints do not block progress");
        rejects(()->s.advance(first),"duplicate completion rejected");
        LearningService restored=new LearningService(catalog,dir,clock);
        check(restored.completedSteps()==1,"completion survives restart");
        QuizSession second=restored.startNext();
        check(second.size()==2 && second.current().id().equals("Q02"),"same judgment returns with different conditions");
        finish(restored,second);
        check(restored.adaptiveQuestions(List.of(),4).isEmpty(),"successful variants resolve earlier rule difficulty");
        check(restored.completedSteps()==2,"next step completed");
        LearningService beforeDue=new LearningService(catalog,dir,Clock.fixed(now.plusSeconds(86399),ZoneOffset.UTC));
        check(beforeDue.adaptiveQuestions(List.of(),4).isEmpty(),"no premature spaced review");
        LearningService due=new LearningService(catalog,dir,Clock.fixed(now.plusSeconds(86400),ZoneOffset.UTC));
        check(due.adaptiveQuestions(List.of(),4).size()==2,"one day review boundary");
        QuizSession third=due.startNext();
        check(third.size()==4,"at most two reviews with two new questions");
        check(third.current().id().equals("Q05"),"new question opens step");
        submit(due,third,true,false);due.advance(third);
        check(third.current().ruleId().equals("force") && third.current().id().equals("Q01"),"review is interleaved and varied");
        finish(due,third);
        LearningService twoDays=new LearningService(catalog,dir,Clock.fixed(now.plus(Duration.ofDays(2)),ZoneOffset.UTC));
        check(twoDays.adaptiveQuestions(List.of(),6).stream().noneMatch(q->q.ruleId().equals("force")),"success expands interval to three days");
        LearningService fourDays=new LearningService(catalog,dir,Clock.fixed(now.plus(Duration.ofDays(4)),ZoneOffset.UTC));
        check(fourDays.adaptiveQuestions(List.of(),6).stream().anyMatch(q->q.ruleId().equals("force")),"expanded interval becomes due");
        QuizSession reinforcement=fourDays.start(x->List.of(catalog.question("Q02").orElseThrow()));finish(fourDays,reinforcement);
        LearningService tenDays=new LearningService(catalog,dir,Clock.fixed(now.plus(Duration.ofDays(10)),ZoneOffset.UTC));
        check(tenDays.adaptiveQuestions(List.of(),6).stream().noneMatch(q->q.ruleId().equals("force")),"three successes expand interval to seven days");
        LearningService elevenDays=new LearningService(catalog,dir,Clock.fixed(now.plus(Duration.ofDays(11)),ZoneOffset.UTC));
        check(elevenDays.adaptiveQuestions(List.of(),6).stream().anyMatch(q->q.ruleId().equals("force")),"seven day boundary");
        LearningService onward=new LearningService(catalog,dir,clock);
        while(!onward.courseComplete())finish(onward,onward.startNext());
        check(onward.completedSteps()==6,"all six steps completable");
        check(onward.startNext().stepNumber()==0,"continued practice after final step");
        finish(onward,onward.startStep(1));check(onward.completedSteps()==6,"replay never relocks progress");
        QuizSession owned=onward.startStep(1);submit(onward,owned,true,false);
        onward.addProfile("새 심판");
        check(onward.completedSteps()==0 && !onward.discovered("force"),"new user independent progress");
        rejects(()->onward.advance(owned),"user switch cannot complete old session");
        onward.selectUser(user);check(onward.completedSteps()==6,"original user's progress preserved");

        LearningService struggle=new LearningService(catalog,Files.createTempDirectory(Path.of("build"),"adaptive-test-"),clock);
        QuizSession one=struggle.startStep(1);
        submit(struggle,one,false,false);struggle.advance(one);submit(struggle,one,true,false);struggle.advance(one);
        QuizSession two=struggle.startStep(2);
        submit(struggle,two,false,false);struggle.advance(two);submit(struggle,two,true,false);struggle.advance(two);
        QuizSession three=struggle.startStep(3);
        check(three.size()==3,"only struggling rule inserted");
        submit(struggle,three,true,false);struggle.advance(three);
        check(three.current().id().equals("Q01") && three.current().level().equals("기본"),"repeated difficulty returns to basic conditions");
        check(new HashSet<>(new StepSelector(3).select(struggle)).size()==3,"no duplicate questions within step");
        System.out.println("PASS: "+checks+" step/adaptive checks");
    }
}
