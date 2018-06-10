package securechat;


import java.io.*;
import java.net.Socket;
import cryptoutils.communication.Request;
import java.security.cert.Certificate;
import java.security.cert.*;
import java.security.*;
import javax.crypto.*;
import cryptoutils.cipherutils.CryptoManager;


public class Client extends HandshakeProtocol implements Runnable{ //Represents Alice in the protocol specifics
    private final int remotePort;
    private final int localPort;
    private Request req;
    private Request myReq;
    private final String hostName;
    private Certificate otherCertificate;
    private final String recipient;
    private Socket sRef;

    public Client(String hostName, int remotePort,int localPort, PrivateKey myKey,String issuer, Certificate myCertificate,Certificate CACertificate,String recipient,X509CRL crl){
        super(myKey,issuer, myCertificate, CACertificate,crl);
        this.remotePort = remotePort;
        this.hostName = hostName;
        this.recipient=recipient;
        this.localPort = localPort;
        System.out.println("NEW CLIENT INITIATED HOSTNAME: "+hostName+"REMOTE PORT: "+remotePort+ " LOCALPORT "+localPort); /////////////////////////////////////////
    }

    public void run(){
            SharedState.getInstance().setConnected(false);
            try(
                Socket s = new Socket(hostName,remotePort);
                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(out);
                ObjectInputStream oin = new ObjectInputStream(in);
            ){
                sRef=s;
                System.out.println("SENDING REQUEST---client");
                oout.writeObject("<Req>"+issuer+"</Req>");
                System.out.println("REQUEST SENT");
                otherCertificate = (Certificate)oin.readObject();
                System.out.println("CERTIFICATE RECEIVED---client");
                myReq = generateRequest();                
                oout.writeObject(myReq.getEncrypted(otherCertificate.getPublicKey())); // THIS PUBLIC KEY MUST BE PROVIDED BY A CERTIICATE REQ OBJECT IS NULL
                if(!getRequest(oin)){
                    System.err.println("REQUEST CORRUPTED THE SIGNATURE IS NOT AUTHENTIC--client");//TODO in request verify
                    return;
                }
                System.out.println("GOT REPLY -- Sending port "+localPort +"---client");                
                sendChallenge(oout,req.getTimestamp().toEpochMilli(),localPort);
                if(!(boolean)receiveChallenge(oin)[0]){
                    System.err.println("CHALLENGE NOT FULFILLED BY THE OTHER USER ---client");
                    return;
                }
                System.out.println("PROTOCOL ENDED CORRECTLY WITH: "+hostName+":"+remotePort+"--client");
                success = true;
                SharedState.getInstance().setConnected(true);
            }catch(Exception e){
                e.printStackTrace();
                System.out.println("EXCEPTION---client");
                SharedState.getInstance().setConnected(false);
            }
        //}
    }
    
    public Certificate getCertificate() {
        return otherCertificate;
    }
    /**
     * Gets the server request, sets the authentication key and verifies if the Certificate in the request is revoked or not 
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
        this.req = Request.fromEncryptedRequest((byte[]) obj.readObject(),myKey); //first we read the length we expect LBA||nb||S(sb,LBA||nb)
        this.authKey = req.getSecretKey();
        System.out.println((crl == null));
        if(crl != null)
            System.out.println((crl.isRevoked(req.getCertificate())));
        return (req.verify(CACertificate, recipient)&& (crl==null || !crl.isRevoked(req.getCertificate()))); 
    }
    /**
     * Generates the symmetric key, adds the timestamp and signs the request with his private key
     * @return
     * @throws CertificateEncodingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws SignatureException 
     */
    private Request generateRequest() throws CertificateEncodingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        this.symKey = CryptoManager.generateAES256RandomSecretKey();
        Request req = new Request(issuer,recipient,myCertificate,symKey); 
        this.myNonce =req.getTimestamp().toEpochMilli();
        req.sign(myKey);
        return req;
    }       
    /*public void stopClient(){
        try{sRef.close();}catch(Exception e){}
    }*/
}
