package baseup.ui;

import baseup.model.UserProfile;
import baseup.service.AuthService;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import static baseup.ui.Theme.*;
import static baseup.ui.LoginPanel.*;

/** 새 계정을 만들거나 기존 학습 기록에 로그인 정보를 연결하는 화면. */
public final class RegisterPanel extends JPanel {
    private final AuthService auth;
    private final Runnable onBack;
    private final java.util.function.Consumer<String> onDone;
    private final JTextField id=new JTextField(), nickname=new JTextField();
    private final JPasswordField password=new JPasswordField(), confirm=new JPasswordField();
    private final JComboBox<UserProfile> oldProfiles = new JComboBox<>();
    private final JRadioButton fresh=new JRadioButton("새 학습 프로필"), existing=new JRadioButton("기존 학습 기록 연결");
    private final JLabel message=label(" ",12,RED,false);

    public RegisterPanel(AuthService auth, Runnable onBack, java.util.function.Consumer<String> onDone) {
        super(new GridBagLayout()); this.auth=auth;this.onBack=onBack;this.onDone=onDone;
        setBackground(new Color(0xFAFBF9));
        JPanel f=new JPanel();f.setOpaque(false);f.setLayout(new BoxLayout(f,BoxLayout.Y_AXIS));
        f.setPreferredSize(new Dimension(370,600));
        addTo(f,label("회원가입",28,INK,true));gap(f,5);
        addTo(f,label("기존 기록은 그대로 유지할 수 있어요.",12,MUTED,false));gap(f,20);
        JPanel mode=new JPanel(new GridLayout(1,2,8,0));mode.setOpaque(false);
        for(JRadioButton r:new JRadioButton[]{fresh,existing}){r.setOpaque(false);r.setFont(font(12,true));mode.add(r);}
        ButtonGroup group=new ButtonGroup();group.add(fresh);group.add(existing);fresh.setSelected(true);
        mode.setMaximumSize(new Dimension(Integer.MAX_VALUE,35));addTo(f,mode);gap(f,14);
        addTo(f,label("아이디 (영문·숫자·_ 4~20자)",12,INK,true));gap(f,6);field(id);addTo(f,id);gap(f,12);
        addTo(f,label("닉네임 (새 프로필 생성 시)",12,INK,true));gap(f,6);field(nickname);addTo(f,nickname);gap(f,8);
        addTo(f,label("연결할 기존 학습 기록",12,INK,true));gap(f,6);
        oldProfiles.setFont(font(13,false));oldProfiles.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));addTo(f,oldProfiles);gap(f,12);
        addTo(f,label("비밀번호 (8자 이상)",12,INK,true));gap(f,6);field(password);addTo(f,password);gap(f,12);
        addTo(f,label("비밀번호 확인",12,INK,true));gap(f,6);field(confirm);addTo(f,confirm);gap(f,9);
        addTo(f,message);gap(f,8);
        JButton register=button("계정 만들기",true,this::submit);register.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));addTo(f,register);gap(f,10);
        JButton back=new JButton("← 로그인으로 돌아가기");link(back);back.addActionListener(e->onBack.run());addTo(f,back);
        fresh.addActionListener(e->changeMode());existing.addActionListener(e->changeMode());
        GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(14,20,14,20);add(f,c);
        reloadProfiles();
    }
    public void reloadProfiles() {
        List<UserProfile> available=auth.unlinkedProfiles();
        oldProfiles.setModel(new DefaultComboBoxModel<>(available.toArray(UserProfile[]::new)));
        // 기본 사용자가 이미 있다면 기록 연결을 명시적으로 선택하도록 한다.
        changeMode();
    }
    private void changeMode() {
        boolean isNew=fresh.isSelected();
        nickname.setEnabled(isNew);oldProfiles.setEnabled(!isNew && oldProfiles.getItemCount()>0);
        if(!isNew && oldProfiles.getItemCount()==0) message.setText("연결 가능한 기존 기록이 없습니다. 새 프로필을 선택해 주세요.");
        else message.setText(" ");
    }
    private void submit() {
        char[] secret=password.getPassword(), verification=confirm.getPassword();
        try {
            if(!Arrays.equals(secret,verification))throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
            if(fresh.isSelected()) auth.registerNew(id.getText(),secret,nickname.getText());
            else {
                UserProfile old=(UserProfile)oldProfiles.getSelectedItem();
                if(old==null)throw new IllegalArgumentException("연결할 학습 기록이 없습니다.");
                auth.linkExisting(id.getText(),secret,old.id());
            }
            String login=id.getText().strip();clear();onDone.accept(login);
        }catch(Exception e) {message.setText(e.getMessage());password.setText("");confirm.setText("");}
        finally {Arrays.fill(secret,'\0');Arrays.fill(verification,'\0');}
    }
    private void clear() {id.setText("");nickname.setText("");password.setText("");confirm.setText("");message.setText(" ");}
}
