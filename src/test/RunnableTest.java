package test;

public class RunnableTest {

	public static void main(String[] args) throws InterruptedException {

		Tickets r1 = new Tickets();

		new Thread(r1, "001").start();
		new Thread(r1, "002").start();

	}

}

class Tickets implements Runnable {
	private int ticket = 10;

	@Override
	public void run() {
		sell();
	}

	private/* synchronized */void sell() {
		while (ticket > 0) {
			synchronized (this) {
				System.out.println("窗口:" + Thread.currentThread().getName() + ",卖了1个，剩余：" + (--ticket));
			}
		}
	}
}
