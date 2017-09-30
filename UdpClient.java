import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

class UdpClient {
	
	public static void main(String args[]) {
		try {
			
			DatagramChannel sc = DatagramChannel.open();
			DatagramSocket ds = sc.socket();
			Console cons = System.console();
			BufferedOutputStream outputS = null;
			FileOutputStream fileSt = null;
			Window window = new Window(5,0);
			Packet packet;
			String IP = "127.0.0.1";//cons.readLine("Enter IP: ");
			String portS = "100";//cons.readLine("Enter port: ");
			int port;

			//Getting port;
			while (true) {

				if (portS.matches("[0-9]+")) {
					port = Integer.parseInt(portS);
					break;
				}
			}
			
			String m = "";

			while (true) {

				//First request
				m = null;
				m = ("test.txt");//cons.readLine("Enter file request ");
				ByteBuffer buf = ByteBuffer.wrap(m.getBytes());
				sc.send(buf, new InetSocketAddress(IP, port));
				

				
				//Recieve filesize
				ByteBuffer buf2 = ByteBuffer.allocate(1024);
				sc.receive(buf2);
				String message = new String(buf2.array()).trim();
				long fileSize = Long.parseLong(message);
				int numPackets = (int) Math.ceil((double) fileSize / 1024);
				long bytesRead = 0;
				System.out.println(String.valueOf(numPackets));
				
				//Array of bytes array to read into file successfully?
				//Possible to read as we go, but would mean only accepting the next seq packet
				//in the series, rather than any in the window.
				byte[][] fileBuilder = new byte[numPackets][1024];
				
				//Start recieving
				while(bytesRead!=fileSize){
					byte[] fileBytes = new byte[1028];
					DatagramPacket dgPacket = new DatagramPacket(fileBytes,1028);
					ds.receive(dgPacket);
					packet = new Packet(dgPacket.getData());
					int seqNum = packet.getSeqNum();
					
					//Is packet recieved in window?
					if(window.WindowApprove(seqNum)){
						fileBuilder[seqNum] = packet.getBytes();
						bytesRead += 1024;
						window.WindowSlotCheck(seqNum);
					}
					
					//Last packet handler
					else if(fileSize-bytesRead<=1024&&seqNum==-1){
						fileBuilder[numPackets-1] = packet.getBytes();
						bytesRead = fileSize;
					}
					
					//Check slots for recieved items
					window.WindowCleaner();
					byte[] sendAckBytes = Integer.toString(seqNum).getBytes();
					int sendAckBytesLen = sendAckBytes.length;
					System.out.print(String.valueOf(sendAckBytesLen));
					DatagramPacket ackSend = new DatagramPacket(sendAckBytes,sendAckBytes.length);
					ds.send(ackSend);
					
					
				}//Stop recieving file
				
				File file = new File("C:\\client\\test.txt");
				fileSt = new FileOutputStream(file);
				outputS = new BufferedOutputStream(fileSt);
				int k = 0;
				while(k!=fileBuilder.length){
					outputS.write(fileBuilder[k]);
					k++;
				}
				
				fileSt.close();
				outputS.close();
				
				

				//System.out.println(message2);

			}

			//sc.close();
		} catch (

		IOException e) {
			System.out.print("Got exception");
		}

	}
}
