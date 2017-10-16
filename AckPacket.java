import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Created by pieterholleman on 10/15/17.
 */
public class AckPacket {

    byte[] packet;
    public final int ACKPACKET_SIZE = 12;
    CRC32 crc;

    public AckPacket(int seqNum){
        packet = new byte[ACKPACKET_SIZE];

        byte[] seq = ByteBuffer.allocate(4).putInt(seqNum).array();
        crc = new CRC32();
        crc.update(seq);
        //System.out.println(crc.getValue());

        byte[] crcBytes = ByteBuffer.allocate(8).putLong(crc.getValue()).array();
        System.arraycopy(seq, 0, packet, 0, 4);
        System.arraycopy(crcBytes, 0, packet, 4, 8);

        //System.out.println(ByteBuffer.wrap(crcBytes).getLong());

        //System.out.println(ByteBuffer.wrap(seq).getInt());

    }

    public AckPacket(byte[] data) {
        packet = new byte[ACKPACKET_SIZE];

        System.arraycopy(data, 0, packet, 0, 12);
        crc = new CRC32();


    }

    public int getSeqNum(){
        return ByteBuffer.wrap(Arrays.copyOf(packet, 4)).getInt();
    }

    public long getCRC(){
        return ByteBuffer.wrap(Arrays.copyOfRange(packet, 4, 12)).getLong();
    }

    public byte[] getBytes(){
        return packet;
    }

    public byte[] getCrcBytes(){
        return Arrays.copyOfRange(packet, 4, 12);
    }

    public byte[] getSeqBytes(){
        return Arrays.copyOfRange(packet, 0, 4);
    }

    public boolean validateCRC(){
        CRC32 crc2 = new CRC32();
        crc2.update(Arrays.copyOfRange(packet, 0,packet.length-8));
        System.out.println(crc2.getValue());
        System.out.println(getCRC());
        if (crc.getValue() != crc2.getValue())return false;
        return true;
    }

    public static void main(String[] args) {
        AckPacket test = new AckPacket(25309521);

        AckPacket test2 = new AckPacket(test.getBytes());

        System.out.println(test.getSeqNum());
        System.out.println(test.getCRC());


        System.out.println(test2.getSeqNum());
        System.out.println(test2.getCRC());
    }
}
