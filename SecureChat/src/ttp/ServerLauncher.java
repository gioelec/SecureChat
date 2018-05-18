package ttp;
import cryptoutils.cipherutils.CertificateManager;
import cryptoutils.communication.TrustedPartyRMIServer;
import cryptoutils.communication.TrustedPartyInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

//if cryptoutils CHANGED REBUILD WITH THE HAMMER
//F9 TO COMPILE
//MAIUSC+F6 TO LAUNCH
public class ServerLauncher {
    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", "0.0.0.0");            
            TrustedPartyRMIServer srv = new TrustedPartyRMIServer("auhtority.cer", "key.pem","crl.pem");
            srv.addToCRL(CertificateManager.readCertFromFile("gioele.crt"));
            TrustedPartyInterface stub = (TrustedPartyInterface) UnicastRemoteObject.exportObject(srv, 9999);
            Registry registry = LocateRegistry.createRegistry(9999);
            registry.bind("TrustedPartyInterface",stub);
            System.out.println("Server is ready...");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
}
