package soc.robot.rl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import fasttester.BotServer;
import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayerNumbers;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.robot.rl.RLStrategy.kLengthCombination;

public class RLStrategyRandom extends RLStrategy {
	
	public RLStrategyRandom(SOCGame game, int pn) {
		super(game, pn);
//		state = new SOCState(ourPlayerNumber, players);
//        state.updateAll(players, board);   
	}

	@Override
	protected float getStateValue(SOCState state) {
		Double stateVal = rnd.nextGaussian()*0.05 + 0.5;
		return stateVal.floatValue();
	}

	@Override
	protected void updateStateValue() {
		currentStateValue = (float)(rnd.nextGaussian()*0.05 + 0.5);
	}

//	@Override
//	protected void writeMemory() {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	protected void readMemory() {
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	protected void synchroniseMemory() {
//		// TODO Auto-generated method stub
//
//	}
//	
	@Override
	public void updateReward() {
		// TODO Auto-generated method stub

	}
	
	protected void writeStats() {
		SOCPlayer[] players = game.getPlayers();
		
		BufferedWriter writer = null;
        try {
        	Path path = Paths.get("log", "RL_RND_stats_");
            writer = new BufferedWriter(new FileWriter(path.toFile()));
            
            for (SOCPlayer pn : players) {
            	writer.write(game.getName() + ", " 
            			+ pn.getPlayerNumber() + "," 
            			+ pn.getName() + ","
//            			+ pn.
            			);
            	
            	pn.getTotalVP();
            }
            

            writer.write("Hello world!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            }
        }
		
//		game.getPlayer(player number), game.getName() for game name (or make ID for it),
//		 * 		player.getName() for player name, 
//		 * 		SOCRobotBrain.getRobotParameters().getStrategyType() (0 = SMART_STRATEGY, 1 = FAST_STRATEGY)
//		 * 		in each robot client we will have: gamesPlayed, gamesFinished = 0, gamesWon
//		 * 		from robot client we can get brain by SOCRobotBrain brain = robotBrains.get(mes.getGame());
		
	}
	
	/* modifying search function to just pick a random choice, without checking 
	 * how the next state would look like
	 * @see soc.robot.rl.RLStrategy#searchPlaceSettlement()
	 */
	protected AbstractMap.SimpleEntry<Float, Integer> searchPlaceSettlement() {
//  	/*DEBUG*/
//  	System.out.println("searchPlaceSettlement() was called");
  	
  	int[] potentialSettlements = ourPlayerData.getPotentialSettlements_arr();
  	
//  	/*DEBUG*/
//  	System.out.println("there are: " + potentialSettlements.length + ""
//  			+ " potential settlements to be searched");
  	
  	int randomChoice = rnd.nextInt(potentialSettlements.length);
  	
  	/*select the place to put the settlement with the random state value */
  	AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
  			getStateValue(state), Integer.valueOf(potentialSettlements[randomChoice]));
  	return result; 
  }
	
	
	protected AbstractMap.SimpleEntry<Float, Integer> searchPlaceRoad() {
//  	/*DEBUG*/
//  	System.out.println("searchPlaceRoad() was called");
  	
  	HashSet<Integer> potentialRoads = new HashSet<Integer>(ourPlayerData.getPotentialRoads());
  	
//  	/*DEBUG*/
//  	System.out.println("there are: " + potentialRoads.size() + ""
//  			+ " potential roads to be searched");
  	
  	int randomChoice = rnd.nextInt(potentialRoads.size());

  	/*we need to iterate through the hashset to get the random road*/
	Iterator<Integer> itRoadCoord = potentialRoads.iterator();

	for (int i=0; i<randomChoice; i++) {
		if (itRoadCoord.hasNext())
			itRoadCoord.next();
		else 
			break;
	}
	
	int roadCoord = itRoadCoord.next();
	 	
  	/*select the place to put the road with the random state value */
  	AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
  			getStateValue(state), Integer.valueOf(roadCoord));
  	return result;  
  }
	
	
	protected AbstractMap.SimpleEntry<Float, Integer> searchPlaceCity() {
//  	/*DEBUG*/
//  	System.out.println(game.getName() + " searchPlaceCity() was called");
 
  	Vector<SOCSettlement> potentialCities = new 
  			Vector<SOCSettlement>(ourPlayerData.getSettlements());
  	
//  	/*DEBUG*/
//  	System.out.println(game.getName() + " there are: " + potentialCities.size() + ""
//  			+ " potential cities to be searched");
  	
  	int randomChoice = rnd.nextInt(potentialCities.size());
	   
  	Integer cityToBuild = Integer.valueOf(potentialCities.get(randomChoice).getCoordinates());
  	AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
  			getStateValue(state), cityToBuild);
  	return result;  
  }
	
	protected AbstractMap.SimpleEntry<Float, int[]> 
	  searchPlaceRobberOrPlayKnight(SOCState currentState, boolean playKnight) {
	  	
//	  	/*DEBUG*/
//	  	System.out.println("searchPlaceRobberOrPlayKnight() was called");
	  	
	  	int[] landHexes = board.getLandHexCoords();
	  	/*we exclude desert and the current rober position*/
	  	int excludedHex = 2;
	  	if (SOCBoard.DESERT_HEX==board.getRobberHex())
	  		excludedHex = 1;
	  	int randomChoice = rnd.nextInt(landHexes.length-excludedHex);
	  	
	  	/*this way we omit desert hex and current robber position*/
	  	for (int i=0; i<=randomChoice; i++) {
			if (landHexes[i] == SOCBoard.DESERT_HEX || landHexes[i] == board.getRobberHex())
				randomChoice++;
		}
	  	
	  	int robPosition = landHexes[randomChoice];
	  	int playerToRob;
	  	
	  	List<SOCPlayer> candidates = game.getPlayersOnHex(robPosition);
	  	List<SOCPlayer> victims = new ArrayList<SOCPlayer>();
	  	
	  	/*first check if there are any valid candidates on the hex to rob from*/
        for (SOCPlayer pl : candidates)
        {
            final int pn = pl.getPlayerNumber();

            if ((pn != ourPlayerNumber) && ( pl.getResources().getTotal() > 0) )
            {
                victims.add(pl);
            }
        }
	  	
	  	if (victims.size()==0) {
	  		playerToRob = -1;
	  	} else {
	  		int randomPlayer = rnd.nextInt(victims.size());
	  		playerToRob = victims.get(randomPlayer).getPlayerNumber();
	  	}
	  	
	  	
	  	int[] placeAndPlayerToRob = new int[2];
		    
	  	placeAndPlayerToRob[0] = robPosition;
	  	placeAndPlayerToRob[1] = playerToRob;
	  	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
	  			getStateValue(state), placeAndPlayerToRob);
	  	return(result);  	
	  }
	  
	  
	  protected float searchBuyDevelopmentCard(){
		  	
//	  	/*DEBUG*/
//	  	System.out.println("searchBuyDevelopmentCard() was called");
		  
		  return getStateValue(state);
	  }
	  
	  protected AbstractMap.SimpleEntry<Float, int[]> searchPlayDiscovery() {
		  	
//	  	/*DEBUG*/
//	  	System.out.println("searchPlayDiscovery() was called");
	  	
	    int[] resourcesToPick = new int[]{rnd.nextInt(5), rnd.nextInt(5)};
	
	  	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
	  			getStateValue(state), resourcesToPick);
	  	return result;  	
	  }
	  
	  
	  protected AbstractMap.SimpleEntry<Float, Integer> searchPlayMonopoly() {
//	  	/*DEBUG*/
//	  	System.out.println("searchPlayMonopoly() was called");
	  	
		AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
				getStateValue(state), rnd.nextInt(5));

	  	return result;
	  }
	  
	  
	  protected AbstractMap.SimpleEntry<Float, Integer[]> searchPlayRoads() {
		  	
//	  	/*DEBUG*/
//	  	System.out.println("searchPlayRoads() was called");
	  	
	  	HashSet<Integer> potentialRoads = (HashSet<Integer>) ourPlayerData.getPotentialRoads().clone();
	  	Integer[] roadsToBuild = new Integer[2];
	  	int firstRoad = rnd.nextInt(potentialRoads.size());
	  	
	  	
//	  	/*DEBUG*/
//	  	System.out.println("In the first run we can check " + potentialRoads.size() +
//	  			" of potential roads");
	  	
	  	SOCState tmpState = state.copySOCState();
	  	
	  	/*we need to iterate through the hashset to get the random road*/
		Iterator<Integer> itRoadCoord = potentialRoads.iterator();

		for (int i=0; i<firstRoad; i++) {
			if (itRoadCoord.hasNext())
				itRoadCoord.next();
			else 
				break;
		}
	  	
		roadsToBuild[0] = itRoadCoord.next();
		SOCRoad tmpRoad = new SOCRoad(ourPlayerData, roadsToBuild[0], board);
  		
  		game.putTempPiece(tmpRoad);
  		
  		SOCPlayer ourPn = game.getPlayer(ourPlayerNumber);
	  	
	  	ArrayList<Float> state_values = new ArrayList<Float>();
	  	ArrayList<Integer[]> twoRoadsCoord = new ArrayList<Integer[]>();
	  	
	  	HashSet<Integer> potentialSecondRoads = (HashSet<Integer>) ourPn.getPotentialRoads().clone();
	  	
	  	/*we could place only one road*/
	  	if ( potentialSecondRoads.size()<1) {
	  		return null;
	  	}
	  	
	  	int secondRoad = rnd.nextInt(potentialSecondRoads.size());
	  	
	  	itRoadCoord = potentialSecondRoads.iterator();
	  	
	  	for (int i=0; i<secondRoad; i++) {
			if (itRoadCoord.hasNext())
				itRoadCoord.next();
			else 
				break;
		}
	  	
	  	roadsToBuild[1] = itRoadCoord.next();
	  	
	  	game.undoPutTempPiece(tmpRoad);	
	  	
	  	
	  	/* two random roads are chosen
	  	 */

	  	AbstractMap.SimpleEntry<Float, Integer[]> result = new AbstractMap.SimpleEntry<Float, Integer[]>(
	  			getStateValue(state), roadsToBuild);
	  	return result;  
	  }
	
	  
	  protected AbstractMap.SimpleEntry<Float, int[]> searchTradeBank() {
		  	
//	  	/*DEBUG*/
//	  	System.out.println("searchTradeBank() was called");
	  	
	  	SOCState tmpState = state.copySOCState();
	  	ArrayList<int[]> tradeOffer = new ArrayList<int[]>();
	  	
	  	boolean[] ports = ourPlayerData.getPortFlags();
	  	int[] plRes = ourPlayerData.getResources().getAmounts(false);
	  	int[] plResources = Arrays.copyOf(plRes, plRes.length);
	  	
	  	int howMuchToGive = 4;
	  	//if we have MISC port we have to give only 3 resources
	  	if (ports[SOCBoard.MISC_PORT]) {
	  		howMuchToGive = 3;
	  	}
	  	
	  	/*
	  	 * For every resource if the player has 4 resources or 3 and MISC port or 2 and 
	  	 * resource port -> we check the state value after trading this resource for every
	  	 * other resource that we can buy. 
	  	 */
	  	for (int j = 0; j < 5; j++) {

	  		int res = plResources[j];
	  		
//	  		/*DEBUG*/
//	      	System.out.println("resource " + Arrays.toString(plResources) + 
//	      			" and j = " + j + " res = " + res);
	  			  		
	  		if (res>=4 || (res>=3 && ports[SOCBoard.MISC_PORT])|| (res>=2 && ports[j+1]) ) {
	  			
//	  			/*DEBUG*/
//	  	    	System.out.println("We can trade " + SOCResourceConstants.resName(j+1) +
//	  	    			" because we have " + res + " cards of this type");
	  			
	  			int quantity = howMuchToGive;
	  			if (ports[j+1]) {
	  				quantity = 2;
	  			}
	  			
	  			int takenResource = rnd.nextInt(5);
	  			//if we randomly selected the same resource we want to give away, we take the next resource.
	  			if (j==takenResource)
	  				takenResource = (takenResource+1)%5;
	  			
	  			tradeOffer.add(new int[] {j, takenResource, quantity});
	  			
	  		}
	  	}
	  	
	  	//if we don't have any possibility to trade
	  	if (tradeOffer.isEmpty()) {
	  		return(new AbstractMap.SimpleEntry<Float, int[]>(Float.valueOf(-1.0f), null));
	  	}

	  	/*random offer is chosen*/
	  	int[] trade = tradeOffer.get( rnd.nextInt( tradeOffer.size() ) );
	  	
	  	/*DEBUG*/
//	  	System.out.println("We decide to trade " + trade[2] + " " + 
//	  			SOCResourceConstants.resName(trade[0]+1) +
//	  			" for " + SOCResourceConstants.resName(trade[1]+1)
//	  			+ " trade array: " + Arrays.toString(trade));
	  	
	  	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
	  			getStateValue(state), trade);
	  	return result;  
	  }
	  
	  
	  protected AbstractMap.SimpleEntry<Float, int[]> searchDiscardAfterSevenRolled(int numDiscards) {
		  	
//	  	/*DEBUG*/
//	  	System.out.println("searchDiscardAfterSevenRolled() was called");
		
		SOCResourceSet discardsn = new SOCResourceSet();
        SOCGame.discardOrGainPickRandom(ourPlayerData.getResources(),numDiscards, 
        		true, discardsn, rnd);
        int[] resourceToDiscard = discardsn.getAmounts(false);
		  
	  	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
	  			getStateValue(state), resourceToDiscard);
	  	
	  	return result;  	
	  }
	  
	  protected float searchRollDice() {
		  	
//	  	/*DEBUG*/
//	  	System.out.println("searchRollDice() was called");
	  	
	  	return getStateValue(state);
	  }   

}
