//BE CAREFULL INDEXES OF RESOURCES CONSTANTS START WITH 1
package soc.robot.rl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Random;

import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCInventory;
import soc.game.SOCPlayer;
import soc.game.SOCPlayerNumbers;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCSettlement;
import soc.game.SOCRoad;
import soc.robot.SOCPlayerTracker;
import soc.robot.SOCPossibleRoad;
import soc.robot.SOCPossibleSettlement;
import soc.robot.SOCRobotBrain;
import soc.util.SOCRobotParameters;

public class RLStrategy {
	
	/** actions to make */
	public static final int PLACE_SETTLEMENT = 0;
	public static final int PLACE_CITY = 1;
	public static final int PLACE_ROAD = 2;
	public static final int BUY_DEVCARD = 3;
	public static final int PLAY_KNIGHT = 4;
	public static final int PLAY_MONO = 5;
	public static final int PLAY_DISC = 6;
	public static final int PLAY_ROADS = 7;
	public static final int TRADE_BANK = 8;
	public static final int TRADE_PLAYER = 9;
	public static final int ROLL = 10;
	public static final int END_TURN = 11;	
	
	
	 /** Our game */
    protected final SOCGame game;
    
    protected SOCRobotBrain brain;
    protected HashMap<Integer,SOCPlayerTracker> playerTrackers;
    protected SOCPlayerTracker ourPlayerTracker;
    protected final SOCPlayer ourPlayerData;
    protected SOCBoard board;
    protected HashMap<SOCPlayer, int[]> oldState;
    protected int[] oldState2;
    protected SOCState state;
    protected Vector<SOCPlayer> opponents;
    protected HashMap<int[], Double> states;
    protected HashMap<int[], Double> states2;
    
    protected double alpha;
    protected double gamma;

    /**
     * {@link #ourPlayerData}'s player number.
     * @since 2.0.00
     */
    protected final int ourPlayerNumber;
    
