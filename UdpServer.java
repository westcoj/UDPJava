import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.Files;

class UdpServer {
	public static void main(String args[]) {
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
					}

					catch (Exception e) {
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
						}

						else {
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
					try{
						ds.receive(clientPacket);
					}
					
					catch(SocketTimeoutException e){
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
