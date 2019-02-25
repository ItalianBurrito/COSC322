/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cosc322;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author 86696549
 */
public class AmazonsGUI extends Amazons{
    private JFrame guiFrame;
    private BoardPanel boardPanel;
    
    public AmazonsGUI(String name, String passwd){
        super(name, passwd);
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
    
    @Override
    void performMove(){
        boardPanel.drawBoard(); 
    }
}

class BoardPanel extends JPanel{
    private static final long serialVersionUID = 1L;
    private  int rows = 10;
    private  int cols = 10; 

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
    private JLabel actionLabel;
   

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
    protected void paintComponent(Graphics gg){        	
            super.paintComponent(gg);
            Graphics g = (Graphics2D) gg;   

            for(int i = 0; i < rows+1; i++){
                    g.drawLine(i * cellDim + offset, offset, i * cellDim + offset, rows * cellDim + offset);
                    g.drawLine(offset, i*cellDim + offset, cols * cellDim + offset, i*cellDim + offset);					 
            }

            for(int r = 0; r < rows; r++){
              for(int c = 0; c < cols; c++){

                            posX = c * cellDim + offset;
                            posY = r * cellDim + offset;

                            posY = (9 - r) * cellDim + offset;

                    if(gameModel.gameBoard[r][c] == BoardGameModel.POS_AVAILABLE){
                            g.clearRect(posX+1, posY+1, 49, 49);					
                    }

                    if(gameModel.gameBoard[r][c] == BoardGameModel.POS_MARKED_BLACK){
                            g.fillOval(posX, posY, 50, 50);
                    } 
                    else if(gameModel.gameBoard[r][c] == BoardGameModel.POS_MARKED_ARROW) {
                            g.clearRect(posX+1, posY+1, 49, 49);
                            g.drawLine(posX, posY, posX + 50, posY + 50);
                            g.drawLine(posX, posY + 50, posX + 50, posY);
                    }
                    else if(gameModel.gameBoard[r][c] == BoardGameModel.POS_MARKED_WHITE){
                            g.drawOval(posX, posY, 50, 50);
                    }
              }
        }   
    }//method

    //JComponent method
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

    public void mousePressed(MouseEvent e) {

            int x = e.getX();
            int y = e.getY();


            if(((x - offset) < -5) || ((y - offset) < -5)){
                return;
            }

            int row = (y - offset) / cellDim;                        
            int col = (x - offset) / cellDim;            

            if(counter == 0){
                actionLabel.setText("Action: Move Queen");
                qfr = row;
                qfc = col;

                qfr = 9 - qfr;
                counter++;
            }
            else if(counter ==1){
                actionLabel.setText("Action: Shoot Arrow");
                qrow = row;
                qcol = col;

                qrow = 9 - qrow;
                counter++;
            }
            else if (counter == 2){
                arow = row;
                acol = col;

                arow = 9 - arow;
                counter++;
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

