package securechat;

import cryptoutils.communication.SecureEndpoint;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
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
    @Override
    public void run(){
        System.out.println("Receiver.run()");
        try(
            ServerSocket ss = new ServerSocket(port);
            Socket s = ss.accept();
            InputStream in = s.getInputStream();
            ObjectInputStream oin = new ObjectInputStream(in);
        ){
            System.out.println("TRY DONE --receiver");
            String msg = null;
            while (true) {
                System.out.println("WAITING TO RECEIVE --receiver");
                msg =new String(SecureEndpoint.secureReceive(oin, symKey, authKey));
                System.out.println("MSG RECEIVED "+msg+" --receiver");
                if(msg==null)
                    System.err.println("Other user left you");
                messageList.add(new Message(sender,new Date(), msg));
                System.out.println("MSG ADDED TO THE LIST -- receiver");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }    
    }
}
