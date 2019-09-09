package fasttester;
/*
 * [done]: delete states visited single time
 * [done]: decrease memory of state size: change Double to Float, change object Integer[] to new
 * object with int[];
 * [done]: -dev cards na razie dla obu bierze takie same => zamień u innych na get total, 
 *       inaczej nie widać, że w ostatnim kroku coś kupił, a dla innych i tak zamienia, 
 *       bo koniec ich kolejki
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.stream.Collectors;

import org.nd4j.config.ND4JEnvironmentVars;

import soc.debug.D;
import soc.game.ResourceSet;
import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCInventory;
import soc.game.SOCMoveRobberResult;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.message.SOCDevCardAction;
import soc.message.SOCPlayerElement;
import soc.robot.rl.RLStrategy;
import soc.robot.rl.RLStrategyLookupTable_small;
import soc.robot.rl.SOCState;
import soc.robot.rl.SOCState_small;
import soc.robot.rl.StateMemoryLookupTable;
import soc.robot.rl.StateValueFunction;
import soc.robot.rl.StateValueFunctionLT;

/**
 * Class for fast games of the Settlers of Catan, uses classes written by Robert S. Thomas
 * for now no trade between players allowed, but there is no full information.
 * Games are played only for 4 players
 * @author Sonia
 *
 */
public class BotServer {

	protected String name;
	
	protected int remainingGames;
	
	protected int synchroniseCount;
	
	/*players connected to the server*/
	protected RLClient[] players;
	
	/*order of players in the game*/
	protected RLClient[] playersGameOrder;
	
	/*stats accumulated for each player*/
	protected int[] wins, loses;
	
	protected SOCGame currentGame;
	
	protected int gamesPlayed;
	
	/*name of the file, where all stats about the game are written*/
	protected String fileName;
	
	/*should we turn on testingHandler*/
	protected boolean isTraining;
	
     /*to get random numbers. */
    protected Random rand = new Random();
    
    /**When no seed is specified at the start of the program, value of 5 is used
     * otherwise this static variable is overwritten by the provided value */
    public static int SEED = 5;
    
    HashMap<Integer,SOCPlayer> playersState;
    
//    protected SOCState state;
    
    /*whether is it training or testing game, and if players use neural networks or lookup table*/
    String gameType;
    
    /*number of memory file, if memory is read from (in case of lookup table) or when weights for NN are read from file*/
    int memoryType;
    
	public BotServer(String name, int games, int syn, String gameType, 
			int memoryType, boolean isTraining) {
		this.name = name;
		this.remainingGames = games;
		this.synchroniseCount = syn;
		this.gameType = gameType;
		this.memoryType=memoryType; 
		this.isTraining=isTraining;
//		wins = new int[4];
//		loses = new int[4];
		gamesPlayed = 0;
		
		/*creating file name*/
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		Date date = new Date();
		fileName = dateFormat.format(date) + "_" + gameType + "_seed" + BotServer.SEED + "_stat";
		rand.setSeed(SEED);
		
		/*printing basic information*/
		System.out.println("game type = " + gameType);
		System.out.println("file name= " + fileName);
		System.out.println("seed = " + SEED);
		
//		startTime = System.currentTimeMillis();
//        numberOfGamesStarted = 0;
//        numberOfGamesFinished = 0;
		
	}
	
	public void createPlayers() {
		players = new RLClient[4];
		Properties pr = new Properties();
		try
        {
            final File pf = new File("players.properties");
            if (pf.exists())
            {
                if (pf.isFile() && pf.canRead())
                {
                	System.err.println("Reading startup properties from players.properties");
                    FileInputStream fis = new FileInputStream(pf);
                    pr.load(fis);
                    fis.close();
                	for (int i=0; i<4; i++) {
                		String prPlayerType = pr.getProperty("pl" + i);
                		createPlayer(i, prPlayerType);
                	}
                }
                else {
                System.err.println
                    ("*** Properties file  players.properties"
                     + " exists but isn't a readable plain file: Exiting.");
                System.exit(1);
                }
            } else {
            	System.out.println
            	("No properties file found. Reading in defaults");
            	createPlayersFromGameType();
            }
        }
		catch (Exception e)
		{
	        // SecurityException from .exists, .isFile, .canRead
	        // IOException from FileInputStream construc [FileNotFoundException], props.load
	        // IllegalArgumentException from props.load (malformed Unicode escape)
	        System.err.println
	            ("*** Error reading properties file  AproximatorNN.properties" 
	             + ", exiting: " + e.toString());
	        if (e.getMessage() != null)
	            System.err.println("    : " + e.getMessage());
	        System.exit(1);
	    }
	}
	
	
	public void createPlayer(int pn, String playerType) {
		switch(playerType) {
		case "random":
			players[pn] = new RLClient(pn, RLClient.RANDOM, memoryType);
			break;
		case "fast":
			players[pn] = new RLClient(pn, RLClient.FAST_BUILTIN , memoryType);
			break;
		case "smart":
			players[pn] = new RLClient(pn, RLClient.SMART_BUILTIN , memoryType);
			break;
		case "testLT":
			players[pn] = new RLClient(pn, RLClient.TEST_LOOKUP_TABLE, memoryType);
			break;
		case "traintLT":
			players[pn] = new RLClient(pn, RLClient.TRAIN_LOOKUP_TABLE, memoryType);
			break;
		case "testNN":
			players[pn] = new RLClient(pn, RLClient.TEST_LOOKUP_TABLE, memoryType);
			break;
		case "traintNN":
			players[pn] = new RLClient(pn, RLClient.TRAIN_LOOKUP_TABLE, memoryType);
			players[pn].startTraining();
			break;
			
		}
	}
	                		
	public void createPlayersFromGameType() {
		
		switch(gameType) {
		
		case "testRandomLT":
			
			//in random tests 1 player plays against 3 random bots
			
//			players[0] = new RLClient(0, RLClient.TEST_LOOKUP_TABLE, memoryType, players[0].getMemory());
			players[0] = new RLClient(0, RLClient.TEST_LOOKUP_TABLE, memoryType);
			players[1] = new RLClient(1, RLClient.RANDOM, memoryType);
			players[2] = new RLClient(2, RLClient.RANDOM, memoryType);
			players[3] = new RLClient(3, RLClient.RANDOM, memoryType);
			break;
		
		case "trainLT":
			for (int i=0; i<players.length; i++) {
				players[i] = new RLClient(i, RLClient.TRAIN_LOOKUP_TABLE, memoryType);
			}
			break;
			
		case "trainNN":
			for (int i=0; i<players.length; i++) {
				players[i] = new RLClient(i, RLClient.TRAIN_NN, memoryType);
				/*back propagation of neural network is started after enough state-action transitions are accumulated*/
				players[i].startTraining();
			}
			break;
			
		case "testRandomNN":
			players[0] = new RLClient(0, RLClient.TEST_NN, memoryType);
			players[1] = new RLClient(1, RLClient.RANDOM, memoryType);
			players[2] = new RLClient(2, RLClient.RANDOM, memoryType);
			players[3] = new RLClient(3, RLClient.RANDOM, memoryType);
			break;
			
		case "sharedLT":
			/*shared object for memory is created and passed to players*/
			StateValueFunction sharedSVFunction;
			if (memoryType!=-1) {
				/*for shared memory id of 100 is used*/
				sharedSVFunction = new StateValueFunctionLT(false, 100);
				sharedSVFunction.readMemory("" + memoryType);
			} else {
				sharedSVFunction = new StateValueFunctionLT(true, 100);
			}
			for (int i=0; i<players.length; i++) {
				players[i] = new RLClient(i, RLClient.TRAIN_LOOKUP_TABLE, -1);
				players[i].setStateValueFunction(sharedSVFunction);
			}
			break;
		
		case "builtIn":
			players[0] = new RLClient_builtIn(0, RLClient.TEST_NN, memoryType);
			players[1] = new RLClient_builtIn(1, RLClient.RANDOM, memoryType);
			players[2] = new RLClient_builtIn(2, RLClient.RANDOM, memoryType);
			players[3] = new RLClient_builtIn(3, RLClient.RANDOM, memoryType);;
			break;
		}
		
	}
	
