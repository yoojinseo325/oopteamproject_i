package baseup.ui;

import baseup.model.UserProfile;
import baseup.service.AuthService;
import java.awt.*;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.*;

/** 기존 홈 화면을 표시하기 전에 나오는 로그인 창. */
public final class LoginFrame extends JFrame {
    private final CardLayout screens=new CardLayout();
    private final JPanel right=new JPanel(screens);
    private final LoginPanel login;
    private final RegisterPanel register;

    public LoginFrame(AuthService auth, Path projectRoot, Consumer<UserProfile> onLogin) {
        super("BASEUP · 야구 규칙 학습 · 로그인");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        login=new LoginPanel(auth, profile->{dispose();onLogin.accept(profile);}, ()->open("register"));
        register=new RegisterPanel(auth,()->open("login"), id->{login.setLoginId(id);open("login");});
        right.add(login,"login");right.add(register,"register");
        GridBagConstraints leftConstraints=new GridBagConstraints();leftConstraints.gridx=0;leftConstraints.gridy=0;
        leftConstraints.weightx=.46;leftConstraints.weighty=1;leftConstraints.fill=GridBagConstraints.BOTH;
        add(new BrandPanel(projectRoot),leftConstraints);
        GridBagConstraints rightConstraints=new GridBagConstraints();rightConstraints.gridx=1;rightConstraints.gridy=0;
        rightConstraints.weightx=.54;rightConstraints.weighty=1;rightConstraints.fill=GridBagConstraints.BOTH;
        add(right,rightConstraints);
        setMinimumSize(new Dimension(1000,690));
        setSize(1150,750);setLocationRelativeTo(null);open("login");
    }
    private void open(String screen){if(screen.equals("register"))register.reloadProfiles();screens.show(right,screen);}
}
