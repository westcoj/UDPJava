import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.zip.CRC32;

/**
 * Created by pieterholleman on 10/1/17.
 */
public class UdpClientV2 {

	private DatagramChannel dataChannel;
	private DatagramSocket dataSocket;

	private BufferedOutputStream outputS = null;
	private FileOutputStream fileSt = null;

	private Console cons;
	private String ip;
	private String portS;

	private int port;
	private final int PACKET_SIZE = 1036;
	//CRC32 ackCRC;
	CRC32 crcCheck;
	//CRC32 crcServer;
	ArrayList<Integer> recieved;
	int numPackets;
	SocketAddress resendAdr;
	Window window;

	byte[][] fileBuilder;

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

			//ip = cons.readLine("Enter IP Address (x.x.x.x,args): ");
			//portS = cons.readLine("Enter port number: ");
			//port = Integer.parseInt(portS);
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public long setupTransfer(String str) throws Exception {


		//Packet fileNamePacket = new Packet(str.getBytes(), -3);
		//DatagramPacket dgPacket = new DatagramPacket(str.getBytes(), str.getBytes().length);
		ByteBuffer buf = ByteBuffer.wrap(str.getBytes());
		InetSocketAddress server = new InetSocketAddress(ip, port);
		//dataSocket.bind(server);
		//dataSocket.send(dgPacket);
		System.out.println("Connected");
		//dataChannel.send(buf, server);
		System.out.println("Requesting file: " + str);
		ByteBuffer buf2 = ByteBuffer.allocate(1024);
		resendAdr = dataChannel.receive(buf2);
		String fileSizeMessage = new String(buf2.array()).trim();
		long fileSize = Long.parseLong(fileSizeMessage);
		System.out.println("File size: " + Long.toString(fileSize));



		return fileSize;



	}

	public void beginTransfer(long fileSize) throws Exception{

		Packet packet = new Packet();
		int seqNum;
		ArrayList<Integer> recieved = new ArrayList<Integer>();
		numPackets = (int) Math.ceil((double) fileSize / 1024);
		boolean finish = false;
		fileBuilder = new byte[numPackets][];
		window = new Window(5, numPackets);
		int finalPacketSize = (int) (fileSize % 1024) + 12;


		System.out.println(String.valueOf(numPackets));

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
			if (window.WindowApprove(-2)) {
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

				//receive packet
				dataSocket.receive(dgPacket);
				packet = new Packet(dgPacket.getData());

				seqNum = packet.getSeqNum();

				//"new" crc to compare
				crcCheck = new CRC32();
				crcCheck.update(packet.getDataSeq());

				//print statements for debug
				System.out.println("Recieved Packet: " + seqNum + "| Size: " + packet.getSize());
				System.out.println("Packet CRC: " + packet.getCRC());
				System.out.println("Check CRC: " + crcCheck.getValue());

				if (seqNum == -2) {
					System.out.println("Last packet, size should be: " + finalPacketSize);
				}

				// Packet corrupted
				if (!packet.validateCRC()) {
					System.out.println("Packet corrupted, Seq Number: " + packet.getSeqNum());
					continue;
				}

				// Packet (most likely) not corrupted
				else {

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
					}


					else if (window.WindowApprove(seqNum)) {

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

					if (recieved.contains(seqNum)) {
						AckPacket ackPacket = new AckPacket(seqNum);

						System.out.println("Acknowledging packet " + Integer.toString(seqNum));
						// System.out.println(window);
						System.out.println("Ack CRC: " + ackPacket.getCRC());
						DatagramPacket ackSend = new DatagramPacket(ackPacket.getBytes(), 12, resendAdr);
						// System.out.println(ackSend.getAddress());

						dataSocket.send(ackSend);
					}
					// Last packet handler

					// bytesRead = fileSize;

				}
			}

			catch (Exception e) {
				System.out.println("Packet grab error");
				e.printStackTrace();
				continue;
			}

		} // Stop recieving file

		//File file = new File("/home/mininet/net/client/" + fileName);
		File file = new File("out2.jpg");
		// File file = new File("E:\\client\\" + fileName);
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



