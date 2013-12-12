public class Recursion {

	public static void main(String[] args) {
		fac(7);
	}

	private static int fac(int i) {
		int retValue = -1;
		if (i == 1)
			retValue = i;
		else
			retValue = i * fac(i - 1);
		return retValue;
	}

}
