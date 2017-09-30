import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
/**
 * Created by pieterholleman on 9/26/17.
 */
public class UdpClient {


    private final int WINDOW_SIZE = 5;
    private final int PACKET_MAX = 1024;

    public static void main(String[] args) {

        File file = new File("C:\\Server\\test.txt");
        try {
            byte fileBytes[] = Files.readAllBytes(file.toPath());
            ArrayList<Packet> test = Packet.toPackets(fileBytes);
            int packets = (int) Math.ceil((double)file.length()/1024);
            System.out.println(String.valueOf(packets));
            for (Packet p : test){
                System.out.println(p.getSeqNum());
                //String text = new String(p.getData());
                //System.out.println(text);
                System.out.println(p.getPacket().length);
                System.out.println(p.getData().length);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }



//        System.out.println();
//        System.out.print("Enter an ip address: ");
//        Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
//        String ip = scanner.next();
//        System.out.print("Enter a port number: ");
//        int port = scanner.nextInt();
//        try {
//
//            DatagramChannel sc = DatagramChannel.open();
//            System.out.print("Enter a message to send: ");
//            String str = scanner.next();
//            if (str.equals("/exit")){
//                sc.disconnect();
//                return;
//            }
//            ByteBuffer buff = ByteBuffer.wrap(str.getBytes());
//            sc.send(buff, new InetSocketAddress(ip, port));
//            buff = ByteBuffer.allocate(4096);
//            sc.receive(buff);
//            String echo = new String(buff.array()).trim();
//            System.out.println(echo);
//        } catch (IOException e){
//            System.out.println("Got an exception");
//
//        }





    }

    public Packet getPacket(){
        return new Packet();
    }




}