		// System.out.println(message2);

	}



	public void requestFile2(){



		boolean finish = false;


		while(true){
			try{

				long fileSize = setupTransfer("dankmeme.jpg");
				beginTransfer(fileSize);


				//File file = new File("/home/mininet/net/client/" + fileName);
				File file = new File("out2.jpg");
				// File file = new File("E:\\client\\" + fileName);
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


			}

			catch (Exception e){

				e.printStackTrace();

			}
		}
	}

	public void writeFile() throws Exception {

		//File file = new File("/home/mininet/net/client/" + fileName);
		File file = new File("out2.jpg");
		// File file = new File("E:\\client\\" + fileName);
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

	}


	public void requestFile() throws IOException {

		String fileName = "";
		Scanner scanner = new Scanner(System.in);
		//fileName = scanner.nextLine();
		InetSocketAddress server = new InetSocketAddress(ip, port);


		while (true) {

			Packet packet = new Packet();


			//Send Filename
			fileName = "dankmeme.jpg"; // cons.readLine("Enter file request ");
			ByteBuffer buf = ByteBuffer.wrap(fileName.getBytes());
//			dataChannel.send(buf, server);
//			dataChannel.receive(buf)
			dataChannel.connect(server);
			System.out.println("Connected");
			//dataChannel.send(buf, server);
			System.out.println("Requesting file: " + fileName);

			/*****/

			Packet fileNamePacket = new Packet("dankmeme.jpg".getBytes(), -3);
			DatagramPacket fileNameDatagram = new DatagramPacket(fileNamePacket.getBytes(), fileNamePacket.getBytes().length);

			dataSocket.send(fileNameDatagram);

//			ByteBuffer fileNameBuff = ByteBuffer.allocate(1036);
//
//			// Recieve filesize
//			ByteBuffer buf2 = ByteBuffer.allocate(1024);
//			SocketAddress resendAdr = dataChannel.receive(buf2);
//			String message = new String(buf2.array()).trim();

			byte[] fileSizeBytes = new byte[1036];
			DatagramPacket fileSizeDatagram = new DatagramPacket(fileSizeBytes, fileSizeBytes.length);
			dataSocket.receive(fileSizeDatagram);

			byte[] recievedSizeBytes = new byte[fileSizeDatagram.getLength()];

			System.arraycopy(
					fileSizeDatagram.getData(),
					fileSizeDatagram.getOffset(),
					recievedSizeBytes,
					0,
					fileSizeDatagram.getLength()
			);
			Packet sizePacket = new Packet(recievedSizeBytes);
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
				if (window.WindowApprove(-2)) {
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
								DatagramPacket ackSend = new DatagramPacket(ackPacket.getBytes(), 12);
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
							DatagramPacket ackSend = new DatagramPacket(ackPacket.getBytes(), 12);
							// System.out.println(ackSend.getAddress());

							dataSocket.send(ackSend);
							recieved.add(seqNum);
							// window.WindowSlotCheck(seqNum);
						}

						if (recieved.contains(seqNum)) {
							AckPacket ackPacket = new AckPacket(seqNum);

							System.out.println("Acknowledging packet " + Integer.toString(seqNum));
							// System.out.println(window);
							System.out.println("Ack CRC: " + ackPacket.getCRC());
							DatagramPacket ackSend = new DatagramPacket(ackPacket.getBytes(), 12);
							// System.out.println(ackSend.getAddress());

							dataSocket.send(ackSend);
						}
						// Last packet handler

						// bytesRead = fileSize;

					}
				}

				catch (Exception e) {
					System.out.println("Packet grab error");
					e.printStackTrace();
					continue;
				}

			} // Stop recieving file

			//File file = new File("/home/mininet/net/client/" + fileName);
			File file = new File("out2.jpg");
			// File file = new File("E:\\client\\" + fileName);
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
			//UdpClientV2 test = new UdpClientV2();
			UdpClientV2 test = new UdpClientV2(2000, "127.0.0.1");
			test.requestFile();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
