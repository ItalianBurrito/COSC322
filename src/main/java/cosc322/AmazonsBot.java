/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cosc322;

//import java.awt.BorderLayout;
//import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
//import java.util.Arrays;
//import javax.swing.Box;
//import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.util.Exceptions;

/**
 *
 * @author 86696549
 */
public class AmazonsBot {
    Amazons player;

    public AmazonsBot(Amazons player){
        this.player = player;


    }



    boolean noMoves(char[][] board, char playerSymbol){
        Point myPieces[] = findPieces(playerSymbol, board);

        boolean validDir[] = new  boolean[8]; //clockwise up to left-up
        int dir[][] = { {0,1}, {1,1}, {1,0}, {1,-1}, {0,-1}, {-1,-1}, {-1,0}, {-1, 1} };
        for(int i = 0; i < 8; i++) validDir[i] = true;

        for (Point myPiece : myPieces) {
            for (int j = 0; j < 8; j++) {
                int x = myPiece.x + 1*dir[j][0];
                int y = myPiece.y + 1*dir[j][1];
                if(x < 0 || x >= board.length || y < 0 || y >= board.length || board[y][x] != BoardGameModel.POS_AVAILABLE) {
                    validDir[j] = false;
                }
            }
        }

        for(int i = 0; i < validDir.length; i++){
            if(validDir[i] == true) return true;
        }
        return false;
    }



   /*
    Uses above methods of evaluating moves but puts each possible move into a node. This allows the bot to look several turns ahead.
    */
    //TODO: finish
    Move findMoveTree() {
        Move bestMove = new Move();
        int bestScore = Integer.MIN_VALUE;
        char[][] board = player.board.gameBoard;


        System.out.println("--Starting Turn--");
        System.out.println("current score: " + HeuristicFunction.calcHeuristic(board, player));

        Node root = new Node(null, board, 0, true);
        ArrayList<Node> children = expandNode(root, player.myQueenSymb);
        System.out.println("Our Children " + children.size());

        int thereSize = 0;
        if(children.size() > 0){
            thereSize = expandNode(children.get(0), player.badQueenSymb).size();
            System.out.println("thereChildren: " + thereSize);
        }

        int depth = 2;

        if (children.size() + thereSize < 500) depth = 3;
        if (children.size() + thereSize < 180) depth = 4;
        if (children.size() + thereSize < 100) depth = 5;
        if (children.size() + thereSize < 60) depth = 6;

        int numThreads = 12;
        if(numThreads > 1){
            BuildTreeThread[] t = new BuildTreeThread[numThreads-1];
            int size = children.size() / numThreads;

            ArrayList<Node>[] workList = new ArrayList[t.length+1];

            for(int i = 0; i < t.length; i++) {
                workList[i] = new ArrayList<>();

                for(int j = i*size; j < i*size+(size-1); j++){
                    workList[i].add(children.get(j));
                }


                t[i] = new BuildTreeThread(this, workList[i], depth);
                //System.out.println("start:" + i*size + " end:" + (i*size+(size-1)));
            }

            for (BuildTreeThread t1 : t) {
                t1.start();
            }

            //System.out.println("start:" + t.length*size + " end:" + root.children.size());
            if(depth > 1){
                workList[workList.length-1] = new ArrayList<>();
                for(int i = t.length*size; i < children.size(); i++)
                    workList[workList.length-1].add(children.get(i));
                minmaxScoreNode(depth, workList[workList.length-1]);
            }

            //wait for threads to finish
            try {
                for (BuildTreeThread t1 : t) {
                    t1.join();
                }
           } catch (InterruptedException ex) {
               Exceptions.printStackTrace(ex);
           }
        }
        else{
            if(depth > 1){
                minmaxScoreNode(depth, children);
            }
        }

        System.out.println("searching tree!");

        for(Node child : children){
            if(child.score > bestScore){
                bestScore = child.score;
                bestMove = child.move;
            }
        }

        return bestMove;
    }

