package baseup;

import baseup.repository.Catalog;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** 깨진 그림이나 연결 오류를 정상 콘텐츠로 받아들이지 않는지 검증한다. */
public final class ContentIntegrationTest {
    private static int checks;
    private static Path data;
    private static void invalid(String file,String from,String to) throws Exception {
        Path path=data.resolve(file);String original=Files.readString(path);
        if(!original.contains(from))throw new AssertionError("Missing fixture: "+from);
        Files.writeString(path,original.replace(from,to));
        try {new Catalog(data);throw new AssertionError("Accepted invalid "+file);}
        catch(IOException|IllegalArgumentException expected){checks++;}finally{Files.writeString(path,original);}
    }
    public static void main(String[] args) throws Exception {
        data=Files.createTempDirectory(Path.of("build"),"content-integration-");
        try(var paths=Files.walk(Path.of("data"))) {
            for(Path path:paths.toList()) {
                Path target=data.resolve(Path.of("data").relativize(path));
                if(Files.isDirectory(path))Files.createDirectories(target);else Files.copy(path,target);
            }
        }
        new Catalog(data);checks++;
        invalid("image_questions.tsv","e-1.png","../e-1.png");
        invalid("image_questions.tsv","e-1.png","missing.png");
        invalid("image_questions.tsv","E01\tdefense","Q01\tdefense");
        invalid("image_questions.tsv","E01\tdefense","E01\tmissing-rule");
        invalid("image_questions.tsv","\tc\t판정: 타자주자 세이프","\tunknown\t판정: 타자주자 세이프");
        invalid("steps.tsv","E01|E05","E01|E01");
        invalid("steps.tsv","E01|E05","E01|missing");
        invalid("steps.tsv","E01|E05","E01");
        invalid("steps.tsv","7\t그림에서","1\t그림에서");
        Path image=data.resolve("images/e-1.png");byte[] original=Files.readAllBytes(image);Files.writeString(image,"not a PNG");
        try{new Catalog(data);throw new AssertionError("Accepted broken image");}catch(IOException expected){checks++;}finally{Files.write(image,original);}
        Catalog restored=new Catalog(data);
        if(restored.questions().size()!=52)throw new AssertionError("Fixture restore failed");checks++;
        System.out.println("PASS: "+checks+" integrated content validation checks");
    }
}