	/** 
	 * server is set to go, games are started in the loop
	 */
	public void startServer() {
		Date startTime = new Date();
		createPlayers();
		
		while(remainingGames>0) {
        	String gaName = "~botsOnly~" + remainingGames;
        	startGame(gaName);
        	remainingGames--;
        	gamesPlayed++;        	
		}
		
		/*after all games are played time of the whole training/testing is calculated*/
		Date now =  new Date();
		long gameSeconds = ((now.getTime() - startTime.getTime())+500L) / 1000L;
        final long gameMinutes = gameSeconds / 60L;
        gameSeconds = gameSeconds % 60L;
         
//        players[0].stateValueFunctionStats();
        
        /*let clients know that it's the end of training, finish NN updates
         * print stats
         */
        for (int i=0; i < players.length; i++) {
        	players[i].endTraining();
        }      
        
        /*print how much time it took*/
		System.out.println("TIME ELAPSED: " + gameMinutes + " minutes " + gameSeconds + " seconds.");
	}
	
	/**
	 * Method {@link RLClient#joinGame(gameName, botnumbers, botnames)} is invoked on 
	 * every client. Next board information is passed to all the clients.
	 * 
	 * @param name - name of the game, used to write stats in the file
	 */
	public void startGame(String name) {
//delete?		/*maybe needed*/
//		//SOCGame.boardFactory = new SOCBoardAtServer.BoardFactoryAtServer();
		currentGame = new SOCGame(name);
		// set the expiration to 90 min. from now
        //game.setExpiration(game.getStartTime().getTime() + (60 * 1000 * GAME_TIME_EXPIRE_MINUTES));
		
		/*DEBUG GAME START*/
//		System.out.println("Started bot-only game: " + name);
		
		currentGame.isBotsOnly = true;
		playersGameOrder = new RLClient[4];
		
		currentGame.setGameState(SOCGame.READY);
		
		/*shuffle robot indexes, the first one added to the game always starts the game, 
		 * so we need to add them in random order */
		int[] robotIndexes = null;
		robotIndexes = robotShuffleForJoin();
		String[] playersNames = new String[4];
		
		for (int i=0; i < players.length; i++) {
			playersNames[i] = players[i].getName();
			players[i].setPlayerNumber(robotIndexes[i]);
			playersGameOrder[robotIndexes[i]] = players[i];
		}
		
		for(int i =0; i<players.length; i++) {
			//*possible that we need to set lastsettlementcord for every SOCPlayer to -1
			if (currentGame.isSeatVacant(players[i].getPlayerNumber()))
            {
				currentGame.addPlayer(players[i].getName(), players[i].getPlayerNumber());
            } 
		}
		
		currentGame.startGame();
		
		final SOCBoard board;
        int[] hexes;
        int[] numbers;
        int robber;

        board = currentGame.getBoard();
        final int bef = board.getBoardEncodingFormat();
        if (bef == SOCBoard.BOARD_ENCODING_6PLAYER ||
            bef == SOCBoard.BOARD_ENCODING_ORIGINAL)
        {
            // v1 or v2
            hexes = board.getHexLayout();
            numbers = board.getNumberLayout();
        } else {
            // v3
            hexes = null;
            numbers = null;
        }
        robber = board.getRobberHex();
        
        final HashSet<Integer> psList = currentGame.getPlayer(0).getPotentialSettlements();
        
        int firstPlayer = currentGame.getFirstPlayer();
		
		for(int i =0; i<players.length; i++) {
			players[i].joinGame(name, robotIndexes, playersNames);
			players[i].setBoardLayout(hexes, numbers, robber);
			players[i].handlePotentialSettlements(psList);
			players[i].handleFirstPlayer(firstPlayer);
		}
		
		/*DEBUG*/
//		playersState = new HashMap<Integer,SOCPlayer>();
//		  
//		for (int i =0; i<currentGame.maxPlayers;i++) {
//			playersState.put(new Integer(i), currentGame.getPlayer(i));
//		}		  
//		state = new SOCState_small(-1, playersState);
//	    state.updateAll(playersState, board);   
		
		sendGameSate();
		/*maybe we should send not played dev card=> but setting initial settlements probably dont need it*/
		
		initialPlacementsHandler();
		actualGameHandler();
		sendEndGame();
	}
	
	/**
	 * To every player send the current game state
	 */
	public void sendGameSate() {
		int gamestate = currentGame.getGameState();
		for(int i =0; i<players.length; i++) {
			players[i].handleGAMESTATE(gamestate);
		}
	}
	
	/**
	 * When game is finished, send to every player the winner. If game was stopped, 
	 * because it was taking too long, winner is -1.
	 * 
	 */
	public void sendEndGame() {
		SOCPlayer pn = currentGame.getPlayerWithWin();
		int winner = -1;
		if (pn!=null) 
			winner = pn.getPlayerNumber();
		for(int i =0; i<players.length; i++) {
			players[i].handleEndGame(winner);
			
			/*DEBUG*/
//			players[i].memoryStats();
//			RLStrategyLookupTable_small st = (RLStrategyLookupTable_small) players[i].getRLStrategy();
//			st.printCounter();
//			System.out.println("Rounds: " + currentGame.getRoundCount());
		}
		
		/*if game is not testing, invoke testingHandler()*/
		if (isTraining && !gameType.contains("test")) {
			if (gamesPlayed<10000 && gamesPlayed%1000==0 && gamesPlayed!=0) {
         		testingHandler();
         	} else if (gamesPlayed%10000==0 && gamesPlayed!=0) {
//         		for (int i=0; i<players.length; i++) {
//    				players[i].changeLR(0.2);
//    			}
         		testingHandler();
         	}
		   	

			/*after given number of games, write the memory file*/
			/*TODO: should we also change LR?*/
        	if (gamesPlayed%synchroniseCount==0 && gamesPlayed!=0) {
        		
        		if (gameType.contains("shared")) {
        			players[0].getStateValueFunction().writeMemory("shared_" + gamesPlayed);
        		} else {
        			for (int i=0; i<players.length; i++) {
            			players[i].writeMemory(gamesPlayed);
//            			players[i].memoryStats();
//            			try
//                        {
//                            System.out.println("writing memory");
//                			Thread.sleep(120000);
//                        }
//                        catch (InterruptedException e) {}
            		}
        		}
        		
//        		/*SHARED memory*/
//        		
//        		if (gamesPlayed==50000) {
//        			for (int i=0; i<players.length; i++) {
//        				players[i].changeLR(0.2);
//        			}
//        		} else if (gamesPlayed==100000) {
//        			for (int i=0; i<players.length; i++) {
//        				players[i].changeLR(0.06);
//        			}
//        		} else if (gamesPlayed==150000) {
//        			for (int i=0; i<players.length; i++) {
//        				players[i].changeLR(0.02);
//        			}
//        		}
        		
//        		try
//                {
//                    System.out.println("writing memory");
//        			Thread.sleep(800000);
//                }
//                catch (InterruptedException e) {}            	
            }
        }	
	}
	
	/**
	 * Initial placement goes in two rounds. First every player is placing 
	 * one settlement and one road, in order of the players in the game. 
	 * In the second round the order is reversed and again every player is 
	 * placing one settlement and one road
	 */
	public void initialPlacementsHandler() {
		int cpn; 
		int settle;
		int road;
		while (currentGame.getGameState()< SOCGame.START2A) {
			cpn = currentGame.getCurrentPlayerNumber();
			sendTurn(cpn, currentGame.getGameState());
			settle = playersGameOrder[cpn].getFirstSettlement();
			handlePutPiece(cpn, SOCPlayingPiece.SETTLEMENT, settle);
			//Game State changes to START1B
			road = playersGameOrder[cpn].getInitRoad();
			handlePutPiece(cpn, SOCPlayingPiece.ROAD, road);
			//Game State back to START1A (until all players put their first settlement, 
			//when it changes to START2A = 10
		}
		
		while (currentGame.getGameState()< SOCGame.ROLL_OR_CARD) {
			cpn = currentGame.getCurrentPlayerNumber();
			sendTurn(cpn, currentGame.getGameState());
			settle = playersGameOrder[cpn].getSecondSettlement();
			handlePutPiece(cpn, SOCPlayingPiece.SETTLEMENT, settle);
			//resources are given by client version of game
			//Game State changes to START2B
			road = playersGameOrder[cpn].getInitRoad();
			handlePutPiece(cpn, SOCPlayingPiece.ROAD, road);
			//Game State back to START2A (until all player put their second settlement,
			//when it changes to ROLL_OR_CARD = 15
		}
		
		currentGame.updateAtTurn();
		sendTurn(currentGame.getCurrentPlayerNumber(), currentGame.getGameState());
		
		/*DEBUG*/
//		System.out.println("Finished initial placement");
//		debugStats(0, 0);
		
	}
	
