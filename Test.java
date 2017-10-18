/**
 * Created by pieterholleman on 10/14/17.
 **/


import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Test {

    public static void main(String[] args) {
    	Window window = new Window(5,15);
    	System.out.println(window.toString());
    	window.WindowSlotCheck(10);
    	System.out.println(window.toString());
    	window.WindowSlotCheck(0);
    	window.WindowSlotCheck(1);
    	window.WindowSlotCheck(3);
    	window.WindowSlotCheck(4);
    	System.out.println(window.toString());
    	window.WindowSlotCheck(5);
      	System.out.println(window.toString());
      	window.WindowCleaner();
      	System.out.println(window.toString());
    }

}
