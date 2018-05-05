package securechat.frontend;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import securechat.model.Message;

public class MessageEntry extends ListCell<Message> {
    private VBox wrapper = new VBox();
    private Label user = new Label();
    private Label date = new Label();
    private Text content = new Text();
    
    public MessageEntry() {
        user.setFont(Font.font("Verdana", FontWeight.BOLD, 12));      
        date.setFont(Font.font("Verdana", FontWeight.LIGHT,9));
        date.setTextFill(Color.GRAY);
        wrapper.getChildren().addAll(user,date,content);
    }
    
    @Override
    protected void updateItem(Message m, boolean bln) {
        super.updateItem(m, bln);
        if(m!= null) {
            user.setText(m.getUser());
            date.setText(m.getDate().toString());
            content.setText(m.getContent());
            if(m.getType() != 0) {
                user.setTextFill(Color.WHITESMOKE); 
                content.setFill(Color.WHITESMOKE);
                date.setTextFill(Color.WHITESMOKE);
            } else {
                this.getStyleClass().add("message");
            }
            switch(m.getType()) {
                case 1: this.setBackground(new Background(new BackgroundFill(Color.BLUE, CornerRadii.EMPTY, Insets.EMPTY)));break;
                case 2: this.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));break;
                case 3: this.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));break;
                default: break;
            }
        } else {
            //NOTHING
        }
        setGraphic(wrapper);
    }
}
