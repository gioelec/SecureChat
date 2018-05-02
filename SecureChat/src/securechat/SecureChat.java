package securechat;

import cryptoutils.cipherutils.CertificateManager;
import cryptoutils.cipherutils.CryptoManager;
import cryptoutils.communication.Request;
import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.*;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.*;
import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import securechat.frontend.MessageEntry;
import securechat.model.Message;

public class SecureChat extends Application {
    private final Label connectToLabel = new Label("NAME AND HOST TO CONNECT WITH");
    private final TextField connectToField = new TextField();
    private final Button connectButton = new Button("CONNECT");
    private final static ObservableList<Message> myL = FXCollections.observableArrayList();
    private final ListView<Message> l = new ListView<>(myL);
    private static BlockingQueue<String> sendBuffer = new LinkedBlockingQueue<>();
    private final TextArea messageArea = new TextArea();
    private final Button sendButton = new Button("SEND");
    private final Properties properties = new Properties();
    private Stage appStage;
    private PrivateKey pk;
    private Certificate myCertificate;
    private String myUsername;
    private Certificate authorityCertificate;
    
    private HBox buildConnectControls() {
        HBox connectControls = new HBox(5);
        connectControls.getChildren().addAll(connectToField,connectButton);
        HBox.setHgrow(connectButton,Priority.ALWAYS);
        HBox.setHgrow(connectToField,Priority.ALWAYS);  
        connectButton.setOnAction(ev -> {handleConnect();});
        return connectControls;
    }
    
