package securechat;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import cryptoutils.communication.SecureEndpoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
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
        while (true) { 
            try (Socket s = new Socket(hostName,port);
                OutputStream out = s.getOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(out);)
            {
                msg = queue.take();
                SecureEndpoint.secureSend(msg.getBytes(), oout, symKey, authKey);
            } catch (Exception ex) {
                ex.printStackTrace();
            }   
        }
        
    }
    
}
