import java.util.ArrayList;

public class Window {
	int size;
	int slotNumber;
	ArrayList<Integer> window;
	int[] slots;

	// set up window
	public Window(int size, int totalSlots) {
		this.size = size;
		slotNumber = 0;
		// slotNumber = size;
		window = new ArrayList<Integer>(size);
		slots = new int[totalSlots];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = i;
			if (i < 5) {
				window.add(i, i);
				slotNumber++;
			}
		}
	}

	public String toString() {
		String str = "";
		for (int i = 0; i < slots.length; ++i) {
			str += Integer.toString(slots[i]);
		}
		return new String(window.toString()); // + "\n" + str);
	}

	// removes all -1 slots from window and adds new ones
	public void WindowCleaner() {
		if (!window.isEmpty()) {
			for (int i = 0; i < window.size(); i++) {
				if (window.get(0) != -1 || slotNumber > slots.length) {
					break;

				}

				// runs into non -1 value (UnAcknowledged slot)
				else {
					if (slotNumber < slots.length) {
						window.remove(0);
						window.add(slots[slotNumber]);
						slotNumber++;
					}
				}
			}
		}
	}

	// for sedning and recieving, if window has the slot open its true (for
	// accepting packets)
	public boolean WindowApprove(int seq) {
		if (seq == -2 || seq >= slots.length - 1) {
			for (int i = 0; i < 4; i++) {
				int x = window.get(i);
				if (x != -1) {
					return false;
				}
			}

			int x = window.get(4);

			return (x == slots.length - 1);

		}

		else {
			if (window.contains(seq))
				return true;
			else
				return false;

		}

	}

	// for checking off acknowledgtements
	public void WindowSlotCheck(int seq) {
		if (seq == -2) {

		}

		else if (this.WindowApprove(seq)) {
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
