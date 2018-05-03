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
    protected boolean receiveChallenge(ObjectInputStream oin){
        byte [] decryptedMsg =  SecureEndpoint.secureReceive(oin, symKey, authKey);  //data,kab,mab
        
        if(decryptedMsg!=null){
            System.out.println("NONCE RECEIVED");
            int receivedNonce =MessageBuilder.toInt(decryptedMsg);
            System.out.println("RECEIVED:"+receivedNonce);
            System.out.println("EXPCETED:"+myNonce);
            return (myNonce == receivedNonce);      
        }
        return false;
    }
    protected boolean sendChallenge(ObjectOutputStream oout,int receivedNonce) throws NoSuchAlgorithmException, InvalidKeyException{
        byte[] myNonce = MessageBuilder.toByteArray(receivedNonce);
        byte[] msg = MessageBuilder.concatBytes(myNonce,HashManager.doMAC(myNonce, authKey, SecureEndpoint.AUTH_ALG));
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