    /**
     * Create an RLStrategy for a {@link SOCRobotBrain}'s player.
     * @param br robot's brain
     */
    public RLStrategy(SOCRobotBrain br)
    {
    	brain = br;
    	playerTrackers = brain.getPlayerTrackers();
        ourPlayerTracker = brain.getOurPlayerTracker();
        ourPlayerData = brain.getOurPlayerData();
        ourPlayerNumber = ourPlayerData.getPlayerNumber();
        game = brain.getGame();
        board = game.getBoard();
        state = new SOCState(ourPlayerNumber, playerTrackers);
        state.updateAll(playerTrackers, board);   
        alpha = 0.6;
        gamma = 1.0;
        
        System.out.println("alpha " + alpha + " gamma " + gamma);
        
        opponents = new Vector<SOCPlayer>();
        Iterator<SOCPlayerTracker> trackersIter = playerTrackers.values().iterator();
        
        while (trackersIter.hasNext())
  	  {
  		  SOCPlayerTracker tracker = trackersIter.next();
  		  if (tracker.getPlayer().getPlayerNumber() != ourPlayerNumber) {
  			  opponents.add(tracker.getPlayer());
  		  }
  	  }
        
        states = new HashMap<int[], Double>();    
        states2 = new HashMap<int[], Double>(); 
        oldState = new HashMap<SOCPlayer, int[]>();
        oldState2 = new int[6];
        
        ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();

    	for (SOCPlayer opp : opponents) {
    		int[] playerState = state.getState(opp);
    		int points = opp.getPublicVP();
    		Double value = Double.valueOf(0.5); //or maybe random?
			states.put(playerState, value);
			oldState.put(opp, playerState);
    		
    		int state_value = Math.round(value.floatValue()*10);	
    		
    		opp_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(state_value)));
    		
    	}
    	
    	int[] secondState = new int[6];
    	
    	for(int i = 0; i<opp_states.size(); i++) {
    		secondState[i*2] = opp_states.get(i).getKey().intValue();
    		secondState[i*2 + 1] = opp_states.get(i).getValue().intValue();
    	}
    	
    	Double value = Double.valueOf(0.5); //or maybe random?
		states2.put(secondState, Double.valueOf(value));
		oldState2 = secondState;
    }
    
    public void getAction(){
    	/*
    	 * preparing information about our player to put into the lookup table
    	 */


    }
    
    protected AbstractMap.SimpleEntry<Float, Integer> searchPlaceSettlement() {
    	int[] potentialSettlements = ourPlayerData.getPotentialSettlements_arr();
    	SOCState tmpState = state.copySOCState();    	
    	ArrayList<Float> state_values = new ArrayList<Float>(potentialSettlements.length);
    	
    	int[] plResources = tmpState.getPlayerState(ourPlayerData).getResources();
		plResources[SOCResourceConstants.SHEEP-1]--;
		plResources[SOCResourceConstants.WHEAT-1]--;
		plResources[SOCResourceConstants.WOOD-1]--;
		plResources[SOCResourceConstants.CLAY-1]--;
    	
    	for(int posSetCoord : potentialSettlements) {
    		SOCSettlement tmpSet = new SOCSettlement(ourPlayerData, posSetCoord, board);
    		HashMap<Integer, SOCPlayerTracker> trackersCopy = 
    				SOCPlayerTracker.tryPutPiece(tmpSet, game, playerTrackers);
    		    		
    		Iterator<SOCPlayerTracker> trackersIter = trackersCopy.values().iterator();

		    while (trackersIter.hasNext())
		    {
		    	SOCPlayerTracker tracker = trackersIter.next();    
		    	tmpState.updatePlaceSettlement(tracker.getPlayer(), board);
		    }
		    
		    state_values.add(Float.valueOf(getStateValue(tmpState)));
		    
		    SOCPlayerTracker.undoTryPutPiece(tmpSet, game);		    
    	}
    		
    	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
    	AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
    			maxAndIndex.getKey(), Integer.valueOf(potentialSettlements[maxAndIndex.getValue()]));
    	return result; 
    }
    
    protected AbstractMap.SimpleEntry<Float, Integer> searchPlaceRoad() {
    	HashSet<Integer> potentialRoads = new HashSet<Integer>(ourPlayerData.getPotentialRoads());
    	SOCState tmpState = state.copySOCState();
    	ArrayList<Float> state_values = new ArrayList<Float>(potentialRoads.size());
    	ArrayList<Integer> roadCoord = new ArrayList<Integer>();
    	
    	int[] plResources = tmpState.getPlayerState(ourPlayerData).getResources();
		plResources[SOCResourceConstants.WOOD-1]--;
		plResources[SOCResourceConstants.CLAY-1]--;

		Iterator<Integer> itRoadCoord = potentialRoads.iterator();
    	while (itRoadCoord.hasNext()) {
    		Integer posRoad = itRoadCoord.next();
    		SOCRoad tmpRoad = new SOCRoad(ourPlayerData, posRoad.intValue(), board);
    		HashMap<Integer, SOCPlayerTracker> trackersCopy = 
    				SOCPlayerTracker.tryPutPiece(tmpRoad, game, playerTrackers);
    		    		
    		Iterator<SOCPlayerTracker> trackersIter = trackersCopy.values().iterator();

		    while (trackersIter.hasNext())
		    {
		    	SOCPlayerTracker tracker = trackersIter.next();    
		    	tmpState.updatePlaceRoad(tracker.getPlayer());
		    }
		    
		    state_values.add(Float.valueOf(getStateValue(tmpState)));
		    roadCoord.add(posRoad);
		    
		    SOCPlayerTracker.undoTryPutPiece(tmpRoad, game);		    
    	}
    	
    	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
    	Integer roadToBuild = roadCoord.get(maxAndIndex.getValue());
    	AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
    			maxAndIndex.getKey(), roadToBuild);
    	return result;  
    }
    
    protected AbstractMap.SimpleEntry<Float, Integer> searchPlaceCity() {
    	Vector<SOCSettlement> potentialCities = new Vector<SOCSettlement>(ourPlayerData.getSettlements());
    	SOCState tmpState = state.copySOCState();
    	ArrayList<Float> state_values = new ArrayList<Float>(potentialCities.size());
    	ArrayList<Integer> cityCoord = new ArrayList<Integer>();
    	
    	int[] plResources = tmpState.getPlayerState(ourPlayerData).getResources();
		plResources[SOCResourceConstants.ORE-1] -= 3;
		plResources[SOCResourceConstants.WHEAT-1] -= 2;
    	
		Iterator<SOCSettlement> itCityCoord = potentialCities.iterator();
    	while (itCityCoord.hasNext()) {
    		SOCSettlement posCity = itCityCoord.next();
    		SOCCity tmpCity = new SOCCity(ourPlayerData, posCity.getCoordinates(), board);
    		HashMap<Integer, SOCPlayerTracker> trackersCopy = 
    				SOCPlayerTracker.tryPutPiece(tmpCity, game, playerTrackers);
    		    		
    		Iterator<SOCPlayerTracker> trackersIter = trackersCopy.values().iterator();

		    while (trackersIter.hasNext())
		    {
		    	SOCPlayerTracker tracker = trackersIter.next();
		    	tmpState.updatePlaceCity(tracker.getPlayer(), board);
		    }
		    
		    state_values.add(Float.valueOf(getStateValue(tmpState)));
		    cityCoord.add(Integer.valueOf(posCity.getCoordinates()));
		    
		    SOCPlayerTracker.undoTryPutPiece(tmpCity, game);		    
    	}
    	
    	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
    	Integer cityToBuild = cityCoord.get(maxAndIndex.getValue());
    	AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
    			maxAndIndex.getKey(), cityToBuild);
    	return result;  
    }
    
    /**
     * Adding state, because it will be used in {@link #searchRollDice()}
     * @param state
     * @param playKnight
     * @return
     */
    protected AbstractMap.SimpleEntry<Float, int[]> 
    searchPlaceRobberOrPlayKnight(SOCState state, boolean playKnight, boolean searchAfterSeven) {
    	int[] landHexes = board.getLandHexCoords();
//    	SOCState tmpState = state.copySOCState();
    	ArrayList<Float> state_values = new ArrayList<Float>();
    	ArrayList<Integer> robPositions = new ArrayList<Integer>();
    	ArrayList<Integer> playersToRob = new ArrayList<Integer>();
    	
    	boolean willGetLA = false;
    	SOCPlayer playerWithLA = game.getPlayerWithLargestArmy();
    	int playedKnights = state.getPlayerState(ourPlayerData).getPlayedKnights();    	
    	if (playKnight) {
        	if (playerWithLA == null) {
        		willGetLA = playedKnights + 1 > 2;
        	} else {
        		if (playerWithLA.getPlayerNumber() != ourPlayerNumber) {
        			int opPlayedKnights = state.getPlayerState(playerWithLA).getPlayedKnights();
        			willGetLA = playedKnights + 1 > opPlayedKnights;
        		}
        	}
    	}
    	
    	int[] prevResource = Arrays.copyOf(state.getPlayerState(ourPlayerData).getResources(), 5);
    	
    	for(int posRob : landHexes) {
    		
    		if (board.getHexTypeFromCoord(posRob) != SOCBoard.DESERT_HEX &&
    				posRob != board.getRobberHex()){
    			
    			int[] adjacentNodes = board.getAdjacentNodesToHex_arr(posRob);
    			
    			Set<SOCPlayer> affectedPlayers = new HashSet<SOCPlayer>();
    			
    			for (int node : adjacentNodes) {
    				SOCPlayingPiece building = board.settlementAtNode(node);
    				
    				if (building!= null) {
    					affectedPlayers.add(building.getPlayer());
    				}
    			}
    			
    			for (SOCPlayer player : affectedPlayers) {
    				if (player.getPlayerNumber() == ourPlayerNumber)
    					continue;
    				ArrayList<Float> state_values_res = new ArrayList<Float>();
    				SOCState tmpState = state.copySOCState();
    				int[] plResources = state.getPlayerState(player).getResources();
    				int[] plResourcesProb = state.getPlayerState(player).getResourceProbabilities();
    				
    				if (playKnight) {
    		    		int[] devCards = tmpState.getPlayerState(ourPlayerData).getDevCards();
    		    		//index of number of old knight cards in our hand
    		    		devCards[7]--;
    		    		tmpState.getPlayerState(ourPlayerData).setPlayedKnights(playedKnights+1);
    		    		if (willGetLA) {
    		    			tmpState.getPlayerState(ourPlayerData).setHasLargestArmy(1);
    		    			tmpState.getPlayerState(playerWithLA).setHasLargestArmy(0);
    		    		}
    		    	}
    				
    				
    				tmpState.updatePlaceRobber(player, board, posRob);
    				
    				for (int resource = SOCResourceConstants.CLAY -1;
    						resource <= SOCResourceConstants.WOOD -1; resource++) {
    					
    					tmpState.updateResources(player, true);
    					
    					if (searchAfterSeven) {
    						int[] res = Arrays.copyOf(prevResource, prevResource.length);
    						tmpState.getPlayerState(ourPlayerData).setResources(res);
    					} else {
    						tmpState.updateResources(ourPlayerData, false);
    					}
    					
    					if (plResources[resource]>0 || 
    							(plResourcesProb[resource] > 0 && plResources[SOCResourceConstants.UNKNOWN-1] > 0 )) {
    						
    						tmpState.updateSteal(player, resource, false);
    						
    						//in future maybe calc weighted mean
    						state_values_res.add(Float.valueOf(getStateValue(tmpState)));    						
    					}
    				}
    				
    				double average = state_values_res.stream().mapToDouble(a -> a).average().orElse(0.);
					state_values.add(Float.valueOf((float) average));
					robPositions.add(Integer.valueOf(posRob));
					playersToRob.add(Integer.valueOf(player.getPlayerNumber()));
    			}
    		}
    	}
    	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
    	int[] placeAndPlayerToRob = new int[2];
    	placeAndPlayerToRob[0] = robPositions.get(maxAndIndex.getValue());
    	placeAndPlayerToRob[1] = playersToRob.get(maxAndIndex.getValue());
    	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
    			maxAndIndex.getKey(), placeAndPlayerToRob);
    	return(result);  	
    }
    
    protected float searchBuyDevelopmentCard(){
    	float state_value = 0.0f;
		SOCState tmpState = state.copySOCState();
		int[] plResources = tmpState.getPlayerState(ourPlayerData).getResources();
		plResources[SOCResourceConstants.SHEEP-1]--;
		plResources[SOCResourceConstants.WHEAT-1]--;
		plResources[SOCResourceConstants.ORE-1]--;
		int[] devCards = tmpState.getPlayerState(ourPlayerData).getDevCards();
		//value of state will be multiplied by probability of getting this card 
		// (based on initial number of dev cards)
		// dev cards in following order: VP cards, road building card, discovery card,
	    // monopoly card, Knight card
		// TO DO: maybe you can track which card were already definitely used
		int[] initDevCardProbs = new int[]{5, 2, 2, 2, 14};
		int[] devCardsToPick = new int[]{0, 2, 4, 6, 8};
		for (int i = 0 ; i < devCardsToPick.length ; i++) {
			tmpState.updateDevCards(ourPlayerData);
			devCards[i]++;
			state_value += getStateValue(tmpState)*initDevCardProbs[i];
		}
		
		//25 = sum of number of dev card at the beginning
		return state_value/25;
    }
    
    protected AbstractMap.SimpleEntry<Float, int[]> searchPlayDiscovery() {
    	SOCState tmpState = state.copySOCState();
    	int[] devCards = tmpState.getPlayerState(ourPlayerData).getDevCards();
    	//index of number of old discoveries cards in our hand
    	devCards[3]--;
    	
    	//there are 13 combinations of 2 resources (out of 5), that we can pick
    	ArrayList<Float> state_values = new ArrayList<Float>(13);
    	ArrayList<int[]> resToPick = new ArrayList<int[]>(13);
    	int[] plResources = tmpState.getPlayerState(ourPlayerData).getResources();
    	
    	for (int i = 0; i < 5; i++) {
    		for (int j = i; j < 5; j++) {
    			tmpState.updateResources(ourPlayerData, false);
    			plResources[i]++;
    			plResources[j]++;
    			state_values.add(Float.valueOf(getStateValue(tmpState)));
    			resToPick.add(new int[]{i, j});
    		}
    	}
    	
    	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
    	int[] resourcesToPick = resToPick.get(maxAndIndex.getValue());
    	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
    			maxAndIndex.getKey(), resourcesToPick);
    	return result;  	
    }
    
    protected AbstractMap.SimpleEntry<Float, Integer> searchPlayMonopoly() {
    	SOCState tmpState = state.copySOCState();
    	int[] devCards = tmpState.getPlayerState(ourPlayerData).getDevCards();
    	//index of number of old monopoly cards in our hand
    	devCards[5]--;
    	
    	ArrayList<Float> state_values = new ArrayList<Float>(5);
    	
    	for (int res = 0; res < 5 ; res++) {
    		tmpState.updateResources(ourPlayerData, false);
    		
    		for (SOCPlayer op : opponents) {
    			tmpState.updateResources(op, true);
    			tmpState.updateSteal(op, res, true);
    		}
    		
    		state_values.add(Float.valueOf(getStateValue(tmpState)));    		
    	}
    	return(getMaxAndIndex(state_values));
    }
    
    protected AbstractMap.SimpleEntry<Float, Integer[]> searchPlayRoads() {
    	HashSet<Integer> potentialRoads = ourPlayerData.getPotentialRoads();
    	SOCState tmpState = state.copySOCState();
    	ArrayList<Float> state_values = new ArrayList<Float>();
    	ArrayList<Integer[]> twoRoadsCoord = new ArrayList<Integer[]>();
    	
    	int[] devCards = tmpState.getPlayerState(ourPlayerData).getDevCards();
    	//index of number of old build roads cards in our hand
    	devCards[1]--;
    	
    	for(Integer posSetCoord : potentialRoads) {
    		SOCRoad tmpRoad = new SOCRoad(ourPlayerData, posSetCoord, board);
    		HashMap<Integer, SOCPlayerTracker> trackersCopy = 
    				SOCPlayerTracker.tryPutPiece(tmpRoad, game, playerTrackers);
    		    		
    		Iterator<SOCPlayerTracker> trackersIter = trackersCopy.values().iterator();

		    while (trackersIter.hasNext())
		    {
		    	SOCPlayerTracker tracker = trackersIter.next();    
		    	tmpState.updatePlaceRoad(tracker.getPlayer());
		    }
		    
		    HashSet<Integer> potentialSecondRoads = ourPlayerData.getPotentialRoads();
		    

	    	for(Integer posSecondSetCoord : potentialSecondRoads) {
	    		
	    		SOCRoad tmpSecondRoad = new SOCRoad(ourPlayerData, posSecondSetCoord, board);
	    		HashMap<Integer, SOCPlayerTracker> trackersSecondCopy = 
	    				SOCPlayerTracker.tryPutPiece(tmpSecondRoad, game, trackersCopy);
	    		    		
	    		Iterator<SOCPlayerTracker> trackersSecondIter = trackersSecondCopy.values().iterator();

			    while (trackersSecondIter.hasNext())
			    {
			    	SOCPlayerTracker tracker = trackersSecondIter.next();    
			    	tmpState.updatePlaceRoad(tracker.getPlayer());
			    }
			    
			    state_values.add(Float.valueOf(getStateValue(tmpState)));
			    twoRoadsCoord.add(new Integer[]{posSetCoord, posSecondSetCoord});
			    
			    SOCPlayerTracker.undoTryPutPiece(tmpSecondRoad, game);
	    	}
		    		    
		    SOCPlayerTracker.undoTryPutPiece(tmpRoad, game);		    
    	}
    	
    	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
    	Integer[] roadsToBuild = twoRoadsCoord.get(maxAndIndex.getValue());
    	AbstractMap.SimpleEntry<Float, Integer[]> result = new AbstractMap.SimpleEntry<Float, Integer[]>(
    			maxAndIndex.getKey(), roadsToBuild);
    	return result;  
    }
    
    
    protected AbstractMap.SimpleEntry<Float, int[]> searchTradeBank() {
    	SOCState tmpState = state.copySOCState();
    	ArrayList<Float> state_values = new ArrayList<Float>();
    	ArrayList<int[]> tradeOffer = new ArrayList<int[]>();
    	
    	int[] ports = tmpState.getPlayerState(ourPlayerData).getPorts();
    	int[] plResources = tmpState.getPlayerState(ourPlayerData).getResources();
    	
    	int howMuchToGive = 4;
    	if (ports[SOCBoard.MISC_PORT]==1) {
    		howMuchToGive = 3;
    	}
    	
    	for (int j = 0; j < plResources.length; j++) {
    		int res = plResources[j];
    		
    		if (res>=4 || (res>=3 && ports[SOCBoard.MISC_PORT]==1)|| (res>=2 && ports[j+1]==1) ) {
    			int quantity = howMuchToGive;
    			if (ports[j+1]==1) {
    				quantity = 2;
    			}
    			
    			for (int i = 0; i < plResources.length; i++) {
    	    		if (i == j) 
    	    			continue;
    	    		tmpState.updateResources(ourPlayerData, false);
    	    		plResources[j] -= quantity;
    	    		plResources[i]++;
    	    		state_values.add(Float.valueOf(getStateValue(tmpState)));	
    	    		tradeOffer.add(new int[] {j, i, quantity});
    	    	}    			
    		}
    	}
    	
    	//if we don't have any possibility to trade
    	if (state_values.isEmpty()) {
    		return(new AbstractMap.SimpleEntry<Float, int[]>(Float.valueOf(-1.0f), null));
    	}

    	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
    	int[] trade = tradeOffer.get(maxAndIndex.getValue());
    	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
    			maxAndIndex.getKey(), trade);
    	return result;  
    }
    
    protected AbstractMap.SimpleEntry<Float, int[]> searchDiscardAfterSevenRolled(int numDiscards) {
    	SOCState tmpState = state.copySOCState();
    	ArrayList<Float> state_values = new ArrayList<Float>();
    	int[] plResources = tmpState.getPlayerState(ourPlayerData).getResources();
    	ArrayList<int[]> allCardsDiscard = new ArrayList<int[]>();
    	
    	kLengthCombination combinationsFinder = new kLengthCombination(plResources, numDiscards);
    	allCardsDiscard = combinationsFinder.getCombinations();
    	
    	for(int[] cards : allCardsDiscard) {
    		
    		tmpState.updateResources(ourPlayerData, false);
    		for(int i = 0; i < cards.length; i++) {
    			plResources[i]-=cards[i];
    		}
    		
    		state_values.add(Float.valueOf(getStateValue(tmpState)));    		   		
    	}
    	
    	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
    	int[] resourceToDiscard = allCardsDiscard.get(maxAndIndex.getValue());
    	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
    			maxAndIndex.getKey(), resourceToDiscard);
    	return result;  	
    }
    
    protected float searchRollDice() {
    	SOCState tmpState = state.copySOCState();
    	float state_value = 0.0f;
//    	ArrayList<Float> state_values = new ArrayList<Float>();
    	
    	SOCPlayerNumbers numbers = ourPlayerData.getNumbers();
    	int robberHex = board.getRobberHex();
    	int[] plResources = tmpState.getPlayerState(ourPlayerData).getResources();
    	
    	float[] FLOAT_VALUES =
    	    {
    	        0.0f, 0.0f, 0.03f, 0.06f, 0.08f, 0.11f, 0.14f, 0.17f, 0.14f, 0.11f,
    	        0.08f, 0.06f, 0.03f
    	    };
    	
    	for(int i = 2; i <= 12; i++) {
    		if (i==7)
    			continue;
    		
    		tmpState.updateResources(ourPlayerData, false);
    		
    		Vector<Integer> resources = numbers.getResourcesForNumber(i, robberHex);
    		for (int res : resources){
    			//numbers.getResourcesForNumber(i, robberHex) returns resources, where CLAY = 1,
    			//so we have to decrease each resource by 1
    			plResources[res-1]++;
    		}
    		
//    		state_values.add(Float.valueOf(getStateValue(tmpState)) * FLOAT_VALUES[i]);
    		state_value += getStateValue(tmpState) * FLOAT_VALUES[i];
    	}
    	
    	//For 7 more happens: discard cards and Place robber
    	tmpState.updateResources(ourPlayerData, false);
    	int numCards = ourPlayerData.getResources().getKnownTotal();
    	if (numCards > 7) {
    		AbstractMap.SimpleEntry<Float, int[]> discards = searchDiscardAfterSevenRolled( (int)numCards/2 );
    		int[] cardsDiscard = discards.getValue();
    		for(int i = 0; i < cardsDiscard.length; i++) {
    			plResources[i]-=cardsDiscard[i];
    		}
    	}
    	//we pass a copy of tmpState, because searchPlaceRobberOrPlayKnight updates resources
		SOCState tmpState2 = tmpState.copySOCState();
    	
    	AbstractMap.SimpleEntry<Float, int[]> resultRobber = searchPlaceRobberOrPlayKnight(tmpState2, false, true);
    	
    	state_value += resultRobber.getKey()*FLOAT_VALUES[7];
    	
    	//probabilities of dice roll sum up to 1.01
    	return(state_value/1.01f);
    }   
    
    protected AbstractMap.SimpleEntry<Float, Integer> getMaxAndIndex(ArrayList<Float> array) {
        if (array.size() == 0) {
            return new AbstractMap.SimpleEntry<Float, Integer>((float) 0, -1); // array contains no elements
        }
        Float max = array.get(0);
        int pos = 0;

        for(int i=1; i<array.size(); i++) {
            if (max < array.get(i)) {
                pos = i;
                max = array.get(i);
            }
        }
        return new AbstractMap.SimpleEntry<Float, Integer>(max, pos);
    }
    
    protected float getStateValue(SOCState tmpState) {
    	ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();

    	for (SOCPlayer opp : opponents) {
    		int[] playerState = tmpState.getState(opp);
    		int points = opp.getPublicVP();
    		Double value = states.get(playerState);
    		if (value!=null) {}
    		else {
    			value = new Random().nextGaussian()*0.05 + 0.5;
    			states.put(playerState, Double.valueOf(value));
    		}
    		
    		int state_value = Math.round(value.floatValue()*10);	
    		
    		opp_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(state_value)));
    		
    	}
    	
    	int[] secondState = new int[6];
    	opp_states.sort(new Comparator<CustomPair>() {
		    public int compare(CustomPair o1, CustomPair o2) {
		        return o2.getKey().compareTo(o1.getKey());
		    }
		});
    	
    	for(int i = 0; i<opp_states.size(); i++) {
    		secondState[i*2] = opp_states.get(i).getKey().intValue();
    		secondState[i*2 + 1] = opp_states.get(i).getValue().intValue();
    	}
    	
    	Double value = states2.get(secondState);
		if (value!=null) {}
		else {
			value = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
			states2.put(secondState, Double.valueOf(value));
		}
		
		return value.floatValue();

    }
    
    public AbstractMap.SimpleEntry<Integer, int[]> rollOrPlayKnight() {

    	updateStateValue();
    	
    	if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.KNIGHT)) {
    		AbstractMap.SimpleEntry<Float, int[]> playKnight = searchPlaceRobberOrPlayKnight(state, true, false);
        	float roll = searchRollDice();
        	
        	if (roll<playKnight.getKey()) {
        		return(new AbstractMap.SimpleEntry<Integer, int[]>(PLAY_KNIGHT, playKnight.getValue()));
        	} 
    	}
    	
    	return(new AbstractMap.SimpleEntry<Integer, int[]>(ROLL, null));  	
    }
    
    public int[] moveRobber() {
    	
    	updateStateValue();
    	
    	return searchPlaceRobberOrPlayKnight(state, true, false).getValue(); 	
    }
    
    public SOCResourceSet discard(int numDiscards) {
    	
    	updateStateValue();
    	
    	SOCResourceSet resSet = new SOCResourceSet(searchDiscardAfterSevenRolled(numDiscards).getValue());
    	return resSet;
    }
    
    public AbstractMap.SimpleEntry<Integer, int[]> buildOrTradeOrPlayCard() {
    	
    	updateStateValue();
    	
    	ArrayList<Float> actionValues = new ArrayList<Float>();
    	ArrayList<Integer> actionNames = new ArrayList<Integer>();
    	AbstractMap.SimpleEntry<Float, int[]> discovery = null;
    	AbstractMap.SimpleEntry<Float, Integer> monopoly = null;
    	AbstractMap.SimpleEntry<Float, Integer[]> roads = null;
    	AbstractMap.SimpleEntry<Float, int[]> knight = null;
    	AbstractMap.SimpleEntry<Float, Integer> settlement = null;
    	AbstractMap.SimpleEntry<Float, Integer> road = null;
    	AbstractMap.SimpleEntry<Float, Integer> city = null;
    	float devCard = -1;
    	
    	if (! ourPlayerData.hasPlayedDevCard()) {
    		if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.DISC)) {
    			System.out.println("search discovery");
    			discovery = searchPlayDiscovery();
    			actionValues.add(discovery.getKey());
    			actionNames.add(PLAY_DISC);
    		}
    		
    		if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.MONO)) {
    			System.out.println("search monopoly");
    			monopoly = searchPlayMonopoly();
    			actionValues.add(monopoly.getKey());
    			actionNames.add(PLAY_MONO);
    		}
    		
    		if ( (ourPlayerData.getNumPieces(SOCPlayingPiece.ROAD) >= 2) 
    				&& ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.ROADS)) {
    			System.out.println("search roads card");
    			roads = searchPlayRoads();
    			actionValues.add(roads.getKey());
    			actionNames.add(PLAY_ROADS);
    		}
    		
    		if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.KNIGHT)) {
    			System.out.println("search play knight");
    			knight = searchPlaceRobberOrPlayKnight(state, true, false);
    			actionValues.add(knight.getKey());
    			actionNames.add(PLAY_KNIGHT);
    		}    		
    	}
    	
    	System.out.println("resources " + ourPlayerData.getResources().toFriendlyString());
    	if (ourPlayerData.getResources().contains(
    			SOCPlayingPiece.getResourcesToBuild(SOCPlayingPiece.SETTLEMENT)) 
    			&& ourPlayerData.hasPotentialSettlement()) {
    		System.out.println("search settlement");
    		settlement = searchPlaceSettlement();
			actionValues.add(settlement.getKey());
			actionNames.add(PLACE_SETTLEMENT);    		
    	}
    	
    	if (ourPlayerData.getResources().contains(
    			SOCPlayingPiece.getResourcesToBuild(SOCPlayingPiece.ROAD))
    			&& ourPlayerData.hasPotentialRoad() ) {
    		System.out.println("search road");
    		road = searchPlaceRoad();
			actionValues.add(road.getKey());
			actionNames.add(PLACE_ROAD);    		
    	}
    	
    	if (ourPlayerData.getResources().contains(
    			SOCPlayingPiece.getResourcesToBuild(SOCPlayingPiece.CITY))
    			&& ourPlayerData.hasPotentialCity()) {
    		System.out.println("search city");
    		city = searchPlaceCity();
			actionValues.add(city.getKey());
			actionNames.add(PLACE_CITY);    		
    	}
    	
    	//SOCPlayingPiece.MAXPLUSONE is used to get cost of the devcard
    	if (ourPlayerData.getResources().contains(
    			SOCPlayingPiece.getResourcesToBuild(SOCPlayingPiece.MAXPLUSONE))) {
    		System.out.println("search development card");
    		devCard = searchBuyDevelopmentCard();
			actionValues.add(devCard);
			actionNames.add(BUY_DEVCARD);    		
    	}
    	
    	AbstractMap.SimpleEntry<Float, int[]> bank = searchTradeBank();
    	if (bank.getKey()>=0) {
    		System.out.println("search trade bank");
    		actionValues.add(bank.getKey());
    		actionNames.add(TRADE_BANK);  
    	}
    	
