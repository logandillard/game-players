package com.dillard.games;

import java.util.Scanner;

public class ConsoleIOUtilities extends IOUtilities implements IOUtilitiesInterface {
    private Scanner sc;

    public ConsoleIOUtilities() {
        sc = new Scanner(System.in);
    }

    @Override
    public String getString(String prompt) {
        print(prompt);
        return sc.nextLine();
    }

    @Override
    public void print(String aString) {
        System.out.print(aString);
    }
}