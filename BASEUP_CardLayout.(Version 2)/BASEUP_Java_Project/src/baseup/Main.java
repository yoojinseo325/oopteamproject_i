package baseup;

import baseup.data.QuestionRepository;
import baseup.data.ResultRepository;
import baseup.ui.MainFrame;
import baseup.ui.Theme;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 프로그램 실행 시작점: VS Code에서 Main.java의 Run을 눌러 실행할 수도 있다. */
public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Theme.install();
                QuestionRepository questions = new QuestionRepository(Path.of("data", "questions.txt"));
                ResultRepository results = new ResultRepository(Path.of("data", "results.txt"));
                MainFrame frame = new MainFrame(questions, results);
                frame.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "프로그램을 시작하지 못했습니다.\n" + ex.getMessage()
                        + "\n\nBASEUP_Java_Project 폴더에서 실행했는지 확인하세요.",
                        "BASEUP 시작 오류", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
    }
}