	/**
	 * Runs whole game after initial placement. Actions chosen by clients
	 * are run in the loop until game state of the game is {@link SOCGame#OVER} or
	 * more than the given number of rounds is played (limit usually set to 500)
	 */
	public void actualGameHandler()  {
		int cpn;

		while(currentGame.getGameState()< SOCGame.OVER) {
			cpn = currentGame.getCurrentPlayerNumber();
			sendTurn(cpn, currentGame.getGameState());
			int action = -1;
			
			if (currentGame.getGameState() == SOCGame.ROLL_OR_CARD) {
				action = playersGameOrder[cpn].rollOrPlayKnight();
				handleAction(action, cpn);
				
//				/*DEBUG*/
//				for (int j =0; j<playersGameOrder.length; j++) {
//					SOCResourceSet resGame = currentGame.getPlayer(j).getResources();
//					for (int i = 0; i< playersGameOrder.length; i++) {
//						SOCResourceSet res = playersGameOrder[i].getGame().getPlayer(j).getResources();
//						
//						if (resGame.getTotal() != res.getTotal()) {
//							debugStats(cpn, action);
//						}
//					}
//				}
//				
//				debugStats(cpn, action);
				
				if (action==RLStrategy.PLAY_KNIGHT && currentGame.getGameState() == SOCGame.ROLL_OR_CARD) {
					handleRoll(cpn); //after roll state will be PLAY1 or WAITING FOR DISCARDS
									// discrds are handled in handleRoll(cpn)
					
//					debugStats(cpn, action);
				}
			}
			
			while (currentGame.getGameState() == SOCGame.PLAY1 && action != RLStrategy.END_TURN) {
				action = playersGameOrder[cpn].buildOrTradeOrPlayCard();
				
//				/*DEBUG*/
//				if (action==8) {
//					debugStats(cpn, action);
//				}
				
				handleAction(action, cpn);	
				
//				debugStats(cpn, action);
				
//				/*DEBUG*/
//				for (int j =0; j<playersGameOrder.length; j++) {
//					SOCResourceSet resGame = currentGame.getPlayer(j).getResources();
//					for (int i = 0; i< playersGameOrder.length; i++) {
//						SOCResourceSet res = playersGameOrder[i].getGame().getPlayer(j).getResources();
//						
//						if (resGame.getTotal() != res.getTotal()) {
//							debugStats(cpn, action);
//							break;
//						}
//					}
//				}
				
			}
			
//			/*DEBUG*/
//			System.out.println("Finished turn player: " + cpn);
//		
//			System.out.println("RESOURCES EQUAL: ");
//			System.out.println("player 0: game: " + currentGame.getPlayer(0).getResources().toFriendlyString());
//			System.out.println("        client: " + playersGameOrder[0].getOurPlayerData().getResources().toFriendlyString());
//			System.out.println("player 1: game: " + currentGame.getPlayer(1).getResources().toFriendlyString());
//			System.out.println("        client: " + playersGameOrder[1].getOurPlayerData().getResources().toFriendlyString());
//			System.out.println("player 2: game: " + currentGame.getPlayer(2).getResources().toFriendlyString());
//			System.out.println("        client: " + playersGameOrder[2].getOurPlayerData().getResources().toFriendlyString());
//			System.out.println("player 3: game: " + currentGame.getPlayer(3).getResources().toFriendlyString());
//			System.out.println("        client: " + playersGameOrder[3].getOurPlayerData().getResources().toFriendlyString());
		
//			/*DEBUG*/
//			for (int j =0; j<playersGameOrder.length; j++) {
//				SOCResourceSet resGame = currentGame.getPlayer(j).getResources();
//				for (int i = 0; i< playersGameOrder.length; i++) {
//					SOCResourceSet res = playersGameOrder[i].getGame().getPlayer(j).getResources();
//					
//					if (resGame.getTotal() != res.getTotal()) {
//						debugStats(cpn, action);
//					}
//					System.out.println("action: " + action + " in player: " + i + " player: " + j
//							+ " res: " + Arrays.toString(res.getAmounts(true)) 
//							+ " gameRes	: " + Arrays.toString(resGame.getAmounts(true))  );
//				
//				}	
//			}
			
//			/*DEBUG*/
//			Iterator<SOCPlayer> playersIter = playersState.values().iterator();
//	    	state.updateAll(playersState, currentGame.getBoard());
//		    while (playersIter.hasNext()) {
//		    	SOCPlayer pn = playersIter.next();
//		    	
////	    		List<Integer> playerState = Arrays.stream(state.getState(pn)).boxed().collect(Collectors.toList());
//	    		
//	    		/*DEBUG*/
//	    		System.out.println("In GAME!: player " 
//	    					+ pn.getPlayerNumber() + " state: " 
//	    					+ Arrays.toString(playerState.toArray()));
//	    		pn.stats();
//		    }
			
			
			if (currentGame.getRoundCount()>500) {
				System.out.println("Game too long, ending");
				break;
			}
		}
		
		/*STATS WRITING*/
		for (SOCPlayer pn : currentGame.getPlayers()) {
        	pn.writeStats(currentGame.getName(), fileName);  
//        	pn.stats();
        }
//		players[0].memoryStats();
        writeStats(currentGame.getName());
        
//        for (int i=0; i<players.length; i++) {
//			players[i].memoryStats();
//		}
	}
	
