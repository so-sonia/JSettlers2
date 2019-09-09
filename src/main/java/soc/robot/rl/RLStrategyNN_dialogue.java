package soc.robot.rl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCResourceConstants;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.robot.rl.RLStrategy.kLengthCombination;

public class RLStrategyNN_dialogue extends RLStrategy {
	
	INDArray oldState;
	
	StateValueFunctionNN stateValueFunction;
	
	SOCState_dialogue stateD;

	public RLStrategyNN_dialogue(SOCGame game, int pn) {
		super(game, pn);
		stateD = new SOCState_dialogue(ourPlayerNumber, players);
		state = stateD;
	    stateD.updateAll(players, board);
	    
	    oldState = Nd4j.create(stateD.getNormalizedStateArray());
	    
	    /*DEBUG*/
//		System.out.println(Arrays.toString(stateD.getNormalizedStateArray()));
//		System.out.println("array length: " + stateD.getNormalizedStateArray().length);
	}
	
	@Override
	protected float getStateValue(SOCState state) {	
		INDArray stateArray = Nd4j.create(stateD.getNormalizedStateArray());	
    	return stateValueFunction.getStates().getStateValue(stateArray);
	}
	
	@Override
	protected void updateStateValue() {
		stateD.updateAll(players, board);
		
		/*DEBUG*/
//		System.out.println(Arrays.toString(stateD.getNormalizedStateArray()));
//		System.out.println("array length: " + stateD.getNormalizedStateArray().length);
		
		INDArray stateArray = Nd4j.create(stateD.getNormalizedStateArray());
	    
	    stateValueFunction.getStates().store(oldState, stateArray, 0.);
		oldState = stateArray;
		currentStateValue = stateValueFunction.getStates().getStateValue(stateArray);
	}
	
	@Override
	public void updateReward() {
	}
	
	public void updateReward(int winner) {
		 stateD.updateAll(players, board);
		 INDArray stateArray = Nd4j.create(stateD.getNormalizedStateArray());
		 double reward = 0.;
		 if (winner == ourPlayerNumber) {
			reward = 1.;
		 } else {
			 reward = ourPlayerData.getTotalVP()*0.5;
		 }
	    
	    stateValueFunction.getStates().store(oldState, stateArray, reward); 
	}
	
	@Override
	public void setStateValueFunction(StateValueFunction svf) {
		this.stateValueFunction = (StateValueFunctionNN) svf;	
	} 
	
	public void updateStateAfterAddingPlayer(){
	}
	
