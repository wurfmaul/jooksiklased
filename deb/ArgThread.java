public class ArgThread extends Thread {
	private final long max;
	public long sum;

	public ArgThread(long max) {
		this.max = max;
		this.sum = 0;
	}

	@Override
	public void run() {
		for (int i = 1; i <= max; i++) {
			sum += i;
		}
		try {
			sleep(10000);
		} catch (InterruptedException e) {
		}
	}

	public static void main(String[] args) {
			assert args.length == 2;

			final int n = Integer.parseInt(args[0]);
			final long max = Long.parseLong(args[1]);
			ArgThread[] threads = new ArgThread[n];
			for (int i = 0; i < n; i++) {
				threads[i] = new ArgThread(max);
				threads[i].start();
			}
			int sum = 0;
			for (int i = 0; i < n; i++) {
				try {
					threads[i].interrupt();
					threads[i].join();
				sum += threads[i].sum;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			assert sum == 10836;
	}
}