	public void testingHandler() {
		
		/*DEBUGA*/
		System.out.println("Testing mode");
		
		RLClient[] playersTemporary = new RLClient[4];
		for (int i=0; i<players.length; i++) {
			playersTemporary[i] = players[i];
			players[i].startTesting();
		}
		
		String fileNameTemporary = fileName;
		fileName = fileName + "_testing";
		String gameTypeTemporary = gameType;
		gameType = "testRandomNN";
		
//		/*for lookup table player*/
//		gameType = "testRandomLT";
//		players[0] = new RLClient(0, RLClient.TEST_LOOKUP_TABLE, memoryType);
//		players[0].setStateValueFunction(playersTemporary[0].getStateValueFunction());
//		//END FOR LT
		
//		players[1] = new RLClient(1, RLClient.RANDOM, -1);
//		players[2] = new RLClient(2, RLClient.RANDOM, -1);
//		players[3] = new RLClient(3, RLClient.RANDOM, -1);
		
//		createPlayers();
		createPlayersFromGameType();
		players[0] = playersTemporary[0];
				
		int testGames = 300;
		while (testGames>0) {
        	String gaName = "~testGameAfter" + gamesPlayed + "~"  + testGames;
        	startGame(gaName);
        	testGames--;
		}
		
		players = playersTemporary;
		fileName = fileNameTemporary;
		gameType = gameTypeTemporary;
		
		/*for NN players*/
		for (int i=0; i<players.length; i++) {
			players[i].startTraining();
		}
		//END FOR NN
		
	}
	
	
	/**
	 * 
	 * @param pn - player number
	 * @param pieceType - piece type
	 * @param coord - coordinations where to put the piece
	 */
	public void handlePutPiece(int pn, int pieceType, int coord) {
		int cpn = currentGame.getCurrentPlayerNumber();
		if (pn!= cpn) {
			/*building error, player tried to build although it's not his turn*/
			System.err.println("Put Piece error: " + playersGameOrder[pn].getName() + " wanted to build, "
					+ "but it's " + cpn + " turn.");
			return;
		}
		
		
		
		int gameState = currentGame.getGameState();
		SOCPlayer player = currentGame.getPlayer(pn);
		
		switch (pieceType)
        {
        case SOCPlayingPiece.ROAD:

            if ((gameState == SOCGame.START1B) || (gameState == SOCGame.START2B) || (gameState == SOCGame.START3B)
                || (gameState == SOCGame.PLACING_ROAD)
                || (gameState == SOCGame.PLACING_FREE_ROAD1) || (gameState == SOCGame.PLACING_FREE_ROAD2))
            {
                if (player.isPotentialRoad(coord) && (player.getNumPieces(SOCPlayingPiece.ROAD) >= 1))
                {
                    final SOCRoad rd = new SOCRoad(player, coord, null);
                    currentGame.putPiece(rd);  // Changes game state and (if initial placement) player

                    // If placing this piece reveals a fog hex, putPiece will call srv.gameEvent
                    // which will send a SOCRevealFogHex message to the game.

                    sendPutPiece(pn, pieceType, coord);

                    //not sending game state, because player changes
                    // If needed, call sendTurn or send SOCRollDicePrompt; maybe important after
//                    handler.sendTurnStateAtInitialPlacement(ga, player, c, gameState);

                } else {
                	SOCPlayingPiece pp = currentGame.getBoard().roadOrShipAtEdge(coord);
                	System.err.println("ILLEGAL ROAD on: " + Integer.toHexString(coord) 
                		+ ": player " + pn + " - pl.isPotentialRoad: "
                        + player.isPotentialRoad(coord)
                        + " - roadExistsAtNode: " + ((pp != null) ? pp : "none"));
                	
                	HashSet<Integer> gameroads = player.getPotentialRoads();
                	Integer[] gameRoads = gameroads.toArray(new Integer[gameroads.size()]);
                	HashSet<Integer> playerroads = playersGameOrder[pn].getOurPlayerData().getPotentialRoads();
                	Integer[] playerRoads = playerroads.toArray(new Integer[playerroads.size()]);
                	int set2away = playersGameOrder[pn].getSet2Away(); 
                	System.err.println("gamePotRoads: " + Arrays.toString(gameRoads) +
                			" playerPotRoads: " + Arrays.toString(playerRoads) + 
                			" road we chose: " + coord +
                			". Road to set " + set2away + " road there: " + 
                			currentGame.getBoard().getAdjacentEdgeToNode2Away(coord,set2away) );
                	
                	HashSet<Integer> sets2away = new HashSet<Integer>();
                	for (int facing = 1; facing <= 6; ++facing)
                    {
                        // each of 6 directions: NE, E, SE, SW, W, NW
                        int tmp = currentGame.getBoard().getAdjacentNodeToNode2Away(coord, facing);
                        if ((tmp != -9) && playersGameOrder[pn].getOurPlayerData().canPlaceSettlement(tmp))
                        	sets2away.add(new Integer(tmp));
                    }
                	
                	System.err.println("Set2Away: " + 
                	Arrays.toString(sets2away.toArray(new Integer[sets2away.size()])));
                	
                	
                	
                	System.err.println("gameLastSet: " + player.getLastSettlementCoord()
                			+ " playerLastSet: " + playersGameOrder[pn].getOurPlayerData().getLastSettlementCoord());      	  	
                	
                	
                	
                	playersGameOrder[pn].getOurPlayerData().clearPotentialRoad(coord);
                	if (gameState <= SOCGame.START3B)
                    {
                        // needed for placeInitRoad() calculations
                		playersGameOrder[pn].cancelBuildRoadAtInit();
                    }
                	
                }
            } else {
            	System.err.println("Cannot place Road right now. Player: " + pn);
            }

            break;

        case SOCPlayingPiece.SETTLEMENT:

            if ((gameState == SOCGame.START1A) || (gameState == SOCGame.START2A)
                || (gameState == SOCGame.START3A) || (gameState == SOCGame.PLACING_SETTLEMENT))
            {
                if (player.canPlaceSettlement(coord) && (player.getNumPieces(SOCPlayingPiece.SETTLEMENT) >= 1))
                {
                    final SOCSettlement se = new SOCSettlement(player, coord, null);
                    currentGame.putPiece(se);   // Changes game state and (if initial placement) player
                    sendPutPiece(pn, pieceType, coord);

                    /* do i need to change turn or send gamestate?*/
                    /* in initial settlement it's still the same player, in normal turn
                     * player can place a settlement and do sth else before ending his turn
                     * in original: sending TURN after both road and settlement
                     */
//                    if (! handler.checkTurn(c, ga))
//                        handler.sendTurn(ga, false);  // Announce new state and new current player
//                    else
//                        handler.sendGameState(ga);
                    sendGameSate();


                } else {
                	SOCPlayingPiece pp = currentGame.getBoard().settlementAtNode(coord);
                	System.err.println("ILLEGAL SETTLEMENT on: " + Integer.toHexString(coord) 
                		+ ": player " + pn + " - pl.isPotentialSettlement: "
                        + player.isPotentialSettlement(coord)
                        + " - settlementExistsAtNode: " + ((pp != null) ? pp : "none"));
                	
                	playersGameOrder[pn].getOurPlayerData().clearPotentialSettlement(coord);
                }
                
            } else {
            	System.err.println("Cannot place Settlement right now. Player: " + pn);
            }

            break;

        case SOCPlayingPiece.CITY:

            if (gameState == SOCGame.PLACING_CITY)
            {
                if (player.isPotentialCity(coord) && (player.getNumPieces(SOCPlayingPiece.CITY) >= 1))
                {

                    final SOCCity ci = new SOCCity(player, coord, null);
                    currentGame.putPiece(ci);  // changes game state and maybe player
                    sendPutPiece(pn, pieceType, coord);
                    sendGameSate();

                } else {
                	
                	SOCPlayingPiece pp = currentGame.getBoard().settlementAtNode(coord);
                	System.err.println("ILLEGAL CITY on: " + Integer.toHexString(coord) 
                		+ ": player " + pn + " - pl.isPotentialCity: "
                        + player.isPotentialCity(coord)
                        + " - city/settlementExistsAtNode: " + ((pp != null) ? pp : "none"));
                	
                	playersGameOrder[pn].getOurPlayerData().clearPotentialCity(coord);
                }
            } else {
            	System.err.println("Cannot place City right now. Player: " + pn);
            }

            break;
            


        } //switch(pieceType)
		
//		if (sendDenyRequest)
//        {        		
//			if (isBuyAndPut)
//                handler.sendGameState(ga);  // is probably now PLACING_*, was PLAY1 or SPECIAL_BUILDING
//            srv.messageToPlayer(c, new SOCCancelBuildRequest(gaName, mes.getPieceType()));
//            if (player.isRobot())
//            {
//                // Set the "force end turn soon" field
//                ga.lastActionTime = 0L;
//            }
//        }
				
	}
	
	/**
	 * Inform all clients about a piece that was built
	 * @param pn - player number
	 * @param pieceType - piece type
	 * @param coord - coordinations where to put the piece
	 */
	public void sendPutPiece(int pn, int pieceType, int coord) {
		for(int i =0; i<players.length; i++) {
			players[i].handlePUTPIECE(pn, pieceType, coord);
		}
	}
	
	/**
	 * Inform all clients about change in turn. 
	 * {@link RLClient#handleTURN(player number, game state)} is invoked for every client.
	 * Also for the current player the playedDevCardFlag is set to false
	 * 
	 * @param pn - current player
	 * @param gs - game state
	 */
	public void sendTurn(int pn, int gs) {
		for(int i =0; i<players.length; i++) {
			players[i].handleTURN(pn, gs);
		}
		playersGameOrder[pn].handlePlayedDevCardFlag(pn, false);
	}
	
