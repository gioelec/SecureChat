/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package securechat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Federico Rossi
 */
public class SharedState {
    private boolean requestResponse = false;
    private boolean responseAvailable = false;
    private AtomicBoolean pendingRequest = new AtomicBoolean(false);
    private Lock t = new ReentrantLock();
    private Condition responseAvailableCondition = t.newCondition();
    private static SharedState _instance;
    
    
    private SharedState() {
        //TODO
    }
    
    public static SharedState getInstance() {
        if(_instance == null) _instance = new SharedState();
        return _instance;
    }
    
    public boolean waitForResponse() {
        t.lock();
        try {
            while(!responseAvailable) {responseAvailableCondition.await();}
            responseAvailable = false;
        } catch(Exception e) {e.printStackTrace(); return false;}
        finally {t.unlock();}
        return requestResponse;
    }
    
    public void setResponse(boolean response) {
        t.lock();
        try {
            responseAvailable=true;
            requestResponse=response;
            System.out.println("STATE: Response set, now signalling");
            responseAvailableCondition.signal();
        } catch(Exception e) {e.printStackTrace();}
        finally {t.unlock();}
    }
    
    public boolean isRequestPending() {
        return pendingRequest.getAndSet(false);
    }
            
    
    
}
