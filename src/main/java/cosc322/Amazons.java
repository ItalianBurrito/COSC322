package cosc322;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JFrame;
//import java.util.Iterator;
import ygraphs.ai.smart_fox.GameMessage;
import ygraphs.ai.smart_fox.games.AmazonsGameMessage;
import ygraphs.ai.smart_fox.games.GameClient;
import ygraphs.ai.smart_fox.games.GamePlayer;

class AmazonSettings{
    final static int ROOM_NUM = 13;
}



class AmazonsAI extends Amazons{
    AmazonsBot mybot;
    long longestTime = 0;

    public AmazonsAI(String name, String passwd) {
        super(name, passwd);
        
        mybot = new AmazonsBot(this);
    }

    @Override
    void performMove(){
        super.performMove();

        long startTime = System.nanoTime();
        Move move = mybot.findMoveTree(startTime);
        long elapsed = (System.nanoTime() - startTime)/1000000000;
        
        if(longestTime < elapsed){
            longestTime = elapsed;
        }
        
         System.out.println("Done Turn!");
        System.out.println("Elapsed:" + elapsed + " sec");
        System.out.println("Longest turn time: " + longestTime);


        boolean validMove = board.positionMarked(move.qDest.y, move.qDest.x, move.arrow.y, move.arrow.x, move.qSrc.y, move.qSrc.x, false);
            //System.out.println("src:"+move.qSrc.x +","+move.qSrc.y + " qdst:" + move.qDest.x + "," + move.qDest.y + " arr:" + move.arrow.x + "," + move.arrow.y);
        if(validMove){
            super.playerMove(move.qDest.y, move.qDest.x, move.arrow.y, move.arrow.x, move.qSrc.y, move.qSrc.x);
        }
        else System.out.println("Invalid move " + move.qSrc.x + ", " + move.qSrc.y + " | " + move.qDest.x + ", " + move.qDest.y + " | " + move.arrow.x + ", " + move.arrow.y);

        super.performMove();
    }

}

/**
 * For testing and demo purposes only. An GUI Amazon client for human players
 * @author yong.gao@ubc.ca
 */
public class Amazons extends GamePlayer{

    public BoardGameModel board = null;

    public String usrName = null;
    public boolean whitePlayer = false;

    char myQueenSymb;
    char badQueenSymb;

    private GameClient gameClient;

    private boolean gameStarted = false;

    private JFrame guiFrame;
    private BoardPanel boardPanel;

    /**
     * Constructor
     * @param name
     * @param passwd
     */
    public Amazons(String name, String passwd){
       this.usrName = name;

       board = new BoardGameModel(10, 10);
       connectToServer(name, passwd);

        guiFrame = new JFrame();

        guiFrame.setSize(800, 600);
        guiFrame.setTitle("Game of the Amazons (COSC 322, )" + this.userName());

        Container contentPane = guiFrame.getContentPane();
        contentPane.setLayout(new  BorderLayout());

        contentPane.add(Box.createVerticalGlue());

        boardPanel = new BoardPanel(this);
        contentPane.add(boardPanel,  BorderLayout.CENTER);

        guiFrame.setLocation(200, 200);
        guiFrame.setVisible(true);
        guiFrame.repaint();
        guiFrame.setLayout(null);

    }

    private void connectToServer(String name, String passwd){
    	// create a GameClient and use "this" class (a GamePlayer) as the delegate.
    	// the client will take care of the communication with the server.
    	gameClient = new GameClient(name, passwd, this);
    }

    @Override
    /**
     * Implements the abstract method defined in GamePlayer. Will be invoked by the GameClient
     * when the server says the login is successful
     */
    public void onLogin() {

	//once logged in, the gameClient will have  the names of available game rooms
	ArrayList<String> rooms = gameClient.getRoomList();

	this.gameClient.joinRoom(rooms.get(AmazonSettings.ROOM_NUM));

    }


