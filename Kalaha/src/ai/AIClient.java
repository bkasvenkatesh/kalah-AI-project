package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 * 
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable
{
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;
    long start_time,startT;
    int utility, move=0, depthLimit=0;
    /**
     * Creates a new client.
     */
    public AIClient()
    {
	player = -1;
        connected = false;
        
        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }
    
    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    /**
     * Adds a text string to the GUI textarea.
     * 
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;
        
        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                    
                    addText("I am player " + player);
                }
                
                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                           start_time=System.currentTimeMillis();
                            int cMove = getMove(currentBoard);
                            
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                
                //Wait
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }
    
    /**
     * This is the method that makes a move each time it is your turn.
     * Here you need to change the call to the random method to your
     * Minimax search.
     * 
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard)
    {
      //  Iterative deepening implemented here
        depthLimit=0;
        while(System.currentTimeMillis()-startT<=5000)
        {
            int u=minMax(depthLimit,currentBoard,true,-999,+999);//2 is the depthLimit
            depthLimit++; //System.out.println(depthLimit);
        }
        int myMove=move;
      //  System.out.println("utility of root="+getUtility(currentBoard));
      //  System.out.println("ambo1="+currentBoard.getSeeds(1, 2));
        return myMove;
    }
    
    
       // implements minimax algorithm with alpha-beta pruning
    public int minMax(int depth, GameState local2currentBoard,boolean max,int alpha, int beta)
    {
        int d=depth;
        int play=local2currentBoard.getNextPlayer();
        
        // base condition checks if this node is leaf or not
        if(d<=0||local2currentBoard.gameEnded()||(System.currentTimeMillis()-startT)>4000)
        {
            utility=getUtility(local2currentBoard);
            return utility;
        }
        if(max==true)
        {
           // expand to child nodes
            for(int i=1; i<7; i++)
            {   
                GameState b=local2currentBoard.clone(); //create a new copy of board for each child
                //checks whether move is valid or not
                if(b.moveIsPossible(i))
                    b.makeMove(i);
                else
                    continue;
                int value;
                // checks for another chance on the same turn
                if(b.getNextPlayer()==play)
                    value=minMax(d,b,true,alpha,beta);
                else
                    value=minMax(d-1,b,false,alpha,beta);
                if(value>alpha)
                {   
                    alpha=value;move=i;
                }
                if(alpha>=beta)
                    break;
            }
            return alpha;
        }
        else
        {
           
            for(int i=1; i<7; i++)
            {
            
               GameState  c=local2currentBoard.clone();
                 int value;
                 if(c.moveIsPossible(i))
                    c.makeMove(i);
                 else
                    continue;
                if(c.getNextPlayer()==play)
                    value=minMax(d,c,false,alpha,beta);
                else
                    value=minMax(d-1,c,true,alpha,beta);
                if(value<beta)
                {
                    beta=value;
                }
                if(alpha>=beta)
                    break;
    
            }
            return beta;
        }
    
    }
    
    public int getUtility(GameState local4currentBoard)
    {
        int seeds_south=0,seeds_north=0;
        int totals=local4currentBoard.getScore(1); 
        int totaln=local4currentBoard.getScore(2);
        for(int i=1;i<7;i++)
        {
            seeds_south+=local4currentBoard.getSeeds(i, 1);
            seeds_north+=local4currentBoard.getSeeds(i, 2);
          //  System.out.println("ambo"+i+"="+local4currentBoard.getScore(1));
        }
        int south=totals-seeds_south;
        int north= totaln-seeds_north;
        
      // System.out.println(local4currentBoard.toString());
        
        if(local4currentBoard.getNextPlayer()==1)
            utility=south-north;
        else
            utility=north-south;
      //  System.out.println("ambo1="+local4currentBoard.getSeeds(1,1));
      //  System.out.println("north="+north+"south="+south);
        return utility;
    }
    
    public int getRandom()
    {
        return 1 + (int)(Math.random() * 6);
    }
}