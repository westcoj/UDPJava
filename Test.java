/**
 * Created by pieterholleman on 10/14/17.
 **/


import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Test {

    public static void main(String[] args) {

      UDPserverThread server = new UDPserverThread();
      server.start();

      UDPclientThread client = new UDPclientThread(2000, "127.0.0.1");
      client.start();

    }

    private static class UDPclientThread extends Thread{

        UdpClientV2 client;

        UDPclientThread(int port, String ip){
            client = new UdpClientV2(port, ip);

        }

        public void run(){
            try{
                client.requestFile();
            } catch(Exception e){
                e.printStackTrace();
            }
        }

    }

    private static class UDPserverThread extends Thread{

        UdpServerV2 server;

        UDPserverThread(){
            try {
                server = new UdpServerV2();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run(){
            server.connect(2000);
            server.awaitRequest();
        }
    }
}
