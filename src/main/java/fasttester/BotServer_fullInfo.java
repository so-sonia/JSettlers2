package fasttester;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Properties;

import soc.client.SOCPlayerClient;
import soc.client.SOCPlayerInterface;
import soc.client.SOCPlayerClient.GameAwtDisplay;
import soc.game.ResourceSet;
import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.message.SOCDevCardAction;
import soc.message.SOCPlayerElement;
import soc.robot.rl.RLStrategy;
import soc.robot.rl.StateValueFunction;
import soc.robot.rl.StateValueFunctionLT;
import soc.robot.rl.StateValueFunctionNN;

/**
 * Class for playing fast games of the Settlers of Catan. 
 * Games is simplified: players have full information about the game and no trade between players is allowed.
 * @author Sonia
 *
 */
public class BotServer_fullInfo extends BotServer {

	public BotServer_fullInfo(String name, int games, int syn, String gameType, 
			int memoryType, boolean isTraining) {
		super(name, games, syn, gameType, memoryType, isTraining);
		System.out.println("server type = fullInfo");
		// TODO Auto-generated constructor stub
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
                		if (prPlayerType!=null) {
                			createPlayer(i, prPlayerType);
                		} 
//                		else {
//                			createPlayer(i, "random");
//                		}
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
			players[pn] = new RLClient_fullInfo(pn, RLClient.RANDOM, memoryType);
			break;
		case "fast":
			players[pn] = new RLClient_builtIn(pn, RLClient.FAST_BUILTIN , memoryType);
			break;
		case "smart":
			players[pn] = new RLClient_builtIn(pn, RLClient.SMART_BUILTIN , memoryType);
			break;
		case "testLT":
			players[pn] = new RLClient_fullInfo(pn, RLClient.TEST_LOOKUP_TABLE, memoryType);
			break;
		case "traintLT":
			players[pn] = new RLClient_fullInfo(pn, RLClient.TRAIN_LOOKUP_TABLE, memoryType);
			break;
		case "testNN":
			players[pn] = new RLClient_fullInfo(pn, RLClient.TEST_NN, memoryType);
			break;
		case "trainNN":
			players[pn] = new RLClient_fullInfo(pn, RLClient.TRAIN_NN, memoryType);
			players[pn].startTraining();
			break;
			
		}
	}
	
	
	public void createPlayersFromGameType() {
				
		switch(gameType) {
		
		case "testRandomLT":
			
			//in random tests 1 player plays against 3 random bots
			
//			players[0] = new RLClient(0, RLClient.TEST_LOOKUP_TABLE, memoryType, players[0].getMemory());
			players[0] = new RLClient_fullInfo(0, RLClient.TEST_LOOKUP_TABLE, memoryType);
			players[1] = new RLClient_fullInfo(1, RLClient.RANDOM, memoryType);
			players[2] = new RLClient_fullInfo(2, RLClient.RANDOM, memoryType);
			players[3] = new RLClient_fullInfo(3, RLClient.RANDOM, memoryType);
			break;
		
		case "trainLT":
			for (int i=0; i<players.length; i++) {
				players[i] = new RLClient_fullInfo(i, RLClient.TRAIN_LOOKUP_TABLE, memoryType);
			}
			break;
			
		case "trainNN":
			for (int i=0; i<players.length; i++) {
				players[i] = new RLClient_fullInfo(i, RLClient.TRAIN_NN, memoryType);
				/*back propagation of neural network is started after enough state-action transitions are accumulated*/
				players[i].startTraining();
			}
			break;
			
		case "sharedNN":
			StateValueFunction sharedNNFunction = new StateValueFunctionNN(true, 100);
			
			for (int i=0; i<players.length; i++) {
				players[i] = new RLClient_fullInfo(i, RLClient.TRAIN_NN, memoryType);
				players[i].setStateValueFunction(sharedNNFunction);
			}
			sharedNNFunction.startTraining();
			
			break;
			
		case "testRandomNN":
			players[0] = new RLClient_fullInfo(0, RLClient.TEST_NN, memoryType);
			players[1] = new RLClient_fullInfo(1, RLClient.RANDOM, memoryType);
			players[2] = new RLClient_fullInfo(2, RLClient.RANDOM, memoryType);
			players[3] = new RLClient_fullInfo(3, RLClient.RANDOM, memoryType);
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
			
		case "builtInFast":
			players[0] = new RLClient_builtIn(0, RLClient.FAST_BUILTIN , memoryType);
			players[1] = new RLClient_builtIn(1, RLClient.FAST_BUILTIN , memoryType);
			players[2] = new RLClient_builtIn(2, RLClient.FAST_BUILTIN , memoryType);
			players[3] = new RLClient_builtIn(3, RLClient.FAST_BUILTIN , memoryType);
			break;
			
		case "builtInSmart":
			players[0] = new RLClient_builtIn(0, RLClient.SMART_BUILTIN , memoryType);
			players[1] = new RLClient_builtIn(1, RLClient.SMART_BUILTIN , memoryType);
			players[2] = new RLClient_builtIn(2, RLClient.SMART_BUILTIN , memoryType);
			players[3] = new RLClient_builtIn(3, RLClient.SMART_BUILTIN , memoryType);
			break;
			
		case "builtInFastRandom":
			players[0] = new RLClient_fullInfo(0, RLClient.RANDOM , memoryType);
			players[1] = new RLClient_builtIn(1, RLClient.FAST_BUILTIN , memoryType);
			players[2] = new RLClient_builtIn(2, RLClient.FAST_BUILTIN , memoryType);
			players[3] = new RLClient_builtIn(3, RLClient.FAST_BUILTIN , memoryType);
			break;
			
		case "builtInFast2Random2":
			players[0] = new RLClient_fullInfo(0, RLClient.RANDOM , memoryType);
			players[1] = new RLClient_fullInfo(1, RLClient.RANDOM , memoryType);
			players[2] = new RLClient_builtIn(2, RLClient.FAST_BUILTIN , memoryType);
			players[3] = new RLClient_builtIn(3, RLClient.FAST_BUILTIN , memoryType);
			break;
			
		case "builtInSmartRandom":
			players[0] = new RLClient_fullInfo(0, RLClient.RANDOM , memoryType);
			players[1] = new RLClient_builtIn(1, RLClient.SMART_BUILTIN , memoryType);
			players[2] = new RLClient_builtIn(2, RLClient.SMART_BUILTIN , memoryType);
			players[3] = new RLClient_builtIn(3, RLClient.SMART_BUILTIN , memoryType);
			break;
			
		case "builtInSmart2Random2":
			players[0] = new RLClient_fullInfo(0, RLClient.RANDOM , memoryType);
			players[1] = new RLClient_fullInfo(1, RLClient.RANDOM , memoryType);
			players[2] = new RLClient_builtIn(2, RLClient.SMART_BUILTIN , memoryType);
			players[3] = new RLClient_builtIn(3, RLClient.SMART_BUILTIN , memoryType);
			break;
		}
		
	}
	
	public void startGame(String name) {

		currentGame = new SOCGame(name);
			
		/*DEBUG START GAME*/
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
			if (currentGame.isSeatVacant(players[i].getPlayerNumber()))
            {
				currentGame.addPlayer(players[i].getName(), players[i].getPlayerNumber());
            } 
		}
		
		currentGame.startGame();
		
		
		/*in full information variant, players will have the same object of the game 
		 * as the one at server, therefore we don't need to pass information about board
		 * and game states
		 */	
		for(int i =0; i<players.length; i++) {
			players[i].joinGame(currentGame);
		}
		