	/**
	 * All actions are handled in switch statement
	 * @param action - id of the action chosen by the player as in {@link RLStrategy#ROLL}
	 * @param pn - player who takes tha action
	 */
	public void handleAction(int action, int pn) {
		int cpn = currentGame.getCurrentPlayerNumber();
		if ( pn != cpn ) {
			/*action error, player tried to build although it's not his turn*/
			System.err.println("Action error: " + playersGameOrder[pn].getName() + " wanted to build, "
					+ "but it's " + cpn + " turn.");
			return;
		}
		
//		/*DEBUG*/
//		System.out.println("GS: " + currentGame.getGameState() + " action: " + action + " cpn: " + cpn 
//				+ " roll: " + currentGame.getCurrentDice());
		
		switch(action)
		{
		case RLStrategy.ROLL:
			handleRoll(pn);
			break;
			
		case RLStrategy.PLAY_KNIGHT:
			handlePlayDevCard(pn, SOCDevCardConstants.KNIGHT);
			if (currentGame.getGameState() == SOCGame.PLACING_ROBBER) {
				int robberPosition = playersGameOrder[pn].getRobberPlace();
				handleMoveRobber(pn, robberPosition);
			}
			break;
			
		case RLStrategy.END_TURN:
			handleEndTurn(pn);
			break;
			
		case RLStrategy.TRADE_BANK:
			SOCResourceSet[] bankTradeOffer =  playersGameOrder[pn].getBankTradeOffer();
			handleBankTrade(pn, bankTradeOffer[0], bankTradeOffer[1]);
			break;
			
		case RLStrategy.PLAY_ROADS:
			handlePlayDevCard(pn, SOCDevCardConstants.ROADS);
			int[] roadsToBuild = playersGameOrder[pn].getRoadsToBuildCoord();
			handlePutPiece(cpn, SOCPlayingPiece.ROAD, roadsToBuild[0]);
			handlePutPiece(cpn, SOCPlayingPiece.ROAD, roadsToBuild[1]);
			break;
			
		case RLStrategy.PLAY_DISC:
			handlePlayDevCard(pn, SOCDevCardConstants.DISC);
			if (currentGame.getGameState() == SOCGame.WAITING_FOR_DISCOVERY) {
				SOCResourceSet resChoice = playersGameOrder[pn].getDicoveryResources();
				if (currentGame.canDoDiscoveryAction(resChoice)) {
					currentGame.doDiscoveryAction(resChoice);
					sendRsrcGainLoss(-1, resChoice, false, pn, -1);
				} else {
					System.err.println("Cannot do discovery!");
				}
			}
			break;
			
		case RLStrategy.PLAY_MONO:
			handlePlayDevCard(pn, SOCDevCardConstants.MONO);
			if (currentGame.getGameState() == SOCGame.WAITING_FOR_MONOPOLY) {
				if (currentGame.canDoMonopolyAction()){
					handleMonopoly(pn);	
	             } else {
	            	 System.err.println("Cannot do monopoly!");
	             }
			}
			
			break;
			
		case RLStrategy.PLACE_SETTLEMENT:
			int settle = playersGameOrder[pn].getBuildingCoord();
			handleBuildRequest(pn, SOCPlayingPiece.SETTLEMENT, settle);
			if (currentGame.getGameState() == SOCGame.PLACING_SETTLEMENT) {
				handlePutPiece(pn, SOCPlayingPiece.SETTLEMENT, settle); //gs = PLAY1
			}
			
			break;
			
		case RLStrategy.PLACE_ROAD:
			int road = playersGameOrder[pn].getBuildingCoord();
			handleBuildRequest(pn, SOCPlayingPiece.ROAD, road);
			if (currentGame.getGameState() == SOCGame.PLACING_ROAD) {
				handlePutPiece(pn, SOCPlayingPiece.ROAD, road); //gs = PLAY1
			}
			break;
			
		case RLStrategy.PLACE_CITY:
			int city = playersGameOrder[pn].getBuildingCoord();
			handleBuildRequest(pn, SOCPlayingPiece.CITY, city);
			if (currentGame.getGameState() == SOCGame.PLACING_CITY) {
				handlePutPiece(pn, SOCPlayingPiece.CITY, city); //gs = PLAY1
			}
			break;
			
		case RLStrategy.BUY_DEVCARD:
			handleBuyDevCard(pn);
			break;
		
		}
		
			
	}
	
	/**
	 * Card is being played at the game, then every player is sent information
	 * about card being played, the playedDevCardFlag is set to true and
	 * game state is sent. Action specific to the card (like moving robber
	 * or picking resource is not handled here, but in {@link #handleAction(action, player)}
	 * 
	 * @param pn - id of the client who played the dev card
	 * @param type - type of development card being played
	 */
	public void handlePlayDevCard(int pn, int type) {
		switch (type)
        {
        case SOCDevCardConstants.KNIGHT:
        	if (currentGame.canPlayKnight(pn)) {
        		currentGame.playKnight();
        		sendDevCardAction(pn, type, SOCDevCardAction.PLAY);
        		sendPlayedDevCardFlag(pn, true);
        		sendPlayerElement_numKnights(pn, SOCPlayerElement.GAIN, 1);
        		sendGameSate();
        	} else {
                System.err.println("You can't play KNIGHT card right now"); 
            } 
        	break;
        
        case SOCDevCardConstants.ROADS:
        	 if (currentGame.canPlayRoadBuilding(pn)) {
        		 currentGame.playRoadBuilding(); 
        		 //gs = PLACING_FREE_ROAD1 or PLACING_FREE_ROAD2 if only 1 road left
        		 sendDevCardAction(pn, type, SOCDevCardAction.PLAY);
        		 sendPlayedDevCardFlag(pn, true); 
        		 sendGameSate(); //maybe not needed
        	 } else {
                 System.err.println("You can't play ROADS card right now"); 
             } 
        	 break;
        	 
        case SOCDevCardConstants.DISC:
            if (currentGame.canPlayDiscovery(pn))
            {
            	currentGame.playDiscovery(); //gs = WAITING_FOR_DISCOVERY
            	sendDevCardAction(pn, type, SOCDevCardAction.PLAY);
            	sendPlayedDevCardFlag(pn, true);
            	sendGameSate(); //maybe not needed
            } else {
                System.err.println("You can't play DISC card right now"); 
            } 
            break;
            
        case SOCDevCardConstants.MONO:
        	if (currentGame.canPlayMonopoly(pn)) {
        		currentGame.playMonopoly(); //gs = WAITING_FOR_MONOPOLY
        		sendDevCardAction(pn, type, SOCDevCardAction.PLAY);
        		sendPlayedDevCardFlag(pn, true);
        		sendGameSate(); //maybe not needed
              } else {
                  System.err.println("You can't play MONO card right now"); 
              } 
                		  
           
        }
	}
	
	/**
	 * Information about card being removed, played or added 
	 * is sent to every player
	 * @param pn - player who played the dev card
	 * @param ctype - type of development card
	 * @param action - as in {@link SOCDevCardAction#PLAY}
	 */
	public void sendDevCardAction(int pn, int ctype, int action) {
		for(int i =0; i<players.length; i++) {
			players[i].handleDEVCARDACTION(pn, ctype, action);
		}
	}
	
	public void sendPlayerElement(int pn, int type, int action, int amount) {
		for(int i =0; i<players.length; i++) {
			players[i].handlePLAYERELEMENT(pn, action, type, amount);
		}
	}
	
	/**
	 * Sent to player(s) information about amount of resources being changed.
	 * @param msgTo id of client to whom message is sent, if -1 it is sent to all the clients
	 * @param pn player for whom to change resource number
	 * @param type type of resource
	 * @param action as in {@link SOCPlayerElement#GAIN}
	 * @param amount
	 */
	public void sendPlayerElement_numRscs(int msgTo, int pn, int type, int action, int amount) {
		if (msgTo==-1) {
			for(int i =0; i<players.length; i++) {
				playersGameOrder[i].handlePlayerElement_numRscs(pn, type, action, amount);
			}
		} else {
			playersGameOrder[msgTo].handlePlayerElement_numRscs(pn, type, action, amount);
		}
//		playersGameOrder[msgTo].handlePlayerElement_numRscs(pn, type, action, amount);
	}
	
	/**
	 * Send information about number of knights that are in possesion of the given player.
	 * Called after knight card was played.
	 * @param pn - player for whom to change number of knights
	 * @param action - as in {@link SOCPlayerElement#GAIN}
	 * @param amount 
	 */
	public void sendPlayerElement_numKnights(int pn, int action, int amount) {
		for(int i =0; i<players.length; i++) {
			players[i].handlePLAYERELEMENT_numKnights(pn, action, amount);
		}
	}
	
	/**
	 * Called at the beginning of the game, when the flag is set to false and
	 * after the dev card is played, when it's set to true. Player can play 
	 * only one card during his turn.
	 * 
	 * @param pn player for which to set playedDevCardFlag
	 * @param value of the flag
	 */
	public void sendPlayedDevCardFlag(int pn, boolean value) {
		for(int i =0; i<players.length; i++) {
			players[i].handlePlayedDevCardFlag(pn, value);
		}
	}
	
