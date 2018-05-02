package securechat;

import cryptoutils.cipherutils.CertificateManager;
import cryptoutils.cipherutils.CryptoManager;
import cryptoutils.communication.TrustedPartyInterface;
import java.io.FileInputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Properties;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Callback;
import securechat.frontend.MessageEntry;
import securechat.model.Message;

public class SecureChat extends Application {
    private final Label connectToLabel = new Label("NAME AND HOST TO CONNECT WITH");
    private final TextField connectToField = new TextField();
    private final Button connectButton = new Button("CONNECT");
    private final ObservableList<Message> myL = FXCollections.observableArrayList();
    private final ListView<Message> l = new ListView<>(myL);
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
        return connectControls;
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
        l.setCellFactory(new Callback<ListView<Message>, ListCell<Message>>(){
            @Override
            public ListCell<Message> call(ListView<Message> p) {
                MessageEntry cell = new MessageEntry();
                return cell;
            }
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
    }
    
    private void loadCryptoSpecs() {
        try(FileInputStream fis = new FileInputStream("config.properties");) {
            properties.load(fis);
            Registry ttpRegistry = LocateRegistry.getRegistry(9999);            
            String me = properties.getProperty("myname");
            TrustedPartyInterface tpi = (TrustedPartyInterface)ttpRegistry.lookup("TrustedPartyInterface");      
            Certificate cert = tpi.getUserCertificate(me);
            Certificate authCert = CertificateManager.readCertFromFile(properties.getProperty("authcertpath"));
            if(cert == null) throw new Exception("Invalid username");
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
            myUsername = me;
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
