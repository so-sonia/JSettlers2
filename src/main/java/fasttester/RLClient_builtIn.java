package fasttester;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import soc.disableDebug.D;
import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCGameOption;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCRoutePiece;
import soc.game.SOCSettlement;
import soc.game.SOCShip;
import soc.game.SOCTradeOffer;
import soc.message.SOCChoosePlayerRequest;
import soc.message.SOCDiscardRequest;
import soc.robot.DiscardStrategy;
import soc.robot.MonopolyStrategy;
import soc.robot.OpeningBuildStrategy;
import soc.robot.RobberStrategy;
import soc.robot.SOCBuildingSpeedEstimate;
import soc.robot.SOCPlayerTracker;
import soc.robot.SOCPossibleCity;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleRoad;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCPossibleShip;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.robot.SOCRobotDM;
import soc.robot.SOCRobotNegotiator;
import soc.robot.rl.RLStrategy;
import soc.robot.rl.RLStrategyLookupTable;
import soc.robot.rl.RLStrategyLookupTable_test;
import soc.robot.rl.RLStrategyNN_oppsum;
import soc.robot.rl.RLStrategyNN_oppsum_test;
import soc.robot.rl.RLStrategyRandom;
import soc.server.SOCServer;
import soc.util.SOCRobotParameters;

public class RLClient_builtIn extends RLClient {
	
	protected HashMap<Integer, SOCPlayerTracker> playerTrackers;
	protected MonopolyStrategy monopolyStrategy;
	protected SOCRobotDM decisionMaker;
	protected SOCRobotNegotiator negotiator;
	protected final Stack<SOCPossiblePiece> buildingPlan;
	SOCRobotParameters robotParameters;
	protected SOCPlayerTracker ourPlayerTracker;

	
	protected Random rnd;

	public RLClient_builtIn(int i, int strategyType, int memoryType) {
		super(i, strategyType, memoryType);
		rnd = new Random(BotServer.SEED);
		buildingPlan = new Stack<SOCPossiblePiece>();
		
		//for now strategy type == 1 (means fast strategy)
		//new SOCRobotParameters(120, 35, 0.13f, 1.0f, 1.0f, 3.0f, 1.0f, 1, 1)
		if (strategyType==RLClient.FAST_BUILTIN) {
			robotParameters = SOCServer.ROBOT_PARAMS_DEFAULT;
		} else {
			//else smart bot plays with smart strategy=0
			robotParameters = new SOCRobotParameters(120, 35, 0.13f, 1.0f, 1.0f, 3.0f, 1.0f, 0, 1);
		}
	}
	
	public void joinGame(SOCGame game) {
		this.game = game;
		
		ourPlayerData = game.getPlayer(getName());
		openingBuildStrategy = new OpeningBuildStrategy(game, ourPlayerData);
		moveRobber = null;
		roadsToBuildCoord = null;
		resourceChoices = new SOCResourceSet();
		monopolyChoice = 0;
		buildingCoord = 0;
		bankTrade = new SOCResourceSet[2];
		
		SOCRobotBrain brain = new SOCRobotBrain(new SOCRobotClient("", name, "", ""), 
												robotParameters, 
												game, null);
		brain.setOurPlayerData();
		playerTrackers = new HashMap<Integer, SOCPlayerTracker>();
        for (int pn = 0; pn < game.maxPlayers; pn++)
        {	
        	if (! game.isSeatVacant(pn))
            {
                SOCPlayerTracker tracker = new SOCPlayerTracker(game.getPlayer(pn), brain);
                playerTrackers.put(new Integer(pn), tracker);
            }
        }
        
        ourPlayerTracker = playerTrackers.get(Integer.valueOf(playerNumber));
        
        buildingPlan.clear();
        decisionMaker = new SOCRobotDM(robotParameters,
        								playerTrackers,
						    		    playerTrackers.get(Integer.valueOf(playerNumber)),
						    		    ourPlayerData,
						    		    buildingPlan);
        monopolyStrategy = new MonopolyStrategy(game, ourPlayerData);
        //negotiator uses buildingPlan from the brain (which is null), 
        // but it's not used in the functions that are important to us
        // like negotiator.getOfferToBank(targetResources, ourPlayerData.getResources())
        negotiator = new SOCRobotNegotiator(brain);
	}
	
	public void joinGame(String name, int[] botNumbers, String[] botNames) {
		game = new SOCGame(name);
		
		for (int i=0; i<botNumbers.length; i++) {
			game.addPlayer(botNames[i], botNumbers[i]);
			
             /**
              * set the robot flag
              */
//			game.getPlayer(botNumbers[i]).setRobotFlag(true, false);
		}
		game.isBotsOnly = true;
		joinGame(game);
	}
	
