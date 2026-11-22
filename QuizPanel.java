package baseup.ui;

import baseup.model.*;
import baseup.service.LearningService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static baseup.ui.Theme.*;

/**
 * 단계별 문제 입력 화면.
 *
 * 화면 표시와 사용자 입력만 담당하며 채점, 출제, 학습 기록 저장은
 * QuizSession과 LearningService에 위임한다.
 */
public final class QuizPanel extends JPanel {
    private final LearningService service;
    private final QuizSession session;
    private final Runnable complete;
    private final Runnable exit;
    private final Consumer<String> message;
    private final Consumer<Exception> error;

    private boolean before = true;
    private FieldPanel field;
    private JLabel caption;
    private ActionButton submit;
    private ActionButton primaryAction;

    private final Map<String, ActionButton> clueButtons = new LinkedHashMap<>();
    private final Map<Integer, ActionButton> choiceButtons = new LinkedHashMap<>();

    public QuizPanel(
            LearningService service,
            QuizSession session,
            Runnable complete,
            Runnable exit,
            Consumer<String> message,
            Consumer<Exception> error
    ) {
        this.service = service;
        this.session = session;
        this.complete = complete;
        this.exit = exit;
        this.message = message;
        this.error = error;

        setLayout(new BorderLayout());
        setOpaque(false);
        render();
    }

    /** 현재 QuizSession 상태를 기준으로 화면 전체를 다시 구성한다. */
    private void render() {
        removeAll();
        clueButtons.clear();
        choiceButtons.clear();
        field = null;
        submit = null;
        primaryAction = null;

        Question q = session.current();
        boolean feedback = session.stage() == QuizSession.Stage.FEEDBACK;

        JPanel page = column();
        page.setBorder(new EmptyBorder(22, 26, 24, 26));

        addHeader(page, q, feedback);
        addProgress(page, q, feedback);
        addContext(page, q);

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);

        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 0.56;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.anchor = GridBagConstraints.NORTH;
        left.insets = new Insets(0, 0, 0, 16);

        int contentHeight = q.hasImage() && q.requiresClue() ? 600 : 530;
        JPanel visual = q.hasImage() ? imageCard(q) : sceneCard(q);
        visual.setMinimumSize(new Dimension(0, 0));
        visual.setPreferredSize(new Dimension(0, contentHeight));
        columns.add(visual, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.weightx = 0.44;
        right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        right.anchor = GridBagConstraints.NORTH;
        right.insets = new Insets(0, 0, 0, 0);

        JPanel question = questionCard(q);
        contentHeight = Math.max(contentHeight, question.getPreferredSize().height + 16);
        visual.setPreferredSize(new Dimension(0, contentHeight));
        question.setMinimumSize(new Dimension(0, 0));
        question.setPreferredSize(new Dimension(0, contentHeight));
        columns.add(question, right);

        // 실제 창 너비에 맞춰 두 열이 함께 줄어들도록 한다.
        columns.setPreferredSize(new Dimension(950, contentHeight));
        columns.setMinimumSize(new Dimension(0, contentHeight));
        Theme.add(page, columns);

        add(scroll(page), BorderLayout.CENTER);
        addActionBar(feedback);

        revalidate();
        repaint();
    }

    private void addHeader(JPanel page, Question q, boolean feedback) {
        JPanel heading = new JPanel(new BorderLayout(16, 0));
        heading.setOpaque(false);

        JPanel title = column();
        String index = String.format("%02d", session.index() + 1);
        String total = String.format("%02d", session.size());
        String prefix = session.stepNumber() > 0
                ? "STEP " + session.stepNumber() + "   /   "
                : "CHALLENGE   /   ";

        Theme.add(title, label(prefix + index + " / " + total + "   ·   " + q.level(), 11, GREEN, true));
        gap(title, 7);
        Theme.add(title, label(q.title(), 26, INK, true));
        heading.add(title, BorderLayout.CENTER);

        ActionButton leave = button("미션 나가기", false, exit);
        leave.setToolTipText("현재 미션을 나갑니다.");
        heading.add(leave, BorderLayout.EAST);

        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        Theme.add(page, heading);
        gap(page, 16);
    }

