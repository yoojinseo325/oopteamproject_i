package baseup.ui;

import baseup.model.UserProfile;
import baseup.service.AuthService;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.Arrays;
import java.util.function.Consumer;
import static baseup.ui.Theme.*;

/** Figma 시안의 오른쪽 로그인 폼. 인증 검증은 AuthService가 담당한다. */
public final class LoginPanel extends JPanel {
    private final AuthService auth;
    private final Consumer<UserProfile> onSuccess;
    private final JTextField id = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel status = label(" ", 12, RED, false);

    public LoginPanel(AuthService auth, Consumer<UserProfile> onSuccess, Runnable showRegister) {
        super(new GridBagLayout()); this.auth=auth;this.onSuccess=onSuccess;
        setBackground(new Color(0xFAFBF9));
        JPanel form = new JPanel();form.setOpaque(false);form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(360, 415));
        addTo(form,label("반가워요!", 30, INK, true));gap(form,8);
        addTo(form,label("로그인하고 나만의 야구 공부를 이어가세요.", 13, MUTED, false));gap(form,32);
        addTo(form,label("아이디", 12, INK, true));gap(form,8);
        field(id); addTo(form,id);gap(form,20);
        addTo(form,label("비밀번호", 12, INK, true));gap(form,8);
        field(password);password.setEchoChar('●');
        JPanel passRow=new JPanel(new BorderLayout());passRow.setOpaque(false);passRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        passRow.add(password,BorderLayout.CENTER);
        JToggleButton reveal=new JToggleButton("표시"); reveal.setFont(font(11,false));reveal.setFocusable(false);
        reveal.addActionListener(e->{password.setEchoChar(reveal.isSelected()?'\0':'●');reveal.setText(reveal.isSelected()?"숨김":"표시");});
        passRow.add(reveal,BorderLayout.EAST);addTo(form,passRow);gap(form,10);
        addTo(form,status);gap(form,18);
        JButton login=button("로그인",true,this::submit);login.setAlignmentX(LEFT_ALIGNMENT);
        login.setMaximumSize(new Dimension(Integer.MAX_VALUE,46));addTo(form,login);gap(form,20);
        JPanel signup=new JPanel(new FlowLayout(FlowLayout.CENTER,3,0));signup.setOpaque(false);
        signup.add(label("야구 공부가 처음이라면?",12,MUTED,false));
        JButton open=new JButton("회원가입");link(open);open.addActionListener(e->showRegister.run());signup.add(open);
        addTo(form,signup);
        GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(22,20,22,20);add(form,c);
        id.addActionListener(e->password.requestFocusInWindow());
        password.addActionListener(e->submit());
    }

    public void setLoginId(String loginId) { id.setText(loginId);password.setText("");status.setText("회원가입이 완료되었습니다. 로그인해 주세요."); }

    private void submit() {
        char[] secret=password.getPassword();
        try {
            UserProfile profile=auth.authenticate(id.getText(),secret);
            status.setText(" ");password.setText("");onSuccess.accept(profile);
        } catch (Exception e) {
            status.setText(e.getMessage());password.setText("");password.requestFocusInWindow();
        } finally {Arrays.fill(secret,'\0');}
    }

    static void field(JTextField text) {
        text.setFont(font(14,false));text.setForeground(INK);text.setBackground(WHITE);
        text.setBorder(new CompoundBorder(new LineBorder(LINE,1,true),new EmptyBorder(10,13,10,13)));
        text.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));text.setPreferredSize(new Dimension(325,44));
    }
    static void addTo(JPanel form, JComponent item){item.setAlignmentX(LEFT_ALIGNMENT);form.add(item);}
    static void link(JButton button){button.setFont(font(12,true));button.setForeground(GREEN);button.setBorder(null);button.setContentAreaFilled(false);button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));}
}
