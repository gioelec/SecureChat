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
    public HandshakeProtocol(PrivateKey myKey,String issuer, Certificate myCertificate,Certificate CACertificate){
        this.myKey = myKey;
        this.issuer = issuer;
        this.myCertificate = myCertificate;
        this.CACertificate = CACertificate;
    }
    protected boolean receiveChallenge(ObjectInputStream oin){
        byte [] decryptedMsg =  SecureEndpoint.secureReceive(oin, symKey, authKey);  //data,kab,mab
        if(decryptedMsg!=null){
            int receivedNonce =MessageBuilder.toInt(MessageBuilder.extractFirstBytes(decryptedMsg, 4));
            return (myNonce == receivedNonce);      
        }
        return false;
    }
    protected boolean sendChallenge(ObjectOutputStream oout) throws NoSuchAlgorithmException, InvalidKeyException{
        byte[] myNonce = MessageBuilder.toByteArray(this.myNonce);
        byte[] msg = MessageBuilder.concatBytes(myNonce,HashManager.doMAC(myNonce, authKey, SecureEndpoint.AUTH_ALG));
        return SecureEndpoint.secureSend(msg, oout, symKey, authKey);
    }
}
