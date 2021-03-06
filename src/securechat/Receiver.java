package securechat;

import cryptoutils.communication.SecureEndpoint;
import java.io.*;
import java.net.*;
import java.util.Date;
import javafx.collections.ObservableList;
import securechat.model.Message;

public class Receiver extends MessagingThread implements Runnable {
    private final ObservableList<Message> messageList;
    private final String sender;
    private ServerSocket ssRef;
    private Socket sRef;
    public Receiver(ObservableList<Message> messageList,String sender,byte[] authKey, byte[] symKey, int port, String hostName) {
        super(authKey, symKey, port, hostName);
        this.messageList = messageList;
        this.sender = sender;
    }

    public void stopReceiver() {
        try {
            sRef.close();
            ssRef.close();
        } catch(Exception e) {}
    }
    
    @Override
    public void run(){
        System.out.println("RECEIVER STARTED: "+hostName+":"+port);
        try(
            ServerSocket ss = new ServerSocket(port);
            Socket s = ss.accept();
            InputStream in = s.getInputStream();
            ObjectInputStream oin = new ObjectInputStream(in);
        ){
            ssRef = ss; sRef = s;
            System.out.println("TRY DONE -- receiver");
            String msg = null;
            byte[] rcv = null;
            while (true) {
                System.out.println("WAITING TO RECEIVE -- receiver");
                rcv = SecureEndpoint.secureReceive(oin, symKey, authKey);
                if(rcv==null){
                    System.err.println("OTHER USER LEFT YOU--receiver");
                    messageList.add(new Message(sender,new Date(),"Connection closed",2));
                    SharedState.getInstance().setConnected(false);                    
                    return;
                }
                msg = new String(rcv);
                System.out.println("MSG RECEIVED "+msg+" -- receiver");
                messageList.add(new Message(sender,new Date(), msg));
                System.out.println("MSG ADDED TO THE LIST -- receiver");
            }
        }catch (Exception ex) {
            System.out.println("RECEIVER STOPPED---receiver");
            System.out.println("RECEIVER EXCEPTION -- " + ex.getMessage());
        }    
        SharedState.getInstance().setConnected(false);        
    }
}
