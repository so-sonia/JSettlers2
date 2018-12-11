package fasttester;
/*
 * TODO: handle informing about LA, LR
 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.stream.Collectors;

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
import soc.robot.rl.SOCState;
import soc.robot.rl.SOCState_small;


public class BotServer {

	protected String name;
	
	protected int remainingGames;
	
	protected int synchroniseCount;
	
	protected RLClient[] players;
	
	protected RLClient[] playersGameOrder;
	
	protected int[] wins, loses;
	
	protected SOCGame currentGame;
	
	protected int gamesPlayed;
	
	/**
     * So we can get random numbers.
     */
    private Random rand = new Random();
    
    HashMap<Integer,SOCPlayer> playersState;
    
    protected SOCState state;
	
	public BotServer(String name, int games, int syn) {
		this.name = name;
		this.remainingGames = games;
		this.synchroniseCount = syn;
		wins = new int[4];
		loses = new int[4];
		gamesPlayed = 0;
		
//		startTime = System.currentTimeMillis();
//        numberOfGamesStarted = 0;
//        numberOfGamesFinished = 0;
		
	}
	
	public void createPlayers() {
		players = new RLClient[4];
		for (int i=0; i<players.length; i++) {
			players[i] = new RLClient(i);
		}
		
	}
	
	public void startServer() {
		Date startTime = new Date();
		createPlayers();
		
		for (int i=0; remainingGames>0; i++ ) {
        	String gaName = "~botsOnly~" + remainingGames;
        	startGame(gaName);
        	remainingGames--;
        	gamesPlayed++;
		}
		
		Date now =  new Date();
		long gameSeconds = ((now.getTime() - startTime.getTime())+500L) / 1000L;
        final long gameMinutes = gameSeconds / 60L;
        gameSeconds = gameSeconds % 60L;
         
		System.out.println("TIME ELAPSED: " + gameMinutes + " minutes " + gameSeconds + " seconds.");
	}
	
	public void startGame(String name) {
		/*maybe needed*/
		//SOCGame.boardFactory = new SOCBoardAtServer.BoardFactoryAtServer();
		currentGame = new SOCGame(name);
		// set the expiration to 90 min. from now
        //game.setExpiration(game.getStartTime().getTime() + (60 * 1000 * GAME_TIME_EXPIRE_MINUTES));
		
		currentGame.isBotsOnly = true;
		playersGameOrder = new RLClient[4];
		
		/*DEBUGA*/
		System.out.println("Started bot-only game: " + name);
		
		currentGame.setGameState(SOCGame.READY);
		
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
		
		/*DEBUGA*/
		playersState = new HashMap<Integer,SOCPlayer>();
		  
		for (int i =0; i<currentGame.maxPlayers;i++) {
			playersState.put(new Integer(i), currentGame.getPlayer(i));
		}
		  
		state = new SOCState_small(-1, playersState);
	    state.updateAll(playersState, board);   
		
		sendGameSate();
		/*maybe we should send not played dev card=> but setting initial settlements probably dont need it*/
		
		initialPlacementsHandler();
		actualGameHandler();
		sendEndGame();
	}
	
	public void sendGameSate() {
		int gamestate = currentGame.getGameState();
		for(int i =0; i<players.length; i++) {
			players[i].handleGAMESTATE(gamestate);
		}
	}
	
	public void sendEndGame() {
		SOCPlayer pn = currentGame.getPlayerWithWin();
		int winner = -1;
		if (pn!=null) 
			winner = pn.getPlayerNumber();
		for(int i =0; i<players.length; i++) {
			players[i].handleEndGame(winner);
		}
	}
	
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
			//Game State back to START1A (until all player put their first settlement, 
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
		
