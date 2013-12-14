public class Overload {
	private int i = -1;

	public Overload(int i) {
		this.i = i;
	}

	public static void main(String[] args) {
		add(1, 2);
		add(3, 4l);
		add(5, 6d);
		Overload ol1 = new Overload(7);
		Overload ol2 = new Overload(8);
		Overload ol3 = new Overload(9);
		ol1.mul(10);
		ol2.mul(11);
		ol3.mul(12);
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

	public int mul(int a) {
		i = i * a;
		return i;
	}

}
