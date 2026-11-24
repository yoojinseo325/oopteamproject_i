package baseup.shortapp;

import baseup.shortapp.data.QuestionRepository;
import baseup.shortapp.model.QuestionType;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 12열 단답형 파일을 읽을 수 있고, 처음 실행 때 백업 후 17열 혼합형으로 변환한다. */
public final class MigrationTest {
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("baseup_migrate_");
  Files.createDirectories(root.resolve("data"));
  List<String> old=new ArrayList<>();
  for(String s:Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8)){
   if(s.startsWith("#")||s.isBlank())continue;
   String[] p=s.split("\\t",-1);
   old.add(String.join("\t",Arrays.copyOf(p,12)));
  }
  Files.write(root.resolve("data/short_questions.tsv"),old,StandardCharsets.UTF_8);
  baseup.shortapp.service.QuestionMigration.ensureCurrentData(root);
  QuestionRepository repo=new QuestionRepository(root.resolve("data/short_questions.tsv"));
  if(repo.getAll().size()!=40)throw new AssertionError("40문항 유지 실패");
  long choices=repo.getAll().stream().filter(q->q.getQuestionType()==QuestionType.MULTIPLE_CHOICE).count();
  if(choices!=20)throw new AssertionError("변환 후 객관식 20문항이어야 합니다: "+choices);
  if(!Files.isRegularFile(root.resolve("data/short_questions.pre-mixed.tsv")))throw new AssertionError("구버전 백업 실패");
  baseup.shortapp.service.QuestionMigration.ensureCurrentData(root);
  if(repo.getAll().size()!=40)throw new AssertionError("반복 마이그레이션 실패");
  System.out.println("PASS: 12-column to 17-column migration, original file backed up, 40 questions retained");
 }
}
