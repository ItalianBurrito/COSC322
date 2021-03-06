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
//import java.util.List;
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
    long star;

    public AmazonsBot(Amazons player){
        this.player = player;


    }

   /*
    Uses above methods of evaluating moves but puts each possible move into a node. This allows the bot to look several turns ahead.
    */
    //TODO: finish
    Move findMoveTree(Long start) {
        Move bestMove = new Move();
        star = start;
        int bestScore = Integer.MIN_VALUE;
        char[][] board = player.board.gameBoard;


        System.out.println("-- Starting Turn --");
        System.out.println("Current Score: " + HeuristicFunction.calcHeuristic(board, player));

        Node root = new Node(null, board, 0, true);
        ArrayList<Node> children = expandNode(root, player.myQueenSymb);


        /*
        if(children.size() < 5){
            for(Node child : children){
                int score = HeuristicFunction.calcHeuristic(child.board, player);
            }
        }
        */



        int thereSize = 0;
        if(children.size() > 0){
            bestMove = children.get(0).move;
            thereSize = expandNode(children.get(0), player.badQueenSymb).size();
        }
        System.out.println("Our Children: " + children.size() + " Their Children: " + thereSize + " Total size: " + (children.size() + thereSize));

        int depth = 2;

        /*
        if (children.size() + thereSize < 500) depth = 3;
        if (children.size() + thereSize < 130) depth = 4;
        if (children.size() + thereSize < 100) depth = 5;
        if (children.size() + thereSize < 60) depth = 6;
        */


        if (children.size() + thereSize < 1100) depth = 3;
        if (children.size() + thereSize < 280) depth = 4;
        if (children.size() + thereSize < 100) depth = 5;
        if (children.size() + thereSize < 55) depth = 6;
        if (children.size() + thereSize < 35) depth = 8;


        System.out.println("Depth: " + depth);
        //HeuristicFunction.useZones = false;

        int numThreads = 8;
        if(numThreads > 1){
            BuildTreeThread[] t = new BuildTreeThread[numThreads-1];
            int size = children.size() / numThreads;

            ArrayList<Node>[] workList = new ArrayList[t.length+1];

            for(int i = 0; i < t.length; i++) {
                workList[i] = new ArrayList<>();

                for(int j = i*size; j < i*size+size; j++){
                    workList[i].add(children.get(j));
                }


                t[i] = new BuildTreeThread(this, workList[i], depth);
                //System.out.println("start:" + i*size + " end:" + (i*size+size));
            }

            for (BuildTreeThread t1 : t) {
                t1.start();
            }

            //System.out.println("start:" + t.length*size + " end:" + children.size());
            if(depth > 1){
                workList[workList.length-1] = new ArrayList<>();
                //System.out.println("start:" + t.length*size + " end:" + children.size());
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

        for(Node child : children){
            if(child.score > bestScore){
                if(child.score == Integer.MAX_VALUE) continue;
                bestScore = child.score;
                bestMove = child.move;
            }
        }
        System.out.println("Best Score:" + bestScore);

        return bestMove;
    }

    void minmaxScoreNode(int maxDepth, ArrayList<Node> children){

        ArrayList<Node> stack = new ArrayList<>();

        children.forEach((node) -> {
            stack.add(node);
        });

        //while there is still nodes left to expand
        while(stack.size() > 0){

            //get next node to expand
            Node node = stack.remove(stack.size() - 1);

            boolean prune = false;
            if(node.parent != null && node.score != Integer.MIN_VALUE && node.score != Integer.MAX_VALUE){
                if(node.maxNode){
                    if(node.score > node.parent.score && node.parent.score != Integer.MIN_VALUE && node.parent.score != Integer.MAX_VALUE) prune = true;
                }
                else{
                    if(node.score < node.parent.score && node.parent.score != Integer.MIN_VALUE && node.parent.score != Integer.MAX_VALUE) prune = true;
                }
            }

            if(prune == true){
                if(node.parent.parent != null) stack.add(node.parent);
                continue;
            }


            //Min node: state generated by us, they pick next state
            //Max node: state generated by them, we pick next state
            char playerSymbol = player.myQueenSymb;
            if(!node.maxNode) {
                playerSymbol = player.badQueenSymb;
            }

            //generate next move
            Move move = node.iterator.getNextMove(playerSymbol, node.board);
            if(move == null){ //no moves left

                if(node.parent.maxNode)
                    node.parent.score = Math.max(node.parent.score, node.score);
                else{
                    node.parent.score = Math.min(node.parent.score, node.score);
                }

                //don't add parent node if parent is root, this branch is done
                if(node.parent.parent == null) continue;
                stack.add(node.parent);
            }
            else{ //create next move
                char[][] rsltBoard = new char[node.board.length][node.board.length];
                for(int k = 0; k < 100; k++){ //set up board that would result from queens move
                    int y = k/10;
                    int x = k%10;
                    rsltBoard[y][x] = node.board[y][x];
                }
                rsltBoard[move.qSrc.y][move.qSrc.x] = BoardGameModel.POS_AVAILABLE;
                rsltBoard[move.qDest.y][move.qDest.x] = playerSymbol;
                rsltBoard[move.arrow.y][move.arrow.x] = BoardGameModel.POS_MARKED_ARROW;


                //System.out.println(BoardGameModel.boardAsString(node.board) );
                //System.out.println(BoardGameModel.boardAsString(rsltBoard) );
                //System.out.println("------------");

                //use the child's score to find this nodes score
                int childScore = HeuristicFunction.calcHeuristic(rsltBoard, player);

                //if the child node is at the bottom of the tree, or it's a win/loss
                if(node.depth+1 == maxDepth || childScore == Integer.MAX_VALUE -1 || childScore == Integer.MIN_VALUE +1){
                    if(node.maxNode)
                        node.score = Math.max(childScore, node.score);
                    else{
                        node.score = Math.min(childScore, node.score);
                    }

                    //add this node to the stack, which discards the child
                    stack.add(node);
                }
                else{
                    Node child = new Node(node, rsltBoard, node.depth+1, !node.maxNode);
                    stack.add(child);
                }
            }
            if(((System.nanoTime() - star)/1000000000) > 18) break;
        }//end of handling node
    }

    //Adds all possible moves to a node of the minmax tree
    ArrayList<Node> expandNode(Node node, char playerSymbol){
        ArrayList<Node> output = new ArrayList<>();
        Point myPiece[] = findPieces(playerSymbol, node.board);

        //set up temp board
        char[][] rsltBoard = new char[node.board.length][node.board.length];
         for(int k = 0; k < 100; k++){
            int y = k/10;
            int x = k%10;
            rsltBoard[y][x] = node.board[y][x];
        }

        //go through all players queens
        for(int i = 0; i < 4; i++){
            DestList myQueensMoves = new DestList(myPiece[i], node.board);

            //go through all the moves of the queen
            for(int j = 0; j < myQueensMoves.numMoves; j++){
                //set up board that would result from queens move
                rsltBoard[myQueensMoves.src.y][myQueensMoves.src.x] = BoardGameModel.POS_AVAILABLE;
                rsltBoard[myQueensMoves.moves[j].y ][myQueensMoves.moves[j].x] = playerSymbol;

                DestList arrowMoves = new DestList(myQueensMoves.moves[j], rsltBoard);
                //go through all the moves of each arrow
                for(int k = 0; k < arrowMoves.numMoves; k++){ //try all the possible arrow positions
                    char[][] nodeBoard = new char[node.board.length][node.board.length];

                    //create board for child
                    for(int l = 0; l < 100; l++){
                        int y = l/10;
                        int x = l%10;
                        nodeBoard[y][x] = rsltBoard[y][x];
                    }
                    nodeBoard[arrowMoves.moves[k].y][arrowMoves.moves[k].x] = BoardGameModel.POS_MARKED_ARROW;
                    Node child = new Node(node, nodeBoard, 1, false);
                    child.move.set(myQueensMoves.src, myQueensMoves.moves[j], new Point(arrowMoves.moves[k].x, arrowMoves.moves[k].y));

                    //child.score = HeuristicFunction.calcHeuristic(nodeBoard, player);

                    output.add(child);
                }
                //reset the board
                rsltBoard[myQueensMoves.src.y][myQueensMoves.src.x] = playerSymbol;
                rsltBoard[myQueensMoves.moves[j].y ][myQueensMoves.moves[j].x] = BoardGameModel.POS_AVAILABLE;
            }//end of queens move
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

        //max node picks highest value between its child score and its score
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
    Point qPos;

    int adir;
    int adist;

    public MoveIterator(){
        qNum = 0;
        qdir = 0;
        qdist = 1;

        adir = 0;
        adist = 1;
    }

    public Move getNextMove(char playerSymbol, char board[][]){

        Point myPiece[] = AmazonsBot.findPieces(playerSymbol, board);
        Point aDest = null;

        while(aDest == null){
            while(qPos == null){
                qPos = getNextQueenDest(myPiece[qNum], board);
                if(qPos == null){
                    qNum++;
                    adir = 0;
                    adist = 1;
                    if(qNum>=4) return null;
                }
            }
            aDest = getNextArrowDest(qPos, board, myPiece[qNum]);
            if(aDest == null){
                qPos = null;
            }
        }
        return new Move(myPiece[qNum], qPos, aDest);
    }

    public Point getNextQueenDest(Point src, char board[][]){

        Point dest = null;
        while(dest == null){

            int x = src.x + qdist*DIRLIST[qdir][0];
            int y = src.y + qdist*DIRLIST[qdir][1];
            qdist++;

            //if it's not a valid move, try next direction
            if(x < 0 || x >= board.length || y < 0 || y >= board.length || board[y][x] != BoardGameModel.POS_AVAILABLE) {
                qdist = 1;
                qdir++;
                if(qdir >= 8) {// no more moves
                    qdir = 0;
                    qdist = 1;
                    return null;
                }
            }
            else{
                dest = new Point(x,y);
            }
        }
        return dest;
    }

    public Point getNextArrowDest(Point src, char board[][], Point origin){

        Point dest = null;
        while(dest == null){
            int x = src.x + adist*DIRLIST[adir][0];
            int y = src.y + adist*DIRLIST[adir][1];
            adist++;

            if(x < 0 || x >= board.length || y < 0 || y >= board.length || (board[y][x] != BoardGameModel.POS_AVAILABLE && !(y == origin.y && x == origin.x))) {
                adir++;
                adist = 1;
                if(adir >= 8) {// no more moves
                    adir = 0;
                    adist = 1;
                    return null;
                }
            }
            else{
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