//		/*creating interface*/
//		SOCPlayerClient client = new SOCPlayerClient();
//		GameAwtDisplay gameDisplay = new GameAwtDisplay(true, client);
//		SOCPlayerInterface pi = new SOCPlayerInterface(currentGame.getName(), gameDisplay , currentGame, null);
//        pi.setVisible(true);
//        for(int i =0; i<players.length; i++) {
//			pi.addPlayer(players[i].getName(), players[i].getPlayerNumber());
//		}
//        pi.startGame();
		
		/*DEBUG*/
//				playersState = new HashMap<Integer,SOCPlayer>();
//				  
//				for (int i =0; i<currentGame.maxPlayers;i++) {
//					playersState.put(new Integer(i), currentGame.getPlayer(i));
//				}		  
//				state = new SOCState_small(-1, playersState);
//			    state.updateAll(playersState, board);   
		
		initialPlacementsHandler();
		actualGameHandler();
		sendEndGame();
	}
	
	/*functions below don't need to report anything, because clients
	 * have te same version of the game as one at the server.
	 * @see fasttester.BotServer#sendGameSate()
	 */
	public void sendGameSate() {
	}
	
//	public void sendPutPiece(int pn, int pieceType, int coord) {
//	}
	
	public void sendTurn(int pn, int gs) {
	}
	
	public void sendRsrcGainLoss(int msgTo, ResourceSet resourceSet, boolean isLoss,  int mainPlayer, int tradingPlayer){
	}
	
	public void sendRobberMoved(int coord) {
	}
	
	public void handleRobbery(int victim, int pn, int rsrc) {
	}
	
	public void sendPlayerElement_numRscs(int msgTo, int pn, int type, int action, int amount) {
	}
	
	public void sendPlayerElement_numKnights(int pn, int action, int amount) {
	}
	
	public void sendDevCardAction(int pn, int ctype, int action) {
	}
	
	public void sendPlayedDevCardFlag(int pn, boolean value) {
	}

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
            	
            	} else {
            		System.err.println("Player " + i + " You can't discard these cards");
            		System.err.println("We wated to discard: " + resourceSet.toFriendlyString() 
            			+ " we have: " + currentGame.getPlayer(i).getResources().toFriendlyString());
            		
            		SOCResourceSet discardsn = new SOCResourceSet();
                    SOCGame.discardOrGainPickRandom(
                    	currentGame.getPlayer(i).getResources(), numDiscards, 
                  		true, discardsn, rand);
                    
                    currentGame.discard(i,discardsn);     		
            	}
            }
		}   
    }
	
	public void handlePlayDevCard(int pn, int type) {
		switch (type)
        {
        case SOCDevCardConstants.KNIGHT:
        	if (currentGame.canPlayKnight(pn)) {
        		currentGame.playKnight();
        	} else {
                System.err.println("You can't play KNIGHT card right now"); 
            } 
        	break;
        
        case SOCDevCardConstants.ROADS:
        	 if (currentGame.canPlayRoadBuilding(pn)) {
        		 currentGame.playRoadBuilding(); 
        		 //gs = PLACING_FREE_ROAD1 or PLACING_FREE_ROAD2 if only 1 road left
        	 } else {
                 System.err.println("You can't play ROADS card right now"); 
             } 
        	 break;
        	 
        case SOCDevCardConstants.DISC:
            if (currentGame.canPlayDiscovery(pn))
            {
            	currentGame.playDiscovery(); //gs = WAITING_FOR_DISCOVERY
            } else {
                System.err.println("You can't play DISC card right now"); 
            } 
            break;
            
        case SOCDevCardConstants.MONO:
        	if (currentGame.canPlayMonopoly(pn)) {
        		currentGame.playMonopoly(); //gs = WAITING_FOR_MONOPOLY
              } else {
                  System.err.println("You can't play MONO card right now"); 
              }    
        }
	}
	
	public void handleMonopoly(int pn) {
		
		final int rsrc = playersGameOrder[pn].getMonopolyChoice();
        currentGame.doMonopolyAction(rsrc);       
	}

	public void handleBuyDevCard(int pn) {
		if (currentGame.couldBuyDevCard(pn)) {
			currentGame.buyDevCard();
			
		} else {
			SOCResourceSet resources = currentGame.getPlayer(currentGame.getCurrentPlayerNumber()).getResources();
			System.err.println("Cannot buy devcard now. resources: " + resources.toFriendlyString() +
					" numDevCards: " + currentGame.getNumDevCards());
		}
	}
	
	public static void main(String[] args) {
		if (args.length < 3)
        {
            System.err.println("Java Settlers BotServer");
            System.err.println("usage: java BotServer name n_games n_write "
            		+ "-DgameType=gameType -Dmemory=memofile -Dseed=seed");

            return;
        }
			
		String gameType="";
		int memoryName=-1;
		int seed = 5;
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
             		
             }		
		}
		
		SEED = seed;
		
		BotServer_fullInfo server = new BotServer_fullInfo(args[0], Integer.parseInt(args[1]), 
				Integer.parseInt(args[2]), gameType, memoryName, isTraining);
		
		
		server.startServer();

	}
	
}
