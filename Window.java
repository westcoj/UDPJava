import java.util.ArrayList;

/**************************************************************************************
 * The following class sets up an ArrayList 5 slots wide and cycles through a
 * given number of slots until all are accounted for. The last slot to be used
 * is interchanged with -2
 * 
 * @author Cody West|Peter Holleman
 * @version Project 2 UDP
 * @date 10/20/2017
 *************************************************************************************/
public class Window {

	/** Size of the windo, number of slots */
	int size;

	/** Next slot to be added to array */
	int slotNumber;

	/** Array containng acceptable slots */
	ArrayList<Integer> window;

	/** Array containing all slots to be used */
	int[] slots;

	/*************************************************************************
	 * Constructer that sets up window given a size and number of slots. Both
	 * the current window and future slots are set up.
	 * 
	 * @param size
	 *            number of slots in the window
	 * @param totalSlots
	 *            total number of slots to be cycled
	 ************************************************************************/
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

	/*************************************************************************
	 * Sets up and returns a string representation of the current window
	 * @return A string representation of the current window
	 ************************************************************************/
	public String toString() {
		String str = "";
		for (int i = 0; i < slots.length; ++i) {
			str += Integer.toString(slots[i]);
		}
		return new String(window.toString()); // + "\n" + str);
	}

	/*************************************************************************
	 * Cycles through the current window and removes all used slots (that have
	 * been turned into -1) until reaching a non -1 slot
	 ************************************************************************/
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

	/*************************************************************************
	 * Compares a given number to the window to see if the number is one of
	 * the slots currently in the window.
	 * @param seq A slot number given to be compared to the current window
	 * @return true if the number given is in the window
	 ************************************************************************/
	public boolean WindowApprove(int seq) {
		if (seq == -2 || seq >= slots.length - 1) {
			for (int i = 0; i < window.size()-1; i++) {
				int x = window.get(i);
				if (x != -1) {
					return false;
				}
			}

			int x = window.get(window.size()-1);

			return (x == slots.length - 1);

		}

		else {
			if (window.contains(seq))
				return true;
			else
				return false;

		}

	}

	/*************************************************************************
	 * Takes a given number and if it is approved removes it from the
	 * window. -2 is ignored as it is interchanged with the last slot
	 * @param seq A slot number given to be compared to the current window and
	 * removed if approved
	 ************************************************************************/
	public void WindowSlotCheck(int seq) {
		if (seq == -2) {

		}

		else if (this.WindowApprove(seq)) {
			window.set(window.indexOf(seq), -1);
		}

	}

	/*************************************************************************
	 * Builds an array consisting of slots still in the window
	 * @return an array of all slots in the current window
	 ************************************************************************/
	public int[] getSlotsRemaining() {
		int[] slotsR = new int[size];
		for (int i = 0; i < window.size(); i++) {
			slotsR[i] = window.get(i);
		}

		return slotsR;
	}

}
