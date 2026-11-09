package baseup.ui;

import baseup.model.QuizQuestion;
import baseup.model.QuizResult;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;

/** 저장된 퀴즈 결과의 이력표. */
public final class HistoryPanel extends JPanel {
    public HistoryPanel(MainFrame frame, List<QuizResult> results) {
        setLayout(new BorderLayout(0, 20));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(30, 55, 35, 55));
        add(Theme.label("학습 기록 / 퀴즈 결과", 27, Theme.WHITE, true), BorderLayout.NORTH);
        if (results.isEmpty()) {
            JPanel card = Theme.card();
            card.setLayout(new BorderLayout());
            card.add(Theme.label("아직 퀴즈 기록이 없습니다. 퀴즈를 완료하면 이곳에 저장됩니다.", 16, Theme.MUTED, false));
            add(card, BorderLayout.CENTER);
            return;
        }
        DefaultTableModel model = new DefaultTableModel(new String[]{"일시", "유형", "정답/전체", "점수", "오답 ID"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        Collections.reverse(results);
        for (QuizResult r : results)
            model.addRow(new Object[]{r.getTimestamp(), QuizQuestion.categoryName(r.getCategory()),
                    r.getCorrect()+"/"+r.getTotal(), r.getScore()+"점", String.join(", ", r.getWrongIds())});
        JTable table = new JTable(model);
        table.setFont(Theme.font(java.awt.Font.PLAIN, 14));
        table.setRowHeight(37);
        table.setBackground(Theme.SURFACE);
        table.setForeground(Theme.WHITE);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(Theme.SURFACE_2);
        table.setSelectionForeground(Theme.WHITE);
        table.getTableHeader().setBackground(Theme.SURFACE_2);
        table.getTableHeader().setForeground(Theme.WHITE);
        table.getTableHeader().setFont(Theme.font(java.awt.Font.BOLD, 14));
        JScrollPane scroll = Theme.scroll(table);
        add(scroll, BorderLayout.CENTER);
    }
}
