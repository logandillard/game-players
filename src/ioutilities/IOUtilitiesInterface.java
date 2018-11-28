package ioutilities;
import java.util.*;

// Interface for utilities that manage user input
// and output.

public interface IOUtilitiesInterface {
    String getString(String prompt);
    int getMenuOption(String header, List options);

    int getInt(String prompt, int low, int high);
    int getInt(String prompt);

    double getDouble(String prompt, double low, double high);
    double getDouble(String prompt);

	// In the first case, pattern is a parse pattern
	// as defined in SimpleDateFormat.  The second
	// case uses the default parse pattern, which
	// which is as of this writing "M/d/yy h:mm a"
    Date getDate(String prompt, String pattern);
    Date getDate(String prompt);

    boolean getYesNo(String prompt);
    void print(String text);
    void println(String text);
}