//    	System.out.println("actionvalues " + actionValues.isEmpty());
    	if (actionValues.isEmpty()) {
    		System.out.println("choose do nothing");
    		return(new AbstractMap.SimpleEntry<Integer, int[]>(END_TURN, null));
    	}
    	
    	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(actionValues);
    	AbstractMap.SimpleEntry<Integer, int[]> result;
    	
    	//if state without any action is better: do nothing
    	if (maxAndIndex.getKey() < states2.get(oldState2)) {
    		System.out.println("choose do nothing");
    		return(new AbstractMap.SimpleEntry<Integer, int[]>(END_TURN, null));
    	}
    	
    	switch (actionNames.get(maxAndIndex.getValue())) {
    	  case PLAY_DISC:
    		System.out.println("choose discovery");
    	    result = new AbstractMap.SimpleEntry<Integer, int[]>(PLAY_DISC, discovery.getValue());
    	    break;

    	  case PLAY_MONO:
    		  System.out.println("choose monopoly");
    		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLAY_MONO, new int[]{monopoly.getValue()});
    		  break;
    		  
    	  case PLAY_ROADS:
    		  System.out.println("choose play roads card");
    		  int[] roadsToBuild = Arrays.stream(roads.getValue()).mapToInt(i -> i).toArray();
    		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLAY_ROADS, roadsToBuild);
    		  break;
    		  
    	  case PLAY_KNIGHT:
    		  System.out.println("choose play knight");
    		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLAY_KNIGHT, new int[]{monopoly.getValue()});
    		  break;
    	
    	  case PLACE_SETTLEMENT:
    		  System.out.println("choose place settlement");
    		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLACE_SETTLEMENT, new int[]{settlement.getValue()});
    		  break;
    	  
    	  case PLACE_CITY:
    		  System.out.println("choose place city");
    		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLACE_CITY, new int[]{city.getValue()});
    		  break;
    		  
    	  case PLACE_ROAD:
    		  System.out.println("choose place road " + ourPlayerData.getPotentialRoads().contains(road.getValue()));
    		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLACE_ROAD, new int[]{road.getValue()});
    		  break;
    		 
    	  case BUY_DEVCARD:
    		  System.out.println("choose buy devcard");
    		  result = new AbstractMap.SimpleEntry<Integer, int[]>(BUY_DEVCARD, null);
    		  break;
    		  
    	  case TRADE_BANK:
    		  System.out.println("choose trade bank");
    		  result = new AbstractMap.SimpleEntry<Integer, int[]>(TRADE_BANK, bank.getValue());
    		  break;

    	  default:
    		  System.out.println("choose do nothing");
    		  result = new AbstractMap.SimpleEntry<Integer, int[]>(END_TURN, null);
    	}
    	
    	return result;
    }
    
    protected void updateStateValue() {
    	state.updateAll(playerTrackers, board);
    	ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();
    	
    	for (SOCPlayer opp : opponents) {
    		int[] oldPlayerState = oldState.get(opp);
    		Double oldPlayerStateValue = states.get(oldPlayerState);
    		
    		int[] newPlayerState = state.getState(opp);
    		Double newPlayerStateValue = states.get(newPlayerState);
    		
    		if (newPlayerStateValue==null) {
    			newPlayerStateValue = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
    			states.put(newPlayerState, Double.valueOf(newPlayerStateValue));
    		}
    		
    		oldPlayerStateValue = oldPlayerStateValue + alpha * (gamma * newPlayerStateValue - oldPlayerStateValue);
    		
    		states.put(oldPlayerState, Double.valueOf(oldPlayerStateValue));
    		oldState.put(opp, newPlayerState);
    		
    		//calculation to get new state array
    		int points = opp.getPublicVP();
    		int roundedPlayerStateValue = Math.round(newPlayerStateValue.floatValue()*10);
    		opp_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(roundedPlayerStateValue)));
    	}
    	
    	int[] newState = new int[6];
    	opp_states.sort(new Comparator<CustomPair>() {
		    public int compare(CustomPair o1, CustomPair o2) {
		        return o2.getKey().compareTo(o1.getKey());
		    }
		});
    	for(int i = 0; i<opp_states.size(); i++) {
    		newState[i*2] = opp_states.get(i).getKey().intValue();
    		newState[i*2 + 1] = opp_states.get(i).getValue().intValue();
    	}
    	Double newStateValue = states2.get(newState);
		if (newStateValue==null) {
//			newStateValue = Double.valueOf(0.5); //or maybe random?
			newStateValue = new Random().nextGaussian()*0.05 + 0.5;
			states2.put(newState, Double.valueOf(newStateValue));
		}
    	
		Double oldStateValue = states2.get(oldState2);
    	
    	oldStateValue = oldStateValue + alpha * (gamma * newStateValue  - oldStateValue);
    	
    	states2.put(oldState2, Double.valueOf(oldStateValue));
    	oldState2 = newState;
    }

    
    private class CustomPair {
        private Integer key;
        private Integer value;
        public CustomPair(Integer k, Integer v){
        	this.key = k;
        	this.value = v;
        }
		public Integer getKey() {
			return key;
		}
		public void setKey(Integer key) {
			this.key = key;
		}
		public Integer getValue() {
			return value;
		}
		public void setValue(Integer value) {
			this.value = value;
		}
    }
    
    private class kLengthCombination {
    	private int[] elements;
    	private ArrayList<int[]> kCombinations;
    	int k;
    	
    	public kLengthCombination(int[] elements, int k) {
    		this.elements = elements;
    		this.k = k;
    		this.kCombinations = new ArrayList<int[]>();
    	}
    	
    	protected void findCombinations(int[] solution, int idx, int length) {
    		
    		if (length== elements.length) {
    			kCombinations.add(solution);
    			return;
    		}
    		
    		for(int i = idx; i < elements.length; i++) {
    			if(solution[i]>= elements[i])
        			continue;
    			int[] newSolution = Arrays.copyOf(solution, solution.length);
    			newSolution[i]++;
    			findCombinations(newSolution, i, length + 1);
    		}
    		
    	}
    	
    	public ArrayList<int[]> getCombinations() {
    		
    		int[] solution = new int[elements.length];
    		findCombinations(solution, 0, 0);  
    		
    		return kCombinations;
    	}
    }
    
}
