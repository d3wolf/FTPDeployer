package test;

public class MultiThreadTest implements Runnable {

	private int index;

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(Math.abs((50 - index)) * 100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName() + "------" + index);
	}

	public static void main(String[] args) {

		for (int i = 0; i < 100; i++) {
			if (i % 5 == 0) {
				MultiThreadTest run = new MultiThreadTest();
				run.setIndex(i);
				Thread thread = new Thread(run);
				thread.start();
			}
		}
	}

}