	/**
	 * we need to override all the methods, because when doing updates, we
	 * only do them for our player and not the oppponents
	 */
	@Override
	protected AbstractMap.SimpleEntry<Float, Integer> searchPlaceSettlement() {
//  	/*DEBUG*/
//  	System.out.println("searchPlaceSettlement() was called");
  	
  	int[] potentialSettlements = ourPlayerData.getPotentialSettlements_arr();
  	
//  	/*DEBUG*/
//  	System.out.println("there are: " + potentialSettlements.length + ""
//  			+ " potential settlements to be searched");
  	
  	SOCState tmpState = state.copySOCState();    	
  	ArrayList<Float> state_values = new ArrayList<Float>(potentialSettlements.length);
  	
  	/* resource array in tmpState starts from 0 not from 1 like in 
  	 * SOCResourceConstants. New state after building settlement has 4 fewer
  	 * resources, which were needed to build the settlement */
		int[] resources = new int[] {SOCResourceConstants.SHEEP-1, SOCResourceConstants.WHEAT-1,
				SOCResourceConstants.WOOD-1, SOCResourceConstants.CLAY-1};		
		int[] amounts = new int[] {-1, -1, -1, -1};
		tmpState.updateAddSubstractResources(resources, amounts);
  	
		/* for checking situation after pacing the settlement we use method built into
		 * SOCGame: SOCGame.putTempPiece */
  	for(int posSetCoord : potentialSettlements) {
  		SOCSettlement tmpSet = new SOCSettlement(ourPlayerData, posSetCoord, board);
  		
  		game.putTempPiece(tmpSet);
  		    		
  		tmpState.updatePlaceSettlement(ourPlayerData, board);
	    
	    state_values.add(Float.valueOf(getStateValue(tmpState)));
	    
	    game.undoPutTempPiece(tmpSet);		    
  	}
  	
  	/*select the place to put the settlement with the highest state value */
  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
  	AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
  			maxAndIndex.getKey(), Integer.valueOf(potentialSettlements[maxAndIndex.getValue()]));
  	return result; 
  }
	
	@Override
	protected AbstractMap.SimpleEntry<Float, Integer> searchPlaceRoad() {
//  	/*DEBUG*/
//  	System.out.println("searchPlaceRoad() was called");
  	
  	HashSet<Integer> potentialRoads = new HashSet<Integer>(ourPlayerData.getPotentialRoads());
  	
//  	/*DEBUG*/
//  	System.out.println("there are: " + potentialRoads.size() + ""
//  			+ " potential roads to be searched");
  	
  	SOCState tmpState = state.copySOCState();
  	ArrayList<Float> state_values = new ArrayList<Float>(potentialRoads.size());
  	ArrayList<Integer> roadCoord = new ArrayList<Integer>();
  	
  	/* resource array in tmpState starts from 0 not from 1 like in 
  	 * SOCResourceConstants. New state after building the road has 2 fewer
  	 * resources, which were needed to build the road */
  	int[] resources = new int[] {SOCResourceConstants.WOOD-1, SOCResourceConstants.CLAY-1};		
		int[] amounts = new int[] {-1, -1};
		tmpState.updateAddSubstractResources(resources, amounts);

		/* for checking the situation after placing the road we use the method built into
		 * SOCGame: SOCGame.putTempPiece */
		Iterator<Integer> itRoadCoord = potentialRoads.iterator();
  	while (itRoadCoord.hasNext()) {
  		Integer posRoad = itRoadCoord.next();
  		SOCRoad tmpRoad = new SOCRoad(ourPlayerData, posRoad.intValue(), board);
  		
  		game.putTempPiece(tmpRoad);

  		tmpState.updatePlaceRoad(ourPlayerData);
	    
	    state_values.add(Float.valueOf(getStateValue(tmpState)));
	    roadCoord.add(posRoad);
	    
	    game.undoPutTempPiece(tmpRoad);		    
  	}
  	
  	/*select the place to put the road with the highest state value */
  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
  	Integer roadToBuild = roadCoord.get(maxAndIndex.getValue());
  	AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
  			maxAndIndex.getKey(), roadToBuild);
  	return result;  
  }
	
	@Override
	protected AbstractMap.SimpleEntry<Float, Integer> searchPlaceCity() {
//  	/*DEBUG*/
//  	System.out.println(game.getName() + " searchPlaceCity() was called");
 
  	Vector<SOCSettlement> potentialCities = new 
  			Vector<SOCSettlement>(ourPlayerData.getSettlements());
  	
//  	/*DEBUG*/
//  	System.out.println(game.getName() + " there are: " + potentialCities.size() + ""
//  			+ " potential cities to be searched");
  	
  	SOCState tmpState = state.copySOCState();
  	ArrayList<Float> state_values = new ArrayList<Float>(potentialCities.size());
  	ArrayList<Integer> cityCoord = new ArrayList<Integer>(potentialCities.size());
  	
  	/* resource array in tmpState starts from 0 not from 1 like in 
  	 * SOCResourceConstants. New state after building the city has 5 fewer
  	 * resources, which were needed to build the city */
  	int[] resources = new int[] {SOCResourceConstants.ORE-1, SOCResourceConstants.WHEAT-1};		
		int[] amounts = new int[] {-3, -2};
		tmpState.updateAddSubstractResources(resources, amounts);
  	
		/* for checking situation after pacing the city we use method built into
		 * SOCGame: SOCGame.putTempPiece */
		Iterator<SOCSettlement> itCityCoord = potentialCities.iterator();
  	while (itCityCoord.hasNext()) {
  		SOCSettlement posCity = itCityCoord.next();
  		SOCCity tmpCity = new SOCCity(ourPlayerData, posCity.getCoordinates(), board);
  		
  		game.putTempPiece(tmpCity);
  		    		
  		tmpState.updatePlaceCity(ourPlayerData, board);
	    
	    state_values.add(Float.valueOf(getStateValue(tmpState)));
	    cityCoord.add(Integer.valueOf(posCity.getCoordinates()));
	    
//		    /*DEBUG*/
//		    System.out.println(game.getName() + " items in state_values: " + state_values.size() + ". items in cityCoord " + cityCoord.size());
	    
	    game.undoPutTempPiece(tmpCity);	    
  	}
  	
  	/*select the place to build the city with the highest state value */
  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
  	
//  	/*DEBUG*/
//	    System.out.println(game.getName() + " maxAndIndex key " + maxAndIndex.getKey() + " maxAndIndex value " + 
//	    		maxAndIndex.getValue() + " cityCoord size " + cityCoord.size());
	   
  	Integer cityToBuild = cityCoord.get(maxAndIndex.getValue());
  	AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
  			maxAndIndex.getKey(), cityToBuild);
  	return result;  
  }
	
	/** we need to override this method, because in RLStrategyNN_dialogue we don't
	 * have information about probabilities of resources and we don't track information
	 * abour the largest army and number of played knights card
	 */
	 protected AbstractMap.SimpleEntry<Float, int[]> 
	  searchPlaceRobberOrPlayKnight(SOCState currentState, boolean playKnight) {
	  	
//	  	/*DEBUG*/
//	  	System.out.println("searchPlaceRobberOrPlayKnight() was called");
	  	
	  	int[] landHexes = board.getLandHexCoords();
//	  	SOCState tmpState = state.copySOCState();
	  	ArrayList<Float> state_values = new ArrayList<Float>();
	  	ArrayList<Integer> robPositions = new ArrayList<Integer>();
	  	ArrayList<Integer> playersToRob = new ArrayList<Integer>();
	  	ArrayList<Float> state_values_no_victims = new ArrayList<Float>();
	  	ArrayList<Integer> robPositions_no_victims = new ArrayList<Integer>();
	  	ArrayList<Integer> playersToRob_no_victims = new ArrayList<Integer>();
	  	ArrayList<Float> state_values_no_stealing = new ArrayList<Float>();
	  	ArrayList<Integer> robPositions_no_stealing = new ArrayList<Integer>();
	  	ArrayList<Integer> playersToRob_no_stealing = new ArrayList<Integer>();
	  	int[] prevResources =  ((SOCState_dialogue) currentState).getResources();
	  	    	
	  	/*go through all land hexes except the desert and the current robber position
	  	 * (cannot put robber back there)
	  	 */
	  	for(int posRob : landHexes) {
	  		
	  		if (board.getHexTypeFromCoord(posRob) != SOCBoard.DESERT_HEX &&
	  				posRob != board.getRobberHex()) {
	  			
	  			/*get all nodes adjacent to the hex and check if there's a building */
	  			List<SOCPlayer> candidates = game.getPlayersOnHex(posRob);
	  			
	  			/*we add hex, even if there's no player affected, because sometimes may 
	  			 * happen that on every hex with other player we have our own building
	  			 * therefore it's better to place the robber on the hex with no players
	  			 * then to block ourselves
	  			 */
	  			if (candidates.size()==0) {
	  				SOCState tmpState = currentState.copySOCState();
	  				
	  				tmpState.updatePlaceRobberAll(players, board, posRob);
	  				
	  				state_values_no_victims.add(Float.valueOf(getStateValue(tmpState)));
					robPositions_no_victims.add(Integer.valueOf(posRob));
					playersToRob_no_victims.add(Integer.valueOf(-1));
					
	  			} else {
	  				
	  				boolean our = false;
	  	  			boolean[] hasRes = new boolean[candidates.size()];
	  	  			boolean anyRes = false;
	  				String candres = "";
	  	  			
	  				for (int i=0; i< candidates.size(); i++) {
	  					hasRes[i] = candidates.get(i).getResources().getTotal() > 0;
	  					candres += " player: " + candidates.get(i).getPlayerNumber() + " res: " +
	  							Arrays.toString(candidates.get(i).getResources().getAmounts(true));
	  					if (hasRes[i]) {
	  						anyRes = true;
	  					}
	  					if (candidates.get(i).getPlayerNumber()==ourPlayerNumber) {
	  						our = true;
	  						break;
	  					}
	  				}
	  					  				
//	  				/*DEBUGA*/
//	  				System.out.println("Player: " + ourPlayerNumber + " hex: " + posRob + 
//	  							" victims size " + candidates.size() + " anyRes: " 
//	  							+ anyRes + " our: " + our + candres);
	  				
	  				if (our)
	  					continue;
	  				
	  				if (candidates.size()==1 && !hasRes[0]) {
	  					SOCState tmpState = currentState.copySOCState();
	  	  				
	  	  				tmpState.updatePlaceRobberAll(players, board, posRob);
	  	  				
	  	  				state_values_no_stealing.add(Float.valueOf(getStateValue(tmpState)));
						robPositions_no_stealing.add(Integer.valueOf(posRob));
						playersToRob_no_stealing.add(Integer.valueOf(-1));
	  				
	  				} else if (anyRes) {
	  					
	  					for (int i=0; i< candidates.size(); i++) {
	  						
	  						if (!hasRes[i])
	  							continue;
	  						
	  						SOCPlayer player = candidates.get(i);
	  						
	  						ArrayList<Float> state_values_res = new ArrayList<Float>();
	  	      				SOCState tmpState = currentState.copySOCState();
	  	      				int[] plResources = player.getResources().getAmounts(true);
	  	      				
	  	      				tmpState.updatePlaceRobberAll(players, board, posRob);
	  	      				
	  	      				/* resource array in tmpState starts from 0 not from 1 like in 
	  	      		    	 * SOCResourceConstants. We don't know what resource we will steal from the other player.
	  	      		    	 * Therefore for every resource we check if that player possesses this resource
	  	      		    	 * or if he has any unknown resources and non zero probability of getting this resource.
	  	      		    	 * If that's the case we calculate state value we would have after stealing this
	  	      		    	 * resource. After checking all resources we calculate average of these state values,
	  	      		    	 * which give us a state value of robbing this particular player. 
	  	      		    	 */
	  	      				for (int resource = SOCResourceConstants.CLAY -1;
	  	      						resource <= SOCResourceConstants.WOOD -1; resource++) {
	  	      					
	  	      					/* if search is done after 7 was rolled first we were discarding,
	  	      					 * so instead of updating resources to true values, we have update them 
	  	      					 * to values after discard (case when this function is called
	  	      					 * from searchRollDice())*/
	  	      					if (!playKnight) {
	  	      						tmpState.updateSetResources(ourPlayerData, prevResources);
	  	      					} else {
	  	      						tmpState.updateResources(ourPlayerData, false);
	  	      					}
	  	      					
	  	      					if (plResources[resource]>0 || plResources[SOCResourceConstants.UNKNOWN-1] > 0 ) {
	  	      						
	  	      						tmpState.updateSteal(player, resource, false);
	  	      						
	  	      						//TO TEST: maybe calculate weighted mean
	  	      						state_values_res.add(Float.valueOf(getStateValue(tmpState)));    						
	  	      					} 
	  	      				}
	  	      				
	  	      				/*we get a state value of placing robber on this particular hex and robbing the
	  	      				 * particular player
	  	      				 */
	  	      				if (state_values_res.size()>0 && !our) {
	  	      					double average = state_values_res.stream().mapToDouble(a -> a).average().orElse(0.);
	  	      					state_values.add(Float.valueOf((float) average));
	  	      					robPositions.add(Integer.valueOf(posRob));
	  	      					playersToRob.add(Integer.valueOf(player.getPlayerNumber()));
	  	      				}
	  					}
	  				}	
	  			}
	  		}
	  	}
	  	
	  	
		if (state_values.size() == 0) {
	  		if (state_values_no_stealing.size() !=0) {
	  			state_values = state_values_no_stealing;
	  			robPositions = robPositions_no_stealing;
	  			playersToRob = playersToRob_no_stealing;
	  		} else if (state_values_no_victims.size() !=0 ){
	  			state_values = state_values_no_victims;
	  			robPositions = robPositions_no_victims;
	  			playersToRob = playersToRob_no_victims;
	  		} else {
	  			/* added hard coded nonsense values, 
	  			 * because 1 in 50 thousand games throws mistake
	  			 */
	  			state_values.add(Float.valueOf(0));
	  			robPositions.add(landHexes[0]);
	  			playersToRob.add(0);
	  		}
	  	}
	  	
	  	/*we chose the hex, where we should place the robber and the player to rob
	  	 * with highest expected state value
	  	 */
	  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
	  	int[] placeAndPlayerToRob = new int[2];
	  	
//	  	/*DEBUG*/
//		    System.out.println(game.getName() + " maxAndIndex key " + maxAndIndex.getKey() + " maxAndIndex value " + 
//		    		maxAndIndex.getValue() + " robPositions size " + robPositions.size() + 
//		    		" playersToRob size " + playersToRob.size());
//		    System.out.println("player to rob " + Arrays.toString(playersToRob.toArray()));
		    
	  	placeAndPlayerToRob[0] = robPositions.get(maxAndIndex.getValue());
	  	placeAndPlayerToRob[1] = playersToRob.get(maxAndIndex.getValue());
	  	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
	  			maxAndIndex.getKey(), placeAndPlayerToRob);
	  	return(result); 
	  }
	  
	  /**we don't track bought development card, so we just remove resources
	   * @see soc.robot.rl.RLStrategy#searchBuyDevelopmentCard()
	   */
	  protected float searchBuyDevelopmentCard(){
		  	
//	  	/*DEBUG*/
//	  	System.out.println("searchBuyDevelopmentCard() was called");
	  
	  	
		SOCState tmpState = state.copySOCState();
		
		/* resource array in tmpState starts from 0 not from 1 like in 
  		* SOCResourceConstants. New state after buying a development card has 3 fewer
  		* resources, which were needed to buy the card */
		int[] resources = new int[] {SOCResourceConstants.SHEEP-1, SOCResourceConstants.WHEAT-1,
				SOCResourceConstants.ORE-1};		
		int[] amounts = new int[] {-1, -1, -1};
		tmpState.updateAddSubstractResources(resources, amounts);
		
		return getStateValue(tmpState);
		
	  }
	  
	  /**
	   * we don't update opponents resources
	   */
	  protected AbstractMap.SimpleEntry<Float, Integer> searchPlayMonopoly() {
//	  	/*DEBUG*/updateResources
//	  	System.out.println("searchPlayMonopoly() was called");
	  	
	  	SOCState tmpState = state.copySOCState();
	  	
	  	ArrayList<Float> state_values = new ArrayList<Float>(5);
	  	
	  	/*for each resource we steal the KNOWN cards of the opponents*/
	  	for (int res = 0; res < 5 ; res++) {
	  		tmpState.updateResources(ourPlayerData, false);
	  		
	  		for (SOCPlayer op : opponents) {
	  			tmpState.updateSteal(op, res, true);
	  		}
	  		
	  		state_values.add(Float.valueOf(getStateValue(tmpState)));    		
	  	}
	  	
	  	return(getMaxAndIndex(state_values));
	  }
	  
	  /**
	   * we use {@link SOCState_dialogue#updateEdges(SOCBoard)} instead of
	   * {@link SOCState_dialogue#updatePlaceRoad(SOCPlayer)}
	   */
	  protected AbstractMap.SimpleEntry<Float, Integer[]> searchPlayRoads() {
		  	
//	  	/*DEBUG*/
//	  	System.out.println("searchPlayRoads() was called");
	  	
	  	HashSet<Integer> potentialRoads = (HashSet<Integer>) ourPlayerData.getPotentialRoads().clone();
	  	
//	  	/*DEBUG*/
//	  	System.out.println("In the first run we can check " + potentialRoads.size() +
//	  			" of potential roads");
	  	
	  	SOCState_dialogue tmpState = (SOCState_dialogue) stateD.copySOCState();
	  	ArrayList<Float> state_values = new ArrayList<Float>();
	  	ArrayList<Integer[]> twoRoadsCoord = new ArrayList<Integer[]>();
	  	
	  	tmpState.updatePlayedDevCard(SOCDevCardConstants.ROADS);
	  	
	  	/* for checking the situation after placing the road we use the method built into
			 * SOCGame: SOCGame.putTempPiece */
	  	for(Integer posSetCoord : potentialRoads) {
//	  		/*DEBUG*/
//	  		System.out.println("road1 number " + posSetCoord);
	  		
	  		SOCRoad tmpRoad = new SOCRoad(ourPlayerData, posSetCoord, board);
	  		
	  		game.putTempPiece(tmpRoad);
	  		
	  		Iterator<SOCPlayer> playersIter = players.values().iterator();
	  		SOCPlayer ourPn = game.getPlayer(ourPlayerNumber);

	  		tmpState.updateEdges(board);
		    
		    /*potential roads after placing the first road*/
		    HashSet<Integer> potentialSecondRoads = (HashSet<Integer>) ourPn.getPotentialRoads().clone();
		    
//			    /*DEBUG*/
//		    	System.out.println("In the second run we can check " +  potentialSecondRoads.size() +
//		    			" of potential roads");

		    	for(Integer posSecondSetCoord : potentialSecondRoads) {
//		    		/*DEBUG*/
//		    		System.out.println("road2 number " + posSecondSetCoord);
		    		
		    		SOCRoad tmpSecondRoad = new SOCRoad(ourPn, posSecondSetCoord, board);
		    		
		    		game.putTempPiece(tmpSecondRoad);
		      		
		      		Iterator<SOCPlayer> playersSecondIter = players.values().iterator();

		      		tmpState.updateEdges(board);
				    
				    state_values.add(Float.valueOf(getStateValue(tmpState)));
				    twoRoadsCoord.add(new Integer[]{posSetCoord, posSecondSetCoord});
				    
				    game.undoPutTempPiece(tmpSecondRoad);
		    	}
			    		    
		    	game.undoPutTempPiece(tmpRoad);	
		    			    	
	  	}
	  	
	  	/*we could place only one road*/
	  	if (twoRoadsCoord.size()<1) {
	  		return null;
	  	}
	  	
	  	/* two roads, that give the highest expected state value, are chosen
	  	 */
	  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
	  	Integer[] roadsToBuild = twoRoadsCoord.get(maxAndIndex.getValue());
	  	AbstractMap.SimpleEntry<Float, Integer[]> result = new AbstractMap.SimpleEntry<Float, Integer[]>(
	  			maxAndIndex.getKey(), roadsToBuild);
	  	return result;  
	  }
	  
	 
	 

}
