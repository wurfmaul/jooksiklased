class Calc {
	final static int a = 12;
	final int b;
	String c = "42";
	long d = 8;
	
	public Calc() {
		b = 13;
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		Calc c = new Calc();
		int tmp = c.calc(2f); // 42
		boolean cmp = c.cmp(tmp); // true
	}
	
	private boolean cmp(int val) {
		boolean retValue = val == Integer.parseInt(c);
		return retValue;
	}

	public int calc(float val) {
		double tmp = a + b; // 25d
		tmp *= val; // 50d
		tmp -= d; // 42d
		return (int) tmp; // 42
	}
}