import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Scanner;
import java.util.zip.CRC32;

/**
 * Created by pieterholleman on 10/1/17.
 */
public class UdpClientV2 {

	DatagramChannel dataChannel;
	DatagramSocket dataSocket;
	BufferedOutputStream outputS = null;
	FileOutputStream fileSt = null;
	Console cons;
	String ip;
	String portS;
	int port;
	final int PACKET_SIZE = 1036;
	CRC32 ackCRC;
	CRC32 crcCheck;

	public UdpClientV2() throws IOException {

		dataChannel = DatagramChannel.open();
		dataSocket = dataChannel.socket();
		cons = System.console();

		while (true) {
			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter an ip address: ");
			ip = scanner.nextLine();
			System.out.print("Enter a port #: ");
			portS = scanner.nextLine();
		    // ip = cons.readLine("Enter IP Address (x.x.x.x,args): ");
			// portS = cons.readLine("Enter port number: ");
			port = Integer.parseInt(portS);
			if (portS.matches("[0-9]+")) {
				port = Integer.parseInt(portS);
				break;
			}
		}

	}

	public UdpClientV2(int port, String ip) {

		try {
			dataChannel = DatagramChannel.open();
			dataSocket = dataChannel.socket();
			this.port = port;
			this.ip = ip;
		} catch(Exception e){
			e.printStackTrace();
		}
	}




	public void requestFile() throws IOException {

		String fileName = "";
		Scanner scanner = new Scanner(System.in);
		fileName = scanner.nextLine();

		while (true) {

			Packet packet = new Packet();

			// First request

			fileName = "jarjar.jpg"; //cons.readLine("Enter file request ");
			ByteBuffer buf = ByteBuffer.wrap(fileName.getBytes());
			InetSocketAddress server = new InetSocketAddress(ip, port);
			System.out.println("Connected");
			dataChannel.send(buf, server);
			System.out.println("Requesting file: " + fileName);

			// Recieve filesize
			ByteBuffer buf2 = ByteBuffer.allocate(1024);
			SocketAddress resendAdr = dataChannel.receive(buf2);
			String message = new String(buf2.array()).trim();
			long fileSize = Long.parseLong(message);
			System.out.println("File size: " + Long.toString(fileSize));
			int numPackets = (int) Math.ceil((double) fileSize / 1024);
			long bytesRead = 0;
			System.out.println(String.valueOf(numPackets));

			// Array of bytes array to read into file successfully?
			// Possible to read as we go, but would mean only accepting the next
			// seq packet
			// in the series, rather than any in the window.
			byte[][] fileBuilder = new byte[numPackets][];

			Window window = new Window(5, numPackets);
			int finalPacketSize = (int) (fileSize % 1024) + 4;
			int seqNum;

			// Start recieving
			while (bytesRead < fileSize) {

				// byte[] fileBytes = new byte[1028];
				DatagramPacket dgPacket;
				byte[] fileBytes;

				if (fileSize - bytesRead < 1024) {
					fileBytes = new byte[finalPacketSize];
					dgPacket = new DatagramPacket(fileBytes, finalPacketSize);
				}

				else {
					fileBytes = new byte[PACKET_SIZE];
					dgPacket = new DatagramPacket(fileBytes, PACKET_SIZE);
				}

				// dataSocket.receive(dgPacket);
				// packet = new Packet(dgPacket.getData());
				// seqNum = packet.getSeqNum();
				try {

					dataSocket.receive(dgPacket);
					packet = new Packet(dgPacket.getData());
					seqNum = packet.getSeqNum();

					String seqNumS = Integer.toString(seqNum);
					crcCheck = new CRC32();
					crcCheck.update(packet.getDataSeq());
					
					//Packet corrupted
					if (crcCheck.getValue() != packet.getCRC()) {
						System.out.println("Packet corrupted, Seq Number: " + packet.getSeqNum());
						continue;
					}
					
					//Packet (most likely) not corrupted
					else {

						if (window.WindowApprove(seqNum)) {
							fileBuilder[seqNum] = packet.getData();
							bytesRead += 1024;
							window.WindowSlotCheck(seqNum);
						}

						// Last packet handler
						else if (seqNum == -2) {
							fileBuilder[numPackets - 1] = packet.getData();
							bytesRead = fileSize;
						}
					}
				}

				catch (Exception e) {
					System.out.println("Packet grab error");
					e.printStackTrace();
					continue;
				}

				System.out.println("Sequence number " + packet.getSeqNum());
				// System.out.println(packet.toString());
				// Is packet recieved in window?

				// Check slots for recieved items
				window.WindowCleaner();

                AckPacket ackPacket = new AckPacket(seqNum);
                byte[] sendAckBytes = ByteBuffer.allocate(4).putInt(seqNum).array();

				//byte[] sendAckBytes = Integer.toString(seqNum).getBytes();
				ackCRC = new CRC32();
				ackCRC.update(sendAckBytes);

				byte [] crcBytes = ByteBuffer.allocate(8).putLong(ackCRC.getValue()).array();
				byte [] acknowledgement = new byte[12];
				System.arraycopy(sendAckBytes, 0, acknowledgement, 0, 4);
				System.arraycopy(crcBytes, 0, acknowledgement, 4, 8);
				//System.arraycopy(crcBytes, 0, sendAckBytes, sendAckBytes.length , 8);
				int sendAckBytesLen = sendAckBytes.length;

				System.out.println("Acknowledging packet " + Integer.toString(seqNum));
				// System.out.println(window);
                  DatagramPacket ackSend = new DatagramPacket(acknowledgement, 12, resendAdr);
				//DatagramPacket ackSend = new DatagramPacket(sendAckBytes, sendAckBytesLen, resendAdr);
				// System.out.println(ackSend.getAddress());

				dataSocket.send(ackSend);

			} // Stop recieving file

			// File file = new File("/home/mininet/net/client/" + fileName);
			File file = new File("out.jpg");
			fileSt = new FileOutputStream(file);
			outputS = new BufferedOutputStream(fileSt);
			int k = 0;
			while (k != fileBuilder.length) {
				outputS.write(fileBuilder[k]);
				k++;
			}

			System.out.println("File successfully written");

			outputS.close();
			fileSt.close();

			break;

			// System.out.println(message2);

		}

	}

	public static void main(String[] args) {

		try {
			UdpClientV2 test = new UdpClientV2(1052, "127.0.0.1");
			test.requestFile();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
