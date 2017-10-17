import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Created by pieterholleman on 9/29/17.
 */
public class Packet {


    /** stores the actual packet **/
    private byte[] packet;
    private final int PACKET_MAX = 1036;
    private final int DATA_MAX = 1024;
    private CRC32 crc;
    public Packet() {

        packet = new byte[PACKET_MAX];

    }
    
    //For client recieving use
    public Packet(byte[] data){
    	packet = data;
    	crc = new CRC32();
    	crc.update(this.getDataSeq());
    }

    public Packet(byte[] segment, int seqNum) {

        crc = new CRC32();
        //if the length of the packet segment is less than 1024
        if (segment.length <= DATA_MAX) {

            //convert sequence number parameter into an array of bytes
            byte[] seq = ByteBuffer.allocate(4).putInt(seqNum).array();

            //declare byte array with space for a 4-byte int on the end
            packet = new byte[segment.length + 12];

            //copy segment into the packet, starting at 0
            System.arraycopy(segment, 0, packet, 0, segment.length);

            //copy the 4 bytes representing the sequence number into the final
            //4 bytes of the packet
            System.arraycopy(seq, 0, packet, segment.length, 4);
            crc.update(Arrays.copyOfRange(packet, 0, segment.length + 4));
            byte [] crcBytes = ByteBuffer.allocate(8).putLong(crc.getValue()).array();
            System.arraycopy(crcBytes, 0, packet, packet.length - 8, 8);

        } else {
            throw new IllegalArgumentException("byte array larger than 1024");
        }

    }

    public byte[] getBytes(){
        return packet;
    }

    public int getSeqNum(){
        byte[] seqnum = Arrays.copyOfRange(packet, packet.length - 12, packet.length - 8);

        return ByteBuffer.wrap(seqnum).getInt();
    }


    public byte[] getPacket(){
        return Arrays.copyOf(packet, packet.length);
    }

    public byte[] getData(){
        return  Arrays.copyOf(packet, packet.length - 12);
    }

    public long getCRC(){
        byte[] crcBytes = Arrays.copyOfRange(packet, packet.length - 8, packet.length);
        return ByteBuffer.wrap(crcBytes).getLong();

    }
    
    
    //For recieving CRC check
    public byte[] getDataSeq(){
    	return Arrays.copyOf(packet, packet.length-8);
    }
    
    public int getSize(){
    	return packet.length;
    }



    public static ArrayList<Packet> toPackets(byte[] file) {

        int fileSize = file.length;
        int lastPacketSize = fileSize % 1024;
        int numPackets = file.length / 1024;
        ArrayList<Packet> packets = new ArrayList<>();

        InputStream byteStream = new ByteArrayInputStream(file);
        Packet current;
        for (int i = 0; i < numPackets; ++i){

            byte[] temp = new byte[1024];

            try {
                byteStream.read(temp);
            } catch (IOException e) {
                e.printStackTrace();
            }

            current = new Packet(temp, i);
            packets.add(current);
        }

        if (lastPacketSize > 0){
            byte[] temp = new byte[lastPacketSize];
            try {
                byteStream.read(temp);
            } catch (IOException e){
                e.printStackTrace();
            }
            current = new Packet(temp, numPackets);
            packets.add(current);
        }


        return packets;
    }

    public String toString(){
        String str = new String(this.getData());
        str += " " + this.getSeqNum();


        return str;
    }

    public boolean validateCRC(){
        CRC32 crc2 = new CRC32();
        crc2.update(Arrays.copyOfRange(packet, 0,packet.length-8));
        System.out.println(crc2.getValue());
        System.out.println(getCRC());
        if (this.getCRC() != crc2.getValue())return false;
        return true;
    }
    public static void main(String[] args) {

        byte[] thing = new byte[1024];
        Packet test = new Packet(thing,5 );


        System.out.println(test.validateCRC());
        System.out.println(test.getSeqNum());
    }
}

