package securechat;

import java.util.concurrent.BlockingQueue;
import cryptoutils.communication.SecureEndpoint;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Sender extends MessagingThread implements Runnable {
    private BlockingQueue<String> queue;
    public Sender(BlockingQueue<String> queue,byte[] authKey,byte[] symKey, int port, String hostName){
        super(authKey, symKey, port, hostName);
        this.queue = queue;
    }
    public void run(){
        String msg;
        System.out.println("STARTED SENDER RUN");
        while (true) { 
            try (Socket s = new Socket(hostName,port);
                OutputStream out = s.getOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(out);)
            {
                System.out.println("WAITING TO GET MSG FROM QUEUE--sender"); 
                msg = queue.take();
                System.out.println("MESSAGE TAKEN--sender");
                SecureEndpoint.secureSend(msg.getBytes(), oout, symKey, authKey);
                System.out.println("MESSAGE SENT---sender");
            } catch (Exception ex) {
                ex.printStackTrace();
                break;   
            }   
        }
        
    }
    
}
