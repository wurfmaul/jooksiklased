class Test {
	
	public static void main(String[] args) {
		int i = 5;
		int j = i * 2;
		hello(j + 1);
	}
	
	public static boolean hello(int l) {
		boolean res = l == "Hello World".length(); 
		return res;
	}
	
}