    void minmaxScoreNode(int maxDepth, ArrayList<Node> children){


        ArrayList<Node> stack = new ArrayList<>();

        children.forEach((node) -> {
            stack.add(node);
        });

        while(stack.size() > 0){


            //Node node = stack.get(stack.size() - 1);
            Node node = stack.remove(stack.size() - 1);



            char playerSymbol = player.myQueenSymb;
            char badSymbol = player.badQueenSymb;
            if(!node.maxNode) {
                playerSymbol = player.badQueenSymb;
                badSymbol = player.myQueenSymb;
            }

            Move move = node.iterator.getNextMove(playerSymbol, node.board);
            if(move == null){
                if(node.parent.parent == null) continue; //don't add parent node

                if(node.parent.maxNode)
                    node.parent.score = Math.max(node.parent.score, node.score);
                else{
                    node.score = Math.min(node.parent.score, node.score);
                }
                stack.add(node.parent);
                //children.add(node.parent);
            }
            else{
                char[][] rsltBoard = new char[node.board.length][node.board.length];
                for(int k = 0; k < 100; k++){ //set up board that would result from queens move
                    int y = k/10;
                    int x = k%10;
                    rsltBoard[y][x] = node.board[y][x];
                }
                rsltBoard[move.qSrc.y][move.qSrc.x] = BoardGameModel.POS_AVAILABLE;
                rsltBoard[move.qDest.y][move.qDest.x] = playerSymbol;
                rsltBoard[move.arrow.y][move.arrow.x] = BoardGameModel.POS_MARKED_ARROW;


//                if(node.maxNode){
//                    if(noMoves(rsltBoard, badSymbol)){
//                        node.score = Integer.MIN_VALUE -1;
//                        continue;
//                    }
//                }else{
//                    if(noMoves(rsltBoard, playerSymbol)){
//                        node.score = Integer.MAX_VALUE -1;
//                        continue;
//                    }
//                }



                /*
                if(noMoves(rsltBoard, badSymbol)){
                    if(node.maxNode) node.score = Integer.MAX_VALUE -1;
                    else node.score = Integer.MIN_VALUE -1;
                    continue;
                }
                else if(noMoves(rsltBoard, playerSymbol)){
                    if(node.maxNode) node.score = Integer.MIN_VALUE -1;
                    else node.score = Integer.MAX_VALUE -1;
                    continue;
                }
                */


                if(node.depth+1 == maxDepth){
                    int childScore;
                    childScore = HeuristicFunction.calcHeuristic(rsltBoard, player);
                    if(node.maxNode)
                        node.score = Math.max(childScore, node.score);
                    else{
                        node.score = Math.min(childScore, node.score);
                    }
                    stack.add(node);
                    //children.add(node);
                }
                else{
                    boolean prune = false;
//                    if(node.parent != null){
//                        if(node.parent.maxNode){
//                            if(node.parent.score > node.score && node.parent.score != Integer.MIN_VALUE && node.score != Integer.MAX_VALUE) prune = true;
//                        }
//                        else{
//                            if(node.parent.score < node.score && node.parent.score != Integer.MAX_VALUE && node.score != Integer.MIN_VALUE) prune = true;
//                        }
//                    }

                    if(prune == true){
                        stack.add(node.parent);
                        //children.add(node);
                        //System.out.println("prunned");
                    }
                    else{
                        Node child = new Node(node, rsltBoard, node.depth+1, !node.maxNode);
                        stack.add(child);
                        //children.add(child);
                    }
                }
                            /*
            System.out.println("x:" + move.qSrc.x + " y:" + move.qSrc.y);

            for(int y = 0; y < rsltBoard.length; y++){
                for(int x = 0; x < rsltBoard.length; x++)
                    System.out.print(node.board[y][x] + " ");
                System.out.println("");
            }
            System.out.println("");
            */



            }
        }
    }

