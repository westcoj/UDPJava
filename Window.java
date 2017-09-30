import java.util.ArrayList;

public class Window {
	int size;
	int slotNumber;
	ArrayList<Integer> window;
	int[] slots;

	// set up window
	public Window(int size, int totalSlots) {
		this.size = size;
		slotNumber = size;
		window = new ArrayList<Integer>(size);
		slots = new int[totalSlots];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = i;
			if (i < 5) {
				window.add(i, i);
			}
		}
	}

	// removes all -1 slots from window and adds new ones
	public void WindowCleaner() {
		if (!window.isEmpty()) {
			for (int i = 0; i < window.size(); i++) {
				if (window.get(i) == -1) {
					window.remove(i);
					window.add(slots[slotNumber++]);
				}

				// runs into non -1 value (UnAcknowledged slot)
				else {
					return;
				}
			}
		}
	}

	// for sedning and recieving, if window has the slot open its true (for
	// accepting packets)
	public boolean WindowApprove(int seq) {
		if (window.contains(seq))
			return true;
		else
			return false;

	}

	// for checking off acknowledgtements
	public void WindowSlotCheck(int seq) {
		if (this.WindowApprove(seq)) {
			window.set(window.indexOf(seq), -1);
		}

	}

	public int[] getSlotsRemaining() {
		int[] slotsR = new int[size];
		for (int i = 0; i < window.size(); i++) {
			slotsR[i] = window.get(i);
		}

		return slotsR;
	}

}