    /**
     * Implements the abstract method defined in GamePlayer. Once the user joins a room,
     * all the game-related messages will be forwarded to this method by the GameClient.
     *
     * See GameMessage.java
     *
     * @param messageType - the type of the message
     * @param msgDetails - A HashMap info and data about a game action
     */
    public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails){

       System.out.println("-- Server Message for " + this.userName() + " --");
       System.out.println(messageType+"\n");
//       Iterator iterator = msgDetails.entrySet().iterator();
//       while (iterator.hasNext()){
//           Map.Entry pair = (Map.Entry)iterator.next();
//           System.out.println(pair.getKey().toString());
//       }

	if(messageType.equals(GameMessage.GAME_ACTION_START)){
            String ourPlayer = "";
            String theirPlayer = "";

            if(((String) msgDetails.get("player-white")).equals(this.userName())){
                whitePlayer = true;
                myQueenSymb = BoardGameModel.POS_MARKED_WHITE;
                badQueenSymb = BoardGameModel.POS_MARKED_BLACK;
                System.out.println(this.userName() + " is the white player");
            }else{
                myQueenSymb = BoardGameModel.POS_MARKED_BLACK;
                badQueenSymb = BoardGameModel.POS_MARKED_WHITE;
                System.out.println(this.userName() + " is the black player");
                performMove();
            }
            guiFrame.setTitle(msgDetails.get("player-white") + "(White Player)  VS  " + msgDetails.get("player-black") + "(Black Player)");
	}

	else if(messageType.equals(GameMessage.GAME_ACTION_MOVE)){
	    handleOpponentMove(msgDetails);
	}
        System.out.println();
	return true;
    }

    //handle the event that the opponent makes a move.
    private void handleOpponentMove(Map<String, Object> msgDetails){
	ArrayList<Integer> qcurr = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR);
	ArrayList<Integer> qnew = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.Queen_POS_NEXT);
	ArrayList<Integer> arrow = (ArrayList<Integer>) msgDetails.get(AmazonsGameMessage.ARROW_POS);

        board.positionMarked(qnew.get(0)-1, qnew.get(1)-1, arrow.get(0)-1, arrow.get(1)-1, qcurr.get(0)-1, qcurr.get(1)-1, true);
        //System.out.println(board.toString());
        performMove();
    }

    void performMove(){
        boardPanel.drawBoard();
    }


    /**
     * handle a move made by this player --- send the info to the server.
     * @param x queen row index
     * @param y queen col index
     * @param arow arrow row index
     * @param acol arrow col index
     * @param qfr queen original row
     * @param qfc queen original col
     */
    public void playerMove(int x, int y, int arow, int acol, int qfr, int qfc){

	int[] qf = new int[2];
	qf[0] = qfr+1;
	qf[1] = qfc+1;

	int[] qn = new int[2];
	qn[0] = x+1;
	qn[1] = y+1;

	int[] ar = new int[2];
	ar[0] = arow+1;
	ar[1] = acol+1;

        gameClient.sendMoveMessage(qf, qn, ar);

	//To send a move message, call this method with the required data
	//this.gameClient.sendMoveMessage(qf, qn, ar);

        //MyTimer task = new MyTimer(gameClient, qf, qn, ar);
        //Timer timer = new Timer();
        //timer.schedule(task, 10000);
        //timer.schedule(task, 10);


	//Task: Replace the above with a timed task to wait for 10 seconds befor sending the move

    }

    public boolean handleMessage(String msg) {
	System.out.println("Time Out ------ " + msg);
	return true;
    }

    @Override
    public String userName() {
	return usrName;
    }

    /**
     * Constructor
     * @param args
     */

    public static void main(String[] args) {

        AmazonsAI game01 = new AmazonsAI("Xena", args[1]);
        //AmazonsAI game02 = new AmazonsAI("larg", args[1]);
        //AmazonsAI game02 = new AmazonsAI("larg", args[1]);
	//Amazons game02 = new Amazons("player-02", "02");
        AmazonsAI game02 = new AmazonsAI("LanaKane", args[1]);

	//Amazons game = new Amazons(args[0], args[1]);
    }
}//end of Amazon