    private void addProgress(JPanel page, Question q, boolean feedback) {
        JProgressBar progress = new JProgressBar(0, Math.max(1, session.size()));
        progress.setValue(Math.min(session.size(), session.index() + (feedback ? 1 : 0)));
        progress.setForeground(GREEN);
        progress.setBackground(LINE);
        progress.setBorderPainted(false);
        progress.setStringPainted(false);
        progress.setPreferredSize(new Dimension(900, 6));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        Theme.add(page, progress);
        gap(page, 13);

        String[] steps = q.requiresClue()
                ? new String[]{"01  장면 보고 판정", "02  단서 찾기", "03  해설 확인"}
                : new String[]{"01  그림 보고 선택", "02  해설 확인"};

        int current = feedback
                ? steps.length - 1
                : session.stage() == QuizSession.Stage.CLUE ? 1 : 0;

        JPanel stages = new JPanel(new GridLayout(1, steps.length, 10, 0));
        stages.setOpaque(false);
        for (int i = 0; i < steps.length; i++) {
            JLabel step = label(steps[i], 12, i == current ? GREEN : MUTED, i == current);
            step.setBorder(new EmptyBorder(6, 0, 6, 0));
            stages.add(step);
        }
        Theme.add(page, stages);
        gap(page, 13);
    }

    private void addContext(JPanel page, Question q) {
        if (!q.hasImage()) {
            Matchup matchup = service.catalog().matchup(q.id());
            JPanel teams = new JPanel(new GridLayout(1, 2, 10, 0));
            teams.setOpaque(false);
            teams.add(teamBadge(matchup.offense(), "공격 · R"));
            teams.add(teamBadge(matchup.defense(), "수비 · D"));
            teams.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            Theme.add(page, teams);
            gap(page, 6);
            Theme.add(page, label("KBO 선수와 함께하는 가상 학습 상황", 11, MUTED, false));
        } else {
            Theme.add(page, label(
                    "PICTURE PLAY   /   그림 속 장면에서 판단의 기준을 찾아요",
                    11, MUTED, false
            ));
        }
        gap(page, 12);
    }