    //Adds all possible moves to a node of the minmax tree
    ArrayList<Node> expandNode(Node node, char playerSymbol){
        ArrayList<Node> output = new ArrayList<>();
        Point myPiece[] = findPieces(playerSymbol, node.board);
        //Point badPiece[] = findPieces(player.badQueenSymb, node.board);

        DestList[] myQueen = new DestList[4];
        for(int i = 0; i < myQueen.length; i++) myQueen[i] = new DestList(myPiece[i], node.board);
        for(int i = 0; i < 4; i++){ //go through all players queens
            char[][] rsltBoard = new char[node.board.length][node.board.length];
            for(int j = 0; j < myQueen[i].numMoves; j++){ //go through all the moves of each queen
                for(int k = 0; k < 100; k++){ //set up board that would result from queens move
                    int y = k/10;
                    int x = k%10;
                    rsltBoard[y][x] = node.board[y][x];
                }
                rsltBoard[myQueen[i].src.y][myQueen[i].src.x] = BoardGameModel.POS_AVAILABLE;
                rsltBoard[myQueen[i].moves[j].y ][myQueen[i].moves[j].x] = playerSymbol;

                DestList arrowMoves = new DestList(myQueen[i].moves[j], rsltBoard);
                for(int k = 0; k < arrowMoves.numMoves; k++){ //try all the possible arrow positions
                    rsltBoard[arrowMoves.moves[k].y][arrowMoves.moves[k].x] = BoardGameModel.POS_MARKED_ARROW;
                    //score up the board and keep the best move
                    int score = HeuristicFunction.calcHeuristic(rsltBoard, player);

                    char[][] nodeBoard = new char[node.board.length][node.board.length];

                    for(int l = 0; l < 100; l++){
                        int y = l/10;
                        int x = l%10;
                        nodeBoard[y][x] = rsltBoard[y][x];
                    }

                    Node child = new Node(node, nodeBoard, 1, false);
                    child.score = score;
                    child.move.set(myQueen[i].src, myQueen[i].moves[j], new Point(arrowMoves.moves[k].x, arrowMoves.moves[k].y));
                    output.add(child);
                    rsltBoard[arrowMoves.moves[k].y][arrowMoves.moves[k].x] = BoardGameModel.POS_AVAILABLE;
                }
            }
        }
        return output;
    }

    //finds location of a players pieces.
    static Point[] findPieces(char type, char board[][]){
        Point pieces[] = new Point[4];
        int count = 0;
        for(int y = 0; y < board.length; y++)
            for(int x = 0; x < board.length; x++){
                if(board[y][x] == type){
                    pieces[count] = new Point(x,y);
                    count++;
                }
            }
        return pieces;
    }
}

class Node{
    int score;
    char[][] board;
    int depth;
    boolean maxNode;

    Node parent;

    Move move;

    MoveIterator iterator;


    public Node(Node parent, char[][] board, int depth, boolean maxNode){
        this.board = board;
        this.parent = parent;
        this.depth = depth;
        this.maxNode = maxNode;

        move = new Move();
        iterator = new MoveIterator();

        if(maxNode) score = Integer.MIN_VALUE;
        else score = Integer.MAX_VALUE;
    }
}

class Point{
    int x;
    int y;

    public Point(){
        x = 0; y =0;
    }
    public Point(int x, int y){
        this.x = x; this.y =y;
    }

    void set(int x, int y){
        this.x = x; this.y = y;
    }
}

class Move{
    Point qSrc, qDest, arrow;
    boolean opponent;

    public Move(){
        qSrc = new Point();
        qDest = new Point();
        arrow = new Point();
    }

    public Move(Point qSrc, Point qDest, Point arrow){
        this.qSrc = qSrc;
        this.qDest = qDest;
        this.arrow = arrow;
    }

    void set(Point qSrc, Point qDest, Point arrow){
        this.qSrc.set(qSrc.x, qSrc.y);
        this.qDest.set(qDest.x, qDest.y);
        this.arrow.set(arrow.x, arrow.y);
    }

    void copyQueen(Point qSrc, Point qDest){
        this.qSrc.set(qSrc.x, qSrc.y);
        this.qDest.set(qDest.x, qDest.y);
    }
}

class MoveIterator{
    final static int[][] DIRLIST = { {0,1}, {1,1}, {1,0}, {1,-1}, {0,-1}, {-1,-1}, {-1,0}, {-1, 1} };

