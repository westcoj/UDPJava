import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by pieterholleman on 9/29/17.
 */
public class Packet {


    /** stores the actual packet **/
    private byte[] packet;
    private final int PACKET_MAX = 1028;
    private final int DATA_MAX = 1024;

    public Packet() {

        packet = new byte[PACKET_MAX];

    }
    
    //For client recieving use
    public Packet(byte[] data){
    	packet = data;
    }

    public Packet(byte[] segment, int seqNum) {

        //if the length of the packet segment is less than 1024
        if (segment.length <= DATA_MAX) {

            //convert sequence number parameter into an array of bytes
            byte[] seq = Integer.toString(seqNum).getBytes();

            //declare byte array with space for a 4-byte int on the end
            packet = new byte[segment.length + 4];

            //copy segment into the packet, starting at 0
            System.arraycopy(segment, 0, packet, 0, segment.length);

            //copy the 4 bytes representing the sequence number into the final
            //4 bytes of the packet
            System.arraycopy(seq, 0, packet, segment.length, seq.length);

        } else {
            throw new IllegalArgumentException("byte array larger than 1024");
        }

    }

    public byte[] getBytes(){
        return packet;
    }

    public int getSeqNum(){
        String str = new String (Arrays.copyOfRange(packet, packet.length - 4, packet.length-1)).trim();
        return Integer.parseInt(str);
    }

    public byte[] getPacket(){
        return Arrays.copyOf(packet, packet.length);
    }

    public byte[] getData(){
        return  Arrays.copyOf(packet, packet.length - 4);
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

    public static void main(String[] args) {

        byte[] thing = new byte[1024];
        Packet test = new Packet(thing, (byte)1);
        byte[] thing2 = test.getBytes();
        System.out.println((char)thing2[0]);
        System.out.println(test.getSeqNum());
    }
}

