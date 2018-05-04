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
    private VBox wrapper = new VBox(2);
    private Label user = new Label();
    private Label date = new Label();
    private Text content = new Text();
    
    public MessageEntry() {
        user.setFont(Font.font("Verdana", FontWeight.BOLD, 12));      
        date.setFont(Font.font("Verdana", FontWeight.LIGHT,10));
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
            if(m.getType() == 1) {
                wrapper.setBackground(new Background(new BackgroundFill(Color.GREENYELLOW, CornerRadii.EMPTY, Insets.EMPTY)));
            }
        } else {
            //NOTHING
        }
        setGraphic(wrapper);
    }
}