    private void addActionBar(boolean feedback) {
        JPanel actions = new JPanel(new BorderLayout(16, 0));
        actions.setBackground(WHITE);
        actions.setBorder(new EmptyBorder(11, 26, 11, 26));

        actions.add(
                label(
                        feedback ? "해설을 확인하고 다음 장면으로 가요." : "선택한 뒤 아래 버튼을 눌러주세요.",
                        12, MUTED, false
                ),
                BorderLayout.CENTER
        );

        if (primaryAction != null) {
            primaryAction.setPreferredSize(new Dimension(330, 48));
            primaryAction.setMinimumSize(new Dimension(190, 46));
            actions.add(primaryAction, BorderLayout.EAST);
        }
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel imageCard(Question q) {
        RoundedPanel card = card();
        card.setBorder(new EmptyBorder(17, 17, 17, 17));
        card.setLayout(new BorderLayout(0, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("PICTURE VIEW   /   장면을 관찰하세요", 11, GREEN, true));

        ActionButton zoom = button("크게 보기", false, () -> openImageDialog(q));
        zoom.setFont(font(11, false));
        top.add(zoom, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        try {
            ImageView image = new ImageView(service.catalog().imagePath(q), q.title());
            image.setPreferredSize(new Dimension(600, 390));
            card.add(image, BorderLayout.CENTER);
        } catch (Exception e) {
            JPanel failed = new JPanel(new GridBagLayout());
            failed.setOpaque(false);
            failed.add(label("그림을 불러오지 못했습니다.", 14, RED, true));
            card.add(failed, BorderLayout.CENTER);
            message.accept("문제 그림을 표시하지 못했습니다.");
        }

        JPanel bottom = column();
        Theme.add(bottom, copy(q.imageGuide(), 12, MUTED, 1));
        gap(bottom, 11);

        if (session.stage() == QuizSession.Stage.CLUE) {
            Theme.add(bottom, label("그림과 설명을 보고, 결정적인 조건을 골라요.", 12, GREEN, true));
            gap(bottom, 9);

            for (Question.Clue clue : q.clueOptions()) {
                ActionButton option = button(
                        "<html>" + html(clue.text()) + "</html>",
                        false,
                        () -> chooseClue(clue.id())
                );
                option.setName("clue-" + clue.id());
                option.setFont(font(12, true));
                option.setHorizontalAlignment(SwingConstants.LEFT);
                option.setAlignmentX(Component.LEFT_ALIGNMENT);
                option.setPreferredSize(new Dimension(300, 50));
                option.setMinimumSize(new Dimension(0, 46));
                option.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                option.mark(clue.id().equals(session.clue()));
                option.getAccessibleContext().setAccessibleName(clue.text());
                clueButtons.put(clue.id(), option);
                Theme.add(bottom, option);
                gap(bottom, 7);
            }
        } else if (session.stage() == QuizSession.Stage.FEEDBACK) {
            Theme.add(bottom, copy(
                    "발견한 규칙  ·  " + service.catalog().rule(q.ruleId()).title(),
                    14, GREEN, 1
            ));
            gap(bottom, 6);
            Theme.add(bottom, copy(service.catalog().rule(q.ruleId()).summary(), 13, INK, 2));
        } else {
            Theme.add(bottom, copy(
                    "정답과 규칙 카드는 답을 제출하면 열려요. 막히면 코치의 힌트를 확인하세요.",
                    12, MUTED, 2
            ));
        }

        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void openImageDialog(Question q) {
        try {
            Window owner = SwingUtilities.getWindowAncestor(this);
            JDialog dialog = new JDialog(owner, "장면 크게 보기", Dialog.ModalityType.APPLICATION_MODAL);
            ImageView image = new ImageView(service.catalog().imagePath(q), q.title());
            dialog.setLayout(new BorderLayout());
            dialog.add(image, BorderLayout.CENTER);

            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            int width = Math.max(480, Math.min(1100, screen.width - 80));
            int height = Math.max(360, Math.min(800, screen.height - 100));
            dialog.setSize(width, height);
            dialog.setLocationRelativeTo(this);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        } catch (Exception e) {
            error.accept(e);
        }
    }

    private JComponent teamBadge(Team team, String role) {
        Color color = new Color(team.color());
        RoundedPanel badge = new RoundedPanel(color, 12);
        badge.setBorder(new EmptyBorder(10, 14, 10, 14));
        badge.setLayout(new BorderLayout(8, 0));
        badge.add(label(team.name(), 15, contrast(color), true), BorderLayout.WEST);
        badge.add(label(role, 12, contrast(color), true), BorderLayout.EAST);
        return badge;
    }

    private JPanel sceneCard(Question q) {
        RoundedPanel card = card();
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        card.setLayout(new BorderLayout(0, 11));

        JPanel top = column();
        Theme.add(top, label("FIELD VIEW   /   장면을 관찰하세요", 11, GREEN, true));
        gap(top, 12);

        JPanel toggle = new JPanel(new GridLayout(1, 2, 8, 0));
        toggle.setOpaque(false);
        ActionButton first = button("장면 1 · 상황 시작", false, () -> setScene(true));
        ActionButton second = button("장면 2 · 판정 순간", false, () -> setScene(false));
        first.setName("before");
        second.setName("after");
        toggle.add(first);
        toggle.add(second);
        Theme.add(top, toggle);
        card.add(top, BorderLayout.NORTH);

        if (session.stage() == QuizSession.Stage.CLUE) {
            before = true;
        }

        Scene shownScene = service.catalog().scene(before ? q.beforeScene() : q.afterScene());
        field = new FieldPanel(shownScene);
        field.setPreferredSize(new Dimension(500, 350));
        first.mark(before);
        second.mark(!before);
        field.putClientProperty("first", first);
        field.putClientProperty("second", second);

        if (session.stage() == QuizSession.Stage.CLUE) {
            field.onSelect(this::chooseClue);
        }

        if (session.stage() == QuizSession.Stage.FEEDBACK) {
            before = true;
            field.scene(service.catalog().scene(q.beforeScene()));
            field.selected(q.clueTarget());
            first.mark(true);
            second.mark(false);
        }

        JPanel middle = new JPanel(new BorderLayout(0, 9));
        middle.setOpaque(false);
        middle.add(field, BorderLayout.CENTER);

        caption = label(
                "<html>" + html(service.catalog().scene(before ? q.beforeScene() : q.afterScene()).caption()) + "</html>",
                12, MUTED, false
        );
        caption.setPreferredSize(new Dimension(300, 44));
        middle.add(caption, BorderLayout.SOUTH);
        card.add(middle, BorderLayout.CENTER);

        JPanel bottom = column();
        Theme.add(bottom, label(
                "R 공격 · D 수비  /  선수 색상 = 소속 구단  /  ! 상황 정보",
                11, MUTED, false
        ));
        gap(bottom, 12);

        if (session.stage() == QuizSession.Stage.CLUE) {
            Theme.add(bottom, label("그림 속 대상을 클릭하거나 아래에서 선택하세요.", 12, GREEN, true));
            gap(bottom, 8);

            JPanel options = new JPanel(new GridLayout(0, 2, 7, 7));
            options.setOpaque(false);

            for (Scene.Token token : service.catalog().scene(q.beforeScene()).tokens()) {
                String optionText = token.player() == null
                        ? html(token.label())
                        : html(token.player().displayName()) + "<br>" + html(token.label());

                ActionButton option = button(
                        "<html><center>" + optionText + "</center></html>",
                        false,
                        () -> chooseClue(token.id())
                );
                option.setFont(font(11, true));
                option.setBorder(new EmptyBorder(8, 5, 8, 5));
                option.setName("clue-" + token.id());
                option.getAccessibleContext().setAccessibleName(token.displayLabel());

                if (token.player() != null) {
                    option.teamColor(new Color(token.player().team().color()));
                }

                option.mark(session.clue().equals(token.id()));
                options.add(option);
                clueButtons.put(token.id(), option);
            }

            Theme.add(bottom, options);
            field.selected(session.clue());
        } else {
            RoundedPanel note = new RoundedPanel(PALE, 12);
            note.setLayout(new BorderLayout());
            note.setBorder(new EmptyBorder(12, 13, 12, 13));

            String noteText = session.stage() == QuizSession.Stage.FEEDBACK
                    ? "노란 테두리의 단서가 판정을 바꿉니다. 장면을 바꾸며 조건을 비교해 보세요."
                    : "장면 1과 장면 2를 번갈아 보세요. 그림은 상황을 단순화한 도식이며, 공의 보유·접촉 시점은 오른쪽 설명을 함께 확인해요.";

            JTextArea noteCopy = text(noteText, 12, MUTED);
            noteCopy.setLineWrap(true);
            noteCopy.setWrapStyleWord(true);
            noteCopy.setEditable(false);
            noteCopy.setFocusable(false);
            note.add(noteCopy, BorderLayout.CENTER);
            Theme.add(bottom, note);
        }

        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel questionCard(Question q) {
        RoundedPanel card = card();
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setLayout(new BorderLayout(0, 11));

        JPanel body = column();
        Theme.add(body, label(
                service.catalog().rule(q.ruleId()).topic() + "  ·  " + q.level(),
                11, GREEN, true
        ));
        gap(body, 10);

        if (session.stage() != QuizSession.Stage.FEEDBACK) {
            Theme.add(body, copy(
                    q.situation(), 14, INK,
                    q.hasImage() && !q.requiresClue() ? 2 : 4
            ));
            gap(body, 13);
        }

        boolean judge = session.stage() == QuizSession.Stage.JUDGE;

        if (session.stage() == QuizSession.Stage.FEEDBACK) {
            addFeedback(body, q);
            primaryAction = button(
                    session.index() + 1 == session.size() ? "미션 결과 보기   →" : "다음 상황으로   →",
                    true,
                    this::advance
            );
        } else {
            JTextArea prompt = copy(judge ? q.prompt() : q.cluePrompt(), 18, INK, 2);
            prompt.setFont(font(18, true));
            Theme.add(body, prompt);
            gap(body, 10);

            if (judge) {
                addChoices(body, q);
            } else {
                addChosenAnswer(body, q);
            }

            gap(body, 15);
            addHint(body, q);
            submit = createSubmitButton(q, judge);
            primaryAction = submit;
        }

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void addFeedback(JPanel body, Question q) {
        Attempt attempt = session.results().get(session.results().size() - 1);

        Theme.add(body, label(
                attempt.understood() ? "좋은 판정이에요!" : "결정적인 조건을 확인해요.",
                21, attempt.understood() ? GREEN : RED, true
        ));
        gap(body, 10);

        String status = "답 " + (attempt.answerCorrect() ? "정답" : "오답")
                + (q.requiresClue() ? "   /   단서 " + (attempt.clueCorrect() ? "정답" : "오답") : "")
                + (attempt.hintUsed() ? "   /   힌트 사용" : "");
        Theme.add(body, label(status, 12, MUTED, true));
        gap(body, 12);

        Theme.add(body, copy(q.explanation(), 14, INK, 3));
        gap(body, 9);

        if (!q.misconception().isBlank()) {
            Theme.add(body, copy(q.misconception(), 13, MUTED, 2));
            gap(body, 9);
        }

        Theme.add(body, copy("정답: " + q.choices().get(q.answer()), 13, GREEN, 1));
        gap(body, 7);

        if (q.requiresClue()) {
            String clueText = service.catalog().clues(q).stream()
                    .filter(clue -> clue.id().equals(q.clueTarget()))
                    .findFirst()
                    .orElseThrow()
                    .text();
            Theme.add(body, copy("핵심 단서: " + clueText, 13, GREEN, 1));
        }
    }

    private void addChoices(JPanel body, Question q) {
        for (int i = 0; i < q.choices().size(); i++) {
            final int selected = i;
            ActionButton option = button(
                    "<html>" + (i + 1) + " &nbsp; " + html(q.choices().get(i)) + "</html>",
                    false,
                    () -> {
                        session.choose(selected);
                        choiceButtons.forEach((id, button) -> button.mark(id == selected));
                        if (submit != null) {
                            submit.setEnabled(true);
                        }
                    }
            );

            option.setName("choice-" + i);
            option.setHorizontalAlignment(SwingConstants.LEFT);
            option.setPreferredSize(new Dimension(300, 56));
            option.setMinimumSize(new Dimension(0, 50));
            option.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
            option.mark(session.choice() == i);
            option.getAccessibleContext().setAccessibleName(
                    "선택지 " + (i + 1) + ": " + q.choices().get(i)
            );

            choiceButtons.put(i, option);
            Theme.add(body, option);
            gap(body, 8);
        }
    }

    private void addChosenAnswer(JPanel body, Question q) {
        RoundedPanel chosen = new RoundedPanel(PALE, 12);
        chosen.setLayout(new BorderLayout());
        chosen.setBorder(new EmptyBorder(11, 12, 11, 12));
        chosen.add(copy("내 판정: " + q.choices().get(session.choice()), 13, GREEN, 1));
        chosen.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        Theme.add(body, chosen);
        gap(body, 11);

        String picked;
        if (session.clue().isBlank()) {
            picked = "왼쪽 장면에서 단서를 선택하세요.";
        } else {
            picked = "선택한 단서: " + service.catalog().clues(q).stream()
                    .filter(clue -> clue.id().equals(session.clue()))
                    .findFirst()
                    .orElseThrow()
                    .text();
        }

        Theme.add(body, copy(picked, 13, MUTED, 2));
        gap(body, 10);
        Theme.add(body, text("판정과 단서를 함께 제출하면 해설이 열립니다.", 12, MUTED));
    }

    private void addHint(JPanel body, Question q) {
        ActionButton hint = button(
                session.hintUsed() ? "힌트 다시 보기" : "막혔다면, 힌트 보기",
                false,
                () -> {
                    session.useHint();
                    JOptionPane.showMessageDialog(
                            this,
                            q.hint(),
                            "코치의 힌트",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    message.accept("힌트를 사용했습니다. 이 판단은 다음 도전에서 다시 연습해요.");
                    render();
                }
        );
        hint.setFont(font(12, false));
        Theme.add(body, hint);
    }

    private ActionButton createSubmitButton(Question q, boolean judge) {
        ActionButton action = button(
                judge
                        ? (q.requiresClue() ? "판정 선택 완료 · 단서 찾기   →" : "답 제출하기   →")
                        : "판정과 단서 제출하기   →",
                true,
                () -> {
                    try {
                        if (judge && q.requiresClue()) {
                            session.beginClue();
                            before = true;
                            render();
                        } else {
                            service.submit(session);
                            message.accept("학습 기록을 저장했습니다.");
                            render();
                        }
                    } catch (Exception e) {
                        error.accept(e);
                    }
                }
        );

        action.setEnabled(judge ? session.choice() >= 0 : !session.clue().isBlank());
        return action;
    }

    private void advance() {
        try {
            service.advance(session);
            before = true;
            if (session.stage() == QuizSession.Stage.COMPLETE) {
                complete.run();
            } else {
                render();
            }
        } catch (Exception e) {
            error.accept(e);
        }
    }

    /**
     * 줄바꿈 가능한 읽기 전용 텍스트 영역.
     * 줄 수를 고정값으로 제한하지 않아 긴 문제 설명도 스크롤로 읽을 수 있다.
     */
    private JTextArea copy(String value, int size, Color color, int minimumRows) {
        JTextArea area = text(value == null ? "" : value, size, color);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);

        FontMetrics metrics = area.getFontMetrics(area.getFont());
        int estimatedRows = 0;
        for (String line : (value == null ? "" : value).split("\n", -1)) {
            int lineWidth = metrics.stringWidth(line);
            estimatedRows += Math.max(1, (int) Math.ceil(lineWidth / 250.0));
        }

        area.setRows(Math.max(minimumRows, Math.min(estimatedRows, 14)));
        area.setMinimumSize(new Dimension(0, Math.max(1, minimumRows) * metrics.getHeight()));
        return area;
    }

    private void setScene(boolean showBefore) {
        if (session.stage() == QuizSession.Stage.CLUE && !showBefore) {
            message.accept("단서는 장면 1에서 찾아주세요. 판정 순간은 단서 제출 후 다시 볼 수 있어요.");
            return;
        }

        before = showBefore;
        Question q = session.current();
        Scene scene = service.catalog().scene(before ? q.beforeScene() : q.afterScene());

        field.scene(scene);
        if (before) {
            field.selected(
                    session.stage() == QuizSession.Stage.FEEDBACK
                            ? q.clueTarget()
                            : session.clue()
            );
        }

        ((ActionButton) field.getClientProperty("first")).mark(before);
        ((ActionButton) field.getClientProperty("second")).mark(!before);
        caption.setText("<html>" + html(scene.caption()) + "</html>");
    }

    private void chooseClue(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        session.chooseClue(id);
        render();
    }
}
