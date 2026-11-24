package baseup.ui;

import baseup.model.LearningStep;
import baseup.model.RuleConcept;
import baseup.service.LearningService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static baseup.ui.Theme.*;

/**
 * 메인페이지 규칙 검색창 + 미리보기.
 * 발견한 규칙은 요약을 보여 주고, 아직 못 만난 규칙은 제목과 "STEP n에서 만나요"만 보여 준다.
 */
final class RuleSearchBox extends JPanel {
    private static final int MAX_RESULTS = 4;
    private final LearningService service;
    private final Consumer<String> openRule;          // 규칙 id를 받아 상세 화면을 여는 동작
    private final baseup.service.RuleEncyclopedia book;   // 검색 로직(기능 5)
    private final JTextField field = input("규칙 검색  ·  예: 태그, 인필드플라이, 보크, balk");
    private final JPanel results = column();

    RuleSearchBox(LearningService service, Consumer<String> openRule) {
        this.service = service;
        this.openRule = openRule;
        this.book = new baseup.service.RuleEncyclopedia(service.catalog());
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));
        add(field, BorderLayout.NORTH);
        add(results, BorderLayout.CENTER);
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        field.addActionListener(e -> {           // 엔터: 가장 관련도 높은 규칙을 백과사전에서 열기
            List<RuleConcept> found = search();
            if (!found.isEmpty()) openRule.accept(found.get(0).id());
        });
    }

    /** 검색 결과: 규칙 백과사전(RuleEncyclopedia)의 관련도 순 결과 최대 4개. 띄어쓰기·영어·별명도 찾는다. */
    private List<RuleConcept> search() {
        String q = field.getText().strip();
        if (q.isEmpty()) return new ArrayList<>();
        List<RuleConcept> found = book.search(q);
        return found.size() > MAX_RESULTS ? found.subList(0, MAX_RESULTS) : found;
    }

    private void refresh() {
        results.removeAll();
        List<RuleConcept> list = search();
        if (!field.getText().isBlank() && list.isEmpty())
            Theme.add(results, label("   찾는 규칙이 없어요. 다른 단어로 검색해 보세요.", 13, MUTED, false));
        for (RuleConcept r : list) { Theme.add(results, row(r)); gap(results, 8); }
        revalidate(); repaint();
        Container top = getTopLevelAncestor();
        if (top != null) { top.revalidate(); top.repaint(); }
    }

    /** 결과 한 줄. */
    private JComponent row(RuleConcept r) {
        // 백과사전은 문제를 풀지 않은 규칙도 보여 주므로 모든 결과를 열 수 있다. 아직 안 푼 규칙은 표시만 다르게 한다.
        boolean open = true, learned = service.discovered(r.id());
        RoundedPanel item = new RoundedPanel(open ? WHITE : new Color(0xECEFEB), 16);
        item.setBorder(new EmptyBorder(14, 18, 14, 18));
        item.setLayout(new BorderLayout(14, 0));
        JPanel text = column();
        Theme.add(text, label(r.category().label() + (learned ? "  ·  학습함" : "  ·  " + stepHint(service, r.id())), 11, GREEN, true));
        gap(text, 5);
        Theme.add(text, label(r.title(), 16, open ? INK : MUTED, true));
        gap(text, 4);
        Theme.add(text, label(r.summary(), 13, INK, false));
        item.add(text, BorderLayout.CENTER);
        if (open) {
            item.add(label("자세히  →", 13, GREEN, true), BorderLayout.EAST);
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            item.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { openRule.accept(r.id()); }
            });
        }
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        return item;
    }

    /** 이 규칙을 처음 다루는 STEP 번호를 찾아 안내 문구로 만든다. */
    static String stepHint(LearningService service, String ruleId) {
        for (LearningStep step : service.steps())
            for (String qid : step.questionIds())
                if (service.catalog().question(qid).map(q -> q.ruleId().equals(ruleId)).orElse(false))
                    return "STEP " + step.number() + "에서 만날 수 있어요";
        return "학습 경로를 진행하면 열려요";
    }

    /** 미리보기 테스트용: 검색어를 넣는다. */
    void search(String text) { field.setText(text); }
}
