package baseup.shortapp.model;

import java.util.List;
import java.util.Map;

/** 마이페이지에 필요한 사용자별 통계 결과 객체. */
public record UserStatistics(
    int totalAnswered, int totalCorrect, int totalWrong, int quizAttempts,
    int learnedRules, int latestScore,
    Map<String,Double> categoryAccuracy,
    List<QuizResult> recentResults, List<String> recentRuleIds) {
    public int overallAccuracy(){return totalAnswered==0?0:(int)Math.round(100.0*totalCorrect/totalAnswered);}
    public int learningProgress(){return 100*learnedRules/40;}
}
