package securechat;

import cryptoutils.cipherutils.*;
import cryptoutils.communication.TrustedPartyInterface;
import cryptoutils.messagebuilder.MessageBuilder;
import java.io.*;
import java.rmi.registry.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import securechat.frontend.MessageEntry;
import securechat.model.Message;

public class SecureChat extends Application {
    private final Label connectToLabel = new Label("NAME AND HOST TO CONNECT WITH");
    private final TextField connectToField = new TextField();
    private final Button connectButton = new Button("CONNECT");
    private final Button acceptButton = new Button("ACCEPT");
    private final Button disconnectButton = new Button("DISCONNECT");
    private final ObservableList<Message> myL = FXCollections.observableArrayList();
    private final ListView<Message> l = new ListView<>(myL);
    private static final BlockingQueue<String> sendBuffer = new LinkedBlockingQueue<>();
    private final TextField messageArea = new TextField();
    private final Button sendButton = new Button("SEND");
    private final Properties properties = new Properties();
    private Stage appStage;
    private X509CRL certificateRevocationList;
    private PrivateKey pk;
    private Certificate myCertificate;
    private String myUsername;
    private Certificate authorityCertificate;
    private int listeningPort;
    private Receiver receiverRunnable;
    private Thread senderThread;
    private Server protocolServerRunnable;
    private Thread protocolServerThread;
    private String port;
    private HBox buildConnectControls() {
        HBox connectControls = new HBox(5);
        connectControls.getChildren().addAll(connectToField,connectButton,acceptButton);
        HBox.setHgrow(connectButton,Priority.ALWAYS);
        HBox.setHgrow(connectToField,Priority.ALWAYS);  
        connectButton.setOnAction(ev -> {handleConnect();});
        acceptButton.setOnAction(ev -> {handleRequestAnswer();});
        return connectControls;
    }
    
