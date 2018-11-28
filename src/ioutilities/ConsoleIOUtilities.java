package ioutilities;

import java.util.Scanner;

public class ConsoleIOUtilities extends IOUtilities implements IOUtilitiesInterface {

    private Scanner sc;

    public ConsoleIOUtilities() {
        sc = new Scanner(System.in);
    }

    public String getString(String prompt) {
        print(prompt);
        return sc.nextLine();
    }


    public void print(String aString) {
        System.out.print(aString);
    }
}