    int qNum;
    int qdir;
    int qdist;

    int aNum;
    int adir;
    int adist;

    public MoveIterator(){
        qNum = 0;
        qdir = 0;
        qdist = 1;
        aNum = 0;
        adir = 0;
        adist = 1;
    }

    public Move getNextMove(char playerSymbol, char board[][]){

        Point myPiece[] = AmazonsBot.findPieces(playerSymbol, board);
        Point aDest;

        Point qPos = getQueenDest(myPiece[qNum], board);
        while(qPos == null){
            qNum++;
            qdir = 0;
            qdist = 1;
            aNum = 0;
            adir = 0;
            adist = 1;
            if(qNum >= 4) return null;
            qPos = getQueenDest(myPiece[qNum], board);
        }

        aDest = getNextArrowDest(qPos, board);
        while(aDest == null){

            //reset arrow
            aNum = 0;
            adir = 0;
            adist = 1;

            //increment to next queen's move
            qdist++;
            qPos = getQueenDest(myPiece[qNum], board);
            while(qPos == null){
                qNum++;
                qdir = 0;
                qdist = 1;

                if(qNum >= 4) return null;
                qPos = getQueenDest(myPiece[qNum], board);
            }
            aDest = getNextArrowDest(qPos, board);
        }
        return new Move(myPiece[qNum], qPos, aDest);

    }

    public Point getQueenDest(Point src, char board[][]){

        Point dest = null;
        while(dest == null){

            int x = src.x + qdist*DIRLIST[qdir][0];
            int y = src.y + qdist*DIRLIST[qdir][1];

            if(x < 0 || x >= board.length || y < 0 || y >= board.length || board[y][x] != BoardGameModel.POS_AVAILABLE) {
                qdir++;
                qdist = 1;
                if(qdir >= 8) return null; // no more moves
            }
            else{
                dest = new Point(x,y);
            }
        }
        return dest;
    }

    public Point getNextArrowDest(Point src, char board[][]){

        Point dest = null;
        while(dest == null){
            int x = src.x + adist*DIRLIST[adir][0];
            int y = src.y + adist*DIRLIST[adir][1];

            if(x < 0 || x >= board.length || y < 0 || y >= board.length || board[y][x] != BoardGameModel.POS_AVAILABLE) {
                adir++;
                adist = 1;
                if(adir >= 8) return null; // no more moves
            }
            else{
                adist++;
                dest = new Point(x,y);
            }
        }
        return dest;
    }
}


/*
 A list of all of the possible moves an object can perform.
 Can only move horizontaly, verticaly and diagionaly to an unblocked location
*/
class DestList{
    Point src;
    Point moves[];
    int numMoves;


    public DestList(Point src, char board[][]){
        moves = new Point[100]; //100 ~= max possible number moves, faster than arraylist
        numMoves = 0;
        this.src = src;

        boolean validDir[] = new  boolean[8]; //clockwise up to left-up
        int dir[][] = { {0,1}, {1,1}, {1,0}, {1,-1}, {0,-1}, {-1,-1}, {-1,0}, {-1, 1} };
        for(int i = 0; i < 8; i++) validDir[i] = true;

        for(int i = 1; i < board.length; i++){
            for(int j = 0; j < 8; j++){
                if(!validDir[j]) continue;
                int x = src.x + i*dir[j][0];
                int y = src.y + i*dir[j][1];

                if(x < 0 || x >= board.length || y < 0 || y >= board.length || board[y][x] != BoardGameModel.POS_AVAILABLE) {
                    validDir[j] = false;
                    continue;
                }

                moves[numMoves] = new Point(x,y);
                numMoves++;
            }
        }
    }
}

class BuildTreeThread extends Thread{

    ArrayList<Node> nodes;
    int depth;
    char myQueenSymb;
    char badQueenSymb;
    AmazonsBot bot;


    public BuildTreeThread(AmazonsBot bot, ArrayList<Node> nodes, int depth){
        this.nodes = nodes;
        this.depth = depth;
        this.bot = bot;
       }

    @Override
    public void run() {
        if(depth > 1){
            bot.minmaxScoreNode(depth, nodes);
        }
    }
}