    private void handleConnect() {
        String query = connectToField.getText();
        String queryElements[] = query.split("@");
        if(queryElements.length < 2) return;
        String username = queryElements[0];
        String hostNamePort = queryElements[1];
        String hostNameElements[] = hostNamePort.split(":");
        if(hostNameElements.length < 2) return;
        String hostName = hostNameElements[0];
        port = hostNameElements[1];
        System.out.println("Starting handshake protocol with: "+hostName+":"+port);
        Client connectThreadRunnable = new Client(hostName, Integer.parseInt(port),listeningPort, pk, myUsername, myCertificate, authorityCertificate,username,certificateRevocationList);
        Thread connectThread = new Thread(connectThreadRunnable);
        connectThread.start();
        try {
            connectThread.join(10000);
            if(!connectThreadRunnable.getHandshakeResult()) {
                connectThread.interrupt();  
                myL.add(new Message(username,new Date(),"Connection error",2));
                return;
            }
            byte[] macKey = connectThreadRunnable.getAuthKey();
            byte[] symKey = connectThreadRunnable.getSymKey();
            receiverRunnable = new Receiver(myL, username, macKey, symKey, listeningPort+1, hostName);
            Runnable senderRunnable = new Sender(sendBuffer, macKey, symKey, Integer.parseInt(port)+1, hostName);
            Thread receiverThread = new Thread(receiverRunnable);
            this.senderThread = new Thread(senderRunnable);
            myL.add(new Message(username,new Date(),"You're connected",3));
            receiverThread.start();
            sendBuffer.clear();
            System.out.println("QUEUE CLEARED--main");
            senderThread.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private void configureTextFields() {
        connectToField.setId("usernameHostInput");
        connectToField.setPromptText("username@host"); 
        messageArea.setPromptText("Write your message here...");
        messageArea.setOnKeyPressed((KeyEvent keyEvent) -> {
            if (keyEvent.getCode() == KeyCode.ENTER)
                handleMessageSend();
        });
    }
    
    private void handleRequestAnswer() {
        SharedState.getInstance().setResponse(true);
        System.out.println("Waiting for HP protocol to terminate...");
        boolean protocolResult = SharedState.getInstance().waitProtocol();
        if(protocolResult) {
            SharedState.getInstance().setConnected(true);
            myL.add(new Message("...",new Date(),"You're connected",3));
        }
        else myL.add(new Message("...",new Date(),"Connection failed",2));
        messageArea.clear();
    }
    
    private void handleMessageSend() {
        String text = messageArea.getText();
        if(text.isEmpty()) return;
        Message m = new Message(myUsername,new Date(),text);
        myL.add(m);
        sendBuffer.add(m.getContent());
        messageArea.clear();         
    }
    
    private void setOnSendButtonClickHandler() {
        sendButton.setOnAction(ev -> {handleMessageSend();});
    }
    private void setOnDisconnectButtonClickHandler() {
        disconnectButton.setOnAction(ev -> {handleDisconnect();});
    }
    

    
    private GridPane buildSceneGrid(HBox connectControls) {
        HBox bottomControls = new HBox(5);
        bottomControls.getChildren().addAll(sendButton,disconnectButton);
        sendButton.setMaxWidth(Double.MAX_VALUE);
        disconnectButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sendButton,Priority.ALWAYS);
        HBox.setHgrow(disconnectButton,Priority.ALWAYS);        
        GridPane root = new GridPane();
        root.setHgap(5);
        root.setVgap(10);
        root.setAlignment(Pos.TOP_CENTER);
        root.add(connectToLabel,0,0,2,1);
        root.add(connectControls,0,1,2,1);
        root.add(l,0,2,2,1);
        root.add(messageArea,0,3,2,1);
        root.add(bottomControls,0,4,2,1);
        GridPane.setHalignment(connectToLabel, HPos.CENTER);
        GridPane.setHalignment(connectControls,HPos.CENTER);
        GridPane.setHalignment(bottomControls, HPos.CENTER);
        GridPane.setHalignment(messageArea, HPos.CENTER);
        GridPane.setFillWidth(bottomControls, Boolean.TRUE);
        GridPane.setFillWidth(disconnectButton, Boolean.TRUE);
        return root;
    }
    
    private void configureListView() {  
        l.setCellFactory((ListView<Message> p) -> {
            MessageEntry cell = new MessageEntry();
            return cell;
        });
        myL.addListener((ListChangeListener.Change<? extends Message> c) -> {
            l.scrollTo(myL.size()-1);
        });
    }
    
    private void loadCRL(){
        try {
            Registry registry = LocateRegistry.getRegistry("localhost",9999);
            TrustedPartyInterface stub = (TrustedPartyInterface) registry.lookup("TrustedPartyInterface");
            byte[] nonce = new byte[4];            
            (new SecureRandom()).nextBytes(nonce);
            byte[] nonceCrlBytesWithSignature = stub.getCRL(nonce);    
            int signatureLength = MessageBuilder.toInt(MessageBuilder.extractFirstBytes(nonceCrlBytesWithSignature, 4));
            byte[] signature = MessageBuilder.extractRangeBytes(nonceCrlBytesWithSignature, 4, 4+signatureLength);
            byte[] noncedCrl = MessageBuilder.extractLastBytes(nonceCrlBytesWithSignature, nonceCrlBytesWithSignature.length-(4+signatureLength));
            byte[] receivedNonce = MessageBuilder.extractLastBytes(nonceCrlBytesWithSignature, 4);
            byte[] crl = MessageBuilder.extractFirstBytes(noncedCrl, noncedCrl.length-4);
            System.out.println(new String(crl));
            System.out.println("SENDED NONCE: "+MessageBuilder.toInt(nonce));
            System.out.println("RECEIVED NONCE: "+MessageBuilder.toInt(receivedNonce));
            if(MessageBuilder.toInt(nonce) != MessageBuilder.toInt(receivedNonce)) {System.out.println("DIFFERENT NONCE RECEIVED"); System.exit(-1);}  
            if(!SignatureManager.verify(noncedCrl, signature, "SHA256withRSA", authorityCertificate)) {System.out.println("SIGNATURE NOT VERIFIED"); System.exit(-1);}
            InputStream is = new ByteArrayInputStream(crl);
            CertificateFactory cf = CertificateFactory.getInstance(("X.509"));
            certificateRevocationList = (X509CRL) cf.generateCRL(is);
            try {
                certificateRevocationList.verify(authorityCertificate.getPublicKey());
            } catch(Exception e) {System.out.println("CRL NOT VERIFIED"); System.exit(-1);}
        } catch(Exception e) {e.printStackTrace();}
    }
    
    private static void dummy(Thread t, Throwable e) {
        System.out.println("...");
    }
    
    @Override
    public void start(Stage primaryStage) {
        sendButton.disableProperty().bind(SharedState.getInstance().sendBinding.or(messageArea.textProperty().isEmpty()));
        disconnectButton.disableProperty().bind(SharedState.getInstance().disconnectBinding);        
        connectButton.disableProperty().bind(SharedState.getInstance().connectBinding.or(connectToField.textProperty().isEmpty()));        
        acceptButton.disableProperty().bind(SharedState.getInstance().acceptBinding);
        messageArea.disableProperty().bind(SharedState.getInstance().sendBinding);
        Thread.setDefaultUncaughtExceptionHandler(SecureChat::dummy);
        configureTextFields();
        setOnSendButtonClickHandler();
        setOnDisconnectButtonClickHandler();
        HBox connectControls = buildConnectControls();
        configureListView();
        GridPane root = buildSceneGrid(connectControls);
        Scene scene = new Scene(root, 380, 550);
        primaryStage.setTitle("SecureChat - Disconnected");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
        root.requestFocus();
        appStage = primaryStage;
        loadCryptoSpecs();
        loadCRL();        
        System.out.println("Protocol listener started...");
        protocolServerRunnable = new Server(listeningPort,pk,myUsername,myCertificate,authorityCertificate,myL,sendBuffer,certificateRevocationList);
        protocolServerThread = new Thread(protocolServerRunnable);
        protocolServerThread.start();
        primaryStage.setOnCloseRequest(ev -> {
            handleDisconnect();
            protocolServerThread.interrupt();
            protocolServerRunnable.stopProtocolServer();
            Platform.exit();
            System.exit(1);
        });
    }
    
    
    private void loadCryptoSpecs() {
        try(FileInputStream fis = new FileInputStream("config.properties");) {
            properties.load(fis);
            myCertificate = CertificateManager.readCertFromFile(properties.getProperty("myCertPath"));
            if(myCertificate == null) throw new Exception("Certificate not found");
            Certificate authCert = CertificateManager.readCertFromFile(properties.getProperty("authcertpath"));
            if(!CertificateManager.verifyCertificate((X509Certificate)authCert, authCert)) throw new Exception("Invalid authority certificate");            
            if(!CertificateManager.verifyCertificate((X509Certificate)myCertificate, authCert)) throw new Exception("Invalid certificate");
            PrivateKey myPrivKey = CryptoManager.readRSAPrivateKeyFromPEMFile(properties.getProperty("privatekeypath"));
            PublicKey myPublicKey = myCertificate.getPublicKey(); 
            byte[] testBytes = {0x8,0x6};
            CryptoManager.decryptRSA(CryptoManager.encryptRSA(testBytes, myPublicKey), myPrivKey); 
            pk = myPrivKey;
            authorityCertificate = authCert;
            myUsername = properties.getProperty("myname");
            appStage.setTitle(myUsername + " - Online (Idle)");
            listeningPort = Integer.parseInt(properties.getProperty("defaultProtocolPort"));
        } catch(Exception e) {
            Alert error = new Alert(AlertType.ERROR);
            error.setTitle("SecureChat");
            error.setHeaderText("Application cannot be loaded correctly");
            error.setContentText("Message: "+e.getMessage());
            error.showAndWait().filter(response -> response == ButtonType.OK);
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void handleDisconnect() {
        disconnectButton.disableProperty();
        Receiver rt;
        Thread st;
        rt=(this.receiverRunnable==null)?protocolServerRunnable.getReceiver():receiverRunnable;
        st=(this.senderThread==null)?protocolServerRunnable.getSender():senderThread;
        if(rt == null || st == null) return;
        if(st.isAlive()){
            st.interrupt();
            System.out.println("ADDING INT TO THE BLOCKINGQUEUE--main");
            sendBuffer.add("int");
        }else
            System.out.println("STOP IT IS ALREADY DEAD----main");
        rt.stopReceiver();
        sendBuffer.clear();
        SharedState.getInstance().setConnected(false);
    }
    
}
