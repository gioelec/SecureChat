/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package securechat.model;

import java.util.Date;

/**
 *
 * @author Federico Rossi
 */
public class Message {
    private String user;
    private Date date;
    private String content;
    
    public Message(String u,Date d,String c) {
        user = u;
        date = d;
        content = c;
    }
    
    public String getUser() {return user;}
    public Date getDate() {return date;}
    public String getContent() {return content;}
    
    public void setUser(String u) {user = u;};
    public void setDate(Date d) {date = d;};
    public void setContent(String c) {content = c;};
}
