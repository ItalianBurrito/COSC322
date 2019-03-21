/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cosc322;

import java.util.ArrayList;
import java.util.Collections;
import org.openide.util.Exceptions;

/**
 *
 * @author 86696549
 */
public class AmazonsBot {
    Amazons player;    
    Node root;
    Node current;
   
    public AmazonsBot(Amazons player){
        this.player = player;     
        //AmazonsGUI gui = new AmazonsGUI(player.userName(), player.toString());
    }
    
    void setBoard(String board){
        
    }
     
    
    /*
        Counts up the number of tiles owned by all of the players queens. A tile is owned by the queen closest to it, only empty tiles can be owned. 
        score = my tiles owned - other player tiles owned.
    */
    int scoreBoard(char[][] board){
        int myScore = 0;
        int badScore = 0;
        
        Point myPieces[] = findPieces(player.myQueenSymb, board);
        Point badPieces[] = findPieces(player.badQueenSymb, board);       
         
        //iterate through each empty tile
        for(int y = 0; y < board.length; y++)
            for(int x = 0; x < board.length; x++){
                if(board[y][x] != BoardGameModel.POS_AVAILABLE) continue;
                int myBest = Integer.MAX_VALUE;
                int badBest = Integer.MAX_VALUE;
                
                //find smallest manhattan block distance betweeen each empty tile and each players queens
                for(int i = 0; i < 4; i++ ){
                    int dist = Math.abs(x-myPieces[i].x) + Math.abs(y-myPieces[i].y);
                    if(dist < myBest) myBest = dist;
                    
                    dist = Math.abs(x-badPieces[i].x) + Math.abs(y-badPieces[i].y);
                    if(dist < badBest) badBest = dist;                    
                }
                
                if(myBest < badBest){
                    myScore++;
                }
                else if( badBest < myBest){
                    badScore++;
                }    
                //else no player gets a point for a tie
            } 
        return myScore - badScore;
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
        //System.out.println("current score: " + scoreBoard(board));
        
        //Node root = new Node(null, 0, board);
        root = new Node(board);
        expandNode(root, player.myQueenSymb); 
        System.out.println(root.children.size());
        
        int depth = -1;
        if (root.children.size() < 1500) depth = 0;
        if (root.children.size() < 130) depth = 1;
         if (root.children.size() < 50) depth = 1;

        
        
        int numThreads = 4;
        if(numThreads > 1){
            BuildTreeThread[] t = new BuildTreeThread[numThreads-1];
            int size = root.children.size() / numThreads;        

            for(int i = 0; i < t.length; i++) {           
                t[i] = new BuildTreeThread(this, root.children, i*size, i*size+(size-1), depth);
                //System.out.println("start:" + i*size + " end:" + (i*size+(size-1)));
            }        

            for (BuildTreeThread t1 : t) {            
                t1.start();
            }
            
            //System.out.println("start:" + t.length*size + " end:" + root.children.size());
            if(depth >= 0){                
                for(int i = t.length*size; i < root.children.size(); i++)
                    expandRecursive(root.children.get(i), depth, false); 
            }

            //wait for threads to finish
            try {
                for(int i = 0; i < t.length; i++) t[i].join();
           } catch (InterruptedException ex) {
               Exceptions.printStackTrace(ex);
           }          
        }
        else{
            if(depth >= 0){
                for( Node child : root.children)
                    expandRecursive(child, depth, false); 
            }   
        }
        
        System.out.println("searching tree!");
        
        for(Node child : root.children){ 
            int score = minMaxTree(child, true);
            if( score > bestScore){
                bestScore = score;
                bestMove = child.move;
            }            
        }      
        root = null;
        return bestMove;   
    }
    
    int minMaxTree(Node node, boolean max){
        if (node.children.size() == 0) return node.score;
        
        Node best = node.children.get(0);
        if(max){
           for(int i = 1; i < node.children.size(); i++){
               if(node.children.get(i).score > best.score) best = node.children.get(i);
           }
        }
        else{
           for(int i = 1; i < node.children.size(); i++){
               if(node.children.get(i).score > best.score) best = node.children.get(i);
           }
        }
        return node.score + minMaxTree(best, !max);
    }
    
