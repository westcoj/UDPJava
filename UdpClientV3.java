import java.io.BufferedOutputStream;
import java.io.Console;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.zip.CRC32;

/**
 * Created by pieterholleman on 10/23/17.
 */
public class UdpClientV3 {

    private DatagramChannel dataChannel;
    private DatagramSocket dataSocket;

    private BufferedOutputStream outputS = null;
    private FileOutputStream fileSt = null;

    private String ip;
    private String portS;
    private int port;

    private final int PACKET_SIZE = 1036;
    long fileSize;
    //CRC32 ackCRC;
    CRC32 crcCheck;
    //CRC32 crcServer;
    ArrayList<Integer> recieved;
    int numPackets;
    SocketAddress resendAdr;
    Window window;

    byte[][] fileBuilder;


    //public UdpClientV3(){
        //connect("2000", "127.0.0.1");
        //requestFile("dankmeme.jpg");
//    }

    public boolean connect(String portString, String ip){

        while (true) {
            try {

                //parsing/validating port input
                //String portS = cons.readLine("Enter port number: ");
                //System.out.print("Enter a port number: ");
                String portS = "2000";
                //String portS = scanner.nextLine().trim();
                //int port = Integer.parseInt(portS);
                if (portS.matches("[0-9]+")) {
                    port = Integer.parseInt(portS);
                } else if (portS.equals("exit")) {
                    System.exit(0);
                } else {
                    continue;
                }

                dataSocket.bind(new InetSocketAddress(port));
                System.out.println("Connected");
                return true;

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }


    }

    //public boolean requestFile(String str){

       //Packet fileNamePacket = new Packet(str.getBytes(), -3);
        //DatagramPacket p = new DatagramPacket(str.getBytes(), str.l);
       //DatagramPacket fileNameDatagram = new DatagramPacket(fileNamePacket.getData(), fileNamePacket.getData().length);
//       while(true) {
//           try {
//               dataSocket.send(fileNameDatagram);
//               System.out.println("Requesting file" + str);
//               InetSocketAddress server = new InetSocketAddress(ip, port);
//               byte[] fileSizeBytes = new byte[1036];
//               DatagramPacket fileSizePacket = new DatagramPacket();
//               SocketAddress resendAddr = dataChannel.receive(fileSizeBuffer);
//               Packet fileSizePacket = new Packet(fileSizeBuffer.array());
//
//               if (!fileSizePacket.validateCRC()) {
//                   System.out.println("Packet corrupted");
//                   continue;
//               }
//               else {
//                   fileSize = Long.parseLong(
//                           new String(fileNamePacket.getBytes()).trim()
//                   );
//                   System.out.println("file size: " + fileSize);
//                   return true;
//               }
//
//           } catch (Exception e) {
//               e.printStackTrace();
//           }
//       }
//    }

    public static void main(String[] args) {

    }
}
