import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.zip.CRC32;

/**************************************************************************************
 * The following class is the function that takes in user input and then sends a
 * request for a file to a server. The client takes packets from the server and builds
 * file
 * 
 * @author Cody West|Peter Holleman
 * @version Project 2 UDP
 * @date 10/20/2017
 *************************************************************************************/
public class UdpClientV2 {

	/** Socket and channel for the main connection and file transfer */
	private DatagramChannel dataChannel;
	private DatagramSocket dataSocket;

	/** Streams for building file recieved */
	private BufferedOutputStream outputS = null;
	private FileOutputStream fileSt = null;

	/** Strings for storing user input */
	private Console cons;
	private String ip;
	private String portS;
	private int port;
	
	/** The max size of a packet */
	private final int PACKET_SIZE = 1036;
	// CRC32 ackCRC;
	/** CRC for checking validity of recieved packets */
	CRC32 crcCheck;
	// CRC32 crcServer;
	/** ArrayList holding the packets already recieved */
	ArrayList<Integer> recieved;
	
	/** Total number of packets to be recieved */
	int numPackets;
	
	/** Integer counting number of failed recieves */
	int fileTimer = 0;
	
	/** Socket Address of Server */
	SocketAddress resendAdr;
	
	/** Transfer window for accepting packets */
	Window window;
	
	
	/** Sockets for the main connection and file transfer */
	byte[][] fileBuilder;

	/*************************************************************************
	 * Constructor creates client thread with proper sockets, channel, 
	 * user input IP address and port.
	 ************************************************************************/
	public UdpClientV2() throws IOException {

		dataChannel = DatagramChannel.open();
		dataSocket = dataChannel.socket();
		dataSocket.setSoTimeout(50);
		cons = System.console();

		while (true) {

			Scanner scanner = new Scanner(System.in);
			System.out.print("Enter an ip address: ");
			ip = scanner.nextLine();
			System.out.print("Enter a port #: ");
			portS = scanner.nextLine();

			// ip = cons.readLine("Enter IP Address (x.x.x.x,args): ");
			// portS = cons.readLine("Enter port number: ");
			// port = Integer.parseInt(portS);
			if (portS.matches("[0-9]+")) {
				port = Integer.parseInt(portS);
				break;
			}
		}

	}
	
