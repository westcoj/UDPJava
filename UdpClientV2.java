import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Created by pieterholleman on 10/1/17.
 */
public class UdpClientV2 {

    DatagramChannel dataChannel;
    DatagramSocket dataSocket;
    BufferedOutputStream outputS = null;
    FileOutputStream fileSt = null;
    String IP;
    String portS;
    int port;
    final int PACKET_SIZE = 1028;

    public UdpClientV2() throws IOException{

        dataChannel = DatagramChannel.open();
        dataSocket = dataChannel.socket();
        portS = "5000";
        IP = "127.0.0.1";

        while (true) {

            if (portS.matches("[0-9]+")) {
                port = Integer.parseInt(portS);
                break;
            }
        }

    }

    public void requestFile() throws IOException {

        String fileName = "";

        while (true) {

            Packet packet = new Packet();

            //First request
            fileName = ("test.txt");//cons.readLine("Enter file request ");
            ByteBuffer buf = ByteBuffer.wrap(fileName.getBytes());
            InetSocketAddress server = new InetSocketAddress(IP, port);
            dataChannel.send(buf, server);
            System.out.println("Requesting file: " + fileName);


            //Recieve filesize
            ByteBuffer buf2 = ByteBuffer.allocate(1024);
            dataChannel.receive(buf2);
            String message = new String(buf2.array()).trim();
            long fileSize = Long.parseLong(message);
            int numPackets = (int) Math.ceil((double) fileSize / 1024);
            long bytesRead = 0;
            System.out.println(String.valueOf(numPackets));

            //Array of bytes array to read into file successfully?
            //Possible to read as we go, but would mean only accepting the next seq packet
            //in the series, rather than any in the window.
            byte[][] fileBuilder = new byte[numPackets][];

            Window window = new Window(5,numPackets);
            int finalPacketSize = (int) (fileSize % 1024) + 4;
            int seqNum;

            //Start recieving
            while(bytesRead < fileSize){

                //byte[] fileBytes = new byte[1028];
                DatagramPacket dgPacket;
                byte[] fileBytes;

                if (fileSize - bytesRead < 1024){
                    fileBytes = new byte[finalPacketSize];
                    dgPacket = new DatagramPacket(fileBytes, finalPacketSize + 4);
                }

                else {
                    fileBytes = new byte[1028];
                    dgPacket = new DatagramPacket(fileBytes,1028);
                }


                dataSocket.receive(dgPacket);
                packet = new Packet(dgPacket.getData());
                seqNum = packet.getSeqNum();

                System.out.println(packet.toString());
                System.out.println(packet.getSeqNum());
                //Is packet recieved in window?
                if(window.WindowApprove(seqNum)){
                    fileBuilder[seqNum] = packet.getBytes();
                    bytesRead += 1024;
                    window.WindowSlotCheck(seqNum);
                }

                //Last packet handler
                else if(seqNum==-2){
                    fileBuilder[numPackets-1] = packet.getBytes();
                    bytesRead = fileSize;
                }

                //Check slots for recieved items
                window.WindowCleaner();
                byte[] sendAckBytes = Integer.toString(seqNum).getBytes();
                int sendAckBytesLen = sendAckBytes.length;
                System.out.println(String.valueOf(sendAckBytesLen));
                DatagramPacket ackSend = new DatagramPacket(
                        sendAckBytes,
                        sendAckBytes.length,
                        InetAddress.getByName("127.0.0.1"),
                        5000);
                //System.out.println(ackSend.getAddress());
                dataSocket.send(ackSend);


            }//Stop recieving file

            File file = new File("out.txt");
            fileSt = new FileOutputStream(file);
            outputS = new BufferedOutputStream(fileSt);
            int k = 0;
            while(k!=fileBuilder.length){
                outputS.write(fileBuilder[k]);
                k++;
            }

            outputS.close();
            fileSt.close();

            break;


            //System.out.println(message2);

        }

    }

    public static void main(String[] args) {

        try {
            UdpClientV2 test = new UdpClientV2();
            test.requestFile();

        } catch (Exception e){
            e.printStackTrace();
        }

    }

}
