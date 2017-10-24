import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.file.Files;
import java.util.zip.CRC32;
import java.util.Scanner;

/**************************************************************************************
 * The following class is the function that takes in user input and then sends a file
 * to a client upon request. A sliding window is incorporated and each acknowledgement
 * is checked for validity.
 * 
 * @author Cody West|Peter Holleman
 * @version Project 2 UDP
 * @date 10/20/2017
 *************************************************************************************/
public class UdpServerV2 {
	
	/** Socket and channel for the main connection and file transfer */
	private DatagramChannel dataChannel;
	private DatagramSocket dataSocket;
	
	/** Sliding window for sending packets */
	private Window window;
	
	/** Console and Scanner for user-input  */
	Console cons;
	Scanner scanner = new Scanner(System.in);

	/*************************************************************************
	 * Constructor creates server class with proper socket and channel, and
	 * sets a timeout on the socket.
	 ************************************************************************/
	public UdpServerV2() throws IOException {

		dataChannel = DatagramChannel.open();
		dataSocket = dataChannel.socket();
		dataSocket.setSoTimeout(50);
		cons = System.console();

	}
	
	
	/*************************************************************************
	 * Method for binding data socket to given port
	 * @param port pre-defined port for client
	 * @return true if binding succesful
	 ************************************************************************/
	public boolean connect(int port) {

		try {
			dataSocket.bind(new InetSocketAddress(port));
			return true;
		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}
	}

