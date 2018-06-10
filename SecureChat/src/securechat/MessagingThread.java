package securechat;

public class MessagingThread {
    protected byte[] symKey;
    protected byte[] authKey;
    protected int port;
    protected String hostName;  
    public MessagingThread(byte[] authKey,byte[] symKey, int port, String hostName){
        this.authKey = authKey;
        this.symKey = symKey;
        this.port = port;
        this.hostName = hostName;     
    }  
}
