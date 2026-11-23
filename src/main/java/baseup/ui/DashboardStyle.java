package baseup.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;

/** 마이페이지 디자인용 공통 시각 컴포넌트. 외부 라이브러리/폰트 파일을 요구하지 않는다. */
public final class DashboardStyle {
    public static final Color DEEP = new Color(0x073F34);
    public static final Color DEEPER = new Color(0x102F2A);
    public static final Color ACCENT = new Color(0x08684C);
    public static final Color MINT = new Color(0xDCEFE4);
    public static final Color CANVAS = new Color(0xF4F7F5);
    private DashboardStyle() { }

    public static final class Surface extends JPanel {
        private final int arc;
        public Surface(Color color, int arc) {
            this.arc = arc; setBackground(color); setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0,0,Math.max(1,getWidth()-1),Math.max(1,getHeight()-1),arc,arc);
            g.dispose(); super.paintComponent(graphics);
        }
    }

    public static final class GradientCard extends JPanel {
        private final int arc;
        public GradientCard(int arc) { this.arc=arc; setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape mask = new RoundRectangle2D.Double(0,0,Math.max(1,getWidth()-1),Math.max(1,getHeight()-1),arc,arc);
            g.setClip(mask);
            g.setPaint(new GradientPaint(0,0,new Color(0x063E33),getWidth(),getHeight(),new Color(0x092F28)));
            g.fill(mask);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(getWidth()*.7f,-20),Math.max(150,getWidth()*.9f),
                    new float[]{0,1},new Color[]{new Color(0x22,0x6E,0x58,120),new Color(0x22,0x6E,0x58,0)}));
            g.fill(mask);
            g.dispose(); super.paintComponent(graphics);
        }
    }

    public static final class Sidebar extends JPanel {
        public Sidebar() { setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setPaint(new GradientPaint(0,0,new Color(0x0A4437),getWidth(),getHeight(),new Color(0x092E28)));
            g.fillRect(0,0,getWidth(),getHeight());
            g.dispose(); super.paintComponent(graphics);
        }
    }

    public static final class NavButton extends JButton {
        private boolean selected;
        private final boolean nested;
        public NavButton(String label, String glyph, boolean nested) {
            super(label, new Glyph(glyph, nested?22:23, new Color(0xE7F6ED)));
            this.nested=nested;
            setFont(Theme.font(nested?13:15,!nested));
            setHorizontalAlignment(SwingConstants.LEFT);
            setIconTextGap(nested?9:17);
            setForeground(new Color(0xDCEAE3));
            setBorder(new EmptyBorder(nested?9:12,nested?29:17, nested?9:12,8));
            setContentAreaFilled(false); setOpaque(false); setFocusPainted(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE,nested?39:51));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        public void setSelectedPage(boolean value) { selected=value; repaint(); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(selected || getModel().isRollover()) {
                g.setColor(selected?new Color(0x246B56):new Color(0x185144));
                g.fillRoundRect(0,0,getWidth(),getHeight(),15,15);
            }
            g.dispose();
            setForeground(selected?Color.WHITE:new Color(0xD0E2D8));
            super.paintComponent(graphics);
        }
    }

    /** 플랫폼 글꼴의 별/화살표 미지원 문제를 피하기 위한 벡터 아이콘. */
    public static final class Glyph implements Icon {
        private final String type;
        private final int size;
        private final Color color;
        public Glyph(String type,int size,Color color) {this.type=type;this.size=size;this.color=color;}
        public int getIconWidth(){return size;}
        public int getIconHeight(){return size;}
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            // 마이페이지 하위 메뉴 3개는 하단 카드와 같은 아이콘 원본을 사용한다.
            // 사이드바는 배경이 어두워 별도 밝은 색상으로 렌더링한다.
            if ("flag".equals(type) || "growth".equals(type) || "notes".equals(type)) {
                Graphics2D iconGraphics=(Graphics2D)graphics.create();
                try {
                    MyPageIcons.paint(iconGraphics, "flag".equals(type) ? "path" : type,
                            x, y, size, color);
                } finally {
                    iconGraphics.dispose();
                }
                return;
            }
            Graphics2D g=(Graphics2D)graphics.create();
            g.translate(x,y); g.scale(size/32.0,size/32.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.setColor(color);
            switch(type) {
                case "home" -> {
                    Path2D p=new Path2D.Double();p.moveTo(4,15);p.lineTo(16,5);p.lineTo(28,15);g.draw(p);
                    g.drawRoundRect(7,14,18,14,2,2);g.drawRoundRect(14,19,5,9,1,1);
                }
                case "rule" -> {
                    g.drawOval(10,3,12,14);g.drawLine(12,18,12,23);g.drawLine(20,18,20,23);
                    g.drawLine(12,23,20,23);g.drawLine(14,27,18,27);
                    g.drawLine(16,0,16,1);g.drawLine(5,7,7,9);g.drawLine(25,9,27,7);
                }
                case "user" -> {g.drawOval(11,4,10,10);g.draw(new RoundRectangle2D.Double(5,19,22,10,7,7));}
                case "gear" -> {
                    g.drawOval(9,9,14,14);g.drawOval(13,13,6,6);
                    for(int i=0;i<8;i++) {double a=i*Math.PI/4;g.draw(new Line2D.Double(16+11*Math.cos(a),16+11*Math.sin(a),16+15*Math.cos(a),16+15*Math.sin(a)));}
                }
                case "star" -> {
                    Path2D p=new Path2D.Double();
                    for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;double r=i%2==0?13:6;
                        double px=16+r*Math.cos(a),py=16+r*Math.sin(a);if(i==0)p.moveTo(px,py);else p.lineTo(px,py);}
                    p.closePath();g.draw(p);
                }
                case "diamond" -> {
                    Path2D p=new Path2D.Double();p.moveTo(16,3);p.lineTo(29,16);p.lineTo(16,29);p.lineTo(3,16);p.closePath();g.draw(p);
                    for(int[] q:new int[][]{{16,5},{27,16},{16,27},{5,16}})g.fillOval(q[0]-2,q[1]-2,4,4);
                    g.drawOval(14,14,4,4);
                }
                case "flag" -> {
                    g.drawLine(8,4,8,27);g.drawLine(8,5,23,8);g.drawLine(23,8,17,15);g.drawLine(17,15,8,13);
                    g.drawLine(4,28,27,28);
                }
                case "growth" -> {
                    for(int i=0;i<3;i++)g.fillRoundRect(5+i*9,22-i*7,5,7+i*7,2,2);
                    Path2D p=new Path2D.Double();p.moveTo(5,16);p.lineTo(14,9);p.lineTo(20,12);p.lineTo(27,4);g.draw(p);
                    g.drawLine(20,4,27,4);g.drawLine(27,4,27,11);
                }
                case "notes" -> {
                    g.drawRoundRect(5,4,20,24,3,3);g.drawLine(10,11,20,11);g.drawLine(10,16,20,16);
                    g.setColor(color);g.drawLine(19,27,29,17);g.drawLine(22,30,30,21);
                }
                case "arrow" -> {g.drawLine(6,16,26,16);g.drawLine(19,9,26,16);g.drawLine(19,23,26,16);}
                default -> g.drawOval(7,7,18,18);
            }
            g.dispose();
        }
    }
}
