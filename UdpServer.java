import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

class UdpServer {
	public static void main(String args[]) {
		File file = null;
		byte[] bytes = null;
		byte[] clientBytes;
		BufferedInputStream inputS;
		FileInputStream fileSt;
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
				String m = cons.readLine("Enter port number: ");
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
				// SocketChannel sc = c.accept();
				int fileSize = 0;
				//buffer to get client's command
				ByteBuffer messageBuffer = ByteBuffer.allocate(1028);
				//buffer for file handling
				ByteBuffer fileBuffer = ByteBuffer.allocate(1028);
				//get address of client
				SocketAddress resend = c.receive(messageBuffer);
				//Decode message
				String message2 = new String(messageBuffer.array()).trim();
				System.out.println("Client Request: " + message2);
				
				//No input handler?
				if(message2.equals(null)||message2.equals("")){
					
				}
				
				//The file sending else
				else{
					
					try {
						file = new File("E:\\server\\" + message2);
					}
		
					catch(Exception e){
						System.out.print("No such file");
						
					}
					

					
				}//Got file
				int numPackets = (int) Math.ceil((double)file.length()/1024);
				window = new Window(5,numPackets);
				int seqNumber = 0;
				
				
				//File seding loop
				while(true){
					if(window.WindowApprove(seqNumber)){
						bytes = new byte[1024];
						fileSt = new FileInputStream(file);
						inputS = new BufferedInputStream(fileSt);
						inputS.read(bytes, seqNumber*1024, 1024*(seqNumber+1));
						packet = new Packet(bytes,seqNumber);
						dgPacket = new DatagramPacket(packet.getBytes(),1028,resend);
						ds.send(dgPacket);
						seqNumber++;
					}
					
					clientBytes = new byte[4];
					clientPacket = new DatagramPacket(clientBytes, 4);
					ds.receive(clientPacket);
					int clientAck = ByteBuffer.wrap(clientBytes).getInt();
					System.out.println("Recieved Acknowledgement: " + String.valueOf(clientAck));
					window.WindowSlotCheck(clientAck);
					window.WindowCleaner();
					
					
				}
				

					
								
			}

		} catch (IOException e) {
			System.out.println("whoops");
		}
	}
}
