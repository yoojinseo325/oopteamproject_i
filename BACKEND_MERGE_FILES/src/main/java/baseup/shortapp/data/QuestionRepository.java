package baseup.shortapp.data;

import baseup.shortapp.model.QuizQuestion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 혼합형 40문항 관리. 12필드 구버전 파일도 읽을 수 있다. */
public final class QuestionRepository {
    private final Path path;
    private final Map<String,QuizQuestion> questions=new LinkedHashMap<>();
    private long revision;
    public QuestionRepository(Path path)throws IOException{this.path=path;load();}
    private void load()throws IOException{
        if(!Files.exists(path))throw new IOException("문제 데이터가 없습니다: "+path.toAbsolutePath());
        int n=0;
        for(String line:Files.readAllLines(path,StandardCharsets.UTF_8)){
            n++;if(line.isBlank()||line.startsWith("#"))continue;
            try{
                QuizQuestion q=QuizQuestion.fromLine(line);
                if(questions.putIfAbsent(q.getId(),q)!=null)throw new IllegalArgumentException("중복 ID "+q.getId());
            }catch(RuntimeException e){throw new IOException(n+"번째 줄의 데이터 오류: "+e.getMessage(),e);}
        }
    }
    public synchronized List<QuizQuestion> getAll(){return new ArrayList<>(questions.values());}
    public synchronized List<QuizQuestion> getByCategory(String category){
        List<QuizQuestion> result=new ArrayList<>();
        for(QuizQuestion q:questions.values())if(q.getCategory().equals(category))result.add(q);
        return result;
    }
    public synchronized QuizQuestion findById(String id){return questions.get(id.trim().toUpperCase());}
    public synchronized long getRevision(){return revision;}
    public synchronized void addAndSave(QuizQuestion q)throws IOException{
        if(questions.containsKey(q.getId()))throw new IllegalArgumentException("이미 존재하는 문제 ID입니다");
        questions.put(q.getId(),q);
        try{save();revision++;}catch(IOException e){questions.remove(q.getId());throw e;}
    }
    public synchronized void deleteAndSave(String id)throws IOException{
        if(!questions.containsKey(id))throw new IllegalArgumentException("문제가 존재하지 않습니다: "+id);
        QuizQuestion prev=questions.remove(id);
        try{save();revision++;}catch(IOException e){questions.put(id,prev);throw e;}
    }
    private void save()throws IOException{
        Files.createDirectories(path.getParent());
        Path tmp=path.resolveSibling(path.getFileName()+".tmp");
        List<String> rows=new ArrayList<>();
        rows.add("# BASEUP 혼합형 문제 17필드 (UTF-8 TSV)");
        rows.add("# id,category,image,guide,term,description,situation,question,answer,aliases,explanation,scenarioGif,questionType,option1,option2,option3,option4");
        for(QuizQuestion q:questions.values())rows.add(q.toLine());
        Files.write(tmp,rows,StandardCharsets.UTF_8);
        try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
    }
}