	public void handlePUTPIECE(int pn, int pieceType, int coord)
    {
    	final SOCPlayer pl =  game.getPlayer(pn);
    	
    	switch (pieceType)
        {
        case SOCPlayingPiece.ROAD:
        	
//        	/*DEBUG*/
//        	if (pn==playerNumber) {
//        		SOCPossibleRoad rd = playerTrackers.get(Integer.valueOf(pn)).getPossibleRoads().get(Integer.valueOf(coord));
//        		boolean isPoss = false;
//        		if (rd!= null)
//        			isPoss = true;
//
//        		System.out.println("player " + name + " potential roads: " +
//        				Arrays.toString(ourPlayerData.getPotentialRoads().stream()
//        						.map(Integer::toHexString).toArray()) +
//        				" is posssible in tracker: " + isPoss);           	
//        	}
        	
        	if (game.isInitialPlacement())  // START1B, START2B, START3B
            {
                //
                // Before processing this road/ship, track the settlement that goes with it.
                // This was deferred until road placement, in case a human player decides
                // to cancel their settlement and place it elsewhere.
                //
                SOCPlayerTracker tr = playerTrackers.get(Integer.valueOf(pn));
                SOCSettlement se = tr.getPendingInitSettlement();
                if (se != null)
                    trackNewSettlement(se, false);
            }
        	SOCRoad newRoad = new SOCRoad(game.getPlayer(pn), coord, null);
        	
//        	if (pn==playerNumber) {
//        		trackNewRoadOrShip(newRoad, false);
//        	} else {
//        		trackNewRoadOrShip(newRoad, false);
//        	}
            trackNewRoadOrShip(newRoad, false);
            
//            /*DEBUG*/
//        	if (pn==playerNumber) {
//        		SOCPossibleRoad rd = ourPlayerTracker.getPossibleRoads().get(Integer.valueOf(coord));
//        		boolean isPoss = false;
//        		if (rd!= null)
//        			isPoss = true;
//        		
//        		String msg = "      has " + ourPlayerTracker.getPossibleSettlements().size() + " potential settlements.";
//        		
//        		 Iterator<SOCPossibleSettlement> posSetsIter = ourPlayerTracker.getPossibleSettlements().values().iterator();
//
//        		 int i=1;
//        		        		 
//                 while (posSetsIter.hasNext())
//                 {
//                	 SOCPossibleSettlement posSets = posSetsIter.next();
//                	 msg += " " + i + ". loc: " + Integer.toHexString(posSets.getCoordinates()) + " roads";
//                	 msg += Arrays.toString(
//                			 posSets.getNecessaryRoads().stream()
//                			 	.map(x -> {
//                			 		int coordNC = x.getCoordinates();
//                			 		if (coordNC == coord )
//                			 			System.out.println("ERROR: road exists at necessary");
//                			 		return Integer.toHexString(coordNC); } )
//                			 	.toArray()
//                			 	);
//                	 i++;
//                 }
//                 
//                Collection<Integer> adjNodeEnum = game.getBoard().getAdjacentNodesToEdge(coord);
//
//        		System.out.println("player " + name + " potential roads: " +
//        				Arrays.toString(ourPlayerData.getPotentialRoads().stream()
//        						.map(Integer::toHexString).toArray()) +
//        				" is posssible in tracker: " + isPoss); 
//        		System.out.println("     road removes from these sets: " + 
//        				Arrays.toString(adjNodeEnum.stream().map(x -> 
//        						Integer.toHexString(x) + ":" + ourPlayerData.canPlaceSettlement(x.intValue()) )
//        						.toArray()));
//        		System.out.println(msg);
//        	}
//            /*END DEBUG*/
                       
            //in fullInfo server calls already putPiece on the game
//        	game.putPiece(new SOCRoad(pl, coord, null));
            break;

        case SOCPlayingPiece.SETTLEMENT:
        	 SOCPlayer newSettlementPl = game.getPlayer(pn);
             SOCSettlement newSettlement = new SOCSettlement(newSettlementPl, coord, null);
             if ((game.getGameState() == SOCGame.START1B) || (game.getGameState() == SOCGame.START2B)
                 || (game.getGameState() == SOCGame.START3B))
             {
                 // Track it soon, after the road is placed
                 // (in handlePUTPIECE_updateGameData)
                 // but not yet, in case player cancels placement.
                 SOCPlayerTracker tr = playerTrackers.get(Integer.valueOf(newSettlementPl.getPlayerNumber()));
                 tr.setPendingInitSettlement(newSettlement);
             }
             else
             {
                 // Track it now
                 trackNewSettlement(newSettlement, false);
             }
//        	game.putPiece(new SOCSettlement(pl, coord, null));
            break;

        case SOCPlayingPiece.CITY:
        	SOCCity newCity = new SOCCity(game.getPlayer(pn), coord, null);
            trackNewCity(newCity, false);
//        	game.putPiece(new SOCCity(pl, coord, null));
            break;

        default:
            System.err.println
                ("handlePUTPIECE: player " + getName() + ": Unknown pieceType " + pieceType);
        }
    }
	
	
	/**
	 * Function copied from {@link SOCRobotBrain} class to have trackers.
	 * @see {@link SOCRobotBrain.trackNewSettlement(SOCSettlement newSettlement, final boolean isCancel)}
	 */
	protected void trackNewSettlement(SOCSettlement newSettlement, final boolean isCancel)
    {
        Iterator<SOCPlayerTracker> trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            SOCPlayerTracker tracker = trackersIter.next();
            if (! isCancel)
                tracker.addNewSettlement(newSettlement, playerTrackers);
            else
                tracker.cancelWrongSettlement(newSettlement);
        }

        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            SOCPlayerTracker tracker = trackersIter.next();
            Iterator<SOCPossibleRoad> posRoadsIter = tracker.getPossibleRoads().values().iterator();

