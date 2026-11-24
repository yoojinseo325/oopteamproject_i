package baseup.ui;

import baseup.model.*;
import baseup.service.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import static baseup.ui.Theme.*;

/** 응원팀 경기 일정·결과: data/games.tsv 를 읽는다(인터넷 API 없이 직접 입력하는 파일). */
final class TeamSchedule {
    private TeamSchedule() { }
    /** 응원팀 경기 기록: data/games.tsv 를 읽는다(직접 입력). 파일이 없으면 빈 목록. */
    static List<GameResult> games() throws java.io.IOException {
        return new baseup.repository.FileRepository<GameResult>(java.nio.file.Path.of("data","games.tsv"),GameResult.HEADER,GameResult::decode,GameResult::encode).load();
    }
    static String teamName(LearningService service, String id) {
        return service.catalog().teams().stream().filter(t->t.id().equals(id)).map(Team::shortName).findFirst().orElse(id);
    }
    static String shortDate(String date) {
        try { java.time.LocalDate d=java.time.LocalDate.parse(date);
            return d.getMonthValue()+"/"+d.getDayOfMonth()+" ("+"월화수목금토일".charAt(d.getDayOfWeek().getValue()-1)+")"; }
        catch(Exception e) { return date; }
    }
}
