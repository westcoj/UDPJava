import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.util.zip.CRC32;

/**
 * Created by pieterholleman on 10/1/17.
 */
public class UdpServerV2 {

	private DatagramChannel dataChannel;
	private DatagramSocket dataSocket;
	private Window window;
	Console cons;

	public UdpServerV2() throws IOException {

		dataChannel = DatagramChannel.open();
		dataSocket = dataChannel.socket();
		dataSocket.setSoTimeout(100);
		cons = System.console();

	}

	public boolean connect() {

		while (true) {
			try {
				while (true) {

					// parsing/validating port input
					String portS ="1052"; //cons.readLine("Enter port number: ");
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

				}

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

				//file = new File("/home/mininet/net/server/" + request);
				file = new File("E:\\server\\" + request);
				fileSt = new FileInputStream(file);
				inputS = new BufferedInputStream(fileSt);

				byte[] test = Files.readAllBytes(file.toPath());
				int numPackets = (int) Math.ceil((double) file.length() / 1024);

				System.out.println("File size: " + test.length);

				window = new Window(5, numPackets);
				int seqNumber = 0;
				int clientAck = 0;
				byte[][] fileBuilder = new byte[numPackets][];

				fileBuffer = ByteBuffer.wrap(String.valueOf(file.length()).getBytes());
				dataChannel.send(fileBuffer, resendAdr);

				int packetSize = 1024;
				// File sending loop, -2 is last packet acknowledgement.
				while (clientAck != -2) {

					if (window.WindowApprove(seqNumber)) {

						if (((seqNumber + 1) * 1024) >= file.length()) {
							packetSize = (int) (file.length() % 1024);
							bytes = new byte[packetSize];
							inputS.read(bytes, 0, packetSize);
							fileBuilder[seqNumber] = bytes;
							packet = new Packet(bytes, -2);
						} else {
							bytes = new byte[packetSize];
							inputS.read(bytes, 0, packetSize);
							fileBuilder[seqNumber] = bytes;
							packet = new Packet(bytes, seqNumber);
						}

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

						dgPacket = new DatagramPacket(packet.getBytes(), packetSize + 4, resendAdr);
						dataSocket.send(dgPacket);
						System.out.println("Sending Packet: " + Integer.toString(seqNumber));
						// window.WindowSlotCheck(seqNumber);
						seqNumber++;
					}

					// Get packet to resend, window isn't moving
					else {
						int sendAgain[] = window.getSlotsRemaining();
						for (int i = 0; i < sendAgain.length; i++) {
							if (sendAgain[i] != -1) {
								bytes = fileBuilder[sendAgain[i]];

								// Last packet to send
								if ((sendAgain[i] * 1024) >= file.length()) {
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

					// Recieve packet from client, runs on timeout
					clientBytes = new byte[12];
					clientPacket = new DatagramPacket(clientBytes, 12);
					

					try {
						dataSocket.receive(clientPacket);

					} catch (SocketTimeoutException e) {
						// No packet to recieve now
						System.out.println("Packet Timeout");
						continue;
					}

					ackCRC= new CRC32();
					ackCRC.update(clientBytes, 0, 4);
					byte[] clientAckCRC = new byte[8];
					System.arraycopy(clientBytes, 4, clientAckCRC, clientAckCRC.length-1 , 8);
					ByteBuffer crcBuffer = ByteBuffer.allocate(Long.BYTES);
					crcBuffer.put(clientAckCRC);
					crcBuffer.flip();
					long clientCRCVal = crcBuffer.getLong();
					
					if(ackCRC.getValue() != clientCRCVal){
						System.out.println("ACK corrupted, ignoring");
						continue;
					}
					
					String ack = new String(ByteBuffer.wrap(clientBytes).array()).trim();
					clientAck = Integer.parseInt(ack);
					System.out.println("Recieved Acknowledgement: " + ack);
					window.WindowSlotCheck(clientAck);
					System.out.println(window.toString());
					window.WindowCleaner();
					
		

				} // End of file sending loop

				fileSt.close();
				inputS.close();
			}

		} catch (Exception e) {

		}

	}

	public static void main(String[] args) {

		try {
			UdpServerV2 test = new UdpServerV2();
			test.connect();
			test.awaitRequest();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
