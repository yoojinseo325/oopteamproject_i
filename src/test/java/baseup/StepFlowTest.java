package baseup;

import baseup.model.*;
import baseup.repository.Catalog;
import baseup.service.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** 혼합 출제, 복습 간격, V1 기록 호환, 저장 실패를 실제 파일로 검증한다. */
public final class StepFlowTest {
    private static int checks;
    private static final Instant NOW=Instant.parse("2030-01-01T00:00:00Z");
    private static final Clock CLOCK=Clock.fixed(NOW,ZoneOffset.UTC);
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message);checks++; }
    @FunctionalInterface interface Work { void run() throws Exception; }
    private static void rejects(Work work,String message) throws Exception {
        try{work.run();}catch(java.io.IOException|IllegalStateException|IllegalArgumentException e){checks++;return;}
        throw new AssertionError(message);
    }
    private static Path temp() throws Exception { return Files.createTempDirectory(Path.of("build"),"step-test-"); }
    private static Attempt submit(LearningService service,QuizSession session,boolean correct,boolean hint) throws Exception {
        Question q=session.current();session.choose(correct?q.answer():(q.answer()+1)%q.choices().size());
        if(q.requiresClue()){session.beginClue();session.chooseClue(q.clueTarget());}
        if(hint)session.useHint();return service.submit(session);
    }
    private static void finish(LearningService service,QuizSession session) throws Exception {
        while(session.stage()!=QuizSession.Stage.COMPLETE){submit(service,session,true,false);service.advance(session);}
    }
    private static QuizSession single(LearningService s,String id) { return s.start(x->List.of(s.catalog().question(id).orElseThrow())); }
    public static void main(String[] args) throws Exception {
        Catalog catalog=new Catalog(Path.of("data"));Path dir=temp();LearningService s=new LearningService(catalog,dir,CLOCK);
        check(s.steps().size()==26 && catalog.questions().size()==52,"26 steps and 52 questions");
        check(catalog.questions().stream().filter(Question::hasImage).count()==40,"all V2 images imported");
        check(catalog.questions().stream().filter(Question::requiresClue).count()==22,"12 field and 10 image judgments");
        check(s.completedSteps()==0 && s.stepUnlocked(1) && !s.stepUnlocked(2),"only first step unlocked");
        rejects(()->s.startStep(2),"future step blocked");rejects(()->s.startStep(0),"invalid step blocked");
        check(!s.discovered("defense"),"rules hidden before encounter");
        QuizSession first=s.startNext();check(first.size()==2 && first.current().id().equals("E01"),"picture foundations first");
        rejects(()->s.advance(first),"cannot skip question");rejects(()->s.submit(first),"answer required");
        first.choose(0);rejects(first::beginClue,"concept skips clue stage");
        Attempt failed=submit(s,first,false,false);check(!failed.understood()&&!failed.clueRequired()&&!failed.clueCorrect(),"no fabricated clue success");
        rejects(()->s.submit(first),"duplicate concept submit blocked");s.advance(first);
        check(s.completedSteps()==0 && s.discovered("defense"),"partial step discovers rule without completion");
        check(new LearningService(catalog,dir,CLOCK).completedSteps()==0,"partial restart preserved");
        Attempt hint=submit(s,first,true,true);check(hint.understood()&&!hint.independent(),"hint counts as assisted knowledge");
        Files.createDirectory(dir.resolve("steps.tsv"));rejects(()->s.advance(first),"completion failure reported");
        check(first.stage()==QuizSession.Stage.FEEDBACK && s.completedSteps()==0,"failed completion does not unlock");
        Files.delete(dir.resolve("steps.tsv"));s.advance(first);check(s.completedSteps()==1,"mistakes do not block advancement");
        rejects(()->s.advance(first),"duplicate completion blocked");
        LearningService restored=new LearningService(catalog,dir,CLOCK);QuizSession second=restored.startNext();
        check(second.size()==4 && second.current().id().equals("E02"),"two prior weaknesses interleaved with new material");
        submit(restored,second,true,false);restored.advance(second);check(second.current().id().equals("E01"),"review in the middle");
        finish(restored,second);check(restored.adaptiveQuestions(List.of(),4).isEmpty(),"successful review clears immediate repeat");
        while(!restored.courseComplete())finish(restored,restored.startNext());
        check(restored.completedSteps()==26 && restored.startNext().stepNumber()==0,"full course and continued practice");
        check(restored.attempts().stream().map(Attempt::questionId).distinct().count()==52,"every imported question reachable");
        finish(restored,restored.startStep(7));check(restored.completedSteps()==26,"replay does not duplicate completion");
        String owner=restored.user().id();QuizSession owned=restored.startStep(1);submit(restored,owned,true,false);restored.addProfile("새 심판");
        check(restored.completedSteps()==0&&!restored.discovered("force"),"isolated progress");
        rejects(()->restored.advance(owned),"cross-user completion blocked");restored.selectUser(owner);
        check(restored.completedSteps()==26,"owner progress preserved");

        Path adaptive=temp();LearningService review=new LearningService(catalog,adaptive,CLOCK);
        submit(review,single(review,"Q01"),false,false);submit(review,single(review,"Q02"),true,false);
        check(review.adaptiveQuestions(List.of(),8).isEmpty(),"different scenario resolves same judgment");
        check(new LearningService(catalog,adaptive,Clock.fixed(NOW.plusSeconds(86399),ZoneOffset.UTC)).adaptiveQuestions(List.of(),8).isEmpty(),"no premature review");
        LearningService day1=new LearningService(catalog,adaptive,Clock.fixed(NOW.plusSeconds(86400),ZoneOffset.UTC));
        List<Question> due=day1.adaptiveQuestions(List.of(),8);
        check(due.size()==1&&due.get(0).id().equals("Q01"),"due review varies scenario without revealing future U08");
        submit(day1,single(day1,"Q01"),true,false);
        check(new LearningService(catalog,adaptive,Clock.fixed(NOW.plus(Duration.ofDays(2)),ZoneOffset.UTC)).adaptiveQuestions(List.of(),8).isEmpty(),"two successes expand interval");
        LearningService day4=new LearningService(catalog,adaptive,Clock.fixed(NOW.plus(Duration.ofDays(4)),ZoneOffset.UTC));
        check(day4.adaptiveQuestions(List.of(),8).size()==1,"three day boundary");submit(day4,single(day4,"Q02"),true,false);
        check(new LearningService(catalog,adaptive,Clock.fixed(NOW.plus(Duration.ofDays(10)),ZoneOffset.UTC)).adaptiveQuestions(List.of(),8).isEmpty(),"seven day interval");
        check(new LearningService(catalog,adaptive,Clock.fixed(NOW.plus(Duration.ofDays(11)),ZoneOffset.UTC)).adaptiveQuestions(List.of(),8).size()==1,"seven day boundary");
        // 별도 인스턴스의 파일 갱신이 없도록 최신 상태를 읽어 이어서 제출한다.
        LearningService mixed=new LearningService(catalog,adaptive,CLOCK);
        submit(mixed,single(mixed,"Q02"),false,false);submit(mixed,single(mixed,"H07"),true,false);
        check(mixed.adaptiveQuestions(List.of(),8).stream().anyMatch(q->q.ruleId().equals("force")&&q.requiresClue()),"concept success cannot erase failed judgment");
        check(mixed.adaptiveQuestions(List.of(),8).stream().noneMatch(q->q.id().equals("U08")),"locked harder variant excluded");
        QuizSession image=single(mixed,"U08");image.choose(image.current().answer());image.beginClue();image.chooseClue("invalid");
        rejects(()->mixed.submit(image),"unknown picture clue blocked");image.chooseClue("a");
        check(!mixed.submit(image).understood(),"correct answer with wrong picture reason not mastered");
        Attempt good=submit(mixed,single(mixed,"U08"),true,false);check(good.independent()&&good.clueRequired(),"image judgment uses same scoring");

        Path legacy=temp();LearningService old=new LearningService(catalog,legacy,CLOCK);String id=old.user().id();
        submit(old,single(old,"Q01"),true,false);old.toggleBookmark("force");old.addNote("force","기존 메모");
        String records="id\tuserId\tstep\tat\n";
        for(int key=1;key<=6;key++)records+=id+":"+key+"\t"+id+"\t"+key+"\t"+NOW+"\n";
        Files.writeString(legacy.resolve("steps.tsv"),records);LearningService migrated=new LearningService(catalog,legacy,CLOCK);
        check(Files.readString(legacy.resolve("steps.tsv")).equals(records),"loading does not rewrite old progress");
        check(migrated.completedSteps()==6&&migrated.nextStepNumber()==1,"new foundations stay incomplete");
        for(int number:List.of(7,8,11,12,16,17))check(migrated.stepCompleted(number)&&migrated.stepUnlocked(number),"old key mapped to new position "+number);
        check(migrated.xp()==30&&migrated.bookmarked("force")&&migrated.notes("").size()==1,"old XP bookmarks notes retained");
        for(int number=1;number<=6;number++)finish(migrated,migrated.startNext());
        check(migrated.nextStepNumber()==9&&migrated.completedSteps()==12,"next action skips already completed old steps");

        Path blocked=temp();LearningService saving=new LearningService(catalog,blocked,CLOCK);Files.createDirectory(blocked.resolve("attempts.tsv"));
        QuizSession retry=single(saving,"E01");retry.choose(retry.current().answer());
        rejects(()->saving.submit(retry),"picture save failure reported");check(retry.stage()==QuizSession.Stage.JUDGE&&saving.attempts().isEmpty(),"failed picture save keeps state");
        Files.delete(blocked.resolve("attempts.tsv"));saving.submit(retry);check(saving.xp()==30,"picture retry saves once");
        System.out.println("PASS: "+checks+" integrated step/review/migration checks");
    }
}
