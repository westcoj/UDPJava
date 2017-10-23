import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.util.zip.CRC32;
import java.util.Scanner;

/**
 * Created by pieterholleman on 10/1/17.
 */
public class UdpServerV2 {

	private DatagramChannel dataChannel;
	private DatagramSocket dataSocket;
	private Window window;
	Console cons;
	Scanner scanner = new Scanner(System.in);

	public UdpServerV2() throws IOException {

		dataChannel = DatagramChannel.open();
		dataSocket = dataChannel.socket();
		dataSocket.setSoTimeout(500);
		cons = System.console();

	}


	public boolean connect(int port) {

		try {
			dataSocket.bind(new InetSocketAddress(port));
			return true;
		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}
	}



	public boolean connect() {

		while (true) {
			try {


				// parsing/validating port input
				//String portS = cons.readLine("Enter port number: ");
				System.out.print("Enter a port number: ");
				String portS = scanner.nextLine().trim();
				int port = Integer.parseInt(portS);
				if (portS.matches("[0-9]+")) {
					port = Integer.parseInt(portS);
				} else if (portS.equals("exit")) {
					System.exit(0);
				} else {
					continue;
				}

				dataSocket.bind(new InetSocketAddress(port));
				return true;

			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
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
		CRC32 ackCRC;
		int timeoutCounter = 0;

		try {

			System.out.println("Awaiting file request");

			while (true) {


				int fileSize = 0;

				/**/
				byte[] requestBytes = new byte[1036];
				DatagramPacket fileRequestDatagram = new DatagramPacket(requestBytes, requestBytes.length);

				try {
					dataSocket.receive(fileRequestDatagram);
					resendAdr = fileRequestDatagram.getSocketAddress();

				} catch (Exception e){
					continue;
				}

				byte[] recievedRequestBytes = new byte[fileRequestDatagram.getLength()];
				System.arraycopy(
						fileRequestDatagram.getData(),
						fileRequestDatagram.getOffset(),
						recievedRequestBytes,
						0,
						fileRequestDatagram.getLength()
				);

				Packet requestPacket = new Packet(recievedRequestBytes);
				String request = new String(requestPacket.getData(), "UTF-8");
				System.out.println("Client Request: " + request);

				/**/
				// buffer to get client's command
				//messageBuffer = ByteBuffer.allocate(256);

				// get address of client
				//resendAdr = dataChannel.receive(messageBuffer);

				// Decode message


				try {

					//file = new File("E:\\server\\" + request);
					//file = new File("/home/mininet/net/server/" + request);
					file = new File(request);
					fileSt = new FileInputStream(file);
					inputS = new BufferedInputStream(fileSt);
				}

				catch (Exception e) {
					System.out.println("Client requested file not found");
					continue;
				}

				int numPackets = (int) Math.ceil((double) file.length() / 1024);
				window = new Window(5, numPackets);
				int seqNumber = 0;
				int clientAck = 0;
				byte[][] fileBuilder = new byte[numPackets][];
				int lastPacketSize = (int) (file.length() % 1024);
				byte[] lastFileBytes = new byte[lastPacketSize];
				int packetSize = 1024;

			 	byte[] fileSizeBytes = ByteBuffer.allocate(84).putLong(file.length()).array();
				Packet fileSizePacket = new Packet(fileSizeBytes, -4);
				DatagramPacket fileSizeDatagram = new DatagramPacket(fileSizePacket.getData(), fileSizePacket.getData().length);
				dataChannel.connect(resendAdr);
				dataSocket.send(fileSizeDatagram);
				
				System.out.println("# of packets: " + numPackets);
				System.out.println("Last packet size: " + lastPacketSize);



				fileBuffer = ByteBuffer.wrap(String.valueOf(file.length()).getBytes());
				//dataChannel.send(fileBuffer, resendAdr);
				boolean finish = false;
				// File sending loop, -2 is last packet acknowledgement.
				while (!finish) {

					if (window.WindowApprove(seqNumber)) {
						if(window.WindowApprove(-2)){
							bytes = new byte[lastPacketSize];
							inputS.read(bytes, 0, lastPacketSize);
							lastFileBytes = bytes;
							packet = new Packet(bytes, -2);
							System.out.println("Sending Packet: " + Integer.toString(-2) + " |Size: " + packet.getSize() + " |should be: " + (lastPacketSize+12));
							
						} 
						else{
							bytes = new byte[packetSize];
							inputS.read(bytes, 0, packetSize);
							fileBuilder[seqNumber] = bytes;
							packet = new Packet(bytes, seqNumber);
							System.out.println("Sending Packet: " + Integer.toString(seqNumber) + " |Size: " + packet.getSize());
						}
//
//						if (((seqNumber + 1) * 1024) >= file.length()) {
//							packetSize = (int) (file.length() % 1024);
//							bytes = new byte[packetSize];
//							inputS.read(bytes, 0, packetSize);
//							fileBuilder[seqNumber] = bytes;
//							packet = new Packet(bytes, -2);
//							System.out.println("Sending Packet: " + Integer.toString(-2));
//						} else {
//							packetSize = 1024;
//							bytes = new byte[packetSize];
//							inputS.read(bytes, 0, packetSize);
//							fileBuilder[seqNumber] = bytes;
//							packet = new Packet(bytes, seqNumber);
//							System.out.println("Sending Packet: " + Integer.toString(seqNumber));
//
//						}

						// System.out.println(packet.toString());
						// bytes = new byte[packetSize];
						// //Reads 1024 bytes into array, should mark position
						// when this occurs multiple times
						// inputS.read(bytes, 0, packetSize);
						// fileBuilder[seqNumber] = bytes;
						//
						// // Last packet to send, sends -2 as sequence number
						// to notify client
						// if ((seqNumber * 1024) >= file.length()) {
						// packetSize = (int) (file.length() % 1024);
						// bytes = new byte[packetSize];
						// inputS.read(bytes, 0, packetSize);
						// fileBuilder[seqNumber] = bytes;
						// packet = new Packet(bytes, -2);
						// } else {
						// packet = new Packet(bytes, seqNumber);
						// }

						dgPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, resendAdr);
						dataSocket.send(dgPacket);
						System.out.println(("Packet CRC: ") + packet.getCRC());
						if (seqNumber < numPackets-1) {
							seqNumber++;
						}
					}

					// Get packet to resend, window isn't moving
					else {
						int sendAgain[] = window.getSlotsRemaining();
						for (int i = 0; i < sendAgain.length; i++) {
							if (sendAgain[i] != -1) {
								//bytes = fileBuilder[sendAgain[i]];

								// Last packet to send
								if (window.WindowApprove(-2)) {
									bytes = new byte[lastPacketSize];
									bytes = lastFileBytes;
									packet = new Packet(bytes, -2);
									System.out.println("Sending Packet Again: " + Integer.toString(-2)+ " |Size: " + packet.getSize() + "|should be| " + (lastPacketSize+12));
								}
								// send the packet
								else {
									bytes = new byte[1024];
									bytes = fileBuilder[sendAgain[i]];
									packet = new Packet(bytes, sendAgain[i]);
									System.out.println("Sending Packet Again: " + Integer.toString(sendAgain[i])+ " |Size: " + packet.getSize());
								}

								
								System.out.println(("Packet CRC: ") + packet.getCRC());
								dgPacket = new DatagramPacket(packet.getBytes(), packet.getBytes().length, resendAdr);
								dataSocket.send(dgPacket);
								break;
							}
						}
					}

					// Recieve packet from client, runs on timeout

					clientBytes = new byte[12];
					clientPacket = new DatagramPacket(clientBytes, 12);
					AckPacket acknowledgement;

					try {
						dataSocket.receive(clientPacket);
						acknowledgement = new AckPacket(clientBytes);
						timeoutCounter = 0;
						if (!validateCRC(acknowledgement)) {
							System.out.println("CRC mismatch");
							continue;
						}

						String ack = String.valueOf(acknowledgement.getSeqNum());
						clientAck = acknowledgement.getSeqNum();
						System.out.println("Recieved Acknowledgement: " + ack);
						
						if(clientAck == -2){
							clientAck = numPackets-1;
							finish = true;
						}
						
						window.WindowSlotCheck(clientAck);
						System.out.println(window.toString());
						window.WindowCleaner();
						System.out.println(window.toString());

					} catch (SocketTimeoutException e) {
						// No packet to recieve now
						System.out.println("Packet Timeout");
						// TIMEOUT INT COUNTER FOR DISCONNECT
						timeoutCounter++;
						if (timeoutCounter == 15) {
							System.out.println("Can no longer reach client, disconnecting");
							break;
						}
						continue;
					}

					if (!validateCRC(clientBytes)) {
						System.out.println("ACK corrupted, ignoring");
						continue;
					}

				} // End of file sending loop

				fileSt.close();
				inputS.close();
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	boolean validateCRC(byte[] footer) {

		CRC32 ackCRC = new CRC32();
		ackCRC.update(footer, 0, 4);
		byte[] clientAckCRC = new byte[8];
		System.arraycopy(footer, 4, clientAckCRC, 0, clientAckCRC.length);
		ByteBuffer crcBuffer = ByteBuffer.allocate(8);
		crcBuffer.put(clientAckCRC);
		crcBuffer.flip();
		long clientCRCVal = crcBuffer.getLong();

		if (ackCRC.getValue() != clientCRCVal)
			return false;

		return true;
	}

	boolean validateCRC(Packet packet) {
		CRC32 crc = new CRC32();
		crc.update(packet.getDataSeq());

		if (crc.getValue() == packet.getCRC()) {
			return true;
		}
		return false;
	}

	boolean validateCRC(AckPacket acknowledgement) {

		CRC32 crc = new CRC32();
		crc.update(acknowledgement.getSeqBytes());
		System.out.println("Server CRC: " + crc.getValue());
		System.out.println("AckPacket CRC: " + acknowledgement.getCRC());
		if (crc.getValue() == acknowledgement.getCRC()) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) {

		try {
			UdpServerV2 test = new UdpServerV2();
			test.connect();
			//test.connect(1502);
			test.awaitRequest();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
