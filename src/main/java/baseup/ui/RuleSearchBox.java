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
    private final JTextField field = input("규칙 검색  ·  예: 태그, 파울, 인필드 플라이");
    private final JPanel results = column();

    RuleSearchBox(LearningService service, Consumer<String> openRule) {
        this.service = service;
        this.openRule = openRule;
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));
        add(field, BorderLayout.NORTH);
        add(results, BorderLayout.CENTER);
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        field.addActionListener(e -> {           // 엔터: 첫 번째 발견한 규칙 열기
            for (RuleConcept r : search()) if (service.discovered(r.id())) { openRule.accept(r.id()); return; }
        });
    }

    /** 검색 결과: 발견한 규칙을 먼저, 최대 4개. 못 만난 규칙은 제목으로만 찾는다(내용 스포일러 방지). */
    private List<RuleConcept> search() {
        String q = field.getText().strip();
        List<RuleConcept> found = new ArrayList<>(), locked = new ArrayList<>();
        if (q.isEmpty()) return found;
        for (RuleConcept r : service.catalog().searchRules(q)) {
            if (service.discovered(r.id())) found.add(r);
            else if (r.title().contains(q)) locked.add(r);
        }
        found.addAll(locked);
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
        boolean open = service.discovered(r.id());
        RoundedPanel item = new RoundedPanel(open ? WHITE : new Color(0xECEFEB), 16);
        item.setBorder(new EmptyBorder(14, 18, 14, 18));
        item.setLayout(new BorderLayout(14, 0));
        JPanel text = column();
        Theme.add(text, label(open ? r.topic() : "🔒  아직 만나지 않은 규칙", 11, open ? GREEN : MUTED, true));
        gap(text, 5);
        Theme.add(text, label(r.title(), 16, open ? INK : MUTED, true));
        gap(text, 4);
        Theme.add(text, label(open ? r.summary() : stepHint(service, r.id()), 13, open ? INK : MUTED, false));
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
