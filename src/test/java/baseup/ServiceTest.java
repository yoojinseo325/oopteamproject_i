package baseup;

import baseup.model.*;
import baseup.repository.*;
import baseup.service.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** JUnit 없이 JDK만으로 실행하는 실제 파일 입출력 통합 테스트. */
public final class ServiceTest {
    private static int checks;
    private static void check(boolean value,String name) {
        if(!value)throw new AssertionError(name);checks++;
    }
    @FunctionalInterface interface Work { void run() throws Exception; }
    private static void rejects(Work work,String name) throws Exception {
        try{work.run();}catch(IOException|IllegalStateException|IllegalArgumentException e){checks++;return;}
        throw new AssertionError(name);
    }
    private static Attempt answer(LearningService s,Question q,int answer,String clue,boolean hint) throws Exception {
        QuizSession session=s.start(x->List.of(q));session.choose(answer);session.beginClue();session.chooseClue(clue);
        if(hint)session.useHint();return s.submit(session);
    }
    public static void main(String[] args) throws Exception {
        Catalog catalog=new Catalog(Path.of("data"));
        check(catalog.rules().size()==6,"six rules");check(catalog.questions().size()==12,"twelve questions");
        check(catalog.searchRules("포스").size()>=1,"Korean search");check(catalog.searchRules("없는단어").isEmpty(),"empty search");
        Path temp=Files.createTempDirectory(Path.of("build"),"service-test-");
        Instant now=Instant.parse("2030-01-01T00:00:00Z");Clock clock=Clock.fixed(now,ZoneOffset.UTC);
        LearningService service=new LearningService(catalog,temp,clock);String firstUser=service.user().id();
        check(service.attempts().isEmpty(),"fresh user");check(service.reviewQuestions().isEmpty(),"no unattempted reviews");
        int[] expected={0,1,0,1,0,1,1,0,0,0,0,1};
        for(int i=0;i<catalog.questions().size();i++) {
            Question q=catalog.questions().get(i);check(q.answer()==expected[i],"curated answer "+q.id());
            QuizSession session=service.start(s->List.of(q));
            rejects(session::beginClue,"reject no choice");rejects(()->session.choose(-1),"invalid choice");
            session.choose(expected[i]);session.beginClue();rejects(()->service.submit(session),"reject no clue");
            session.chooseClue("not-a-token");rejects(()->service.submit(session),"reject unknown clue");
            session.chooseClue(q.clueTarget());Attempt a=service.submit(session);
            check(a.independent(),"full correct "+q.id());check(a.seconds()==0,"injected clock");
            rejects(()->service.submit(session),"duplicate submit");
            session.next();check(session.stage()==QuizSession.Stage.COMPLETE,"complete");rejects(session::next,"duplicate next");
        }
        check(service.solvedCount()==12,"all solved");check(service.xp()==360,"XP maximum");
        check(service.reviewQuestions().isEmpty(),"no immediate successful review");
        LearningService restored=new LearningService(catalog,temp,clock);
        check(restored.attempts().size()==12,"restart restores attempts");check(restored.xp()==360,"restart restores XP");
        LearningService nextDay=new LearningService(catalog,temp,Clock.fixed(now.plus(Duration.ofDays(1)),ZoneOffset.UTC));
        check(nextDay.reviewQuestions().size()==12,"24-hour boundary");
        LearningService justBefore=new LearningService(catalog,temp,Clock.fixed(now.plusSeconds(86399),ZoneOffset.UTC));
        check(justBefore.reviewQuestions().isEmpty(),"before 24-hour boundary");
        Question first=catalog.questions().get(0);
        answer(restored,first,1,first.clueTarget(),false);
        check(restored.reviewQuestions().size()==1,"wrong answer queued");
        answer(restored,first,first.answer(),"batter",false);
        check(restored.solvedCount()==11,"wrong reason not mastered");
        answer(restored,first,first.answer(),first.clueTarget(),true);
        check(restored.reviewQuestions().size()==1,"hint remains queued");
        answer(restored,first,first.answer(),first.clueTarget(),false);
        check(restored.reviewQuestions().isEmpty(),"successful review clears queue");
        check(restored.xp()==360,"repeats do not farm XP");
        restored.toggleBookmark("force");check(restored.bookmarked("force"),"add bookmark");
        LearningService bookmarkReload=new LearningService(catalog,temp,clock);check(bookmarkReload.bookmarked("force"),"bookmark persisted");
        bookmarkReload.toggleBookmark("force");check(!new LearningService(catalog,temp,clock).bookmarked("force"),"bookmark deletion persisted");
        String note="내 메모\t한글\n공이 든 글러브 \\ 경로 <b>";
        bookmarkReload.addNote("tag",note);LearningService noteReload=new LearningService(catalog,temp,clock);
        check(noteReload.notes("글러브").size()==1,"note search");check(noteReload.notes("").get(0).text().equals(note),"escaped TSV round trip");
        String noteId=noteReload.notes("").get(0).id();noteReload.deleteNote(noteId);
        check(new LearningService(catalog,temp,clock).notes("").isEmpty(),"note deletion persisted");
        rejects(()->noteReload.addNote("tag","  "),"blank note");rejects(()->noteReload.addProfile("  "),"blank name");
        rejects(()->noteReload.addProfile("가".repeat(17)),"long name");rejects(()->noteReload.addProfile("신입 심판"),"duplicate name");
        QuizSession owned=noteReload.start(s->List.of(first));owned.choose(0);owned.beginClue();owned.chooseClue(first.clueTarget());
        UserProfile second=noteReload.addProfile("팀원 둘");check(noteReload.attempts().isEmpty(),"user isolation");
        rejects(()->noteReload.submit(owned),"cross-user session blocked");
        noteReload.toggleBookmark("tag");noteReload.addNote("tag","둘의 메모");
        noteReload.selectUser(firstUser);check(noteReload.notes("").isEmpty(),"notes isolated");check(!noteReload.bookmarked("tag"),"bookmarks isolated");
        noteReload.selectUser(second.id());check(noteReload.notes("").size()==1,"second user data restored");
        check(new TopicSelector("포스와 태그").select(noteReload).size()==4,"topic selector");
        check(new ReviewSelector().select(noteReload).isEmpty(),"review selector independent");
        Path broken=Files.createTempDirectory(Path.of("build"),"broken-test-");
        Files.writeString(broken.resolve("profiles.tsv"),"wrong-header\n");
        rejects(()->new LearningService(catalog,broken),"corruption reported");
        check(Files.readString(broken.resolve("profiles.tsv")).equals("wrong-header\n"),"corrupt file preserved");
        Path blocked=Files.createTempDirectory(Path.of("build"),"save-test-");
        LearningService saving=new LearningService(catalog,blocked,clock);Files.createDirectory(blocked.resolve("attempts.tsv"));
        QuizSession retry=saving.start(s->List.of(first));retry.choose(0);retry.beginClue();retry.chooseClue(first.clueTarget());
        rejects(()->saving.submit(retry),"failed save reported");check(saving.attempts().isEmpty(),"failed save does not mutate memory");
        check(retry.stage()==QuizSession.Stage.CLUE,"failed save preserves submit state");
        Files.delete(blocked.resolve("attempts.tsv"));saving.submit(retry);check(saving.attempts().size()==1,"retry succeeds exactly once");
        System.out.println("PASS: "+checks+" checks (content, persistence, profiles, review, state, save failure)");
    }
}
