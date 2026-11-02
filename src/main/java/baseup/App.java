package baseup;

import baseup.repository.Catalog;
import baseup.service.LearningService;
import baseup.ui.MainFrame;
import baseup.ui.Theme;
import javax.swing.*;
import java.nio.channels.*;
import java.nio.file.*;

public final class App {
    private App() { }
    public static void main(String[] args) {
        Theme.install();
        Path root = Path.of(System.getProperty("baseup.home", ".")).toAbsolutePath().normalize();
        Path userDir = Path.of(System.getProperty("baseup.userdata", root.resolve("userdata").toString()));
        try {
            Files.createDirectories(userDir);
            FileChannel channel = FileChannel.open(userDir.resolve(".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                throw new IllegalStateException("이 학습 기록을 사용하는 BASEUP 창이 이미 열려 있습니다.");
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { lock.release(); channel.close(); } catch (Exception ignored) { }
            }));
            LearningService service = new LearningService(new Catalog(root.resolve("data")), userDir);
            SwingUtilities.invokeLater(() -> new MainFrame(service).setVisible(true));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "BASEUP을 시작할 수 없습니다.\n\n" + e.getMessage()
                    + "\n\n기존 파일은 자동으로 초기화하지 않습니다. README의 문제 해결을 확인해 주세요.",
                    "BASEUP 시작 안내", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
