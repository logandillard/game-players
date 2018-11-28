package games;
import ioutilities.*;

public class SigmoidTest {
	public static void main (String[] args) {
		IOUtilitiesInterface iou = new ConsoleIOUtilities();
		
		iou.println("Welcome to Sigmoid solving program.\n");
		boolean cont = true;
		
		String menu = "What do you want to do:\n" +
						"1. calculate sigmoid\n" +
						"2. calculate sigmoid derivative\n" +
						"3. calculate derivative of a sigmoid output\n" +
						"4. quit\n";
		
		int choice;
		double x;
		
		while(cont) {
			choice = iou.getInt(menu, 1, 4);
			
			switch(choice) {
			case 1: x = iou.getDouble("enter x: ");	
				iou.println("\nsigmoid output is: " + sigmoid(x));
				break;
			case 2: x = iou.getDouble("enter x: ");	
				iou.println("\nsigmoid derivative output is: " + sigmoidDeriv(x));
				break;
			case 3:  x = iou.getDouble("enter sigmoid output: ");	
			iou.println("\nsigmoid derivative output is: " + (x * (1-x)));
			break;
			case 4:
				cont = false;
			}
		}
		
		iou.println("Exiting...");
	}
	
	private static double sigmoid(double x) {
		return 1.0 / (1.0 + Math.pow(Math.E, -x));
	}
	
	private static double sigmoidDeriv(double x) {
		double s = sigmoid(x);
		return s * (1.0 - s);
	}
}
