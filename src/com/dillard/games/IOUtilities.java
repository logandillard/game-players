package com.dillard.games;

import java.util.*;
import java.text.*;

public abstract class IOUtilities {
	abstract void print(String s);
	abstract String getString(String s);

	public void println(String s) {
		print(s + "\n");
	}

	  public int getInt(String prompt, int low, int high) {
	        if (high < low) {
	            throw new IllegalArgumentException("Bad parameters");
	        }

			try {
		        String userInput = getString(prompt);
				int candidate = Integer.parseInt(userInput);
				if (candidate >= low && candidate <= high) {
					return candidate;
				}
			} catch (NumberFormatException e) {
			}
			println("Input must be a number between " + low + " and " + high);
			return getInt(prompt, low, high);
    }

       public int getInt(String prompt) {
	        return getInt(prompt, Integer.MIN_VALUE, Integer.MAX_VALUE);
	    }

    public double getDouble(String prompt, double low, double high) {
	    if (high < low) {
            throw new IllegalArgumentException("Bad parameters");
        }

        try {
        	String userInput = getString(prompt);
       		double candidate = Double.parseDouble(userInput);
       		if (candidate >= low && candidate <= high) {
				return candidate;
			}
		} catch (NumberFormatException e) {
		}
		println("Input must be a number between " + low + " and " + high);
		return getDouble(prompt, low, high);
	}

  	public double getDouble(String prompt) {
		return getDouble(prompt, - Double.MAX_VALUE, Double.MAX_VALUE);
	}

	   public boolean getYesNo(String prompt) {
	        String answer = getString(prompt).trim().toUpperCase();
	        if (answer.equals("Y") || answer.equals("YES")
	               || answer.equals("N") || answer.equals("NO")) {
	          return answer.charAt(0) == 'Y';
	         }
	  		println("Please answer yes or no (or y or n)");
	  		return getYesNo(prompt);
    }

       public Date getDate(String prompt, String pattern) {
	        while (true) {
	            try {
					SimpleDateFormat sdf = (pattern == null) ?
								new SimpleDateFormat() :
								new SimpleDateFormat(pattern);
	             	return sdf.parse(getString(prompt));
	            } catch (ParseException p) {
	            }
	            println("Please enter a date in the format " + pattern);
	        }
	    }

	    	public Date getDate(String prompt) {
				return getDate(prompt, null);
	}

	  public int getMenuOption(String prompt, List choices) {
	        int numChoices = choices.size();
	        String realPrompt = prompt + "\n";
	        for (int i = 0; i < numChoices; i++) realPrompt += "  " + (i+1) + ". " + choices.get(i) + "\n";
	        return getInt(realPrompt + "==> ", 1, numChoices) - 1;
	    }

}