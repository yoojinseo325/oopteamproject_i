package baseup.ui;

import baseup.model.Scene;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.function.Consumer;

/** 장면을 그리는 역할만 수행한다. 정답 여부는 알지 못한다. */
public final class FieldPanel extends JPanel {
    private Scene scene;
    private String selected = "";
    private Consumer<String> onSelect;
    public FieldPanel(Scene scene) {
        this.scene = scene; setOpaque(false); setPreferredSize(new Dimension(640, 400));
        setMinimumSize(new Dimension(320, 200));
        getAccessibleContext().setAccessibleName("야구장 장면. 단서는 아래 선택 버튼으로도 선택할 수 있습니다.");
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (onSelect == null) return;
                double scale = Math.min(getWidth()/800.0, getHeight()/500.0);
                double x = (e.getX() - (getWidth()-800*scale)/2)/scale;
                double y = (e.getY() - (getHeight()-500*scale)/2)/scale;
                for (Scene.Token t : FieldPanel.this.scene.tokens()) {
                    int labelX = labelX(t);
                    int half = Math.max(35, t.label().length()*10);
                    boolean body = Math.abs(x-t.x())<26 && Math.abs(y-t.y())<26;
                    boolean caption = Math.abs(x-labelX)<half && y>t.y()+24 && y<t.y()+55;
                    boolean name = t.player()!=null && Math.abs(x-labelX)<t.player().displayName().length()*10
                            && y>t.y()-57 && y<t.y()-28;
                    if (body || caption || name) { onSelect.accept(t.id()); break; }
                }
            }
        });
    }
    public void scene(Scene value) { scene = value; selected = ""; repaint(); }
    public void selected(String id) { selected = id; repaint(); }
    public void onSelect(Consumer<String> callback) { onSelect = callback; setCursor(Cursor.getPredefinedCursor(callback == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR)); }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D outer = (Graphics2D) graphics.create();
        outer.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        outer.setColor(new Color(0xE9F0E8)); outer.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
        double scale = Math.min(getWidth()/800.0, getHeight()/500.0);
        outer.translate((getWidth()-800*scale)/2, (getHeight()-500*scale)/2); outer.scale(scale, scale);
        Graphics2D g = outer;
        g.setColor(new Color(0xB4D1AA));
        g.fill(new Arc2D.Double(15, -25, 770, 900, 42, 96, Arc2D.PIE));
        Shape clip = g.getClip();
        g.clip(new Arc2D.Double(15, -25, 770, 900, 42, 96, Arc2D.PIE));
        for (int i=0; i<10; i++) { g.setColor(new Color(i%2==0 ? 0xABCAA1 : 0xB4D1AA)); g.fillRect(0,i*52,800,52); }
        g.setClip(clip);
        Polygon diamond = new Polygon(new int[]{400,610,400,190},new int[]{425,265,105,265},4);
        g.setColor(new Color(0xD6B992)); g.fill(diamond);
        Polygon inner = new Polygon(new int[]{400,551,400,249},new int[]{378,265,152,265},4);
        g.setColor(new Color(0x93B98A)); g.fill(inner);
        g.setColor(new Color(0xF9F9E9)); g.setStroke(new BasicStroke(3));
        g.drawLine(400,425,65,170); g.drawLine(400,425,735,170);
        g.setColor(new Color(0xD6B992)); g.fillOval(369,235,62,48);
        g.setColor(Color.WHITE); g.fillRoundRect(387,255,26,8,3,3);
        base(g,600,265,"1루",27,4); base(g,400,112,"2루",24,-5); base(g,200,265,"3루",-54,4);
        g.setColor(Color.WHITE);
        g.fillPolygon(new int[]{388,412,412,400,388},new int[]{414,414,427,436,427},5);
        g.setFont(Theme.font(15,true)); g.setColor(new Color(0x4B6F55)); g.drawString("HOME",376,464);
        g.setColor(new Color(0x72956A)); g.setFont(Theme.font(12,true)); g.drawString("BASEUP  ·  FIELD NOTES",34,468);
        for (Scene.Token t : scene.tokens()) token(g,t);
        g.dispose();
    }
    private void base(Graphics2D g, int x, int y, String text, int dx, int dy) {
        g.setColor(Color.WHITE); g.fillPolygon(new int[]{x,x+10,x,x-10},new int[]{y-10,y,y+10,y},4);
        g.setFont(Theme.font(16,true)); g.setColor(new Color(0x54724D)); g.drawString(text,x+dx,y+dy);
    }
    private void token(Graphics2D g, Scene.Token token) {
        int x=token.x(),y=token.y(),labelX=labelX(token); boolean chosen=selected.equals(token.id());
        if (chosen) { g.setColor(new Color(0xF1BA54)); g.setStroke(new BasicStroke(4)); g.drawOval(x-28,y-28,56,56); }
        Color color = switch(token.kind()) {
            case "runner" -> new Color(0xE6A052); case "fielder" -> new Color(0x286859);
            case "ball" -> Color.WHITE; default -> new Color(0x3A5450);
        };
        if (token.player()!=null) color = new Color(token.player().team().color());
        g.setColor(new Color(0,0,0,25)); g.fillOval(x-20,y-16,40,40);
        g.setColor(color); g.fillOval(x-20,y-21,40,40);
        if (token.player()!=null) {
            g.setColor(new Color(token.player().team().accent()));g.setStroke(new BasicStroke(3));g.drawOval(x-20,y-21,40,40);
        }
        g.setColor(Theme.contrast(color));
        g.setFont(Theme.font(16,true));
        String symbol=switch(token.kind()) {case "runner" -> "R"; case "fielder" -> "D"; case "ball" -> "●"; default -> "!";};
        g.drawString(symbol,x-g.getFontMetrics().stringWidth(symbol)/2,y+5);
        if (token.player()!=null) {
            g.setFont(Theme.font(19,true));String name=token.player().displayName();
            int nameWidth=g.getFontMetrics().stringWidth(name)+18;
            g.setColor(color);
            if(labelX!=x){g.setStroke(new BasicStroke(2));g.drawLine(labelX,y-28,x,y-21);}
            g.fillRoundRect(labelX-nameWidth/2,y-57,nameWidth,29,8,8);
            g.setColor(Theme.contrast(color));g.drawString(name,labelX-nameWidth/2+9,y-36);
        }
        g.setFont(Theme.font(19,true)); int width=g.getFontMetrics().stringWidth(token.label())+20;
        g.setColor(chosen ? new Color(0xFFF1CC) : new Color(255,255,255,240));
        g.fillRoundRect(labelX-width/2,y+24,width,31,9,9);
        g.setColor(Theme.INK); g.drawString(token.label(),labelX-width/2+10,y+46);
    }
    /** 태그처럼 가까이 있는 선수는 위치를 옮기지 않고 이름표만 양옆으로 분리한다. */
    private int labelX(Scene.Token token) {
        if(token.player()==null)return token.x();
        for(Scene.Token other:scene.tokens()) {
            if(other.player()!=null && !other.id().equals(token.id())
                    && Math.abs(other.x()-token.x())<130 && Math.abs(other.y()-token.y())<100)
                return token.x()+(token.x()<other.x()?-100:100);
        }
        return token.x();
    }
}
