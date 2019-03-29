/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cosc322;

import static cosc322.AmazonsBot.findPieces;

/**
 *
 * @author james
 */
public class HeuristicFunction {

    final static int[][] DIRLIST = { {0,1}, {1,1}, {1,0}, {1,-1}, {0,-1}, {-1,-1}, {-1,0}, {-1, 1} };

    static boolean useZones = false;

    public static int calcHeuristic(char[][] board, Amazons player){

        int score = 0;

        score = findZoneSize(board, player);
        if(score == 0) score = scoreBoard(board, player);
        /*
        if(!useZones)
            score = scoreBoard(board, player);
        else
            score = findZoneSize(board, player);
          */

        /*
        if(!hasMoves(board, player.myQueenSymb)){
            score = Integer.MIN_VALUE + 1;
        }
        else if(!hasMoves(board, player.myQueenSymb)){
            score = Integer.MAX_VALUE - 1;
        }
        */

        return score;
    }

        /*
        Counts up the number of tiles owned by all of the players queens. A tile is owned by the queen closest to it, only empty tiles can be owned.
        score = my tiles owned - other player tiles owned.
    */
    static int scoreBoard(char[][] board, Amazons player){
        int myScore = 0;
        int badScore = 0;

        //Finds our queens and opponent queens.
        Point myPieces[] = findPieces(player.myQueenSymb, board);
        Point badPieces[] = findPieces(player.badQueenSymb, board);


        for(int i = 0; i < myPieces.length; i++){
            myScore += new DestList(myPieces[i], board).numMoves;
            badScore += new DestList(badPieces[i], board).numMoves;
        }


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
                    myScore += 1;
                }
                else if( badBest < myBest){
                    badScore +=1;
                }
                //else no player gets a point for a tie
            }
        return myScore - badScore;
    }

    static int findZoneSize(char[][] board, Amazons player){
        Point[] myPieces = AmazonsBot.findPieces(player.myQueenSymb, board);
        Point[] badPieces = AmazonsBot.findPieces(player.badQueenSymb, board);

        boolean[][] searched = new boolean[10][10];

        int score = 0;
        int[] badNum = new int[1];
        for(int i = 0; i < myPieces.length; i++){
            if(!searched[myPieces[i].y][myPieces[i].x]) continue;

            badNum[0] = 0;
            int myScore = findTiles(myPieces[i], board, searched, player.badQueenSymb, badNum);
            if(badNum[0] > 0) myScore = myScore/badNum[0];
            score += myScore;
        }

        searched = new boolean[10][10];
        for(int i = 0; i < badPieces.length; i++){
            if(!searched[badPieces[i].y][badPieces[i].x]) continue;

            badNum[0] = 0;
            int myScore = findTiles(badPieces[i], board, searched, player.myQueenSymb,badNum);
            if(badNum[0] > 0) myScore = myScore/badNum[0];
            score -= myScore;
        }

        return score;
    }

    static int findTiles(Point src, char[][] board, boolean[][] searched, char badSymb, int[] badFound){
            int num = 0;
            for(int j = 0; j < 8; j++){
                int x = src.x + 1*DIRLIST[j][0];
                int y = src.y + 1*DIRLIST[j][1];

                if(x < 0 || x >= board.length || y < 0 || y >= board.length) continue;
                if(searched[y][x] == true) continue;

                if(board[y][x] != BoardGameModel.POS_AVAILABLE) {
                    if(board[y][x] == badSymb) badFound[0]++;
                    continue;
                }

                searched[y][x] = true;
                num++;

                num += findTiles(new Point(x,y), board, searched, badSymb, badFound);

            }
        return num;
    }

    static boolean hasMoves(char[][] board, char playerSymbol){
        Point myPieces[] = findPieces(playerSymbol, board);

        //go through all the pieces, return true if we find at least one move
        for (int p = 0; p < myPieces.length; p++) {
            Point myPiece = myPieces[p];
            boolean validDir[] = new  boolean[8]; //clockwise up to left-up
            for(int i = 0; i < 8; i++) validDir[i] = true;

            //try all the directions of the piece
            for (int j = 0; j < 8; j++) {
                int x = myPiece.x + 1*DIRLIST[j][0];
                int y = myPiece.y + 1*DIRLIST[j][1];
                if(x < 0 || x >= board.length || y < 0 || y >= board.length || board[y][x] != BoardGameModel.POS_AVAILABLE) {
                    validDir[j] = false;
                }
            }//end for all possible directions
            for(int i = 0; i < validDir.length; i++){
                if(validDir[i] == true) return true;//we have at least one move
            }
        }
        return false;
    }



}
