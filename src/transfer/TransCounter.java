package transfer;

public class TransCounter {

	private int count;
	
	public TransCounter(){
		this.count = 0;
	}
	
	public int getCount(){
		return this.count;
	}
	
	synchronized public void countUp(){
		count ++;
	}
}
