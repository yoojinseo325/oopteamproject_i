package baseup.shortapp;

import baseup.shortapp.data.*;
import baseup.shortapp.model.*;
import baseup.shortapp.service.*;
import java.nio.file.*;
import java.util.*;

/** Run with: java -ea ... MixedBackendTest data/short_questions.tsv */
public final class MixedBackendTest {
    private static int assertions;
    private static void ok(boolean value, String message){assertions++;if(!value)throw new AssertionError(message);}
    private static void throwsError(Runnable r,String message){boolean fail=false;try{r.run();}catch(RuntimeException expected){fail=true;}ok(fail,message);}

    public static void main(String[] args)throws Exception {
        if(args.length==0)throw new IllegalArgumentException("문제 데이터 파일 경로를 제공해 주세요");
        QuestionRepository repo=new QuestionRepository(Path.of(args[0]));
        List<QuizQuestion> all=repo.getAll();
        ok(all.size()==40,"총 40문항");
        Map<String,Integer> counts=new LinkedHashMap<>(),shorts=new LinkedHashMap<>(),choices=new LinkedHashMap<>();
        for(String category:List.of("E","M","H","U")){counts.put(category,0);shorts.put(category,0);choices.put(category,0);}
        for(QuizQuestion q:all){
            counts.compute(q.getCategory(),(k,v)->v+1);
            ok(QuizQuestion.fromLine(q.toLine()).getQuestionType()==q.getQuestionType(),q.getId()+" 왕복 직렬화");
            if(q.isMultipleChoice()){
                choices.compute(q.getCategory(),(k,v)->v+1);
                ok(q.getOptions().size()==4,q.getId()+" 보기 수 4");
                ok(AnswerChecker.checkChoice(q,q.getCorrectOptionNumber()),q.getId()+" 객관식 정답 인식");
                ok(!AnswerChecker.checkChoice(q,q.getCorrectOptionNumber()%4+1),q.getId()+" 객관식 오답 인식");
            }else{
                shorts.compute(q.getCategory(),(k,v)->v+1);
                ok(AnswerChecker.checkShortAnswer(q,q.getCorrectAnswer()),q.getId()+" 단답형 정답 인식");
                ok(!AnswerChecker.checkShortAnswer(q,"___wrong_answer___"),q.getId()+" 단답형 오답 인식");
                if(q.getAcceptedAnswers().size()>1)ok(AnswerChecker.checkShortAnswer(q,q.getAcceptedAnswers().get(1)),q.getId()+" 허용 정답");
            }
        }
        for(String c:counts.keySet()){
            ok(counts.get(c)==10,c+" 10문항");
            ok(shorts.get(c)==5,c+" 단답형 5문항");
            ok(choices.get(c)==5,c+" 객관식 5문항");
        }
        List<String> scenarioIds=new ScenarioManager(Path.of(".")).getScenarioIds(all);
        ok(scenarioIds.equals(List.of("U03","U08","U09")),"3개 영상 시나리오");
        ok(repo.findById("U03").isMultipleChoice(),"영상 U03 객관식");
        ok(!repo.findById("U08").isMultipleChoice(),"영상 U08 단답형");
        ok(repo.findById("U09").isMultipleChoice(),"영상 U09 객관식");
        ScenarioManager.ScenarioMedia media=new ScenarioManager(Path.of(".")).getMedia(repo.findById("U03"));
        ok(media.gifPath().endsWith("scenarios/u-3.gif"),"영상 경로 연결");

        // 10개 문제 중 첫 문항은 고의로 틀리고 나머지는 정답으로 제출
        QuizSession session=new QuizSession(repo.getByCategory("E"));
        for(int i=0;i<10;i++){
            QuizQuestion q=session.getCurrentQuestion();
            boolean graded;
            if(q.isMultipleChoice())graded=session.submitChoice(i==0?(q.getCorrectOptionNumber()%4+1):q.getCorrectOptionNumber());
            else graded=session.submit(i==0?"wrong":q.getCorrectAnswer());
            ok(graded==(i!=0),"문제 "+i+" 채점");
            throwsError(()->{if(q.isMultipleChoice())session.submitChoice(1);else session.submit("중복");},"이중 제출 방지");
            ok(session.getProgressPercent()==(i+1)*10,"진행 게이지 "+i);
            session.next();
        }
        ok(session.isFinished(),"세션 종료");
        QuizResult result=session.toResult();
        ok(result.getCorrect()==9 && result.getTotal()==10 && result.getScore()==90,"점수 90");
        ok(result.getWrongIds().equals(List.of("E01")),"오답 ID 저장");
        ok(result.getTypedAnswers().size()==10,"선택/입력 답안 기록 저장");
        ok(QuizResult.fromLine(result.toLine()).getTypedAnswers().equals(result.getTypedAnswers()),"결과 파일 왕복");

        // 통계: 같은 카테고리 90점, 70점 -> 회차별 평균 80점
        Path temp=Files.createTempDirectory("baseup-mixed-test-");
        ResultRepository aResult=new ResultRepository(temp.resolve("a/results.tsv"));
        StudyRepository aStudy=new StudyRepository(temp.resolve("a/study.tsv"));
        aResult.append(result);
        aResult.append(QuizResult.now("E",7,10,List.of("E01","E02","E03"),Map.of("E01","wrong")));
        aStudy.markViewed("E01");aStudy.markViewed("E02");
        UserStatistics stats=new StatisticsManager(aResult,aStudy).calculate();
        ok(stats.totalAnswered()==20 && stats.totalCorrect()==16 && stats.totalWrong()==4,"누적 정오답");
        ok(stats.quizAttempts()==2 && stats.overallAccuracy()==80,"도전 횟수/전체 정답률");
        ok(Math.abs(stats.categoryAccuracy().get("E")-80.0)<0.001,"난이도별 평균 정답률");
        ok(stats.learnedRules()==2 && stats.recentResults().size()==2,"학습 기록/최근 점수");
        ResultRepository bResult=new ResultRepository(temp.resolve("b/results.tsv"));
        StudyRepository bStudy=new StudyRepository(temp.resolve("b/study.tsv"));
        ok(new StatisticsManager(bResult,bStudy).calculate().totalAnswered()==0,"다른 사용자와 기록 분리");

        String old=all.get(0).toLine();
        String[] oldFields=old.split("\\t",-1);
        ok(QuizQuestion.fromLine(String.join("\t",Arrays.copyOf(oldFields,12))).getQuestionType()==QuestionType.SHORT_ANSWER,"구버전 12필드 호환");
        System.out.println("PASS: " + assertions + " backend assertions; 40 questions (20 MCQ + 20 SHORT); scenario 3");
    }
}
