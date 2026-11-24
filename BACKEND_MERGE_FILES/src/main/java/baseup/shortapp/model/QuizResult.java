package baseup.shortapp.model;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 1회 퀴즈 결과. 실제 입력한 답안도 기록한다. */
public final class QuizResult {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String timestamp,category;
    private final int correct,total;
    private final List<String> wrongIds;
    private final Map<String,String> typed;
    public QuizResult(String timestamp,String category,int correct,int total,List<String> wrongIds,Map<String,String> typed){
        this.timestamp=timestamp;this.category=category;this.correct=correct;this.total=total;
        this.wrongIds=List.copyOf(wrongIds);this.typed=Map.copyOf(typed);
    }
    public static QuizResult now(String category,int correct,int total,List<String> wrong,Map<String,String> typed){
        return new QuizResult(LocalDateTime.now().format(FORMAT),category,correct,total,wrong,typed);
    }
    public String toLine(){
        StringBuilder answers=new StringBuilder();
        for(var e:typed.entrySet()){
            if(answers.length()>0)answers.append('&');
            answers.append(URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)).append('=')
                   .append(URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8));
        }
        return String.join("\t",timestamp,category,""+correct,""+total,String.join(",",wrongIds),answers.toString());
    }
    public static QuizResult fromLine(String line){
        String[] p=line.split("\t",-1);
        if(p.length!=5 && p.length!=6)throw new IllegalArgumentException("기록 파일 형식 오류");
        List<String> wrong=p[4].isBlank()?List.of():Arrays.asList(p[4].split(","));
        Map<String,String> map=new LinkedHashMap<>();
        if(p.length==6 && !p[5].isBlank()){
            for(String pair:p[5].split("&")){
                String[] a=pair.split("=",2);
                if(a.length==2)map.put(URLDecoder.decode(a[0],StandardCharsets.UTF_8),URLDecoder.decode(a[1],StandardCharsets.UTF_8));
            }
        }
        return new QuizResult(p[0],p[1],Integer.parseInt(p[2]),Integer.parseInt(p[3]),wrong,map);
    }
    public String getTimestamp(){return timestamp;}
    public String getCategory(){return category;}
    public int getCorrect(){return correct;}
    public int getTotal(){return total;}
    public int getScore(){return total==0?0:(int)Math.round(100.0*correct/total);}
    public List<String> getWrongIds(){return Collections.unmodifiableList(wrongIds);}
    public Map<String,String> getTypedAnswers(){return Collections.unmodifiableMap(typed);}
}
