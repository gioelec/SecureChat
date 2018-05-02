package securechat;

import cryptoutils.communication.SecureEndpoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import securechat.model.Message;

public class Receiver extends MessagingThread implements Runnable {
    private ObservableList<Message> messageList;
    private String sender;
    public Receiver(ObservableList<Message> messageList,String sender,byte[] authKey, byte[] symKey, int port, String hostName) {
        super(authKey, symKey, port, hostName);
        this.messageList = messageList;
        this.sender = sender;
    }
    public void run(){
        try(
            ServerSocket ss = new ServerSocket(port);
            Socket s = ss.accept();
            InputStream in = s.getInputStream();
            ObjectInputStream oin = new ObjectInputStream(in);
        ){
            String msg = null;
            while (true) {
                msg =new String(SecureEndpoint.secureReceive(oin, symKey, authKey));
                if(msg==null)
                    System.err.println("Other user left you");
                messageList.add(new Message(sender,new Date(), msg));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }    
    }
}
