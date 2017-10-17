/**
 * Created by pieterholleman on 10/14/17.
 **/


import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Test {

    public static void main(String[] args) {
        String message = "asfkjhaksfhigjshfjkdh";
        Packet stuff = new Packet(message.getBytes(), 15);
        Packet stuff2 = new Packet(stuff.getBytes());

        byte[] bytes = new byte[12];
        byte[] seq = ByteBuffer.allocate(4).putInt(100000).array();
        CRC32 crc = new CRC32();
        crc.update(seq);
        System.out.println(crc.getValue());
        byte[] crcBytes = ByteBuffer.allocate(8).putLong(crc.getValue()).array();
        System.arraycopy(seq, 0, bytes, 0, 4);
        System.arraycopy(crcBytes, 0, bytes, 4, 8);
        System.out.println(ByteBuffer.wrap(crcBytes).getLong());
        System.out.println(ByteBuffer.wrap(seq).getInt());
        //        System.out.println(stuff.getCRC());
//        System.out.println(stuff.toString());
//        System.out.println(stuff2.getCRC());
//        System.out.println(stuff.getSeqNum());
//        System.out.println(stuff2.toString());

    }

}
