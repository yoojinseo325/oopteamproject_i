package baseup.ui;

import javax.swing.JPanel;

/** 그림과 야구장 도식이 공유하는 선택 표시 계약. 채점은 하지 않는다. */
abstract class QuestionVisual extends JPanel {
    abstract void selectClue(String id);
}