	/**
	 * All clients are informed about the new location of the robber and
	 * robbery is handled. If client gave incorrect id of the victim, random 
	 * victim is chosen from eligible victims
	 * 
	 * @param pn - player who moves the robber
	 * @param coord - id of hex, where robber is being moved
	 */
	public void handleMoveRobber(int pn, int coord) {
		if (currentGame.canMoveRobber(pn, coord)) {
			
			SOCMoveRobberResult result = currentGame.moveRobber(pn, coord);
			sendRobberMoved(coord);
			final List<SOCPlayer> victims = result.getVictims();
			
			/*DEBUG*/
//			debugStats(pn, -1);
			
			if (victims.size() == 1) {
				 SOCPlayer victim = victims.get(0);
				 handleRobbery(victim.getPlayerNumber(), pn, result.getLoot());
			} else if (victims.size() > 1) {
				int victim = playersGameOrder[pn].getRobberVictim();
				if (currentGame.canChoosePlayer(victim))
                {
                    final int rsrc = currentGame.choosePlayerForRobbery(victim);
                    handleRobbery(victim, pn, rsrc);
                } 
				else {
					System.err.println("Robbery mistake: cannot rob this player. Rob on: " + coord);
					int[] pnNums = new int[victims.size()];
					for (int i=0; i< victims.size(); i++) {
						pnNums[i] = victims.get(i).getPlayerNumber();
					}
					SOCPlayer vic = playersGameOrder[victim].getOurPlayerData();
					System.err.println("Player " + pn + 
							" Victims we found: " + Arrays.toString(playersGameOrder[pn].getVictims(coord))
							+ " from whom we chose " + victim + " game victims: " + 
							Arrays.toString(pnNums) 
							+ " victim has res:" + vic.getResources().toFriendlyString());

					/*DEBUGA*/
					debugStats(pn, -1);
					
					int forcedVictim = victims.get(0).getPlayerNumber();
					if (currentGame.canChoosePlayer(forcedVictim))
	                {
						final int rsrc = currentGame.choosePlayerForRobbery(forcedVictim);
	                    handleRobbery(forcedVictim, pn, rsrc);
	                }
				}
			}
		} else {
			System.err.println("You cannot move robber there");
		}
	}
	
	/**
	 * Every client is sent the information about the location of the robber. 
	 * Send after knight card was played or 7 was rolled.
	 * @param coord - id of hex where robber is moved
	 */
	public void sendRobberMoved(int coord) {
		for(int i =0; i<players.length; i++) {
			players[i].handleRobberMoved(coord);
		}
	}
	
	/**
	 * Both victim and player who robs are sent information what resource was stolen.
	 * Other players get information about {@link SOCPlayerElement#UNKNOWN}
	 * resource stolen
	 * 
	 * @param victim - player from whom resources are stolen
	 * @param pn - player who steals resources
	 * @param rsrc - resource being stolen
	 */
	public void handleRobbery(int victim, int pn, int rsrc) {

		sendPlayerElement_numRscs(pn, pn, rsrc, SOCPlayerElement.GAIN, 1);
		sendPlayerElement_numRscs(pn, victim, rsrc, SOCPlayerElement.LOSE, 1);
		sendPlayerElement_numRscs(victim, pn, rsrc, SOCPlayerElement.GAIN, 1);
		sendPlayerElement_numRscs(victim, victim, rsrc, SOCPlayerElement.LOSE, 1);
		
		for(int i =0; i<players.length; i++) {
			if (i==victim || i ==pn) {
				continue;
			} else {
				sendPlayerElement_numRscs(i, victim, SOCPlayerElement.UNKNOWN, SOCPlayerElement.LOSE, 1);
				sendPlayerElement_numRscs(i, pn, SOCPlayerElement.UNKNOWN, SOCPlayerElement.GAIN, 1);
			}
				
		}
	}
	
	
	/**
	 * After roll sent to every player resources everyone gained. 
	 * If 7 was rolled wait for discard and then move robber.
	 * @param pn - player who's turn it is to roll
	 */
	public void handleRoll(int pn) {
		
		if (currentGame.canRollDice(pn)) {
			
			SOCGame.RollResult roll = currentGame.rollDice();
			//sendDicerresult( game.setCurrentDice(((SOCDiceResult) mes).getResult());)
			 if (currentGame.getCurrentDice() != 7) {
				 
				 for(int i =0; i<players.length; i++) {
					 
					 if (!currentGame.isSeatVacant(i))
                     {
                         SOCPlayer pp = currentGame.getPlayer(i);
                         SOCResourceSet rsrcs = pp.getRolledResources();

                         if (rsrcs.getKnownTotal() != 0)
                         {
                        	 sendRsrcGainLoss(-1, rsrcs, false, i, -1);
                         }
                     }
				 }
             } else {
            	 if (currentGame.getGameState() == SOCGame.WAITING_FOR_DISCARDS) {
                     sendGameState_sendDiscardRequests();
                 }
            	 if (currentGame.getGameState() == SOCGame.PLACING_ROBBER) {
     				int robberPosition = playersGameOrder[pn].moveRobber();
     				handleMoveRobber(pn, robberPosition);
     			}
             }

			//if 7 check for discards and call waiting for discard
			//send gainlos rsrc
		} else {
			System.err.println("Cannot roll now. gs=" + currentGame.getGameState());
		}
	}
	
	/**
	 * to report known gains and losses: after trade or dice roll
	 * 
	 * @param msgTo id of client to whom message is sent, if -1 it is sent to all the clients
	 * @param resourceSet resources being reported
	 * @param isLoss is it loss, for example after discard
	 * @param mainPlayer player for whom we report main action (gain)
	 * @param tradingPlayer if function was called after trade
	 */
	public void sendRsrcGainLoss(int msgTo, ResourceSet resourceSet, boolean isLoss,  int mainPlayer, int tradingPlayer){
		//TODO: can be optimized
		final int losegain  = isLoss ? SOCPlayerElement.LOSE : SOCPlayerElement.GAIN;  // for pnA
        final int gainlose  = isLoss ? SOCPlayerElement.GAIN : SOCPlayerElement.LOSE;  // for pnB
        
        for (int res = SOCResourceConstants.CLAY; res <= SOCResourceConstants.WOOD; ++res)
        {
            // This works because SOCPlayerElement.SHEEP == SOCResourceConstants.SHEEP.

            final int amt = resourceSet.getAmount(res);
            if (amt <= 0)
                continue;
            
            if (msgTo==-1) {
            	for(int i =0; i<players.length; i++) {
                	sendPlayerElement_numRscs(i, mainPlayer, res, losegain, amt);
                	
                	if(tradingPlayer!=-1) {
                		sendPlayerElement_numRscs(i, tradingPlayer, res, gainlose, amt);
                	}
                	
                }
            } else {
            	
            	sendPlayerElement_numRscs(msgTo, mainPlayer, res, losegain, amt);
            	
            	if(tradingPlayer!=-1) {
            		sendPlayerElement_numRscs(msgTo, tradingPlayer, res, gainlose, amt);
            	}
            	
            }
        }
	}
	
	/**
	 * After 7 was rolled. Checks if any player needs to discard and asks him
	 * for resources he wants to discard. Player is then informed about resources 
	 * he got rid of and the rest of the players are informes about the number
	 * of unknown resources being discarded.
	 * 
	 * If the set to discard chosen by player is invalid, random set of his resources
	 * are discarded.
	 */
	public void sendGameState_sendDiscardRequests()
    {
		 for(int i =0; i<players.length; i++) {
			 
            final SOCPlayer pl = currentGame.getPlayer(i);
            if (( ! currentGame.isSeatVacant(i)) && pl.getNeedToDiscard())
            {
            	int numDiscards = pl.getResources().getTotal() / 2;
            	SOCResourceSet resourceSet = playersGameOrder[i].handleDiscard(numDiscards);
            	
            	if (currentGame.canDiscard(i,resourceSet)) {
            		currentGame.discard(i,resourceSet);
            		//tell the player client that the player discarded the resources
            		sendRsrcGainLoss(i, resourceSet, true, i, -1);
            		//tell everyone else that the player discarded unknown resources
            		for(int j =0; j<players.length; j++) {
            			if (i==j)
            				continue;
                    	sendPlayerElement_numRscs(j, i, SOCPlayerElement.UNKNOWN, SOCPlayerElement.LOSE, numDiscards);
            		}
            	} else {
            		System.err.println("Player " + i + " You can't discard these cards");
            		System.err.println("We wated to discard: " + resourceSet.toFriendlyString() 
            			+ " we have: " + currentGame.getPlayer(i).getResources().toFriendlyString());
            		
            		SOCResourceSet discardsn = new SOCResourceSet();
                    SOCGame.discardOrGainPickRandom(
                    	currentGame.getPlayer(i).getResources(), numDiscards, 
                  		true, discardsn, rand);
                    
                    currentGame.discard(i,discardsn);
            		//tell the player client that the player discarded the resources
            		sendRsrcGainLoss(i, discardsn, true, i, -1);
            		//tell everyone else that the player discarded unknown resources
            		for(int j =0; j<players.length; j++) {
            			if (i==j)
            				continue;
                    	sendPlayerElement_numRscs(j, i, SOCPlayerElement.UNKNOWN, SOCPlayerElement.LOSE, numDiscards);
            		}
            		
            	}
            }
        }
    }
	