            while (posRoadsIter.hasNext())
            {
                posRoadsIter.next().clearThreats();
            }

            Iterator<SOCPossibleSettlement> posSetsIter = tracker.getPossibleSettlements().values().iterator();

            while (posSetsIter.hasNext())
            {
                posSetsIter.next().clearThreats();
            }
        }

        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            SOCPlayerTracker tracker = trackersIter.next();
            tracker.updateThreats(playerTrackers);
        }

        if (isCancel)
        {
            return;  // <--- Early return, nothing else to do ---
        }

        ///
        /// see if this settlement bisected someone else's road
        ///
        int[] roadCount = { 0, 0, 0, 0, 0, 0 };  // Length should be SOCGame.MAXPLAYERS
        SOCBoard board = game.getBoard();
        Enumeration<Integer> adjEdgeEnum = board.getAdjacentEdgesToNode(newSettlement.getCoordinates()).elements();

        while (adjEdgeEnum.hasMoreElements())
        {
            final int adjEdge = adjEdgeEnum.nextElement().intValue();
            Enumeration<SOCRoutePiece> roadEnum = board.getRoadsAndShips().elements();

            while (roadEnum.hasMoreElements())
            {
                final SOCRoutePiece rs = roadEnum.nextElement();

                if (rs.getCoordinates() == adjEdge)
                {
                    final int roadPN = rs.getPlayerNumber();

                    roadCount[roadPN]++;

                    if (roadCount[roadPN] == 2)
                    {
                        if (roadPN != playerNumber)
                        {
                            ///
                            /// this settlement bisects another players road
                            ///
                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = trackersIter.next();

                                if (tracker.getPlayer().getPlayerNumber() == roadPN)
                                {
                                    //D.ebugPrintln("$$ updating LR Value for player "+tracker.getPlayer().getPlayerNumber());
                                    //tracker.updateLRValues();
                                }

                                //tracker.recalcLongestRoadETA();
                            }
                        }

                        break;
                    }
                }
            }
        }

        final int pNum = newSettlement.getPlayerNumber();

        ///
        /// update the speedups from possible settlements
        ///
        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            SOCPlayerTracker tracker = trackersIter.next();

            if (tracker.getPlayer().getPlayerNumber() == pNum)
            {
                Iterator<SOCPossibleSettlement> posSetsIter = tracker.getPossibleSettlements().values().iterator();

                while (posSetsIter.hasNext())
                {
                    posSetsIter.next().updateSpeedup();
                }

                break;
            }
        }

        ///
        /// update the speedups from possible cities
        ///
        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            SOCPlayerTracker tracker = trackersIter.next();

            if (tracker.getPlayer().getPlayerNumber() == pNum)
            {
                Iterator<SOCPossibleCity> posCitiesIter = tracker.getPossibleCities().values().iterator();

                while (posCitiesIter.hasNext())
                {
                    posCitiesIter.next().updateSpeedup();
                }

                break;
            }
        }
    }
	
	
	
	/**
	 * Function copied from {@link SOCRobotBrain} class to have trackers.
	 * @see {@link SOCRobotBrain.trackNewRoadOrShip(final SOCRoutePiece newPiece, final boolean isCancel)}
	 */
	protected void trackNewRoadOrShip(final SOCRoutePiece newPiece, final boolean isCancel)
    {
        final int newRoadPN = newPiece.getPlayerNumber();

        Iterator<SOCPlayerTracker> trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            SOCPlayerTracker tracker = trackersIter.next();
            tracker.takeMonitor();

            try
            {
                if (! isCancel) {
                	if (tracker.getPlayer().getPlayerNumber()==playerNumber && 
                			newPiece.getPlayerNumber()==playerNumber) {
                		tracker.addOurNewRoadOrShip2(newPiece, playerTrackers,1);
                	} else {
                		tracker.addNewRoadOrShip(newPiece, playerTrackers);
                	}
                }
                else
                    tracker.cancelWrongRoadOrShip(newPiece);
            }
            catch (Exception e)
            {
                tracker.releaseMonitor();
            }

            tracker.releaseMonitor();
        }

        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            SOCPlayerTracker tracker = trackersIter.next();
            tracker.takeMonitor();

            try
            {
                Iterator<SOCPossibleRoad> posRoadsIter = tracker.getPossibleRoads().values().iterator();

                while (posRoadsIter.hasNext())
                {
                    posRoadsIter.next().clearThreats();
                }

                Iterator<SOCPossibleSettlement> posSetsIter = tracker.getPossibleSettlements().values().iterator();

                while (posSetsIter.hasNext())
                {
                    posSetsIter.next().clearThreats();
                }
            }
            catch (Exception e)
            {
                tracker.releaseMonitor();
            }

            tracker.releaseMonitor();
        }

        ///
        /// update LR values and ETA
        ///
        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            SOCPlayerTracker tracker = trackersIter.next();
            tracker.updateThreats(playerTrackers);
            tracker.takeMonitor();

            try
            {
                if (tracker.getPlayer().getPlayerNumber() == newRoadPN)
                {
                    //D.ebugPrintln("$$ updating LR Value for player "+tracker.getPlayer().getPlayerNumber());
                    tracker.updateLRValues();
                }

                tracker.recalcLongestRoadETA();
            }
            catch (Exception e)
            {
                tracker.releaseMonitor();
            }

            tracker.releaseMonitor();
        }
    }
	
	/**
	 * Function copied from {@link SOCRobotBrain} class to have trackers.
	 * @see {@link SOCRobotBrain.trackNewCity(final SOCCity newCity, final boolean isCancel)}
	 */
	 protected void trackNewCity(final SOCCity newCity, final boolean isCancel)
	    {
	        final int newCityPN = newCity.getPlayerNumber();

	        Iterator<SOCPlayerTracker> trackersIter = playerTrackers.values().iterator();

	        while (trackersIter.hasNext())
	        {
	            SOCPlayerTracker tracker = trackersIter.next();

	            if (tracker.getPlayer().getPlayerNumber() == newCityPN)
	            {
	                if (! isCancel)
	                    tracker.addOurNewCity(newCity);
	                else
	                    tracker.cancelWrongCity(newCity);

	                break;
	            }
	        }

	        if (isCancel)
	        {
	            return;  // <--- Early return, nothing else to do ---
	        }

	        ///
	        /// update the speedups from possible settlements
	        ///
	        trackersIter = playerTrackers.values().iterator();

	        while (trackersIter.hasNext())
	        {
	            SOCPlayerTracker tracker = trackersIter.next();

	            if (tracker.getPlayer().getPlayerNumber() == newCityPN)
	            {
	                Iterator<SOCPossibleSettlement> posSetsIter = tracker.getPossibleSettlements().values().iterator();

	                while (posSetsIter.hasNext())
	                {
	                    posSetsIter.next().updateSpeedup();
	                }

	                break;
	            }
	        }

	        ///
	        /// update the speedups from possible cities
	        ///
	        trackersIter = playerTrackers.values().iterator();

	        while (trackersIter.hasNext())
	        {
	            SOCPlayerTracker tracker = trackersIter.next();

	            if (tracker.getPlayer().getPlayerNumber() == newCityPN)
	            {
	                Iterator<SOCPossibleCity> posCitiesIter = tracker.getPossibleCities().values().iterator();

	                while (posCitiesIter.hasNext())
	                {
	                    posCitiesIter.next().updateSpeedup();
	                }

	                break;
	            }
	        }
	    }
	
	/**
	 * Built-in bot has a simple rule for playing the knight card before the dice roll: 
	 * if we have a knight card and the robber is on one of our numbers, 
	 * play the knight card
	 */
	public int rollOrPlayKnight() {
		if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.KNIGHT)
                && ! ourPlayerData.getNumbers().hasNoResourcesForHex(game.getBoard().getRobberHex()))
        {
			moveRobber();
//			/*DEBUG*/
//			System.out.println("Player " + name + " playing knight");
			return RLStrategy.PLAY_KNIGHT;
        }
        else 
        {
//        	/*DEBUG*/
//			System.out.println("Player " + name + " is rolling");
        	return RLStrategy.ROLL;
        }	
	}
	
	/**
	 * returns the place, where the robber should be moved. Also saves what player should be robbed.
	 */
	public int moveRobber() {
		final int bestHex = RobberStrategy.getBestRobberHex(game, ourPlayerData, playerTrackers, rnd);
		List<SOCPlayer> victims = game.getPlayersOnHex(bestHex);
		moveRobber = new int[2];
		moveRobber[0] = bestHex;
		if (victims.size()>1) {
			boolean[] isVictim = new boolean[game.maxPlayers];
			Arrays.fill(isVictim, false);
			for (SOCPlayer pn : victims) {
				if (pn.getPlayerNumber()!=playerNumber && pn.getResources().getTotal() > 0) {
					isVictim[pn.getPlayerNumber()] = true;
				}
			}			
			moveRobber[1] = RobberStrategy.chooseRobberVictim(isVictim, game, playerTrackers);
		}
		
//		/*DEBUG*/
//		System.out.println("Player " + name + " robber moved to: " 
//					+ Integer.toHexString(moveRobber[0]) + " victim: " + moveRobber[1]);
		
		return bestHex;
	}
	

	/**
	 * Main game logic for built in bot. First we check if we can play a knight card.
	 * It is played if we have a knight card in our inventory, we haven't played yet
	 * a dev card in this turn and we will get the largest army if we play the devcard.
	 * 
	 * Then we check if we have roads dev card and our building plan is to build roads.
	 * If we have a discovery dev card and for our building plan 
	 * we need exactly 2 resources we play the card.
	 * If we have a monopoly card and we can get from it number of resources that 
	 * will allow us to trade it for two other resources we use the card.
	 * Decision and chosen resource type are based on which type our player
     * could trade for the most resources (given our player's ports),
     * not on the resources needed for we currently want to build.
     * 
     * If we have enough resources to build a target piece (or to buy a dev card) do it
	 * 
	 */
	public int buildOrTradeOrPlayCard() {
		
		//check if we can play a knight card
		if (game.canPlayKnight(playerNumber)) {
			final boolean canGrowArmy;
			final SOCPlayer laPlayer = game.getPlayerWithLargestArmy();
			if ((laPlayer == null) || (laPlayer.getPlayerNumber() != playerNumber))
            {
                final int larmySize;

                if (laPlayer == null)
                    larmySize = 3;
                else
                    larmySize = laPlayer.getNumKnights() + 1;

                canGrowArmy =
                    ((ourPlayerData.getNumKnights()
                      + ourPlayerData.getInventory().getAmount(SOCDevCardConstants.KNIGHT))
                      >= larmySize);
            } else {
                canGrowArmy = false;  // we already have largest army
            }
			if (canGrowArmy) {
				moveRobber();
//				/*DEBUG*/
//				System.out.println("Player " + name + " playing knight");
	    		return RLStrategy.PLAY_KNIGHT;
			}		
		}
		
		if (buildingPlan.isEmpty()) {
			decisionMaker.planStuff(robotParameters.getStrategyType());
//			/*DEBUG*/
//			System.out.println("player " + name + " making building plan of size: " + buildingPlan.size());
			
			if (buildingPlan.isEmpty()) {
				buildingPlan.clear();
////				 /*DEBUG*/
//	             System.out.println("Player " + name + ": Decided to end the turn");
				 return RLStrategy.END_TURN;
			}
		}
		
		/*DEBUG*/
//		printBuildingPlan();
		
		
		//check if we can play a roads dev card
		if ( (! ourPlayerData.hasPlayedDevCard())
	            && (ourPlayerData.getNumPieces(SOCPlayingPiece.ROAD) >= 2)
	            && ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.ROADS))
	        {

	            SOCPossiblePiece topPiece = buildingPlan.pop();

	            if ((topPiece != null) && (topPiece instanceof SOCPossibleRoad))
	            {
	                SOCPossiblePiece secondPiece = (buildingPlan.isEmpty()) ? null : buildingPlan.peek();

	                if ((secondPiece != null) && (secondPiece instanceof SOCPossibleRoad))
	                {
	                	
	                    roadsToBuildCoord = new int[2];
	                    roadsToBuildCoord[0] = topPiece.getCoordinates();
	                    roadsToBuildCoord[1] = secondPiece.getCoordinates();
	                    
//	                    /*DEBUG*/
//	                    System.out.println("Player " + name + ": Decided to play roads card. Roads: " 
//	                    		+ Arrays.toString(Arrays.stream(roadsToBuildCoord).mapToObj(Integer::toHexString).toArray()));
	                   
	                    /*if topPiece is not our potential road*/
	                    if (!ourPlayerData.isPotentialRoad(roadsToBuildCoord[0])) {
	                    	
//	                    	/*DEBUG*/
//        	        		System.out.println("Ending after bad roads card road placement 1");
        	        		return RLStrategy.END_TURN;
        	        		
	                    //Also check if the second road is potential or has first road on its necessary roads
	                    } else if (!ourPlayerData.isPotentialRoad(roadsToBuildCoord[1])) {
	                    	
	                    	SOCRoad tmpRoad = new SOCRoad(ourPlayerData, roadsToBuildCoord[0], game.getBoard());
	                  		
	                  		game.putTempPiece(tmpRoad);
	                  		
	                  		if (ourPlayerData.isPotentialRoad(roadsToBuildCoord[1])) {
	                  			game.undoPutTempPiece(tmpRoad);	
	                  			/*DEBUG*/
	        	        		System.out.println("Ending after bad roads card road placement 4");
	        	        		return RLStrategy.END_TURN;
	                  		}
	                  		
	                  		game.undoPutTempPiece(tmpRoad);	
	                	    
//	                	    /*potential roads after placing the first road*/
//	                	    HashSet<Integer> potentialSecondRoads = (HashSet<Integer>) ourPn.getPotentialRoads().clone();
//	                    	
//	                    	SOCPossibleRoad posRoad = ourPlayerTracker.getPossibleRoads()
//	                    			.get(Integer.valueOf(roadsToBuildCoord[1]));
//	                    	if (posRoad==null) {
////	                    		/*DEBUG*/
////	        	        		System.out.println("Ending after bad roads card road placement 2");
//	        	        		return RLStrategy.END_TURN;
//	                    	}
//	                    	List<SOCPossibleRoad> necRoads = posRoad.getNecessaryRoads();
//                    		if (necRoads==null || necRoads.isEmpty() || !necRoads.contains(topPiece)) {
//	                    		/*DEBUG*/
////	        	        		System.out.println("Ending after bad roads card road placement 3");
//	        	        		return RLStrategy.END_TURN;
	                    		
	                    }
	                    
	                    return RLStrategy.PLAY_ROADS;	                    
	                }
	                else
	                {
	                    buildingPlan.push(topPiece);
	                }
	            }
	            else
	            {
	                buildingPlan.push(topPiece);
	            }
	        }
		
		 SOCPossiblePiece targetPiece = buildingPlan.peek();
		 