    void expandRecursive(Node node, int depth, boolean myTurn){    
        if (myTurn) expandNode(node, player.myQueenSymb);
        else expandNode(node, player.badQueenSymb); 
        
        if(depth > 0){ //having base case here saves some method calls
            for( Node child : node.children)
                expandRecursive(child, depth - 1, !myTurn);
        }        
    }
    
    //Adds all possible moves to a node of the minmax tree
    void expandNode(Node node, char playerSymbol){
        
        Point myPiece[] = findPieces(playerSymbol, node.board);
        //Point badPiece[] = findPieces(player.badQueenSymb, node.board);        
                
        DestList[] myQueen = new DestList[4];
        for(int i = 0; i < myQueen.length; i++) myQueen[i] = new DestList(myPiece[i], node.board);        
        for(int i = 0; i < 4; i++){ //go through all players queens
            //System.out.println("Queen: " + i );
            char[][] rsltBoard = new char[node.board.length][node.board.length];
            for(int j = 0; j < myQueen[i].numMoves; j++){ //go through all the moves of each queen
                //System.out.println("Num moves: " + j );
                for(int k = 0; k < 100; k++){ //set up board that would result from queens move
                    int y = k/10;
                    int x = k%10;
                    rsltBoard[y][x] = node.board[y][x];
                }
                rsltBoard[myQueen[i].src.y][myQueen[i].src.x] = BoardGameModel.POS_AVAILABLE;
                rsltBoard[myQueen[i].moves[j].y ][myQueen[i].moves[j].x] = playerSymbol;
                
                DestList arrowMoves = new DestList(myQueen[i].moves[j], rsltBoard);
                for(int k = 0; k < arrowMoves.numMoves; k++){ //try all the possible arrow positions    
                    //System.out.println("Arrow: " + k );
                    rsltBoard[arrowMoves.moves[k].y][arrowMoves.moves[k].x] = BoardGameModel.POS_MARKED_ARROW;
                    //score up the board and keep the best move
                    int score = scoreBoard(rsltBoard);                  
                    
                    char[][] nodeBoard = new char[node.board.length][node.board.length];
                    for(int l = 0; l < 100; l++){
                        int y = l/10;
                        int x = l%10;
                        nodeBoard[y][x] = rsltBoard[y][x];
                    }                    
                    
                    Node child = new Node(node, score, nodeBoard);
                    //Node child = new Node(node, score);
                   
                    child.move.set(myQueen[i].src, myQueen[i].moves[j], new Point(arrowMoves.moves[k].x, arrowMoves.moves[k].y));
                    node.children.add(child);
                    rsltBoard[arrowMoves.moves[k].y][arrowMoves.moves[k].x] = BoardGameModel.POS_AVAILABLE;
                }                
            }                
        }
    }           
    
    //finds location of a players pieces.
    private Point[] findPieces(char type, char board[][]){
        //char[][] temp = board;
        Point pieces[] = new Point[4];
//        ArrayList<Move> moveSet = new ArrayList<>(); 
//        
//        do{
//            if(node.move == null){
//                break;
//            }else{
//                moveSet.add(node.getMove());
//                node = node.getParent();
//            }
//        }while(node.hasLast());
        
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
    
    Node parent;
    ArrayList<Node> children;    
    
    Move move;
   
    public Node(Node parent, int score, char[][] board){
        this.score = score;
        this.board = board;
        this.parent = parent;
        children = new ArrayList<>();
        move = new Move();
    }
    
    public Node(Node parent, int score){
        this.score = score;
        this.parent = parent;
        children = new ArrayList<>();
        move = new Move();
    }
    
    public Node(char[][] board){
        this.board = board;
        this.parent = null;
        children = new ArrayList<>();
    }
    
    boolean hasLast(){
        if(this.parent != null){
            return true;
        }else{
            return false;
        }
    }
    
    Move getMove(){
        return this.move;
    }
    
    Node getParent(){
        return this.parent;
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
    int start;
    int end;
    int depth;      
    char myQueenSymb;
    char badQueenSymb;
    AmazonsBot bot;
    

    public BuildTreeThread(AmazonsBot bot, ArrayList<Node> nodes, int start, int end, int depth){
        this.nodes = nodes;
        this.start = start;
        this.end = end;
        this.depth = depth;
        this.bot = bot;
       }
    
    @Override
    public void run() {
        if(depth >= 0){
            for(int i = start; i < end; i++){
                bot.expandRecursive(nodes.get(i), depth, false); 
            }
        }
    }    
}


