/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cosc322;

import java.util.ArrayList;
import java.util.Arrays;
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
        
        Node root = new Node(null, board, 0, true);
        ArrayList<Node> children = expandNode(root, player.myQueenSymb); 
        System.out.println(children.size());
        
        int depth = 2;
        if (children.size() < 450) depth = 3;
        if (children.size() < 300) depth = 4;
        if (children.size() < 200) depth = 5;
        //if (root.children.size() < 130) depth = 3;
        //if (root.children.size() < 50) depth = 4;

        
        
        int numThreads = 7;
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

            for(int i = 0; i < t.length; i++) t[i].start();            
            
            //System.out.println("start:" + t.length*size + " end:" + root.children.size());
            if(depth > 1){  
                workList[workList.length-1] = new ArrayList<>();
                for(int i = t.length*size; i < children.size(); i++)
                    workList[workList.length-1].add(children.get(i));
                minmaxScoreNode(depth, workList[workList.length-1]);
            }

            //wait for threads to finish
            try {
                for(int i = 0; i < t.length; i++) t[i].join();
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

    public void minmaxScoreNode(int maxDepth, ArrayList<Node> children){
        
        
        ArrayList<Node> stack = new ArrayList<>();
        
        for(Node node : children){
            stack.add(node);
        }
        
        while(stack.size() > 0){
            Node node = stack.get(stack.size() - 1);
            stack.remove(stack.size() - 1);
            

            char playerSymbol = player.myQueenSymb;
            if(!node.maxNode) playerSymbol = player.badQueenSymb;
            
            Move move = node.iterator.getNextMove(playerSymbol, node.board);
            if(move == null){
                if(node.parent.parent == null) continue; //don't add parent node
                
                if(node.parent.maxNode)
                    node.parent.score = Math.max(node.parent.score, node.score);
                else{
                    node.score = Math.min(node.parent.score, node.score);
                }   
                stack.add(node.parent);                
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

                if(node.depth+1 == maxDepth){
                    int childScore = scoreBoard(rsltBoard);
                    if(node.maxNode)
                        node.score = Math.max(childScore, node.score);
                    else{
                        node.score = Math.min(childScore, node.score);
                    }   
                    stack.add(node);
                }
                else{                    
                    boolean prune = false;
                    if(node.parent != null){
                        if(node.parent.maxNode){
                            if(node.parent.score > node.score && node.parent.score != Integer.MIN_VALUE && node.score != Integer.MAX_VALUE) prune = true;
                        }
                        else{
                            if(node.parent.score < node.score && node.parent.score != Integer.MAX_VALUE && node.score != Integer.MIN_VALUE) prune = true;
                        }
                    }
                    
                    if(prune == true){
                        stack.add(node);
                        System.out.println("prunned");
                    }
                    else{
                        Node child = new Node(node, rsltBoard, node.depth+1, !node.maxNode);
                        stack.add(child); 
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
                    int score = scoreBoard(rsltBoard);                  
                    
                    char[][] nodeBoard = new char[node.board.length][node.board.length];

                    for(int l = 0; l < 100; l++){
                        int y = l/10;
                        int x = l%10;
                        nodeBoard[y][x] = rsltBoard[y][x];
                    }       

                    Node child = new Node(node, nodeBoard, 1, true);
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
    public  static Point[] findPieces(char type, char board[][]){
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
    final static int dirList[][] = { {0,1}, {1,1}, {1,0}, {1,-1}, {0,-1}, {-1,-1}, {-1,0}, {-1, 1} };
    
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
        Point aDest = null;
        
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
            
            int x = src.x + qdist*dirList[qdir][0];
            int y = src.y + qdist*dirList[qdir][1];
        
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
            int x = src.x + adist*dirList[adir][0];
            int y = src.y + adist*dirList[adir][1];
        
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


