package baseup.ui;

import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import static baseup.ui.Theme.*;

/** 작은 창에서도 퀴즈 본문의 높이를 확보하는 공통 표현 규칙. */
final class QuizText {
    private QuizText() {
    }
    static JTextArea copy(String value,int size,Color color,int minimumRows) {
        JTextArea area=text(value,size,color);
        // 최소 창에서도 긴 상황과 질문을 잘라 버리지 않도록 본문 높이를 확보한다.
        FontMetrics metrics=area.getFontMetrics(font(size,true));
        int rows=0;
        for(String line:value.split("\n",-1))rows+=Math.max(1,(int)Math.ceil(metrics.stringWidth(line)/240.0));
        area.setRows(Math.max(minimumRows,rows));
        area.setMinimumSize(new Dimension(0,area.getRows()*metrics.getHeight()));
        return area;
    }
}
