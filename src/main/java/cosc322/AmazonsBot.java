/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cosc322;

import java.util.ArrayList;

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
        Creates the board produced by all possible moves the player can perform. Scores up each board with the scoreboard function.
    */    
    Move findMove(){              
        Move bestMove = new Move();
        int bestScore = Integer.MIN_VALUE;
        char[][] board = player.board.gameBoard;        
        Point myPiece[] = findPieces(player.myQueenSymb, board);
        Point badPiece[] = findPieces(player.badQueenSymb, board);
        
        System.out.println("current score: " + scoreBoard(board));

        char[][] rsltBoard = new char[board.length][board.length];        
        DestList[] myQueen = new DestList[4];
        for(int i = 0; i < myQueen.length; i++) myQueen[i] = new DestList(myPiece[i], board);
        
        for(int i = 0; i < 4; i++){ //go through all players queens
            for(int j = 0; j < myQueen[i].numMoves; j++){ //go through all the moves of each queen
                for(int k = 0; k < 100; k++){ //set up board that would result from queens move
                    int y = k/10;
                    int x = k%10;
                    rsltBoard[y][x] = board[y][x];
                }
                rsltBoard[myQueen[i].src.y][myQueen[i].src.x] = BoardGameModel.POS_AVAILABLE;
                rsltBoard[myQueen[i].moves[j].y ][myQueen[i].moves[j].x] = player.myQueenSymb;
                
                DestList arrowMoves = new DestList(myQueen[i].moves[j], board);
                for(int k = 0; k < arrowMoves.numMoves; k++){ //try all the possible arrow positions
                    rsltBoard[arrowMoves.moves[k].y][arrowMoves.moves[k].x] = BoardGameModel.POS_MARKED_ARROW;
                    //score up the board and keep the best move
                    int score = scoreBoard(rsltBoard);
                    if(score > bestScore){
                        bestScore = score;
                        bestMove.set(myQueen[i].src, myQueen[i].moves[j], new Point(arrowMoves.moves[k].x, arrowMoves.moves[k].y));
                    }
                    rsltBoard[arrowMoves.moves[k].y][arrowMoves.moves[k].x] = BoardGameModel.POS_AVAILABLE;
                }                
            }
        }
        return bestMove;
    }
    
    int scoreBoard(char[][] board){
        int myScore = 0;
        int badScore = 0;
        
        Point myPieces[] = findPieces(player.myQueenSymb, board);
        Point badPieces[] = findPieces(player.badQueenSymb, board);       
        
        for(int y = 0; y < board.length; y++)
            for(int x = 0; x < board.length; x++){
                if(board[y][x] != BoardGameModel.POS_AVAILABLE) continue;
                int myBest = Integer.MAX_VALUE;
                int badBest = Integer.MAX_VALUE;
                
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
            } 
        return myScore - badScore;
    }
    
    
    
   /*
    Finds the next move using a min max tree.
    */ 
    //TODO: finish
    Move findMoveTree(){        
        Move bestMove = new Move();
        int bestScore = Integer.MIN_VALUE;
        char[][] board = player.board.gameBoard;  
        
        System.out.println("current score: " + scoreBoard(board));
        
        Node root = new Node(null, 0, board);
        expandNode(root);
       
        for(Node child : root.children){
            if(child.score > bestScore){
                bestScore = child.score;
                bestMove = child.move;
            }
        }

        return bestMove;        
    }
    
    //Adds all possible moves to a node of the minmax tree
    void expandNode(Node node){
        
        Point myPiece[] = findPieces(player.myQueenSymb, node.board);
        Point badPiece[] = findPieces(player.badQueenSymb, node.board);        
                
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
                rsltBoard[myQueen[i].moves[j].y ][myQueen[i].moves[j].x] = player.myQueenSymb;
                
                DestList arrowMoves = new DestList(myQueen[i].moves[j], node.board);
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
                    
                    Node child = new Node(node, score, nodeBoard);
                   
                    child.move.set(myQueen[i].src, myQueen[i].moves[j], new Point(arrowMoves.moves[k].x, arrowMoves.moves[k].y));
                    node.children.add(child);
                    rsltBoard[arrowMoves.moves[k].y][arrowMoves.moves[k].x] = BoardGameModel.POS_AVAILABLE;
                }                
            }                
        }
    }     
        

    
    private Point[] findPieces(char type, char board[][]){
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