//		 /*DEBUG*/
//	    	System.out.println("current target: " + getPossiblePieceType(targetPiece) + " at loc: " + 
//					 Integer.toHexString(targetPiece.getCoordinates()));
		 	 
	     SOCResourceSet targetResources = targetPiece.getResourcesToBuild();  // may be null
	     
	     // check if we can use the discovery dev card
	     // if we have it and we need at least 2 resources, play the card
	     if ((! ourPlayerData.hasPlayedDevCard())
	             && ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.DISC))
	         {
	             if (chooseFreeResourcesIfNeeded(targetResources, 2, false))
	             {
	            	 //resourceChoices is already set in chooseFreeResourcesIfNeeded()
//	            	 /*DEBUG*/
//	                 System.out.println("Player " + name + ": Decided to play discovery card. Resource choice: " 
//	                		 + resourceChoices.toFriendlyString());
	            	 return RLStrategy.PLAY_DISC;
	             }
	         }
	     
	     //check if we can use the monopoly dev card
	     if ((! ourPlayerData.hasPlayedDevCard())
	                && ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.MONO)
	                && monopolyStrategy.decidePlayMonopoly())
	            {
	    	 		monopolyChoice = monopolyStrategy.getMonopolyChoice();
	    	 		
//	    	 		/*DEBUG*/
//                    System.out.println("Player " + name + ": Decided to play monopoly card. Monopoly choice: " + monopolyChoice);
	    	 		return RLStrategy.PLAY_MONO; 
	            }

	    //check if we can trade bank to get resources that we need 
	    if (tradeToTarget2(targetResources)) {
	    	//bankTrade field is already set by the function tradeToTarget2(SOCResourceSet targetResources)
	    	/*DEBUG*/
//            System.out.println("Player " + name + ": Decided to trade bank give: "
//            		+ bankTrade[0].toFriendlyString() + " get: " + bankTrade[1].toFriendlyString());
	    	return RLStrategy.TRADE_BANK;
	    }
		
	    //check if we can build the planned piece if not end turn	    
	    if (ourPlayerData.getResources().contains(targetResources)) {
	    	targetPiece = buildingPlan.pop();
//	    	/*DEBUG*/
//	    	System.out.println("current target: " + getPossiblePieceType(targetPiece) + " at loc: " + 
//	    			Integer.toHexString(targetPiece.getCoordinates()));
	    	
	    	switch (targetPiece.getType())
	        {
	        case SOCPossiblePiece.CARD:
	        	/*DEBUG*/
//                System.out.println("Player " + name + ": Decided to buy dev card.");
	        	return RLStrategy.BUY_DEVCARD;
	            
	        case SOCPossiblePiece.ROAD:
	        	buildingCoord = targetPiece.getCoordinates();
	        	if (!ourPlayerData.isPotentialRoad(buildingCoord)) {
	        		refreshTracker();
	        		buildingPlan.clear();
	        		/*DEBUG*/
//	        		System.out.println("Player " + name + " Ending after bad road placement");
	        		return RLStrategy.END_TURN;
	        	}
	        	
//	        	/*DEBUG*/
//                System.out.println("Player " + name + ": Decided to set road at edge: " + 
//                		Integer.toHexString(buildingCoord));
//                /*DEBUG*/
//                SOCPossibleRoad rd =  ourPlayerTracker.getPossibleRoads().get(Integer.valueOf(buildingCoord));
//        		boolean isPoss = false;
//        		if (rd!= null)
//        			isPoss = true;
//
//        		System.out.println("player " + name + " potential roads: " +
//        				Arrays.toString(ourPlayerData.getPotentialRoads().stream()
//        						.map(Integer::toHexString).toArray()) +
//        				" is posssible in tracker: " + isPoss);   
	        	return RLStrategy.PLACE_ROAD;
	        	
	        case SOCPlayingPiece.SETTLEMENT:
	        	buildingCoord = targetPiece.getCoordinates();
	        	if (!ourPlayerData.isPotentialSettlement(buildingCoord)) {
	        		/*DEBUG*/
//	        		System.out.println("Ending after bad settlement placement");
	        		return RLStrategy.END_TURN;
	        	}
//	        	/*DEBUG*/
//                System.out.println("Player " + name + ": Decided to set settlement at node: " + 
//                		Integer.toHexString(buildingCoord));
	        	return RLStrategy.PLACE_SETTLEMENT;
	        	
	        case SOCPlayingPiece.CITY:
	        	buildingCoord = targetPiece.getCoordinates();
	        	if (!ourPlayerData.isPotentialCity(buildingCoord)) {
	        		/*DEBUG*/
//	        		System.out.println("Ending after bad city placement");
	        		return RLStrategy.END_TURN;
	        	}
//	        	/*DEBUG*/
//                System.out.println("Player " + name + ": Decided to set city at node: " + 
//                		Integer.toHexString(buildingCoord));
	        	return RLStrategy.PLACE_CITY;
	        } 	
	    	
	    } else {
	    	/*DEBUG*/
//            System.out.println("Player " + name + ": wanted to build " + getPossiblePieceType(targetPiece) +
//            		" at " + Integer.toHexString(targetPiece.getCoordinates()) + " but he has " + 
//            		ourPlayerData.getResources().toFriendlyString());
	    	buildingPlan.clear();
	    	return RLStrategy.END_TURN;
	    }
	    
		return -1;
	}
	
	
	public SOCResourceSet handleDiscard(int numCards) {
		return DiscardStrategy.discard(numCards, buildingPlan, rnd,
	            ourPlayerData, robotParameters, decisionMaker, negotiator);
	}
	
	/**
	 * Function copied from {@link SOCRobotBrain} class to check 
	 * if we want to play discovery dev card
	 * @see {@link SOCRobotBrain.chooseFreeResourcesIfNeeded(SOCResourceSet targetResources, 
	 * 		final int numChoose, final boolean chooseIfNotNeeded)}
	 */
	 protected boolean chooseFreeResourcesIfNeeded
     (SOCResourceSet targetResources, final int numChoose, final boolean chooseIfNotNeeded)
 {
     if (targetResources == null)
         return false;

     if (chooseIfNotNeeded)
         resourceChoices.clear();

     final SOCResourceSet ourResources = ourPlayerData.getResources();
     int numMore = numChoose;

     // Used only if chooseIfNotNeeded:
     int buildingItem = 0;  // for ourBuildingPlan.peek
     boolean stackTopIs0 = false;

     /**
      * If ! chooseIfNotNeeded, this loop
      * body will only execute once.
      */
     do
     {
         int numNeededResources = 0;
         if (targetResources == null)  // can be null from SOCPossiblePickSpecialItem.cost
             break;

         for (int resource = SOCResourceConstants.CLAY;
                 resource <= SOCResourceConstants.WOOD;
                 resource++)
         {
             final int diff = targetResources.getAmount(resource) - ourResources.getAmount(resource);
             if (diff > 0)
                 numNeededResources += diff;
         }

         if ((numNeededResources == numMore)  // TODO >= numMore ? (could change details of current bot behavior)
             || (chooseIfNotNeeded && (numNeededResources > numMore)))
         {
             chooseFreeResources(targetResources, numMore, ! chooseIfNotNeeded);
             return true;
         }

         if (! chooseIfNotNeeded)
             return false;

         // Assert: numNeededResources < numMore.
         // Pick the first numNeeded, then loop to pick additional ones.
         chooseFreeResources(targetResources, numMore, false);
         numMore = numChoose - resourceChoices.getTotal();

         if (numMore > 0)
         {
             // Pick a new target from building plan, if we can.
             // Otherwise, choose our least-frequently-rolled resources.

             ++buildingItem;
             final int bpSize = buildingPlan.size();
             if (bpSize > buildingItem)
             {
                 if (buildingItem == 1)
                 {
                     // validate direction of stack growth for buildingPlan
                     stackTopIs0 = (0 == buildingPlan.indexOf(buildingPlan.peek()));
                 }

                 int i = (stackTopIs0) ? buildingItem : (bpSize - buildingItem) - 1;

                 SOCPossiblePiece targetPiece = buildingPlan.elementAt(i);
                 targetResources = targetPiece.getResourcesToBuild();  // may be null

                 // Will continue at top of loop to add
                 // targetResources to resourceChoices.

             } else {

                 // This will be the last iteration.
                 // Choose based on our least-frequent dice rolls.

                 final int[] resourceOrder =
                     SOCBuildingSpeedEstimate.getRollsForResourcesSorted(ourPlayerData);

                 int curRsrc = 0;
                 while (numMore > 0)
                 {
                     resourceChoices.add(1, resourceOrder[curRsrc]);
                     --numMore;
                     ++curRsrc;
                     if (curRsrc == resourceOrder.length)
                         curRsrc = 0;
                 }

                 // now, numMore == 0, so do-while loop will exit at bottom.
             }
         }

     } while (numMore > 0);

     return true;
 }
	 
	 /**
	 * Function copied from {@link SOCRobotBrain} class to check 
	 * if we want to play discovery dev card
	 * @see {@link SOCRobotBrain.chooseFreeResources(final SOCResourceSet targetResources, 
	 * final int numChoose, final boolean clearResChoices)}
	 */
	 protected boolean chooseFreeResources
     (final SOCResourceSet targetResources, final int numChoose, final boolean clearResChoices)
 {
     /**
      * clear our resource choices
      */
     if (clearResChoices)
         resourceChoices.clear();

     /**
      * find the most needed resource by looking at
      * which of the resources we still need takes the
      * longest to acquire
      */
     SOCResourceSet rsCopy = ourPlayerData.getResources().copy();
     SOCBuildingSpeedEstimate estimate = new SOCBuildingSpeedEstimate(ourPlayerData.getNumbers());
     int[] rollsPerResource = estimate.getRollsPerResource();

     for (int resourceCount = 0; resourceCount < numChoose; resourceCount++)
     {
         int mostNeededResource = -1;

         for (int resource = SOCResourceConstants.CLAY;
                 resource <= SOCResourceConstants.WOOD; resource++)
         {
             if (rsCopy.getAmount(resource) < targetResources.getAmount(resource))
             {
                 if (mostNeededResource < 0)
                 {
                     mostNeededResource = resource;
                 }
                 else
                 {
                     if (rollsPerResource[resource] > rollsPerResource[mostNeededResource])
                     {
                         mostNeededResource = resource;
                     }
                 }
             }
         }

         if (mostNeededResource == -1)
             return false;  // <--- Early return: couldn't choose enough ---

         resourceChoices.add(1, mostNeededResource);
         rsCopy.add(1, mostNeededResource);
     }

     return true;
 	}
	 
	 /**
	 * Function copied from {@link SOCRobotBrain} class to check 
	 * if we want to trade with bank
	 * @see {@link SOCRobotBrain.tradeToTarget2(SOCResourceSet targetResources)}
	 */
	 protected boolean tradeToTarget2(SOCResourceSet targetResources)
	    {
	        if ((targetResources == null) || ourPlayerData.getResources().contains(targetResources))
	        {
	            return false;
	        }

	        SOCTradeOffer bankOffer = negotiator.getOfferToBank(targetResources, ourPlayerData.getResources());

	        if ((bankOffer != null) && (ourPlayerData.getResources().contains(bankOffer.getGiveSet())))
	        {
	        	bankTrade[0] = bankOffer.getGiveSet();
	            bankTrade[1] = bankOffer.getGetSet();
	            return true;
	        }

	        return false;
	    }
	 
	 public void handleEndGame(int winner) {
		}
	 
	 protected void refreshTracker() {
		Iterator<SOCPossibleSettlement> itPossSet = ourPlayerTracker.getPossibleSettlements().values().iterator();
		TreeMap<Integer, SOCPossibleRoad> possRoads =  ourPlayerTracker.getPossibleRoads();
		
		while (itPossSet.hasNext()) {
			SOCPossibleSettlement possSet  = itPossSet.next();
			List<SOCPossibleRoad> necRoads = possSet.getNecessaryRoads();
			for (SOCPossibleRoad rd :  necRoads) {
				int rdCoord = rd.getCoordinates();
				if (!possRoads.containsKey(Integer.valueOf(rdCoord)) && !ourPlayerData.isPotentialRoad(rdCoord)) {
					 necRoads.remove(rd);
				}
			}
		}
	 }
	 
	 protected void printBuildingPlan() {
		 String msg = "player " + name + " wants to build: ";
		 for (int i=0; i<5 && i<buildingPlan.size() ;i++) {
			 SOCPossiblePiece targetPiece = buildingPlan.elementAt(i);			 
			 msg += (i+1) + ". " + getPossiblePieceType(targetPiece) + 
					 " at location: " + Integer.toHexString(targetPiece.getCoordinates()) + " ";	 
		 }
		 
		 System.out.println(msg);
	 }
	 
	 protected String getPossiblePieceType(SOCPossiblePiece piece) {
		 String type = "";
		 
		 switch (piece.getType()) {
		 case SOCPossiblePiece.CARD:
			 type = "card";
			 break;
		 case SOCPossiblePiece.ROAD:
			 type = "road";
			 break;
		 case SOCPossiblePiece.SETTLEMENT:
			 type = "settlement";
			 break;
		 case SOCPossiblePiece.CITY:
			 type = "city";
			 break;
		 }
		 
		 return type;
	 }
}


