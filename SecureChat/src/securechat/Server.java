package securechat;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import cryptoutils.communication.Request;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import cryptoutils.cipherutils.CryptoManager;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import javafx.collections.ObservableList;
import securechat.model.Message;


public class Server extends HandshakeProtocol implements Runnable{ //Represents Alice in the protocol specifics
    private final int port;
    private Request req;
    private Request myReq;
    private BlockingQueue<String> sendBuffer;
    private ObservableList<Message> messageList;
    private int clientPort;
    private Receiver receiverRunnable = null;
    private Sender senderRunnable = null;
    
    public Server(int port, PrivateKey myKey,String issuer, Certificate myCertificate,Certificate CACertificate, ObservableList<Message> messageList, BlockingQueue<String> sendBuffer,ArrayList<Certificate> crl){
        super(myKey,issuer, myCertificate, CACertificate,crl);
        this.port = port;
        System.out.println(messageList==null);
        this.messageList = messageList;
        this.sendBuffer = sendBuffer;
        System.out.println("SERVER ISTANTIATED @ PORT: "+port);///////////////////////////
    }

    @Override
    public void run(){
        while(true){
            String requestIpAddress = null;
            int requestPort = -1;
            Object[] confReturn = null;
            try(
                ServerSocket ss = new ServerSocket(port, 1, InetAddress.getByName("0.0.0.0"));
               // ServerSocket ss = new ServerSocket(port);
                Socket s = ss.accept();
                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();
                ObjectInputStream oin = new ObjectInputStream(in);
                ObjectOutputStream oout = new ObjectOutputStream(out);
            ){
                System.out.println("SERVER WAITING FOR REQUEST");///////////////////////////////////////////////
                String requestHeader = (String)oin.readObject();        
                
                System.out.println("RECEIVED REQUEST");
                try {
                    SharedState.getInstance().setPendingRequest(true);
                    messageList.add(new Message(requestHeader, new Date(), "Do you want to connect? Y/N?",1));
                    System.out.println("WAITING FOR RESPONSE...");
                    boolean res = SharedState.getInstance().waitForResponse();
                    System.out.println("RESPONSE: "+res);                    
                    if(!res) continue;
                } catch(Exception e) {e.printStackTrace();}                
                // CHECK REQUEST HEADER FORMAT
                //SEND CERTIFICATE
                oout.writeObject(myCertificate);
                if(!getRequest(oin)){
                    System.err.println("Request corrupted the signature is not authentic");//TODO in request verify
                    continue;
                }

                myReq = generateRequest();
                oout.writeObject(myReq.getEncrypted(req.getPublicKey()));
                Object[] receivedChallenge = receiveChallenge(oin);
                if(!(boolean)receivedChallenge[0]){
                    System.err.println("Challenge not fulfilled by the other user");
                    throw new Exception("Challenge not fulfilled by the other user");
                }
                clientPort = (int)receivedChallenge[1];
                sendChallenge(oout,req.getChallengeNonce(),-1);
                success = true;
                requestIpAddress = s.getInetAddress().getHostAddress();
                requestPort = s.getPort();
            }catch(Exception e){
                SharedState.getInstance().protocolDone(false);
                e.printStackTrace();
                continue;
            }
            if(!success) continue;
            System.out.println("PROTOCOL ENDED CORRECTLY WITH: "+requestIpAddress+":"+clientPort);
            System.out.println("Creating messaging thread with: "+requestIpAddress+":"+clientPort+1);
            receiverRunnable = new Receiver(messageList, req.getIssuer(), authKey, symKey, port+1, requestIpAddress);
            senderRunnable = new Sender(sendBuffer, authKey, symKey, clientPort+1, requestIpAddress);
            Thread receiverThread = new Thread(receiverRunnable);
            Thread senderThread = new Thread(senderRunnable);
            senderThread.start();
            receiverThread.start();
            SharedState.getInstance().protocolDone(success);
            try {
                senderThread.join(); receiverThread.join();
            } catch(Exception e) {}
        }
    }
    private boolean getRequest(ObjectInputStream obj) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, CertificateException{
        this.req = Request.fromEncryptedRequest((byte []) obj.readObject(),myKey); //first we read the length we expect LBA||nb||S(sb,LBA||nb)
        this.symKey = req.getSecretKey();
        System.out.println((crl == null));
        System.out.println((crl.contains(req.getCertificate())));        
        return (req.verify(CACertificate, null) && (crl == null || !crl.contains(req.getCertificate()))); 
    }
    
    private Request generateRequest() throws CertificateEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        this.authKey = CryptoManager.generateAES256RandomSecretKey();
        Request req = new Request(issuer,this.req.getIssuer(),myCertificate,authKey);
        this.myNonce =req.setRandomChallenge();
        req.sign(myKey);
        return req;
    }      
    public Sender getSender(){
        return senderRunnable;
    }
    public Receiver getReceiver(){
        return receiverRunnable;
    }
}
