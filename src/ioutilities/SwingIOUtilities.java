package ioutilities;

import javax.swing.*;

public class SwingIOUtilities extends IOUtilities implements IOUtilitiesInterface {

    public String getString(String prompt) {
      String result = JOptionPane.showInputDialog(prompt);
      return (result == null) ? "" : result;
    }

    public void print(String aString) {
    JOptionPane.showMessageDialog(null, aString);
    }

    public int getMenuOption(String prompt, String[] choices) {
        return JOptionPane.showOptionDialog(
                        null,
                        prompt,
                        "Choice menu",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        choices,
                        choices[0]);
                    }

}
