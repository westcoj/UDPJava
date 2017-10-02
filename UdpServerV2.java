import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;


/**
 * Created by pieterholleman on 10/1/17.
 */
public class UdpServerV2 {

    private DatagramChannel dataChannel;
    private DatagramSocket dataSocket;
    private Window window;


    public UdpServerV2() throws IOException {

        dataChannel = DatagramChannel.open();
        dataSocket = dataChannel.socket();
        dataSocket.setSoTimeout(100);


    }

    public boolean connect() {

        int port = 0;
        String portStr = "5000";


        try {
            //while (true) {

            //parsing/validating port input
            //Console cons = System.console;
            if (portStr.matches("[0-9]+")) {
                port = Integer.parseInt(portStr);
            } else if (portStr.equals("exit")) {
                System.exit(0);
            } else {
                //continue;
            }

            dataSocket.bind(new InetSocketAddress(port));


            //}

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public void awaitRequest() {

        File file;
        FileInputStream fileSt;
        BufferedInputStream inputS;
        byte[] bytes;
        ByteBuffer messageBuffer;
        ByteBuffer fileBuffer;
        SocketAddress resendAdr;
        Packet packet;
        DatagramPacket dgPacket;
        byte[] clientBytes;
        DatagramPacket clientPacket;


        try {

            while (true) {

                System.out.println("Awaiting file request");
                int fileSize = 0;

                // buffer to get client's command
                messageBuffer = ByteBuffer.allocate(256);

                // get address of client
                resendAdr = dataChannel.receive(messageBuffer);

                // Decode message
                String request = new String(messageBuffer.array()).trim();
                System.out.println("Client Request: " + request);


                file = new File(request);
                fileSt = new FileInputStream(file);
                inputS = new BufferedInputStream(fileSt);



                byte[] test = Files.readAllBytes(file.toPath());
                int numPackets = (int) Math.ceil((double) file.length() / 1024);

                System.out.println("File size: " + test.length);

                window = new Window(5, numPackets);
                int seqNumber = 0;
                int clientAck = 0;
                byte[][] fileBuilder = new byte[numPackets][1024];

                fileBuffer = ByteBuffer.wrap(String.valueOf(file.length()).getBytes());
                dataChannel.send(fileBuffer, resendAdr);

                // File seding loop, -2 is last packet acknowledgement.
                while (clientAck != -2) {
                    if (window.WindowApprove(seqNumber)) {
                        bytes = new byte[1024];

                        //Reads 1024 bytes into array, should mark position when this occurs multiple times
                        inputS.read(bytes, 0, 1024);
                        fileBuilder[seqNumber] = bytes;

                        // Last packet to send, sends -2 as sequence number to notify client
                        if ((seqNumber * 1024) >= file.length()) {
                            packet = new Packet(bytes, -2);
                        } else {
                            packet = new Packet(bytes, seqNumber);
                        }

                        dgPacket = new DatagramPacket(packet.getBytes(), 1028, resendAdr);
                        dataSocket.send(dgPacket);
                        seqNumber++;
                    }

                    // Get packet to resend, window isn't moving
                    else {
                        int sendAgain[] = window.getSlotsRemaining();
                        for (int i = 0; i < sendAgain.length; i++) {
                            if (sendAgain[i] != -1) {
                                bytes = fileBuilder[sendAgain[i]];

                                // Last packet to send
                                if ((seqNumber * 1024) >= file.length()) {
                                    packet = new Packet(bytes, -2);
                                }

                                // send the packet
                                else {
                                    packet = new Packet(bytes, sendAgain[i]);
                                }

                                dgPacket = new DatagramPacket(packet.getBytes(), 1028, resendAdr);
                                dataSocket.send(dgPacket);
                                break;
                            }
                        }
                    }

                    //Recieve packet from client, runs on timeout
                    clientBytes = new byte[4];
                    clientPacket = new DatagramPacket(clientBytes, 4);
                    try {
                        dataSocket.receive(clientPacket);
                    } catch (SocketTimeoutException e) {
                        //No packet to recieve now
                        continue;
                    }

                    clientAck = ByteBuffer.wrap(clientBytes).getInt();
                    System.out.println("Recieved Acknowledgement: " + String.valueOf(clientAck));
                    window.WindowSlotCheck(clientAck);
                    window.WindowCleaner();

                } //End of file sending loop

                fileSt.close();
                inputS.close();
            }

        } catch (Exception e) {

        }

    }


    public static void main(String[] args) {

        File file = null;
        byte[] bytes = null;
        byte[] clientBytes;
        BufferedInputStream inputS = null;
        FileInputStream fileSt = null;
        Window window;
        Packet packet;
        Packet clPacket;
        DatagramPacket dgPacket;
        DatagramPacket clientPacket;
        String clientSeqAck;

        try {
            DatagramChannel c = DatagramChannel.open();
            DatagramSocket ds = c.socket();
            ds.setSoTimeout(100);
            int port = 100;

            while (true) {
                Console cons = System.console();
                String m = "5000"; //cons.readLine("Enter port number: ");
                if (m.matches("[0-9]+")) {
                    port = Integer.parseInt(m);
                } else if (m.equals("exit")) {
                    System.exit(0);
                } else {
                    continue;
                }

                try {
                    ds.bind(new InetSocketAddress(port));
                    break;
                } catch (IOException e) {
                    System.out.println("Unavailable Port");
                    continue;
                }
            }

            while (true) {
                //SocketChannel sc = c.accept();

                System.out.println("Awaiting file request");
                int fileSize = 0;
                // buffer to get client's command
                ByteBuffer messageBuffer = ByteBuffer.allocate(1028);
                // buffer for file handling
                ByteBuffer fileBuffer;
                // get address of client
                SocketAddress resend = c.receive(messageBuffer);
                // Decode message
                String message2 = new String(messageBuffer.array()).trim();
                System.out.println("Client Request: " + message2);

                // No input handler?
                if (message2.equals(null) || message2.equals("")) {

                }

                // The file sending else
                else {

                    try {
                        file = new File(message2);
                        fileSt = new FileInputStream(file);
                        inputS = new BufferedInputStream(fileSt);
                    } catch (Exception e) {
                        System.out.print("No such file");

                    }

                } // Got file
                byte[] test = Files.readAllBytes(file.toPath());
                int numPackets = (int) Math.ceil((double) file.length() / 1024);
                System.out.println("File size: " + test.length);
                window = new Window(5, numPackets);
                int seqNumber = 0;
                int clientAck = 0;
                byte[][] fileBuilder = new byte[numPackets][1024];

                fileBuffer = ByteBuffer.wrap(String.valueOf(file.length()).getBytes());
                c.send(fileBuffer, resend);

                // File seding loop, -2 is last packet acknowledgement.
                while (clientAck != -2) {
                    if (window.WindowApprove(seqNumber)) {
                        bytes = new byte[1024];

                        //Reads 1024 bytes into array, should mark position when this occurs multiple times
                        inputS.read(bytes, 0, 1024);
                        fileBuilder[seqNumber] = bytes;

                        // Last packet to send, sends -2 as sequence number to notify client
                        if ((seqNumber * 1024) >= file.length()) {
                            packet = new Packet(bytes, -2);
                        } else {
                            packet = new Packet(bytes, seqNumber);
                        }

                        dgPacket = new DatagramPacket(packet.getBytes(), 1028, resend);
                        ds.send(dgPacket);
                        seqNumber++;
                    }

                    // Get packet to resend, window isn't moving
                    else {
                        int sendAgain[] = window.getSlotsRemaining();
                        for (int i = 0; i < sendAgain.length; i++) {
                            if (sendAgain[i] != -1) {
                                bytes = fileBuilder[sendAgain[i]];

                                // Last packet to send
                                if ((seqNumber * 1024) >= file.length()) {
                                    packet = new Packet(bytes, -2);
                                }

                                // send the packet
                                else {
                                    packet = new Packet(bytes, sendAgain[i]);
                                }

                                dgPacket = new DatagramPacket(packet.getBytes(), 1028, resend);
                                ds.send(dgPacket);
                                break;
                            }
                        }
                    }

                    //Recieve packet from client, runs on timeout
                    clientBytes = new byte[4];
                    clientPacket = new DatagramPacket(clientBytes, 4);
                    try {
                        ds.receive(clientPacket);
                    } catch (SocketTimeoutException e) {
                        //No packet to recieve now
                        continue;
                    }

                    clientAck = ByteBuffer.wrap(clientBytes).getInt();
                    System.out.println("Recieved Acknowledgement: " + String.valueOf(clientAck));
                    window.WindowSlotCheck(clientAck);
                    window.WindowCleaner();

                } //End of file sending loop

                fileSt.close();
                inputS.close();
            }

        } catch (IOException e) {
            System.out.println("whoops");
        }


    }

}