	/*************************************************************************
	 * Constructor creates client thread with proper sockets, channel, 
	 * IP address and port.
	 * @param port pre-defined port for client
	 * @param ip pre-defined ip for client
	 ************************************************************************/
	public UdpClientV2(int port, String ip) {

		try {
			dataChannel = DatagramChannel.open();
			dataSocket = dataChannel.socket();
			dataSocket.setSoTimeout(50);
			this.port = port;
			this.ip = ip;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/*************************************************************************
	 * Method for getting the file to be requested and sending request to 
	 * the server. Downloads the file in packets and checks them for validity.
	 * Ends when the file download is finished (client has all packets).
	 ************************************************************************/
	public void requestFile() throws IOException {

		String fileName = "";
		Scanner scanner = new Scanner(System.in);
		// fileName = scanner.nextLine();
		InetSocketAddress server = new InetSocketAddress(ip, port);
		//dataChannel.connect(server);
		//dataSocket = dataChannel.socket();
		SocketAddress resendAdr = null;

		while (true) {

			Packet packet = new Packet();

			// Send Filename
			fileName = "heyman.txt"; // cons.readLine("Enter file request ");
			//ByteBuffer buf = ByteBuffer.wrap(fileName.getBytes());
			// dataChannel.send(buf, server);
			// dataChannel.receive(buf)

			// System.out.println("Connected");
			// dataChannel.send(buf, server);
			System.out.println("Requesting file: " + fileName);

			/*****/

			Packet fileNamePacket = new Packet(fileName.getBytes(), -4);
			DatagramPacket fileNameDatagram = new DatagramPacket(fileNamePacket.getBytes(), fileNamePacket.getBytes().length, server);

			dataSocket.send(fileNameDatagram);

			// ByteBuffer fileNameBuff = ByteBuffer.allocate(1036);
			//
			// // Recieve filesize
			// ByteBuffer buf2 = ByteBuffer.allocate(1024);
			//SocketAddress resendAdr = dataChannel.receive(buf2);
			// String message = new String(buf2.array()).trim();

			byte[] fileSizeBytes = new byte[1036];
			DatagramPacket fileSizeDatagram = new DatagramPacket(fileSizeBytes, fileSizeBytes.length);
			
			try{
			dataSocket.receive(fileSizeDatagram);
			resendAdr = fileSizeDatagram.getSocketAddress();
			fileTimer=0;
			}
			
			catch(SocketTimeoutException e){
				System.out.println("Filesize Packet Timeout");
				fileTimer++;
				if(fileTimer>14){
					System.out.println("Can't reach Server");
					break;
				}
			}

			byte[] recievedSizeBytes = new byte[fileSizeDatagram.getLength()];

			System.arraycopy(fileSizeDatagram.getData(), fileSizeDatagram.getOffset(), recievedSizeBytes, 0, fileSizeDatagram.getLength());

			Packet sizePacket = new Packet(recievedSizeBytes);
			if (!sizePacket.validateCRC())
				continue;
			if (sizePacket.getSeqNum() != -3)
				continue;
			long fileSize = ByteBuffer.wrap(sizePacket.getData()).getLong();

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
			int finalPacketSize = (int) (fileSize % 1024) + 12;
			int seqNum;
			boolean finish = false;
			recieved = new ArrayList<Integer>();

			// Start recieving
			while (!finish) {

				// byte[] fileBytes = new byte[1028];
				DatagramPacket dgPacket;
				byte[] fileBytes;

				// Needs to be changed if packets are coming out of order.
				// Either that or set reciever to take only next packet
				// Use an arraylist to keep track of acknowledged numbers
				// and only take last packet if everything else is gotten?
				// Basically the problem is that the last packet is using a size
				// too big and the crc/seq number get messed up
				//if (window.WindowApprove(-2)) {
					//fileBytes = new byte[finalPacketSize];
					//dgPacket = new DatagramPacket(fileBytes, finalPacketSize);
				//}

				// else {
				fileBytes = new byte[PACKET_SIZE];
				dgPacket = new DatagramPacket(fileBytes, PACKET_SIZE);
				// }

				// dataSocket.receive(dgPacket);
				// packet = new Packet(dgPacket.getData());
				// seqNum = packet.getSeqNum();
				try {

					dataSocket.receive(dgPacket);
					System.out.println("Datagram Size: " + dgPacket.getLength());
					byte[] packetBytes = new byte[dgPacket.getLength()];
					System.arraycopy(dgPacket.getData(), dgPacket.getOffset(), packetBytes, 0, dgPacket.getLength());
					System.out.println("Packet Data Pre Packeting Size: " + packetBytes.length);

					packet = new Packet(packetBytes);
					seqNum = packet.getSeqNum();

					String seqNumS = Integer.toString(seqNum);
					crcCheck = new CRC32();
					crcCheck.update(packet.getDataSeq());
					System.out.println("Recieved Packet: " + seqNum + "| Size: " + packet.getSize());
					System.out.println("Packet CRC: " + packet.getCRC());
					System.out.println("Client CRC: " + crcCheck.getValue());
					if (seqNum == -2) {
						System.out.println("Last packet, size should be: " + finalPacketSize);
					}

					// Packet corrupted
					if (crcCheck.getValue() != packet.getCRC()) {
						System.out.println("Packet corrupted, Seq Number: " + packet.getSeqNum());
						continue;
					}

					// Packet (most likely) not corrupted
					else {
						
						//Check if packet already recieved
						if (recieved.contains(seqNum)) {
							AckPacket ackPacket = new AckPacket(seqNum);

							System.out.println("Acknowledging packet " + Integer.toString(seqNum));
							// System.out.println(window);
							System.out.println("Ack CRC: " + ackPacket.getCRC());
							DatagramPacket ackSend = new DatagramPacket(ackPacket.getBytes(), 12, resendAdr);
							// System.out.println(ackSend.getAddress());

							dataSocket.send(ackSend);
							continue; // Don't process the packet
						}

						if (seqNum == -2) {
							if (window.WindowApprove(-2)) {
								fileBuilder[numPackets - 1] = packet.getData();

								window.WindowSlotCheck(numPackets - 1);
								System.out.println(window.toString());
								window.WindowCleaner();
								System.out.println(window.toString());
								System.out.println("Acknowledging last packet " + Integer.toString(seqNum));

								AckPacket ackPacket = new AckPacket(seqNum);
								System.out.println("Ack CRC: " + ackPacket.getCRC());
								DatagramPacket ackSend = new DatagramPacket(ackPacket.getBytes(), 12, resendAdr);
								dataSocket.send(ackSend);
								finish = true;
								recieved.add(numPackets - 1);
							}
						} else if (window.WindowApprove(seqNum)) {

							fileBuilder[seqNum] = packet.getData();
							window.WindowSlotCheck(seqNum);
							System.out.println(window.toString());
							window.WindowCleaner();
							System.out.println(window.toString());

							AckPacket ackPacket = new AckPacket(seqNum);

							System.out.println("Acknowledging packet " + Integer.toString(seqNum));
							// System.out.println(window);
							System.out.println("Ack CRC: " + ackPacket.getCRC());
							DatagramPacket ackSend = new DatagramPacket(ackPacket.getBytes(), 12, resendAdr);
							// System.out.println(ackSend.getAddress());

							dataSocket.send(ackSend);
							recieved.add(seqNum);
							// window.WindowSlotCheck(seqNum);
						}

						// Last packet handler

						// bytesRead = fileSize;

					}
				}

				catch (Exception e) {
					System.out.println("Packet Timeout");
					//e.printStackTrace();
					continue;
				}

			} // Stop recieving file

			File file = new File("/home/mininet/net/client/" + fileName);
			//File file = new File("out2.jpg");
			//File file = new File("E:\\client\\" + fileName);
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
			dataChannel.disconnect();

			break;

			// System.out.println(message2);

		}

	}

	/*************************************************************************
	 * Method for comparing packet's CRC with a client built CRC to make sure
	 * no corruption has occured.
	 * @param packet Packet recieved and submitted for validation
	 ************************************************************************/
	boolean validateCRC(Packet packet) {
		CRC32 crc = new CRC32();
		crc.update(packet.getDataSeq());
		if (crc.getValue() == packet.getCRC()) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) {

		try {
			UdpClientV2 test = new UdpClientV2();
			//UdpClientV2 test = new UdpClientV2(1502, "127.0.0.1");
			test.requestFile();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