	/**
	 * We check if player can end the turn and inform all clients about change in turn 
	 * @param pn player that ends the turn
	 */
	public void handleEndTurn(int pn) {
		
		SOCPlayer pl = currentGame.getPlayer(pn);
		if ((pl != null) && currentGame.canEndTurn(pn)) {
			currentGame.endTurn();
			sendTurn(pn, currentGame.getGameState());	
		} else {
			System.err.println("You can't end the turn");
		}
	}
	
	/**
	 * Every client is informed what resource was monopolized and how many
	 * cards the player gained. Losses of all the other players are also reported.
	 * 
	 * @param pn player who played monopoly card
	 */
	public void handleMonopoly(int pn) {
		
		final int rsrc = playersGameOrder[pn].getMonopolyChoice();
        final int[] monoPicks = currentGame.doMonopolyAction(rsrc);
        final boolean[] isVictim = new boolean[currentGame.maxPlayers];
        final int cpn = currentGame.getCurrentPlayerNumber();
        int total = 0;
        for (int i = 0; i < players.length; ++i) {
            final int n = monoPicks[i];
            if (n > 0) {
                isVictim[i] = true;
                total += n;
            }
        }
        
        /*tell everyone what the player gained by monopolizing*/
        sendPlayerElement_numRscs(-1, cpn, rsrc, SOCPlayerElement.GAIN, total);
        
        /*tell everyone who lost what*/
        for (int i = 0; i < currentGame.maxPlayers; ++i)
            if ((i != cpn) && isVictim[i])  {
            	sendPlayerElement_numRscs(-1, i, rsrc, SOCPlayerElement.LOSE,
            			monoPicks[i]);
            }
        		


	}
	
	/**
	 * buy building piece and report resources used
	 * @param pn player who wants to build
	 * @param type type of piece that is built
	 */
	public void handleBuildRequest(int pn, int type, int coord) {
		
		SOCPlayer player = currentGame.getPlayer(pn);
		
		 switch (type)
	        {
	        case SOCPlayingPiece.ROAD:

	            if (currentGame.couldBuildRoad(pn) && player.isPotentialRoad(coord))
	            {
	            	currentGame.buyRoad(pn); //gs = PLACING_ROAD
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.CLAY, SOCPlayerElement.LOSE, 1);
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.WOOD, SOCPlayerElement.LOSE, 1);
	            } else {
	            	SOCPlayer cpn = currentGame.getPlayer(pn);
	            	SOCResourceSet resources = cpn.getResources();
	            	System.err.println("Cannot build road now! resources: " + resources.toFriendlyString() +
	            			" has potential road: " + cpn.hasPotentialRoad());
	            	
	            	SOCPlayingPiece pp = currentGame.getBoard().roadOrShipAtEdge(coord);
                	System.err.println("ILLEGAL ROAD on: " + Integer.toHexString(coord) 
                		+ ": player " + pn + " - pl.isPotentialRoad: "
                        + player.isPotentialRoad(coord)
                        + " - roadExistsAtNode: " + ((pp != null) ? pp : "none"));
	            }

	            break;

	        case SOCPlayingPiece.SETTLEMENT:

