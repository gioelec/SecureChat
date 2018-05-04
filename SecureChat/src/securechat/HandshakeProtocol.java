package securechat;

import cryptoutils.communication.SecureEndpoint;
import cryptoutils.hashutils.HashManager;
import cryptoutils.messagebuilder.MessageBuilder;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public class HandshakeProtocol {
    protected PrivateKey myKey;
    protected byte[] authKey;
    protected byte[] symKey;  //KAB
    protected String issuer;
    protected Certificate myCertificate;
    protected Certificate CACertificate;
    protected int myNonce;
    protected boolean success = false;
    public HandshakeProtocol(PrivateKey myKey,String issuer, Certificate myCertificate,Certificate CACertificate){
        this.myKey = myKey;
        this.issuer = issuer;
        this.myCertificate = myCertificate;
        this.CACertificate = CACertificate;
    }
    protected Object[] receiveChallenge(ObjectInputStream oin){
        byte [] decryptedMsg =  SecureEndpoint.secureReceive(oin, symKey, authKey);  //data,kab,mab
        Object[] returns = new Object[2];   //0 nonce verified or not, 1 port needed for the server
        if(decryptedMsg!=null){
            System.out.println("NONCE RECEIVED");
            int receivedNonce =MessageBuilder.toInt(MessageBuilder.extractFirstBytes(decryptedMsg, 4));
            System.out.println("RECEIVED:"+receivedNonce);
            System.out.println("EXPECTED:"+myNonce);
            if(decryptedMsg.length>4)
               returns[1] = MessageBuilder.extractLastBytes(decryptedMsg, 4);
            returns[0] = (myNonce == receivedNonce);      
        }
        returns[0] = false;
        return returns;
    }
    protected boolean sendChallenge(ObjectOutputStream oout,int receivedNonce,int port) throws NoSuchAlgorithmException, InvalidKeyException{
        byte[] myNonce = MessageBuilder.toByteArray(receivedNonce);
        byte[] msg = null;
        if(port!=-1){
            byte[] clientPort = MessageBuilder.toByteArray(port);
            byte[] nonce_port = MessageBuilder.concatBytes(myNonce,clientPort);
            msg = MessageBuilder.concatBytes(nonce_port,HashManager.doMAC(nonce_port, authKey, SecureEndpoint.AUTH_ALG));
        }else
            msg = MessageBuilder.concatBytes(myNonce,HashManager.doMAC(myNonce, authKey, SecureEndpoint.AUTH_ALG));
        return SecureEndpoint.secureSend(msg, oout, symKey, authKey);
    }
    public byte[] getSymKey(){
        return symKey;
    }
    public byte[] getAuthKey(){
        return authKey;
    }
    
    public boolean getHandshakeResult() {
        return success;
    }
}
