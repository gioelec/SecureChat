
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import javax.crypto.*;


public class Server implements Runnable { //Represents Alice in the protocol specifics
    private final int port;
    private Request req;
    private Request myReq;
    private PrivateKey myKey;
    private String issuer;
    private Certificate myCertificate;
    private Certificate CACertificate;
    
    /**
     * 
     * @param port
     * @param myKey
     * @param issuer
     * @param myCertificate
     * @param CACertificate 
     */
    public Server(int port, PrivateKey myKey,String issuer, Certificate myCertificate,Certificate CACertificate){
        this.port = port;
        this.myKey = myKey;
        this.issuer = issuer;
        this.myCertificate = myCertificate;
        this.CACertificate = CACertificate;
    }

    public void run(){
        while(true){
            try(
                ServerSocket ss = new ServerSocket(port);
                Socket s = ss.accept();
                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();
                ObjectInputStream oin = new ObjectInputStream(in);
                ObjectOutputStream oout = new ObjectOutputStream(out);
            ){
                if(!getRequest(oin)){
                    System.err.println("Request corrupted the signature is not authentic");//TODO in request verify
                    continue;
                }
                myReq = generateRequest();
                oout.writeObject(myReq.getEncrypted(req.getPublicKey()));
                
            }catch(Exception e){
                System.err.println(e.getMessage());
            }
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
        Request req = new Request(issuer,this.req.getIssuer(),myCertificate,myKey.getEncoded());
        req.setRandomChallenge();
        req.sign(myKey);
        return req;
    }   
    private byte[] receiveChallenge(){
        //TODO
        return null;
    }
        
    
}
