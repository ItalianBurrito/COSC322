/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cosc322;

import ygraphs.ai.smart_fox.games.GameModel;

    /**
     * The game board
     * 
     * @author yongg
     *
     */
public class BoardGameModel extends GameModel {

    public static final char POS_MARKED_BLACK = 'b';
    public static final char POS_MARKED_WHITE = 'w';
    public static final char POS_MARKED_ARROW = 'x';
    public static final char POS_AVAILABLE = '0';

    char[][] gameBoard = null; 	

    public BoardGameModel(int rows, int columns){

            gameBoard = new char[rows][columns];
            //posScores = new int[rows][columns];
            for(int y = 0; y < rows; y++)
                    for(int x = 0; x < columns; x++)
                            gameBoard[y][x] = BoardGameModel.POS_AVAILABLE;	
            
        char tagB = POS_MARKED_BLACK;
        char tagW = POS_MARKED_WHITE;

        gameBoard[0][3] = tagW;
        gameBoard[0][6] = tagW;
        gameBoard[2][0] = tagW;
        gameBoard[2][9] = tagW;

        gameBoard[7][0] = tagB;
        gameBoard[7][9] = tagB;
        gameBoard[9][3] = tagB;
        gameBoard[9][6] = tagB;
    }


    public boolean positionMarked(int row, int column, int arow, int acol,
        int qfr, int qfc, boolean opponentMove){
        boolean valid = true;		
        if(row >= gameBoard.length | column >= gameBoard.length 
                         || row < 0 || column < 0)
        {
            valid = false;
        }
        else if (gameBoard[row][column] != POS_AVAILABLE){
            valid = false;
        }
        else if (gameBoard[arow][acol] != POS_AVAILABLE && arow != qfr && acol != qfc){
            valid = false;
        }
        else if (gameBoard[qfr][qfc] != POS_MARKED_WHITE && gameBoard[qfr][qfc] != POS_MARKED_BLACK){
            valid = false;
        }

        if(valid){
                gameBoard[row][column] = gameBoard[qfr][qfc];		
                gameBoard[qfr][qfc] = BoardGameModel.POS_AVAILABLE;		
                gameBoard[arow][acol] = BoardGameModel.POS_MARKED_ARROW;
        }
        return valid;
    }	

    public String toString(){
        String b = null;

        for(int i = 0; i < 10; i++){
                for(int j = 0; j< 10; j++){
                  b = b + gameBoard[i][j] + " ";
                }
                b = b + "\n";
        }  	  
        return b;
    }	
}
