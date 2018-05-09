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
import java.util.ArrayList;


public class Client extends HandshakeProtocol implements Runnable{ //Represents Alice in the protocol specifics
    private int remotePort;
    private int localPort;
    private Request req;
    private Request myReq;
    private String hostName;
    private Certificate otherCertificate;
    private String recipient;

    public Client(String hostName, int remotePort,int localPort, PrivateKey myKey,String issuer, Certificate myCertificate,Certificate CACertificate,String recipient,ArrayList<Certificate> crl){
        super(myKey,issuer, myCertificate, CACertificate,crl);
        this.remotePort = remotePort;
        this.hostName = hostName;
        this.recipient=recipient;
        this.localPort = localPort;
        System.out.println("NEW CLIENT INITIATED HOSTNAME: "+hostName+"REMOTE PORT: "+remotePort+ " LOCALPORT "+localPort); /////////////////////////////////////////
    }

    public void run(){
        //while(true){
            try(
                Socket s = new Socket(hostName,remotePort);
                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(out);
                ObjectInputStream oin = new ObjectInputStream(in);
            ){
                System.out.println("Sending request");
                oout.writeObject("<REQUEST>"+issuer+"</REQUEST>");
                System.out.println("REQUEST SENT");
                otherCertificate = (Certificate)oin.readObject();
                System.out.println("CERTIFICATE RECEIVED");
                myReq = generateRequest();                
                oout.writeObject(myReq.getEncrypted(otherCertificate.getPublicKey())); // THIS PUBLIC KEY MUST BE PROVIDED BY A CERTIICATE REQ OBJECT IS NULL
                if(!getRequest(oin)){
                    System.err.println("Request corrupted the signature is not authentic");//TODO in request verify
                    return;
                }
                System.out.println("Got reply -- Sending port "+localPort);                
                sendChallenge(oout,req.getChallengeNonce(),localPort);
                if(!(boolean)receiveChallenge(oin)[0]){
                    System.err.println("Challenge not fulfilled by the other user");
                    return;
                }
                System.out.println("PROTOCOL ENDED CORRECTLY WITH: "+hostName+":"+remotePort);
                success = true;
            }catch(Exception e){
                e.printStackTrace();
            }
        //}
    }
    
    public Certificate getCertificate() {
        return otherCertificate;
    }
    
    private boolean getRequest(ObjectInputStream obj) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, CertificateException{
        this.req = Request.fromEncryptedRequest((byte[]) obj.readObject(),myKey); //first we read the length we expect LBA||nb||S(sb,LBA||nb)
        this.authKey = req.getSecretKey();
        System.out.println((crl == null));
        System.out.println((crl.contains(req.getCertificate())));
        return (req.verify(CACertificate, recipient)&& (crl==null || !crl.contains(req.getCertificate()))); 
    }
    private Request generateRequest() throws CertificateEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        this.symKey = CryptoManager.generateAES256RandomSecretKey();
        Request req = new Request(issuer,recipient,myCertificate,symKey); 
        this.myNonce =req.setRandomChallenge();
        req.sign(myKey);
        return req;
    }       
}
