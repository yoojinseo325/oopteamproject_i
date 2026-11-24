package baseup.shortapp.service;

import baseup.shortapp.data.ResultRepository;
import baseup.shortapp.data.StudyRepository;
import baseup.shortapp.model.QuizResult;
import baseup.shortapp.model.UserStatistics;
import java.io.IOException;
import java.util.*;

/** 사용자 퀴즈 이력과 학습 기록 집계. 난이도별 정답률은 회차별 점수의 평균이다. */
public final class StatisticsManager {
    private final ResultRepository results;
    private final StudyRepository studies;
    public StatisticsManager(ResultRepository results,StudyRepository studies){this.results=results;this.studies=studies;}
    public UserStatistics calculate()throws IOException{
        List<QuizResult> rows=results.load();int answered=0,correct=0;
        Map<String,int[]> per=new LinkedHashMap<>();
        for(String key:new String[]{"E","M","H","U"})per.put(key,new int[2]);
        for(QuizResult r:rows){
            answered+=r.getTotal();correct+=r.getCorrect();
            int[] slot=per.get(r.getCategory());
            if(slot!=null){slot[0]+=r.getScore();slot[1]++;}
        }
        Map<String,Double> accuracy=new LinkedHashMap<>();
        per.forEach((k,v)->accuracy.put(k,v[1]==0?0:(1.0*v[0]/v[1])));
        List<QuizResult> latest=new ArrayList<>(rows);Collections.reverse(latest);
        return new UserStatistics(answered,correct,answered-correct,rows.size(),
            studies.coreVisitedCount(),rows.isEmpty()?0:rows.get(rows.size()-1).getScore(),
            Collections.unmodifiableMap(accuracy),List.copyOf(latest.subList(0,Math.min(5,latest.size()))),
            List.copyOf(studies.recentIds().subList(0,Math.min(6,studies.recentIds().size()))));
    }
}
