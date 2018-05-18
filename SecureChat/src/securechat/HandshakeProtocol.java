package securechat;

import cryptoutils.communication.SecureEndpoint;
import cryptoutils.messagebuilder.MessageBuilder;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.util.ArrayList;

public class HandshakeProtocol {
    protected PrivateKey myKey;
    protected byte[] authKey;
    protected byte[] symKey;  //KAB
    protected String issuer;
    protected Certificate myCertificate;
    protected Certificate CACertificate;
    protected long myNonce;
    protected boolean success = false;
    protected X509CRL crl;

    public HandshakeProtocol(PrivateKey myKey,String issuer, Certificate myCertificate,Certificate CACertificate,X509CRL crl){
        this.myKey = myKey;
        this.issuer = issuer;
        this.myCertificate = myCertificate;
        this.CACertificate = CACertificate;
        this.crl = crl;
    }
    protected Object[] receiveChallenge(ObjectInputStream oin){
        byte [] decryptedMsg =  SecureEndpoint.secureReceive(oin, symKey, authKey);  //data,kab,mab
        Object[] returns = new Object[2];   //0 nonce verified or not, 1 port needed for the server
        if(decryptedMsg!=null){
            System.out.println("NONCE RECEIVED");
            long receivedNonce =MessageBuilder.toLong(MessageBuilder.extractFirstBytes(decryptedMsg, 8));
            System.out.println("RECEIVED:"+receivedNonce);
            System.out.println("EXPECTED:"+myNonce);
            if(decryptedMsg.length>4){
               returns[1] = MessageBuilder.toInt(MessageBuilder.extractLastBytes(decryptedMsg, 4));
               System.out.println("CLIENT PORT "+returns[1]);
            }
            returns[0] = (myNonce == receivedNonce);     
        }else
            returns[0] = false;
        return returns;
    }
    protected boolean sendChallenge(ObjectOutputStream oout,long receivedNonce,int port) throws NoSuchAlgorithmException, InvalidKeyException{
        System.out.println("PORT TO SEND: "+port);
        byte[] myNonce = MessageBuilder.toByteArray(receivedNonce);
        byte[] msg;
        if(port!=-1){
            byte[] clientPort = MessageBuilder.toByteArray(port);
            msg = MessageBuilder.concatBytes(myNonce,clientPort);
        }else
            msg = myNonce;
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
