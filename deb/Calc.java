class Calc {
	final static int a = 12;
	final int b;
	String c = "42";
	long d = 8;
	
	public Calc() {
		b = 13;
	}
	
	public static void main(String[] args) {
		Calc c = new Calc();
		int tmp = c.calc(2f);
		boolean cmp = c.cmp(tmp);
		System.out.println("Test passed? " + cmp);
	}
	
	private boolean cmp(int val) {
		boolean retValue = val == Integer.parseInt(c);
		return retValue;
	}

	public int calc(float val) {
		double tmp = a + b;
		tmp *= val;
		tmp -= d;
		return (int) tmp;
	}
}