package securechat;


import java.io.IOException;
import java.io.ObjectInputStream;
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


public class Client extends HandshakeProtocol implements Runnable{ //Represents Alice in the protocol specifics
    private final int port;
    private Request req;
    private Request myReq;
    private String hostName;
    
    /**
     * 
     * @param port
     * @param myKey
     * @param issuer
     * @param myCertificate
     * @param CACertificate 
     */
    public Client(String hostName, int port, PrivateKey myKey,String issuer, Certificate myCertificate,Certificate CACertificate){
        super(myKey,issuer, myCertificate, CACertificate);
        this.port = port;
        this.hostName = hostName;
    }

    public void run(){
        //while(true){
            try(
                Socket s = new Socket();
                //s.connect(new InetSocketAddress(hostName, port), 2000);
                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();
                ObjectInputStream oin = new ObjectInputStream(in);
                ObjectOutputStream oout = new ObjectOutputStream(out);
            ){
                System.out.println("Sending request");
                myReq = generateRequest();                
                oout.writeObject(myReq.getEncrypted(req.getPublicKey()));
                if(!getRequest(oin)){
                    System.err.println("Request corrupted the signature is not authentic");//TODO in request verify
                    return;
                }
                System.out.println("Got reply");                
                sendChallenge(oout);
                if(!receiveChallenge(oin)){
                    System.err.println("Challenge not fulfilled by the other user");
                    return;
                }
                System.out.println("PROTOCOL ENDED CORRECTLY");
                success = true;
            }catch(Exception e){
                System.err.println(e.getMessage());
            }
        //}
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
        Request req = new Request(issuer,this.req.getIssuer(),myCertificate,myKey.getEncoded());
        this.myNonce =req.setRandomChallenge();
        req.sign(myKey);
        return req;
    }       
}
