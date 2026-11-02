package baseup;

import baseup.model.*;
import baseup.repository.Catalog;
import baseup.ui.FieldPanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** 구단/선수 연결과 실제 화면의 색상·클릭을 함께 검증한다. */
public final class KboContentTest {
    private static int checks;
    private static void check(boolean condition,String name) { if(!condition)throw new AssertionError(name);checks++; }
    public static void main(String[] args) throws Exception {
        Catalog catalog=new Catalog(Path.of("data"));
        check(catalog.teams().size()==10,"ten KBO teams");check(catalog.players().size()==18,"verified player roster");
        Set<String> used=new HashSet<>();
        Path images=Path.of("build/qa/kbo");Files.createDirectories(images);
        for(Question q:catalog.questions()) {
            Matchup matchup=catalog.matchup(q.id());used.add(matchup.offense().id());used.add(matchup.defense().id());
            check(!matchup.offense().equals(matchup.defense()),"opposing teams");
            Map<String,String> identities=new HashMap<>();
            for(Scene scene:List.of(catalog.scene(q.beforeScene()),catalog.scene(q.afterScene()))) {
                SwingUtilities.invokeAndWait(()->{
                    FieldPanel field=new FieldPanel(scene);field.setSize(800,500);
                    BufferedImage image=new BufferedImage(800,500,BufferedImage.TYPE_INT_RGB);
                    Graphics2D g=image.createGraphics();field.paint(g);g.dispose();
                    for(Scene.Token token:scene.tokens()) {
                        if(token.player()==null)continue;
                        check(token.player().team().equals(token.kind().equals("runner")?matchup.offense():matchup.defense()),"correct side "+scene.id());
                        String previous=identities.putIfAbsent(token.id(),token.player().id());
                        check(previous==null||previous.equals(token.player().id()),"player stable between scenes");
                        int expected=token.player().team().color(), pixels=0;
                        for(int x=token.x()-14;x<=token.x()+14;x++) for(int y=token.y()-14;y<=token.y()+14;y++)
                            if((image.getRGB(x,y)&0xffffff)==expected)pixels++;
                        check(pixels>200,"rendered team color "+scene.id()+"/"+token.id());
                        String[] picked={null};field.onSelect(id->picked[0]=id);
                        field.dispatchEvent(new MouseEvent(field,MouseEvent.MOUSE_CLICKED,0,0,token.x(),token.y(),1,false));
                        check(token.id().equals(picked[0]),"player click still selects correct clue");
                    }
                    try { ImageIO.write(image,"png",images.resolve(scene.id()+".png").toFile()); } catch(Exception e){throw new RuntimeException(e);}
                });
            }
        }
        check(used.size()==10,"all ten teams appear");
        Scene.Token batter=catalog.scene("Q01a").tokens().stream().filter(t->t.id().equals("batter")).findFirst().orElseThrow();
        check(batter.player().name().equals("김도영") && batter.player().team().color()==0xEA0029,"KIA batter is red");
        Path broken=Files.createTempDirectory(Path.of("build"),"kbo-invalid-");
        for(String file:List.of("teams.tsv","players.tsv","matchups.tsv","scenes.tsv","questions.tsv","rules.tsv"))
            Files.copy(Path.of("data",file),broken.resolve(file));
        rejectEdit(broken,"players.tsv","kia_kim\t김도영\tKIA","kia_kim\t김도영\tUNKNOWN");
        rejectEdit(broken,"scenes.tsv",",kia_kim",",unknown_player");
        rejectEdit(broken,"scenes.tsv",",kia_kim",",sam_kim");
        rejectEdit(broken,"teams.tsv","#EA0029","#NOTRGB");
        rejectEdit(broken,"matchups.tsv","Q01\tKIA\tSAMSUNG","Q01\tKIA\tKIA");
        System.out.println("PASS: "+checks+" KBO content/render checks");
    }
    private static void rejectEdit(Path dir,String file,String from,String to) throws Exception {
        Path path=dir.resolve(file);String original=Files.readString(path);Files.writeString(path,original.replace(from,to));
        try {new Catalog(dir);throw new AssertionError("Invalid content accepted: "+file);}
        catch(java.io.IOException expected){checks++;}finally{Files.writeString(path,original);}
    }
}
