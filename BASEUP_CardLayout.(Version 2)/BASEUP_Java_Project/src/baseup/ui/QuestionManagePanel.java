package baseup.ui;

import baseup.data.QuestionRepository;
import baseup.model.QuizQuestion;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.border.EmptyBorder;

/** 문제 검색 / 추가 / 삭제, 40문항 데이터 유지관리. */
public final class QuestionManagePanel extends JPanel {
    private final MainFrame frame;
    private final QuestionRepository repo;
    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;

    public QuestionManagePanel(MainFrame frame, QuestionRepository repo) {
        this.frame = frame;
        this.repo = repo;
        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(28, 45, 33, 45));
        JPanel header = Theme.panel();
        header.setLayout(new BorderLayout(0, 14));
        header.add(Theme.label("문제 관리", 27, Theme.WHITE, true), BorderLayout.NORTH);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);
        JTextField search = new JTextField(25);
        Theme.styleTextField(search);
        controls.add(search);
        JButton add = Theme.button("문제 추가", true);
        JButton delete = Theme.button("선택한 문제 삭제", false);
        controls.add(add); controls.add(delete);
        header.add(controls, BorderLayout.CENTER);
        header.add(Theme.label("ID·유형·용어·질문으로 검색합니다. 변경 내용은 data/questions.txt에 저장됩니다.", 13, Theme.MUTED, false), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        model = new DefaultTableModel(new Object[]{"문제 ID", "유형", "학습 용어", "이미지", "질문"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(33);
        table.setFont(Theme.font(java.awt.Font.PLAIN, 13));
        table.setBackground(Theme.SURFACE);
        table.setForeground(Theme.WHITE);
        table.setSelectionBackground(Theme.SURFACE_2);
        table.setSelectionForeground(Theme.WHITE);
        table.setGridColor(Theme.BORDER);
        table.getTableHeader().setBackground(Theme.SURFACE_2);
        table.getTableHeader().setForeground(Theme.WHITE);
        table.getTableHeader().setFont(Theme.font(java.awt.Font.BOLD, 14));
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(570);
        add(Theme.scroll(table), BorderLayout.CENTER);
        reload();
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filter() {
                String s = search.getText().trim();
                sorter.setRowFilter(s.isEmpty() ? null : RowFilter.regexFilter("(?i)"+Pattern.quote(s)));
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        add.addActionListener(e -> openAddDialog());
        delete.addActionListener(e -> deleteSelected());
    }
    private void reload() {
        model.setRowCount(0);
        for (QuizQuestion q : repo.getAll())
            model.addRow(new Object[]{q.getId(), QuizQuestion.categoryName(q.getCategory()),
                    q.getLearningTerm(), q.getImageFile(), q.getQuestion()});
    }
    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { frame.warning("먼저 삭제할 문제를 선택해 주세요."); return; }
        String id = (String)model.getValueAt(table.convertRowIndexToModel(row), 0);
        int yn = JOptionPane.showConfirmDialog(this, id+" 문제를 삭제할까요?", "문제 삭제", JOptionPane.YES_NO_OPTION);
        if (yn != JOptionPane.YES_OPTION) return;
        try { repo.deleteAndSave(id); reload(); }
        catch (IOException | IllegalArgumentException ex) { frame.warning("삭제 실패: " + ex.getMessage()); }
    }
    private void openAddDialog() {
        JDialog d = new JDialog(frame, "새 퀴즈 문제 추가", true);
        JPanel form = Theme.card();
        form.setLayout(new GridBagLayout());
        JTextField id = field(18), image = field(30), guide = field(40), term = field(30);
        JTextField description = field(45), situation = field(45), question = field(45), explanation = field(45);
        JTextField[] options = {field(40), field(40), field(40), field(40)};
        JComboBox<String> category = new JComboBox<>(new String[]{"E - 쉬움", "M - 중간", "H - 어려움", "U - 심판 판정"});
        JSpinner answer = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        JComponentRow[] rows = {
                new JComponentRow("문제 ID (예: E11)", id),
                new JComponentRow("유형", category),
                new JComponentRow("이미지 파일 (예: e-11.png)", image),
                new JComponentRow("이미지 제작 가이드 (정답 노출 주의)", guide),
                new JComponentRow("학습 용어", term),
                new JComponentRow("학습 설명", description),
                new JComponentRow("심판 상황 설명 (필요할 때)", situation),
                new JComponentRow("퀴즈 질문", question),
                new JComponentRow("1번 보기", options[0]),
                new JComponentRow("2번 보기", options[1]),
                new JComponentRow("3번 보기", options[2]),
                new JComponentRow("4번 보기", options[3]),
                new JComponentRow("정답 번호 (1~4)", answer),
                new JComponentRow("정답 해설", explanation)
        };
        for (int i = 0; i < rows.length; i++) {
            GridBagConstraints left = new GridBagConstraints();
            left.gridx=0; left.gridy=i; left.insets=new Insets(5, 0, 5, 15); left.anchor=GridBagConstraints.WEST;
            JLabel name = Theme.label(rows[i].label, 13, Theme.MUTED, false);
            form.add(name, left);
            GridBagConstraints right = new GridBagConstraints();
            right.gridx=1; right.gridy=i; right.insets=new Insets(5, 0, 5, 0);
            right.weightx=1; right.fill=GridBagConstraints.HORIZONTAL;
            form.add(rows[i].component, right);
        }
        JPanel bottom = Theme.panel();
        bottom.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = Theme.button("취소", false);
        cancel.addActionListener(e -> d.dispose());
        JButton save = Theme.button("문제 저장", true);
        save.addActionListener(e -> {
            try {
                String cat = ((String)category.getSelectedItem()).substring(0,1);
                String[] opts = {options[0].getText(), options[1].getText(), options[2].getText(), options[3].getText()};
                QuizQuestion q = new QuizQuestion(id.getText(), cat, image.getText(), guide.getText(),
                        term.getText(), description.getText(), situation.getText(), question.getText(),
                        opts, (Integer)answer.getValue(), explanation.getText());
                repo.addAndSave(q);
                reload();
                d.dispose();
            } catch (IOException | IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(d, "저장 실패: " + ex.getMessage(), "BASEUP", JOptionPane.WARNING_MESSAGE);
            }
        });
        bottom.add(cancel); bottom.add(save);
        d.setLayout(new BorderLayout());
        d.add(Theme.scroll(form), BorderLayout.CENTER);
        d.add(bottom, BorderLayout.SOUTH);
        d.setSize(new Dimension(850, 680));
        d.setLocationRelativeTo(frame);
        d.setVisible(true);
    }
    private static JTextField field(int cols) {
        JTextField f = new JTextField(cols);
        Theme.styleTextField(f);
        return f;
    }
    private static final class JComponentRow {
        final String label;
        final javax.swing.JComponent component;
        JComponentRow(String label, javax.swing.JComponent component) { this.label=label; this.component=component; }
    }
}
