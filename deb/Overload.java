
public class Overload {

	public static void main(String[] args) {
		add(1, 2);
		add(3, 4l);
		add(5, 6d);
	}
	
	public static int add(int a, int b) {
		return a + b;
	}
	
	public static long add(int a, long b) {
		return a + b;
	}
	
	public static double add(int a, double b) {
		return a + b;
	}
	
}