	/*************************************************************************
	 * Method for binding data socket to user inputted port
	 * @return true if binding succesful
	 ************************************************************************/
	public boolean connect() {

		while (true) {
			try {

				// parsing/validating port input
				// String portS = cons.readLine("Enter port number: ");
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

	/*************************************************************************
	 * Method for obtaining client file request and breaking up file into
	 * packets which are then sent to the client. Method uses a sliding 
	 * window and only send valid packets. Each acknowledgement is 
	 * checked for validity. Only ends upon recieving last acknowledgement
	 * (Client only accepts last packet if all other packets are recieved
	 ************************************************************************/
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
		int crcCounter = 0;
		System.out.println("Awaiting file request");
		
		try {
			while (true) {
				
				int fileSize = 0;

				/**/
				byte[] requestBytes = new byte[1036];
				DatagramPacket fileRequestDatagram = new DatagramPacket(requestBytes, requestBytes.length);

				try {
					dataSocket.receive(fileRequestDatagram);
					resendAdr = fileRequestDatagram.getSocketAddress();

				} catch (Exception e) {
					continue;
				}

				byte[] recievedRequestBytes = new byte[fileRequestDatagram.getLength()];
				System.arraycopy(fileRequestDatagram.getData(), fileRequestDatagram.getOffset(), recievedRequestBytes, 0, fileRequestDatagram.getLength());

				Packet requestPacket = new Packet(recievedRequestBytes);
				if (!requestPacket.validateCRC())
					continue;
				if (requestPacket.getSeqNum() != -4)
					continue;
				resendAdr = fileRequestDatagram.getSocketAddress();
				String request = new String(requestPacket.getData(), "UTF-8");
				System.out.println("Client Request: " + request);

				/**/
				// buffer to get client's command
				// messageBuffer = ByteBuffer.allocate(256);

				// get address of client
				// resendAdr = dataChannel.receive(messageBuffer);

				// Decode message

				try {

					//file = new File("E:\\server\\" + request);
					file = new File("/home/mininet/net/server/" + request);
					// file = new File(request);
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

				byte[] fileSizeBytes = ByteBuffer.allocate(8).putLong(file.length()).array();
				Packet fileSizePacket = new Packet(fileSizeBytes, -3);
				DatagramPacket fileSizeDatagram = new DatagramPacket(fileSizePacket.getBytes(), fileSizePacket.getBytes().length, resendAdr);
				// dataChannel.connect(resendAdr);

				dataSocket.send(fileSizeDatagram);

				System.out.println("# of packets: " + numPackets);
				System.out.println("Last packet size: " + lastPacketSize);

				fileBuffer = ByteBuffer.wrap(String.valueOf(file.length()).getBytes());
				// dataChannel.send(fileBuffer, resendAdr);
				boolean finish = false;
				boolean lastSent = false;
				// File sending loop, -2 is last packet acknowledgement.
				while (!finish) {
					
					if(seqNumber == -4){
						break;
					}

					if (window.WindowApprove(seqNumber)) {
						if (window.WindowApprove(-2)&&lastSent==false) {
							bytes = new byte[lastPacketSize];
							fileBuilder[numPackets-1] = bytes;
							inputS.read(bytes, 0, lastPacketSize);
							lastFileBytes = bytes;
							packet = new Packet(bytes, -2);
							lastSent=true;
							System.out.println(
									"Sending Packet: " + Integer.toString(-2) + " |Size: " + packet.getSize() + " |should be: " + (lastPacketSize + 12));

						} else {
							bytes = new byte[packetSize];
							inputS.read(bytes, 0, packetSize);
							fileBuilder[seqNumber] = bytes;
							packet = new Packet(bytes, seqNumber);
							System.out.println("Sending Packet: " + Integer.toString(seqNumber) + " |Size: " + packet.getSize());
						}
						//
						// if (((seqNumber + 1) * 1024) >= file.length()) {
						// packetSize = (int) (file.length() % 1024);
						// bytes = new byte[packetSize];
						// inputS.read(bytes, 0, packetSize);
						// fileBuilder[seqNumber] = bytes;
						// packet = new Packet(bytes, -2);
						// System.out.println("Sending Packet: " +
						// Integer.toString(-2));
						// } else {
						// packetSize = 1024;
						// bytes = new byte[packetSize];
						// inputS.read(bytes, 0, packetSize);
						// fileBuilder[seqNumber] = bytes;
						// packet = new Packet(bytes, seqNumber);
						// System.out.println("Sending Packet: " +
						// Integer.toString(seqNumber));
						//
						// }

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
						if (seqNumber < numPackets - 1) {
							seqNumber++;
						}
					}

					// Get packet to resend, window isn't moving
					else {
						int sendAgain[] = window.getSlotsRemaining();
						for (int i = 0; i < sendAgain.length; i++) {
							if (sendAgain[i] != -1) {
								// bytes = fileBuilder[sendAgain[i]];

								// Last packet to send
								if (window.WindowApprove(-2)) {
									bytes = new byte[lastPacketSize];
									bytes = lastFileBytes;
									packet = new Packet(bytes, -2);
									System.out.println("Sending Packet Again: " + Integer.toString(-2) + " |Size: " + packet.getSize() + "|should be| "
											+ (lastPacketSize + 12));
								}
								// send the packet
								else {
									bytes = new byte[1024];
									bytes = fileBuilder[sendAgain[i]];
									packet = new Packet(bytes, sendAgain[i]);
									System.out.println("Sending Packet Again: " + Integer.toString(sendAgain[i]) + " |Size: " + packet.getSize());
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
							crcCounter++;
							if(crcCounter>14){
								System.out.println("Too much error, restarting");
								System.out.println("Awaiting file request");
								crcCounter=0;
								break;
							}
							continue;
						}

						String ack = String.valueOf(acknowledgement.getSeqNum());
						clientAck = acknowledgement.getSeqNum();
						System.out.println("Recieved Acknowledgement: " + ack);
						crcCounter=0;

						if (clientAck == -2) {
							clientAck = numPackets - 1;
							finish = true;
							System.out.println("Awaiting file request");
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
							System.out.println("Awaiting file request");
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
				// dataChannel.disconnect();
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	/*************************************************************************
	 * Method for comparing acknowledgement packet's CRC with a server built
	 * CRC to make sure no corruption has occured.
	 * @param footer bytes recieved and submitted for validation
	 ************************************************************************/
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


	/*************************************************************************
	 * Method for comparing acknowledgement packet's CRC with a server built
	 * CRC to make sure no corruption has occured.
	 * @param acknowledgement Packet recieved and submitted for validation
	 ************************************************************************/
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
