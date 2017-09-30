import java.util.ArrayList;
import java.util.Iterator;


public class Window {
	int size;
	int slotNumber;
	ArrayList<Integer> window;
	int[] slots;
	Iterator iterator;
	
	// set up window
	public Window(int size, int totalSlots){
		this.size = size;
		slotNumber = size;
		window = new ArrayList<Integer>(size);
		slots = new int[totalSlots];
		for(int i=0;i<slots.length;i++){
			slots[i] = i;
			if(i<5){
				window.add(i, i);
			}
		}
	}
	
	//removes all -1 slots from window and adds new ones
	public void WindowCleaner(){
		for(int i = 0;i<size;i++){
			if(window.get(i)==-1){
				window.remove(i);
				window.add(slots[slotNumber++]);
			}
			
			//runs into non -1 value (UnAck slot)
			else{
				return;
			}
		}
	}
	
	
	// for sedning and recieving
	public boolean WindowApprove(int seq){
			if(window.contains(seq))
				return true;
			else
				return false;
		
	}
	
	
	//for checking off acknowledgtements
	public void WindowSlotCheck(int seq){
		if(this.WindowApprove(seq)){
			window.set(window.indexOf(seq),-1);
		}
		
	}
	
	
	

}