    private void handleConnect() {
        String query = connectToField.getText();
        String queryElements[] = query.split("@");
        if(queryElements.length < 2) return;
        String username = queryElements[0];
        String hostName = queryElements[1];
        System.out.println("Starting handshake protocol with: "+hostName);
        Client connectThreadRunnable = new Client(hostName, 9999, pk, myUsername, myCertificate, authorityCertificate);
        Thread connectThread = new Thread(connectThreadRunnable);
        connectThread.start();
        try {
            connectThread.join();
            if(!connectThreadRunnable.getHandshakeResult()) return;
            byte[] macKey = connectThreadRunnable.getAuthKey();
            byte[] symKey = connectThreadRunnable.getSymKey();
            Receiver messageReceiverRunnable = new Receiver(myL, username, macKey, symKey, 9999+1, hostName);
            Sender messageSenderRunnable = new Sender(sendBuffer, macKey, symKey, 9999+1, hostName);
            Thread receiverThread = new Thread(messageReceiverRunnable);
            Thread senderThread = new Thread(messageSenderRunnable);
            receiverThread.start();
            senderThread.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private void configureTextFields() {
        connectToField.setId("usernameHostInput");
        connectToField.setPromptText("username@host"); 
        messageArea.setPromptText("Write your message here...");
        connectToField.setOnKeyTyped((KeyEvent event) -> {
            if(connectToField.getText().isEmpty()) connectButton.setDisable(true);
            else connectButton.setDisable(false);
        });
        messageArea.setOnKeyTyped((KeyEvent event) -> {
            if(messageArea.getText().isEmpty()) sendButton.setDisable(true);
            else sendButton.setDisable(false);            
        });
        messageArea.setOnKeyPressed((KeyEvent keyEvent) -> {
            KeyCombination submit = new KeyCodeCombination(KeyCode.ENTER, KeyCodeCombination.SHIFT_DOWN);
            if (submit.match(keyEvent))  {
                String text = messageArea.getText();
                if(text.isEmpty()) return;
                /*
                ////HERE WE ACTUALLY SEND THE MESSAGE OVER THE SECURE CHANNEL
                */
                myL.add(new Message(myUsername,new Date(),text));
                messageArea.clear();         
                sendButton.setDisable(true);
            }
        });
    }
    
    private void setOnSendButtonClickHandler() {
        sendButton.setDisable(true);
        sendButton.setOnAction(ev -> {
            String text = messageArea.getText();
            myL.add(new Message(myUsername,new Date(),text));
            messageArea.clear();            
            sendButton.setDisable(true);
        });
    }

    private void setOnConnectButtonClickHandler() {
        connectButton.setDisable(true);
        connectButton.setOnAction(ev -> {
            Alert a = new Alert(AlertType.INFORMATION);
            a.setTitle("Connection");
            a.setContentText("Connecting to "+connectToField.getText());
            a.showAndWait().filter(response -> response == ButtonType.OK);
        });
    }    
    
    
    
    private GridPane buildSceneGrid(HBox connectControls) {
        GridPane root = new GridPane();
        root.setPadding(new Insets(10,10,10,10));
        root.setHgap(5);
        root.setVgap(10);
        root.setAlignment(Pos.TOP_CENTER);
        root.add(connectToLabel,0,0,2,1);
        root.add(connectControls,0,1,2,1);
        root.add(l,0,2,2,1);
        root.add(messageArea,0,3,2,1);
        root.add(sendButton,0,4,2,1);
        GridPane.setHalignment(connectToLabel, HPos.CENTER);
        GridPane.setHalignment(connectControls,HPos.CENTER);
        GridPane.setHalignment(messageArea, HPos.CENTER);
        sendButton.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHalignment(sendButton, HPos.CENTER);   
        GridPane.setFillWidth(sendButton, Boolean.TRUE);
        return root;
    }
    
    private void configureListView() {
        l.setCellFactory((ListView<Message> p) -> {
            MessageEntry cell = new MessageEntry();
            return cell;
        });
    }
    
    @Override
    public void start(Stage primaryStage) {
        configureTextFields();
        setOnConnectButtonClickHandler();
        setOnSendButtonClickHandler();
        HBox connectControls = buildConnectControls();
        configureListView();
        GridPane root = buildSceneGrid(connectControls);
       
        Scene scene = new Scene(root, 320, 600);
        primaryStage.setTitle("SecureChat - Disconnected");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();
        root.requestFocus();
        l.scrollTo(myL.size()-1);
        appStage = primaryStage;
        loadCryptoSpecs();
        /* HERE SOMETHING TO START THREADS AND THINGS */
        System.out.println("Protocol listener started...");
        Server protocolServerRunnable = new Server(9999,pk,myUsername,myCertificate,authorityCertificate);
        Thread protocolServerThread = new Thread(protocolServerRunnable);
    }
    
    public static Object[] askRequestConfirmation(Request request) {
        Alert confAlert = new Alert(AlertType.CONFIRMATION);
        confAlert.setTitle("SecureChat");
        confAlert.setHeaderText("Incoming request");
        confAlert.setContentText("Request from"+request.getIssuer()+"\nCertificate released to: "+request.getCertificateSubject()+"\nCertificate released by: "+request.getCertificateIssuer()+"\nAccept?");
        Object[] returns = new Object[3];
        returns[0] = (confAlert.showAndWait().get() == ButtonType.OK);
        returns[1] = myL;
        returns[2] = sendBuffer;
        return returns;
    }
    
    private void loadCryptoSpecs() {
        try(FileInputStream fis = new FileInputStream("config.properties");) {
            properties.load(fis);
            Certificate cert = CertificateManager.readCertFromFile(properties.getProperty("myCertPath"));
            Certificate authCert = CertificateManager.readCertFromFile(properties.getProperty("authcertpath"));
            if(!CertificateManager.verifyCertificate((X509Certificate)cert, authCert)) throw new Exception("Invalid certificate");
            PrivateKey myPrivKey = CryptoManager.readRSAPrivateKeyFromPEMFile(properties.getProperty("privatekeypath"));
            PublicKey myPublicKey = cert.getPublicKey(); //GET CERTIFICATE FROM SERVER WHITH RMI
            byte[] testBytes = {0x8,0x6};
            CryptoManager.decryptRSA(CryptoManager.encryptRSA(testBytes, myPublicKey), myPrivKey); //Should raise an exception and terminate the program if wrong
            
            Alert success = new Alert(AlertType.INFORMATION);
            success.setTitle("SecureChat");
            success.setHeaderText("Application started successfully");
            success.setContentText("1)Private key loaded\n2)Authority certificate loaded\n3)Identity online");
            success.showAndWait().filter(response -> response == ButtonType.OK);
            appStage.setTitle("SecureChat - Online (Idle)");
            
            pk = myPrivKey;
            myCertificate = cert;
            authorityCertificate = authCert;
            myUsername = properties.getProperty("myname");
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
    
}