class BoardPanel extends JPanel{
    private static final long serialVersionUID = 1L;
    private final int rows = 10;
    private final int cols = 10;

    int width = 500;
    int height = 500;
    int cellDim = width / 10;
    int offset = width / 20;

    int posX = -1;
    int posY = -1;

    int r = 0;
    int c = 0;

    Amazons player = null;
    private BoardGameModel gameModel = null;
    private final JLabel actionLabel;


    boolean playerAMove;

    public BoardPanel(Amazons player){
        this.player = player;
        gameModel = player.board;

        actionLabel = new JLabel("Action: Click Queen");
        this.add(actionLabel);

        //if(!game.isGamebot){
                addMouseListener(new  GameEventHandler());
        //}

    }
    public void drawBoard(){
            repaint();
    }

        // JCmoponent method
    @Override
    protected void paintComponent(Graphics gg){
            super.paintComponent(gg);
            Graphics g = (Graphics2D) gg;

            for(int i = 0; i < rows+1; i++){
                    g.drawLine(i * cellDim + offset, offset, i * cellDim + offset, rows * cellDim + offset);
                    g.drawLine(offset, i*cellDim + offset, cols * cellDim + offset, i*cellDim + offset);
            }

            for(int j = 0; j < rows; j++){
              for(int k = 0; k < cols; k++){

                            posX = k * cellDim + offset;
                            posY = j * cellDim + offset;

                            posY = (9 - j) * cellDim + offset;

                    if(gameModel.gameBoard[j][k] == BoardGameModel.POS_AVAILABLE){
                            g.clearRect(posX+1, posY+1, 49, 49);
                    }

                  switch (gameModel.gameBoard[j][k]) {
                      case BoardGameModel.POS_MARKED_BLACK:
                          g.fillOval(posX, posY, 50, 50);
                          break;
                      case BoardGameModel.POS_MARKED_ARROW:
                          g.clearRect(posX+1, posY+1, 49, 49);
                          g.drawLine(posX, posY, posX + 50, posY + 50);
                          g.drawLine(posX, posY + 50, posX + 50, posY);
                          break;
                      case BoardGameModel.POS_MARKED_WHITE:
                          g.drawOval(posX, posY, 50, 50);
                          break;
                      default:
                          break;
                  }
              }
        }
    }//method

    //JComponent method
    @Override
    public Dimension getPreferredSize() {
            return new Dimension(500,500);
     }

        /**
         * Handle mouse events
         *
         * @author yongg
         */
    public class GameEventHandler extends MouseAdapter{
        int counter = 0;

        int qrow = 0;
        int qcol = 0;

        int qfr = 0;
        int qfc = 0;

        int arow = 0;
        int acol = 0;

        @Override
    public void mousePressed(MouseEvent e) {

            int x = e.getX();
            int y = e.getY();


            if(((x - offset) < -5) || ((y - offset) < -5)){
                return;
            }

            int row = (y - offset) / cellDim;
            int col = (x - offset) / cellDim;

            switch (counter) {
                case 0:
                    actionLabel.setText("Action: Move Queen");
                    qfr = row;
                    qfc = col;
                    qfr = 9 - qfr;
                    counter++;
                    break;
                case 1:
                    actionLabel.setText("Action: Shoot Arrow");
                    qrow = row;
                    qcol = col;
                    qrow = 9 - qrow;
                    counter++;
                    break;
                case 2:
                    arow = row;
                    acol = col;
                    arow = 9 - arow;
                    counter++;
                    break;
                default:
                    break;
            }

            if(counter == 3){
              actionLabel.setText("Action: Click Queen");
              counter = 0;
              boolean validMove = gameModel.positionMarked(qrow, qcol, arow, acol, qfr, qfc, false);
              if(validMove){
                drawBoard();
                player.playerMove(qrow, qcol, arow, acol, qfr, qfc); //to server
              }

              qrow = 0;
              qcol = 0;
              arow = 0;
              acol = 0;

            }
        }
     }//end of GameEventHandler
}//end of GameBoard
