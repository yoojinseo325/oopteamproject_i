package baseup.shortapp.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 사용자별 학습한 규칙을 기록. 검색 결과를 미리 보여주는 것만으로는 학습으로 카운트하지 않는다. */
public final class StudyRepository {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path path;
    private final Set<String> visited=new LinkedHashSet<>();
    private final List<String> log=new ArrayList<>();
    public StudyRepository(Path path)throws IOException{
        this.path=path;
        if(Files.isRegularFile(path))for(String row:Files.readAllLines(path,StandardCharsets.UTF_8)){
            if(row.isBlank()||row.startsWith("#"))continue;
            String[] a=row.split("\t",2);
            if(a.length!=2 || !a[1].matches("(?:[EMHU][0-9]{2,3}|G01)"))throw new IOException("학습 기록 형식 오류");
            log.add(row);visited.add(a[1]);
        }
    }
    public synchronized void markViewed(String id)throws IOException{
        if(id==null||!id.matches("(?:[EMHU][0-9]{2,3}|G01)"))throw new IllegalArgumentException("잘못된 규칙 ID");
        if(visited.contains(id))return;
        String row=LocalDateTime.now().format(FORMAT)+"\t"+id;
        Files.createDirectories(path.getParent());
        Files.writeString(path,row+System.lineSeparator(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        visited.add(id);log.add(row);
    }
    public synchronized Set<String> visitedIds(){return Set.copyOf(visited);}
    public synchronized int coreVisitedCount(){
        int n=0;for(String id:visited)if(id.matches("[EMHU][0-9]{2,3}"))n++;
        return n;
    }
    public synchronized List<String> recentIds(){
        List<String> result=new ArrayList<>();
        for(int i=log.size()-1;i>=0;i--)result.add(log.get(i).split("\t")[1]);
        return result;
    }
}
