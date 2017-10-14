/**
 * Created by pieterholleman on 10/14/17.
 **/


import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Test {

    public static void main(String[] args) {
        String message = "asfkjhaksfhigjshfjkdh";
        Packet stuff = new Packet(message.getBytes(), 15);
        Packet stuff2 = new Packet(message.getBytes(), 10);
        System.out.println(stuff.getCRC());
        System.out.println(stuff.toString());
        System.out.println(stuff2.getCRC());
        System.out.println(stuff.getSeqNum());
        System.out.println(stuff2.toString());

    }
}