	            if (currentGame.couldBuildSettlement(pn)  && player.isPotentialSettlement(coord))
	            {
	            	currentGame.buySettlement(pn); //gs = PLACING_SETTLEMENT
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.CLAY, SOCPlayerElement.LOSE, 1);
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.WOOD, SOCPlayerElement.LOSE, 1);
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.SHEEP, SOCPlayerElement.LOSE, 1);
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.WHEAT, SOCPlayerElement.LOSE, 1);
	            	
	            } else {
	            	SOCPlayer cpn = currentGame.getPlayer(pn);
	            	SOCResourceSet resources = cpn.getResources();
	            	System.err.println("Cannot build settlement now! resources: " + resources.toFriendlyString() +
	            			" has potential settlement: " + cpn.hasPotentialSettlement());
	            	
	            	SOCPlayingPiece pp = currentGame.getBoard().settlementAtNode(coord);
                	System.err.println("ILLEGAL SETTLEMENT on: " + Integer.toHexString(coord) 
                		+ ": player " + pn + " - pl.isPotentialSettlement: "
                        + player.isPotentialSettlement(coord)
                        + " - settlementExistsAtNode: " + ((pp != null) ? pp : "none"));
	            }

	            break;

	        case SOCPlayingPiece.CITY:

	            if (currentGame.couldBuildCity(pn)  && player.isPotentialCity(coord))
	            {
	            	currentGame.buyCity(pn); //gs = PLACING_CITY
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.ORE, SOCPlayerElement.LOSE, 3);
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.WHEAT, SOCPlayerElement.LOSE, 2);
	            	
	            } else {
	            	SOCPlayer cpn = currentGame.getPlayer(pn);
	            	SOCResourceSet resources = cpn.getResources();
	            	System.err.println("Cannot build city now! resources: " + resources.toFriendlyString() +
	            			" has potential city: " + cpn.hasPotentialCity());
	            	
	            	SOCPlayingPiece pp = currentGame.getBoard().settlementAtNode(coord);
                	System.err.println("ILLEGAL CITY on: " + Integer.toHexString(coord) 
                		+ ": player " + pn + " - pl.isPotentialCity: "
                        + player.isPotentialCity(coord)
                        + " - city/settlementExistsAtNode: " + ((pp != null) ? pp : "none"));
	            }

	            break;
	        }
	}
	
	/**
	 * Take resources needed to pay the dev card and inform everyone that uknown card was
	 * bought. The player who bought the card is informed about the true type of the card.
	 * @param pn player who want to buy the development card
	 */
	public void handleBuyDevCard(int pn) {
		if (currentGame.couldBuyDevCard(pn)) {
			int card = currentGame.buyDevCard();
			final int devCount = currentGame.getNumDevCards();
			sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.ORE, SOCPlayerElement.LOSE, 1);
        	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.WHEAT, SOCPlayerElement.LOSE, 1);
        	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.SHEEP, SOCPlayerElement.LOSE, 1);
        	sendDevCardCount(devCount);
        	
        	playersGameOrder[pn].handleDEVCARDACTION(pn, card, SOCDevCardAction.DRAW);
        	for (int i = 0; i < players.length; ++i) {
        		if (i==pn)
        			continue;
        		playersGameOrder[i].handleDEVCARDACTION(pn, SOCDevCardConstants.UNKNOWN, SOCDevCardAction.DRAW);
    		}
			
		} else {
			SOCResourceSet resources = currentGame.getPlayer(currentGame.getCurrentPlayerNumber()).getResources();
			System.err.println("Cannot buy devcard now. resources: " + resources.toFriendlyString() +
					" numDevCards: " + currentGame.getNumDevCards());
		}
	}
	
	/**
	 * send everyone information about how many development cards are in the game
	 * @param value
	 */
	public void sendDevCardCount(int value) {
		for (int i = 0; i < players.length; ++i) {
			players[i].handleDevCardCount(value);
		}
	}
	
	/**
	 * All clients are informed what resources the player gave away 
	 * and what resources he gained
	 * 
	 * @param pn player making the trade with the bank
	 * @param give resources he gives away
	 * @param get resources he gets from the bank
	 */
	public void handleBankTrade(int pn, SOCResourceSet give, SOCResourceSet get) {
		if (currentGame.canMakeBankTrade(give, get))
        {
			currentGame.makeBankTrade(give, get);
			sendRsrcGainLoss(-1, give, true, pn, -1);
	        sendRsrcGainLoss(-1, get, false, pn, -1);
        } else {
        	SOCPlayer cpn = currentGame.getPlayer(currentGame.getCurrentPlayerNumber());
        	int wantedRes=-1;
        	for (int i = SOCResourceConstants.CLAY; i <= SOCResourceConstants.WOOD; i++)
            {
                final int giveAmt = give.getAmount(i);
                if (giveAmt > 0)
                {
                	wantedRes = i;
                }
            }
        	System.err.println("Illegal bank trade: gs=" + currentGame.getGameState() + 
        			" wanted to trade: " + give.toFriendlyString() + 
        			" for: " + get.toFriendlyString() + " num: " + wantedRes +
        			" has give res=" + cpn.getResources().contains(give) + 
        			" has misc port=" + cpn.getPortFlag(SOCBoard.MISC_PORT) +
        			" has res port=" + cpn.getPortFlag(wantedRes)
        			); 
        }
	}
	
	public void writeStats(String gameName) {
		BufferedWriter writer = null;
        try {
        	/*STATS*/
//        	Path path = Paths.get("log", "SE_RL_RND_stat.txt");
        	Path path = Paths.get(fileName + ".txt");
            writer = new BufferedWriter(new FileWriter(path.toFile(), true));
            Date now = new Date();
            Date gstart = currentGame.getStartTime();
            int firstPlayer = currentGame.getFirstPlayer();
            SOCPlayer pn = currentGame.getPlayerWithWin();
    		int winner = -1;
    		if (pn!=null) 
    			winner = pn.getPlayerNumber();
            if (gstart != null)
            {
                final int gameRounds = currentGame.getRoundCount();
                long gameSeconds = ((now.getTime() - gstart.getTime())+500L) / 1000L;
//                final long gameMinutes = gameSeconds / 60L;
//                gameSeconds = gameSeconds % 60L;
                writer.write(gameName + ", " 
                		+ winner + ", "
            			+ gameRounds + ", " 
//            			+ gameMinutes + ", "
            			+ gameSeconds + ", "
            			+ firstPlayer
            			);
                writer.newLine();
            }
                       
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            }
        }
    }
	
	public void debugStats(int cpn, int action) {
		System.out.println("GS: " + currentGame.getGameState() + " action: " + action + " cpn: " + cpn 
				+ " roll: " + currentGame.getCurrentDice());
			
		for (int j =0; j<playersGameOrder.length; j++) {
			SOCPlayer gamepn = currentGame.getPlayer(j);
			int[] gamedevCards = new int[2];
	    	SOCInventory gameinv = gamepn.getInventory();
	    	gamedevCards[0] = gameinv.getAmount(SOCInventory.OLD, SOCDevCardConstants.UNKNOWN);
	    	gamedevCards[1] = gameinv.getAmount(SOCInventory.NEW, SOCDevCardConstants.UNKNOWN);  
			System.out.println("   player: " + j 
					+ " gameRes:     " + Arrays.toString(gamepn.getResources().getAmounts(true))
					+ " devCards: " + Arrays.toString(gamedevCards)
					+ " ports: " + Arrays.toString(gamepn.getPortFlags())
					+ " public vp: " + gamepn.getPublicVP() + " total vp " + gamepn.getTotalVP()
					+ " pot sets: " + Arrays.toString(gamepn.getPotentialSettlements_arr())
					+ " sets: " + Arrays.toString(gamepn.getSettlements().toArray())
					+ " cities: " + Arrays.toString(gamepn.getCities().toArray())
					+ " roads: " + Arrays.toString(gamepn.getRoadsAndShips().toArray())
					);
			
			for (int i = 0; i< playersGameOrder.length; i++) {
				SOCPlayer pn = playersGameOrder[i].getGame().getPlayer(j);
				int[] devCards = new int[2];
		    	SOCInventory inv = gamepn.getInventory();
		    	devCards[0] = inv.getAmount(SOCInventory.OLD, SOCDevCardConstants.UNKNOWN);
		    	devCards[1] = inv.getAmount(SOCInventory.NEW, SOCDevCardConstants.UNKNOWN);  
				
				System.out.println("in player: " + i 
						+ " res	: " + Arrays.toString(pn.getResources().getAmounts(true))
						+ " devCards: " + Arrays.toString(gamedevCards)
						+ " ports: " + Arrays.toString(gamepn.getPortFlags())
						+ " public vp: " + pn.getPublicVP() + " total vp " + pn.getTotalVP()
						+ " pot sets: " + Arrays.toString(gamepn.getPotentialSettlements_arr())
						+ " sets: " + Arrays.toString(gamepn.getSettlements().toArray())
						+ " cities: " + Arrays.toString(gamepn.getCities().toArray())
						+ " roads: " + Arrays.toString(gamepn.getRoadsAndShips().toArray())
						);
			
			}	
		}
	}
	
	
	 /**
     * shuffle the indexes to distribute load among {@link #robots}
     * @return a shuffled array of robot indexes, from 0 to ({@link #robots}.size() - 1)
     * @since 1.1.06
     */
    int[] robotShuffleForJoin()
    {
        int[] robotIndexes = new int[players.length];

        for (int i = 0; i < players.length; i++)
        {
            robotIndexes[i] = i;
        }

        for (int j = 0; j < 3; j++)
        {
            for (int i = 0; i < robotIndexes.length; i++)
            {
                // Swap a random robot, below the ith robot, with the ith robot
                int idx = Math.abs(rand.nextInt() % (robotIndexes.length - i));
                int tmp = robotIndexes[idx];
                robotIndexes[idx] = robotIndexes[i];
                robotIndexes[i] = tmp;
            }
        }
        return robotIndexes;
    }
    
    public static void setSeed(int seed) {
    	SEED = seed;
    }

	public static void main(String[] args) {
		if (args.length < 3)
        {
            System.err.println("Java Settlers BotServer");
            System.err.println("usage: java BotServer name n_games n_write "
            		+ "-DgameType=gameType -Dmemory=memofile -Dseed=seed"
            		+ "-DserverType=serverType -DtestMode=testMode");
            System.err.println("-DgameType one of the types: testRandomLt, testRandomNN, trainNN, trainLT, sharedLT");
            System.err.println("-Dmemory number of the file where state memory is located");
            System.err.println("-DserverType normal or fullInfo");
            System.err.println("-DtestMode indicates how many times experiment should be repeat with the given settings."
            		+ "each time random seed is changed");
            return;
        }
			
		String gameType="";
		int memoryName=-1;
		int seed = 5;
		String serverType = "normal";
		int testMode = 1;
		boolean isTraining = true;
		
		for (int aidx =0; aidx<args.length; aidx++) {
			String arg = args[aidx];
			
			if (!arg.startsWith("-D")) 
				continue;
			
			String name = arg.substring(2, arg.length());
			 String value = null;
             int posEq = name.indexOf("=");
             if (posEq > 0)
             {
                 value = name.substring(posEq + 1);
                 name = name.substring(0, posEq);
             }
			
             switch(name) {
             	case "gameType":
             		gameType = value;
             		break;
             	case "memory":
             		memoryName = Integer.parseInt(value);
             		break;
             	case "seed":
             		seed = Integer.parseInt(value);
             		break;
             	case "serverType":
             		serverType = value;
             		break;
             	case "testMode":
             		testMode = Integer.parseInt(value);
             	case "isTraining":
             		isTraining = Boolean.parseBoolean(value);
             		
             }		
		}
		
		int[] seeds = new int[] {seed, 10, 22, 38, 61};
		
		for (int i=0; i<testMode; i++) {
			BotServer.setSeed(seeds[i]);
			
			if(serverType.equals("normal")) {
				BotServer server = new BotServer(args[0], Integer.parseInt(args[1]), 
						Integer.parseInt(args[2]), gameType, memoryName, isTraining);
				
				
				server.startServer();
			} else {
				BotServer_fullInfo server = new BotServer_fullInfo(args[0], Integer.parseInt(args[1]), 
						Integer.parseInt(args[2]), gameType, memoryName, isTraining);
								
				server.startServer();
			}
		}

	}

}
