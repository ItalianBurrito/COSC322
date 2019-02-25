
package cosc322;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import ygraphs.ai.smart_fox.GameMessage;
import ygraphs.ai.smart_fox.games.AmazonsGameMessage;
import ygraphs.ai.smart_fox.games.GameClient;
import ygraphs.ai.smart_fox.games.GamePlayer;

/**
 * An example showing how to implement a GamePlayer 
 * @author yong.gao@ubc.ca
 */
public class COSC322Test extends GamePlayer{

    private GameClient gameClient;
	
    private String userName = null;
 
	
    /**
     * The main method
     * @param args for name and passwd (current, any string would work)
     *//*
    public static void main(String[] args) {				 
	COSC322Test player_01 = new COSC322Test(args[0], args[1]);  		 
    }
	*/
    /**
     * Any name and passwd 
     * @param userName
     * @param passwd
     */
    public COSC322Test(String userName, String passwd) {
	this.userName = userName;
	gameClient = new GameClient(userName, passwd, this);	 
    }
 
    @Override
    public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
	//This method will be called by the GameClient when it receives a game-related message
	//from the server.
	
       System.out.println("-- Server Message --");
       System.out.println(messageType);        
       Iterator iterator = msgDetails.entrySet().iterator();
       while (iterator.hasNext()){
           Map.Entry pair = (Map.Entry)iterator.next();
           System.out.println(pair.getKey().toString());           
       }
        
	//For a detailed description of the message types and format, 
	//see the method GamePlayer.handleGameMessage() in the game-client-api document. 
	return true;
    }

    @Override
    public void onLogin() {
	System.out.println("Congratualations!!! "
	    + "I am called because the server indicated that the login is successfully");
	System.out.println("The next step is to find a room and join it: "
	    + "the gameClient instance created in my constructor knows how!");
        
        ArrayList<String> rooms = this.gameClient.getRoomList();        
        this.gameClient.joinRoom(rooms.get(0));
        
        
        
        
	//this.gameClient.logout();
    }
    
    @Override
    public String userName() {
	return userName;
    }
}//end of class
