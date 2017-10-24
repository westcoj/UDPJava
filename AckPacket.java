import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**************************************************************************************
 * The following class takes in bytes and creates a packet to be sent across a
 * datagram. A crc and seq number are added into the bytes that are sent. This can 
 * then be easily deconstrcuted by the reciever into appropriate parts. 
 * 
 * @author Cody West|Peter Holleman
 * @version Project 2 UDP
 * @date 10/20/2017
 *************************************************************************************/
public class AckPacket {

	/** byte array storage of acknowledgement */
    byte[] packet;
    
    /** Max size of an acknowledgement packet */
    public final int ACKPACKET_SIZE = 12;
    
    /** CRC built into packet from sequence number bytes */
    CRC32 crc;

    /*************************************************************************
     * Constructer that builds a packet using the given sequence
     * number, adds on a CRC and stores all bytes into one array
     * @param seqNum adjoined sequence number of data segment
     ************************************************************************/
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

    /*************************************************************************
     * Constructer that builds a packet using the given data and stores all
     * bytes into one array
     * @param data data submitted to be used by packet
     ************************************************************************/
    public AckPacket(byte[] data) {
        packet = new byte[ACKPACKET_SIZE];

        System.arraycopy(data, 0, packet, 0, 12);
        crc = new CRC32();


    }

    /*************************************************************************
     * Method for getting the sequence number of the packet
     * @return the sequence number of the packet
     ************************************************************************/
    public int getSeqNum(){
        return ByteBuffer.wrap(Arrays.copyOf(packet, 4)).getInt();
    }

    /*************************************************************************
     * Method for getting the CRC of the packet
     * @return the CRC of the packet
     ************************************************************************/
    public long getCRC(){
        return ByteBuffer.wrap(Arrays.copyOfRange(packet, 4, 12)).getLong();
    }

    /*************************************************************************
     * Method for getting the entirity of the packet's storage
     * @return the entirity of the packet's storage
     ************************************************************************/
    public byte[] getBytes(){
        return packet;
    }

    /*************************************************************************
     * Method for getting the bytes of the CRC of the packet
     * @return the bytes of the CRC of the packet
     ************************************************************************/
    public byte[] getCrcBytes(){
        return Arrays.copyOfRange(packet, 4, 12);
    }

    /*************************************************************************
     * Method for getting the bytes of the sequence number of the packet
     * @return the bytes of the sequence number of the packet
     ************************************************************************/
    public byte[] getSeqBytes(){
        return Arrays.copyOfRange(packet, 0, 4);
    }

    /*************************************************************************
     * Method for validating included CRC with a second packet built CRC.
     * @return true if CRC's match and no corruption is found
     ************************************************************************/
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