//		/*DEBUG*/
//		System.out.println("Finished initial placement");
//		debugStats(0, 0);
		
	}
	
	public void actualGameHandler() {
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
				
//				debugStats(cpn, action);
				
				if (action==RLStrategy.PLAY_KNIGHT && currentGame.getGameState() == SOCGame.ROLL_OR_CARD) {
					handleRoll(cpn); //state PLAY1 or WAITING FOR DISCARDS
					
//					debugStats(cpn, action);
				}
			}
			
			while (currentGame.getGameState() == SOCGame.PLAY1 && action != RLStrategy.END_TURN) {
				action = playersGameOrder[cpn].buildOrTradeOrPlayCard();
				
//				/*DEBUG*/
//				if (action==5) {
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
			
//			/*DEBUGA*/
//			Iterator<SOCPlayer> playersIter = playersState.values().iterator();
//	    	state.updateAll(playersState, currentGame.getBoard());
//		    while (playersIter.hasNext()) {
//		    	SOCPlayer pn = playersIter.next();
//		    	
////	    		List<Integer> playerState = Arrays.stream(state.getState(pn)).boxed().collect(Collectors.toList());
//	    		
//	    		/*DEBUG*/
////	    		System.out.println("In GAME!: player " 
////	    					+ pn.getPlayerNumber() + " state: " 
////	    					+ Arrays.toString(playerState.toArray()));
//	    		pn.stats();
//		    }
			
			
			if (currentGame.getRoundCount()>500) {
				System.out.println("Game too long, ending");
				break;
			}
		}
		
		
		for (SOCPlayer pn : currentGame.getPlayers()) {
        	pn.writeStats(currentGame.getName());        	
        }
        writeStats(currentGame.getName());
        
        for (int i=0; i<players.length; i++) {
			players[i].memoryStats();
		}
        
        if (gamesPlayed%2000==0) {
        	for (int i=0; i<players.length; i++) {
    			players[i].writeMemory(gamesPlayed, false);
    		}
        }
		
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
	 * Inform all clients about change in turn
	 * @param pn - player number
	 * @param gs - game state
	 */
	public void sendTurn(int pn, int gs) {
		for(int i =0; i<players.length; i++) {
			players[i].handleTURN(pn, gs);
		}
		playersGameOrder[pn].handlePlayedDevCardFlag(pn, false);
	}
	
	public void handleAction(int action, int pn) {
		int cpn = currentGame.getCurrentPlayerNumber();
		if ( pn != cpn ) {
			/*action error, player tried to build although it's not his turn*/
			System.err.println("Action error: " + playersGameOrder[pn].getName() + " wanted to build, "
					+ "but it's " + cpn + " turn.");
			return;
		}
		
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
			handleBuildRequest(pn, SOCPlayingPiece.SETTLEMENT);
			if (currentGame.getGameState() == SOCGame.PLACING_SETTLEMENT) {
				int settle = playersGameOrder[pn].getBuildingCoord();
				handlePutPiece(pn, SOCPlayingPiece.SETTLEMENT, settle); //gs = PLAY1
			}
			
			break;
			
		case RLStrategy.PLACE_ROAD:
			handleBuildRequest(pn, SOCPlayingPiece.ROAD);
			if (currentGame.getGameState() == SOCGame.PLACING_ROAD) {
				int road = playersGameOrder[pn].getBuildingCoord();
				handlePutPiece(pn, SOCPlayingPiece.ROAD, road); //gs = PLAY1
			}
			break;
			
		case RLStrategy.PLACE_CITY:
			handleBuildRequest(pn, SOCPlayingPiece.CITY);
			if (currentGame.getGameState() == SOCGame.PLACING_CITY) {
				int city = playersGameOrder[pn].getBuildingCoord();
				handlePutPiece(pn, SOCPlayingPiece.CITY, city); //gs = PLAY1
			}
			break;
			
		case RLStrategy.BUY_DEVCARD:
			handleBuyDevCard(pn);
			break;
		
		}
		
			
	}
	
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
	
	public void sendPlayerElement_numKnights(int pn, int action, int amount) {
		for(int i =0; i<players.length; i++) {
			players[i].handlePLAYERELEMENT_numKnights(pn, action, amount);
		}
	}
	
	public void sendPlayedDevCardFlag(int pn, boolean value) {
		for(int i =0; i<players.length; i++) {
			players[i].handlePlayedDevCardFlag(pn, value);
		}
	}
	
	public void handleMoveRobber(int pn, int coord) {
		if (currentGame.canMoveRobber(pn, coord)) {
			
			SOCMoveRobberResult result = currentGame.moveRobber(pn, coord);
			sendRobberMoved(coord);
			final List<SOCPlayer> victims = result.getVictims();
			
			/*DEBUGA*/
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
							+ " victim has res:" + Arrays.toString(playersGameOrder[victim].getRLStrategy()
								.getStateRes(vic)) );
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
	
	public void sendRobberMoved(int coord) {
		for(int i =0; i<players.length; i++) {
			players[i].handleRobberMoved(coord);
		}
	}
	
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
             }else {
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
	
	public void handleEndTurn(int pn) {
		
		SOCPlayer pl = currentGame.getPlayer(pn);
		if ((pl != null) && currentGame.canEndTurn(pn)) {
			currentGame.endTurn();
			sendTurn(pn, currentGame.getGameState());	
		} else {
			System.err.println("You can't end the turn");
		}
	}
	
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
	 * */
	public void handleBuildRequest(int pn, int type) {
		 switch (type)
	        {
	        case SOCPlayingPiece.ROAD:

	            if (currentGame.couldBuildRoad(pn))
	            {
	            	currentGame.buyRoad(pn); //gs = PLACING_ROAD
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.CLAY, SOCPlayerElement.LOSE, 1);
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.WOOD, SOCPlayerElement.LOSE, 1);
	            } else {
	            	SOCPlayer cpn = currentGame.getPlayer(pn);
	            	SOCResourceSet resources = cpn.getResources();
	            	System.err.println("Cannot build road now! resources: " + resources.toFriendlyString() +
	            			" has potential road: " + cpn.hasPotentialRoad());
	            }

	            break;

	        case SOCPlayingPiece.SETTLEMENT:

	            if (currentGame.couldBuildSettlement(pn))
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
	            }

	            break;

	        case SOCPlayingPiece.CITY:

	            if (currentGame.couldBuildCity(pn))
	            {
	            	currentGame.buyCity(pn); //gs = PLACING_CITY
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.ORE, SOCPlayerElement.LOSE, 3);
	            	sendPlayerElement_numRscs(-1, pn, SOCPlayerElement.WHEAT, SOCPlayerElement.LOSE, 2);
	            	
	            } else {
	            	SOCPlayer cpn = currentGame.getPlayer(pn);
	            	SOCResourceSet resources = cpn.getResources();
	            	System.err.println("Cannot build city now! resources: " + resources.toFriendlyString() +
	            			" has potential city: " + cpn.hasPotentialCity());
	            }

	            break;
	        }
	}
	
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
    			players[i].handleDEVCARDACTION(pn, SOCDevCardConstants.UNKNOWN, SOCDevCardAction.DRAW);
    		}
			
		} else {
			SOCResourceSet resources = currentGame.getPlayer(currentGame.getCurrentPlayerNumber()).getResources();
			System.err.println("Cannot buy devcard now. resources: " + resources.toFriendlyString() +
					" numDevCards: " + currentGame.getNumDevCards());
		}
	}
	
	public void sendDevCardCount(int value) {
		for (int i = 0; i < players.length; ++i) {
			players[i].handleDevCardCount(value);
		}
	}
	
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
        	Path path = Paths.get("SE_RL_RND_stat.txt");
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

	public static void main(String[] args) {
		if (args.length < 3)
        {
            System.err.println("Java Settlers BotServer");
            System.err.println("usage: java BotServer name n_games n_synchronise");

            return;
        }
		
		
		BotServer server = new BotServer(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		
		server.startServer();

	}

}
