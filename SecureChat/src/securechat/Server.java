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


public class Server extends HandshakeProtocol implements Runnable{ //Represents Alice in the protocol specifics
    private final int port;
    private Request req;
    private Request myReq;
    /**
     * 
     * @param port
     * @param myKey
     * @param issuer
     * @param myCertificate
     * @param CACertificate 
     */
    public Server(int port, PrivateKey myKey,String issuer, Certificate myCertificate,Certificate CACertificate){
        super(myKey,issuer, myCertificate, CACertificate);
        this.port = port;
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
                // CHECK REQUEST HEADER FORMAT
                //SEND CERTIFICATE
                oout.writeObject(myCertificate);
                if(!getRequest(oin)){
                    System.err.println("Request corrupted the signature is not authentic");//TODO in request verify
                    continue;
                }
                //confReturn = SecureChat.askRequestConfirmation(req);
                //if(!(boolean)confReturn[0]) continue;
                myReq = generateRequest();
                oout.writeObject(myReq.getEncrypted(req.getPublicKey()));
                if(!receiveChallenge(oin)){
                    System.err.println("Challenge not fulfilled by the other user");
                    continue;
                }
                sendChallenge(oout,req.getChallengeNonce());
                success = true;
                requestIpAddress = s.getInetAddress().getHostAddress();
                requestPort = s.getPort();
            }catch(Exception e){
                e.printStackTrace();
            }
            if(!success) continue;
            System.out.println("PROTOCOL ENDED CORRECTLY WITH: "+requestIpAddress+":"+requestPort);
            System.out.println("Creating messaging thread with: "+requestIpAddress+":"+requestPort+1);
       //     Receiver messageReceiverRunnable = new Receiver((ObservableList<Message>) confReturn[1], req.getIssuer(), authKey, symKey, 9999+1, requestIpAddress);
          //  Sender messageSenderRunnable = new Sender((BlockingQueue<String>) confReturn[2], authKey, symKey, 9999+1, requestIpAddress);
           // Thread receiverThread = new Thread(messageReceiverRunnable);
           // Thread senderThread = new Thread(messageSenderRunnable);
            //senderThread.start();
           // receiverThread.start();
            try {
                //senderThread.join(); receiverThread.join();
            } catch(Exception e) {}
        }
    }
    
    /**
     * 
     * @param obj
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws CertificateException 
     */
    private boolean getRequest(ObjectInputStream obj) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, CertificateException{
        this.req = Request.fromEncryptedRequest((byte []) obj.readObject(),myKey); //first we read the length we expect LBA||nb||S(sb,LBA||nb)
        this.symKey = req.getSecretKey();
        return (req.verifySignature() && req.verifyCertificate(CACertificate)); //TODO verify name
    }
    
    /**
     * 
     * @return
     * @throws CertificateEncodingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException 
     */
    private Request generateRequest() throws CertificateEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        this.authKey = CryptoManager.generateAES256RandomSecretKey();
        Request req = new Request(issuer,this.req.getIssuer(),myCertificate,authKey);
        this.myNonce =req.setRandomChallenge();
        req.sign(myKey);
        return req;
    }       
}
