package soc.robot.rl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;


import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayerNumbers;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCSettlement;
import soc.game.SOCRoad;

public abstract class RLStrategy {
	
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
  protected HashMap<Integer,SOCPlayer> players;
  protected final SOCPlayer ourPlayerData;
  protected SOCBoard board;
  
  /** state variable */
  protected SOCState state;
  
  /**vector of SOCPlayers for all opponents */
  protected Vector<SOCPlayer> opponents;
  
  protected Double currentStateValue;
  
  /** macroparameters for reinforcement learning algorithm */
  protected double alpha;
  protected double gamma;

  /**
   * {@link #ourPlayerData}'s player number.
   * @since 2.0.00
   */
  protected final int ourPlayerNumber;
  
  /**
   * Create an RLStrategy for a {@link RLClient}'s player.
   * @param game
   * @param pn
   */
  public RLStrategy(SOCGame game, int pn)
  {
	  this.game = game;
	  players = new HashMap<Integer,SOCPlayer>();
	  
	  for (int i =0; i<game.maxPlayers;i++) {
		  players.put(new Integer(i), game.getPlayer(i));
	  }
	  
	  ourPlayerNumber = pn;
	  ourPlayerData = game.getPlayer(pn);
      board = game.getBoard();
      alpha = 0.6;
      gamma = 1.0;
      
//      System.out.println("alpha " + alpha + " gamma " + gamma); 
        
      state = new SOCState(ourPlayerNumber, players);
      state.updateAll(players, board);   
      
      opponents = new Vector<SOCPlayer>();
      Iterator<SOCPlayer> playersIter = players.values().iterator();
      
      while (playersIter.hasNext())
	  {
		  SOCPlayer player = playersIter.next();
		  if (player.getPlayerNumber() != ourPlayerNumber) {
			  opponents.add(player);
		  }
	  }
      
      currentStateValue = new Double(0.0);
  }
  
  public void updateStateAfterAddingPlayer(){
  	  state = new SOCState(ourPlayerNumber, players);
      state.updateAll(players, board); 
      /*should update players???*/
      
      opponents = new Vector<SOCPlayer>();
      Iterator<SOCPlayer> playersIter = players.values().iterator();
      
      while (playersIter.hasNext())
	  {
		  SOCPlayer player = playersIter.next();
		  if (player.getPlayerNumber() != ourPlayerNumber) {
			  opponents.add(player);
		  }
	  }
  }
  
  /**
   * Checks the value of the new state after placing the settlement by the player. 
   * All places, where it is possible to set a settlement (for a given player) 
   * are checked.
   * <ul>
   * <li>
   * Removes resources needed to build a settlement
   * <li>
   * Updates state statistics after placing the settlement using 
   * {@link SOCGame#putTempPiece(tmpSet)}
   * </ul>
   * 
   * Function does not check, if player has enough resources to place the settlement. 
   * (This should be checked in {@link #buildOrTradeOrPlayCard() })
   */
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
  		    		
  		Iterator<SOCPlayer> playersIter = players.values().iterator();

		    while (playersIter.hasNext())
		    {
		    	tmpState.updatePlaceSettlement(playersIter.next(), board);
		    }
		    
		    state_values.add(Float.valueOf(getStateValue(tmpState)));
		    
		    game.undoPutTempPiece(tmpSet);		    
  	}
  	
  	/*select the place to put the settlement with the highest state value */
  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
  	AbstractMap.SimpleEntry<Float, Integer> result = new AbstractMap.SimpleEntry<Float, Integer>(
  			maxAndIndex.getKey(), Integer.valueOf(potentialSettlements[maxAndIndex.getValue()]));
  	return result; 
  }
  
  /**
   * Checks the value of the new state after placing the road by the player. 
   * All places, where it is possible to set a road (for a given player) 
   * are checked.
   * <ul>
   * <li>
   * Removes resources needed to build a road
   * <li>
   * Updates state statistics after placing the road using 
   * {@link SOCGame.putTempPiece(tmpRoad)}
   * </ul>
   * 
   * 
   * Function does not check, if player has enough resources to place the road.
   * This should be checked in {@link #buildOrTradeOrPlayCard() }
   */
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

  		Iterator<SOCPlayer> playersIter = players.values().iterator();

	    while (playersIter.hasNext())
	    {   
	    	tmpState.updatePlaceRoad(playersIter.next());
	    }
	    
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
  
  /**
   * Checks the value of the new state after placing the city by the player. 
   * All places, where it is possible to set a city (for a given player) 
   * are checked (that is all places where currently player has a settlement.
   * 
   * <ul>
   * <li>
   * Removes resources needed to build a city
   * <li>
   * Updates state statistics after placing the city using 
   * {@link SOCGame#putTempPiece(tmpCity)}
   * </ul>
   * 
   * Function does not check, if player has enough resources to place the road. 
   * This should be checked in {@link #buildOrTradeOrPlayCard() }
   */
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
  		    		
  		Iterator<SOCPlayer> playersIter = players.values().iterator();

	    while (playersIter.hasNext())
	    {
	    	tmpState.updatePlaceCity(playersIter.next(), board);
	    }
	    
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
  
  /**
   * Checks the value of the new state after placing robber or playing the knight
   * (which has the same effect as throwing 7). Param {@link searchAfterSeven} is 
   * used to distinguish between these two situations. All possible placements 
   * for robber are checked and if two or more opponents have buildings adjusted 
   * the hex, who it is better to rob.
   * <P>
   * Hexes adjacent to the buildings of our player are not taken into account.
   * <P>
   * If the knight card is played, we also check if the player will get the largest
   * army points.
   * <P>
   * Function does not check, if player has a knight card to play. 
   * This should be checked in {@link #buildOrTradeOrPlayCard() } or
   * in {@link #rollOrPlayCard() }
   * <P>
   * Adding state, because it will be used in {@link #searchRollDice()}
   * 
   * @param state
   * @param playKnight - function was called because a knight card was played
   * @param searchAfterSeven
   * @return map, first element is the state value of the best action, second element
   * is array, where first element is the position, where we should place the robber
   * and second element is the player, who we should rob.
   */
  protected AbstractMap.SimpleEntry<Float, int[]> 
  searchPlaceRobberOrPlayKnight(SOCState currentState, boolean playKnight) {
  	
//  	/*DEBUG*/
//  	System.out.println("searchPlaceRobberOrPlayKnight() was called");
  	
  	int[] landHexes = board.getLandHexCoords();
//  	SOCState tmpState = state.copySOCState();
  	ArrayList<Float> state_values = new ArrayList<Float>();
  	ArrayList<Integer> robPositions = new ArrayList<Integer>();
  	ArrayList<Integer> playersToRob = new ArrayList<Integer>();
  	ArrayList<Float> state_values_no_victims = new ArrayList<Float>();
  	ArrayList<Integer> robPositions_no_victims = new ArrayList<Integer>();
  	ArrayList<Integer> playersToRob_no_victims = new ArrayList<Integer>();
  	ArrayList<Float> state_values_no_stealing = new ArrayList<Float>();
  	ArrayList<Integer> robPositions_no_stealing = new ArrayList<Integer>();
  	ArrayList<Integer> playersToRob_no_stealing = new ArrayList<Integer>();
  	int[] prevResources =  currentState.getPlayerState(ourPlayerData).getResources();

  	
  	/*checking if the player will get largest army (in case he's playing
  	 * the knight card
  	 */
  	boolean willGetLA = false;
  	SOCPlayer playerWithLA = game.getPlayerWithLargestArmy();
  	int playedKnights = ourPlayerData.getNumKnights();   	
  	if (playKnight) {
      	if (playerWithLA == null) {
      		/*player need minimum 3 knights to get largest army */
      		willGetLA = playedKnights + 1 > 2;
      	} else {
      		if (playerWithLA.getPlayerNumber() != ourPlayerNumber) {
      			int opPlayedKnights = playerWithLA.getNumKnights();
      			willGetLA = playedKnights + 1 > opPlayedKnights;
      		}
      	}
      	
      	
//      	/*DEBUG*/
//      	System.out.println("Will get largest army: " + willGetLA);	
  	}
  	    	
  	/*go through all land hexes except the desert and the current robber position
  	 * (cannot put robber back there)
  	 */
  	for(int posRob : landHexes) {
  		
  		if (board.getHexTypeFromCoord(posRob) != SOCBoard.DESERT_HEX &&
  				posRob != board.getRobberHex()){
  			
  			/*get all nodes adjacent to the hex and check if there's a building */
  			List<SOCPlayer> candidates = game.getPlayersOnHex(posRob);
  			
  			
  			/*we add hex, even if there's no player affected, because sometimes may 
  			 * happen that on every hex with other player we have our own building
  			 * therefore it's better to place the robber on the hex with no players
  			 * then to block ourselves
  			 */
  			if (candidates.size()==0) {
  				SOCState tmpState = currentState.copySOCState();
				if (playKnight) {					
  					tmpState.updatePlayedKnightCard(ourPlayerData, willGetLA, playerWithLA);
  		    	}
  				
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
  				
  				
  				
//  				/*DEBUGA*/
//  				System.out.println("Player: " + ourPlayerNumber + " hex: " + posRob + 
//  							" victims size " + candidates.size() + " anyRes: " 
//  							+ anyRes + " our: " + our + candres);
  				
  				if (our)
  					continue;
  				
  				if (candidates.size()==1 && hasRes[0]) {
  					SOCState tmpState = currentState.copySOCState();
  					if (playKnight) {					
  	  					tmpState.updatePlayedKnightCard(ourPlayerData, willGetLA, playerWithLA);
  	  		    	}
  	  				
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
  	      				int[] plResources = currentState.getPlayerState(player).getResources();
  	      				int[] plResourcesProb = currentState.getPlayerState(player).getResourceProbabilities();

  	      				if (playKnight) {					
  	      					tmpState.updatePlayedKnightCard(ourPlayerData, willGetLA, playerWithLA);
  	      		    	}
  	      				
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
  	      					
  	      					tmpState.updateResources(player, true);
  	      					
  	      					/* if search is done after 7 was rolled first we were discarding,
  	      					 * so instead of updating resources to true values, we have update them 
  	      					 * to values after discard (case when this function is called
  	      					 * from searchRollDice())*/
  	      					if (!playKnight) {
  	      						tmpState.updateSetResources(ourPlayerData, prevResources);
  	      					} else {
  	      						tmpState.updateResources(ourPlayerData, false);
  	      					}
  	      					
  	      					if (plResources[resource]>0 || 
  	      							(plResourcesProb[resource] > 0 && plResources[SOCResourceConstants.UNKNOWN-1] > 0 )) {
  	      						
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
  		} else {
  			state_values = state_values_no_victims;
  			robPositions = robPositions_no_victims;
  			playersToRob = playersToRob_no_victims;
  		}
  	}
  	
  	/*we chose the hex, where we should place the robber and the player to rob
  	 * with highest expected state value
  	 */
  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
  	int[] placeAndPlayerToRob = new int[2];
  	
//  	/*DEBUG*/
//	    System.out.println(game.getName() + " maxAndIndex key " + maxAndIndex.getKey() + " maxAndIndex value " + 
//	    		maxAndIndex.getValue() + " robPositions size " + robPositions.size() + 
//	    		" playersToRob size " + playersToRob.size());
//	    System.out.println("player to rob " + Arrays.toString(playersToRob.toArray()));
	    
  	placeAndPlayerToRob[0] = robPositions.get(maxAndIndex.getValue());
  	placeAndPlayerToRob[1] = playersToRob.get(maxAndIndex.getValue());
  	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
  			maxAndIndex.getKey(), placeAndPlayerToRob);
  	return(result);  	
  }
  
  /**
   * Checks the value of the new state after buying a development card.
   * Because we don't know what type of the development card we will get we 
   * calculate the average over different types of cards weighted by their initial
   * number. 
   *  <P>
   * Function does not check, if player has resources to buy a development card. 
   * This should be checked in {@link #buildOrTradeOrPlayCard() }.
   * <P>
   * @return state value after buying the development card.
   */
  protected float searchBuyDevelopmentCard(){
  	
//  	/*DEBUG*/
//  	System.out.println("searchBuyDevelopmentCard() was called");
  	
  	float state_value = 0.0f;
  	
		SOCState tmpState = state.copySOCState();
		int[] plResources = tmpState.getPlayerState(ourPlayerData).getResources();
		
		/* resource array in tmpState starts from 0 not from 1 like in 
  	 * SOCResourceConstants. New state after buying a development card has 3 fewer
  	 * resources, which were needed to buy the card */
		int[] resources = new int[] {SOCResourceConstants.SHEEP-1, SOCResourceConstants.WHEAT-1,
				SOCResourceConstants.ORE-1};		
		int[] amounts = new int[] {-1, -1, -1};
		tmpState.updateAddSubstractResources(resources, amounts);
		
		// value of state will be multiplied by probability of getting this card 
		// (based on initial number of dev cards)
		// dev cards in following order: VP cards, road building card, discovery card,
	    // monopoly card, Knight card
		// TO DO: maybe you can track which card were already definitely used
		int[] initDevCardProbs = new int[]{5, 2, 2, 2, 14};
		//indexes of dev cards as in SOCPlayerState
		int[] devCardsToPick = new int[]{0, 2, 4, 6, 8};
		for (int i = 0 ; i < devCardsToPick.length ; i++) {
			tmpState.updateBuyDevCard(ourPlayerData, i);
			state_value += getStateValue(tmpState)*initDevCardProbs[i];
			tmpState.undoUpdateBuyDevCard(ourPlayerData, i);
		}
		
		//25 = sum of number of dev cards at the beginning
		return state_value/25;
  }
  
  /**
   * Checks the value of the new state after playing the discovery card.
   * <P>
   * Function does not check, if player has a discovery card.
   * This should be checked in {@link #buildOrTradeOrPlayCard() }.
   * <P>
   * @return map, first element is the state value of the best action, second element
   * is array with two resources that we should pick.
   */
  protected AbstractMap.SimpleEntry<Float, int[]> searchPlayDiscovery() {
  	
//  	/*DEBUG*/
//  	System.out.println("searchPlayDiscovery() was called");
  	
  	SOCState tmpState = state.copySOCState();
  	tmpState.updatePlayedDevCard(SOCDevCardConstants.DISC);
  	
  	/* There are 13 combinations of 2 resources (out of 5), that we can pick.
  	 * For each pair increase resources in our hand and check new state value
  	 */
  	ArrayList<Float> state_values = new ArrayList<Float>(13);
  	ArrayList<int[]> resToPick = new ArrayList<int[]>(13);
  	
  	for (int i = 0; i < 5; i++) {
  		for (int j = i; j < 5; j++) {
  			int[] resources = new int[] {i, j};
  			int[] amounts = new int[] {1, 1};
  			tmpState.updateResources(ourPlayerData, false);
  			tmpState.updateAddSubstractResources(resources, amounts);

  			state_values.add(Float.valueOf(getStateValue(tmpState)));
  			resToPick.add(new int[]{i, j});
  		}
  	}
  	
  	/*we chose resources, that give the highest expected state value
  	 */
  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
  	int[] resourcesToPick = resToPick.get(maxAndIndex.getValue());
  	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
  			maxAndIndex.getKey(), resourcesToPick);
  	return result;  	
  }
  
  /**  
   * Checks the value of the new state after playing the monopoly card and which
   * resource we should pick.
   * <P>
   * Function does not check, if player has a monopoly card.
   * This should be checked in {@link #buildOrTradeOrPlayCard() }.
   * <P>
   * @return map, first element is the state value of the best action, second element
   * is the index of resource that we should pick.
   */
  protected AbstractMap.SimpleEntry<Float, Integer> searchPlayMonopoly() {
//  	/*DEBUG*/
//  	System.out.println("searchPlayMonopoly() was called");
  	
  	SOCState tmpState = state.copySOCState();
  	tmpState.updatePlayedDevCard(SOCDevCardConstants.MONO);
  	
  	ArrayList<Float> state_values = new ArrayList<Float>(5);
  	
  	/*for each resource we steal the KNOWN cards of the opponents*/
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
  
  /**
   * Checks the value of the new state after playing the roads building card and shows
   * two best roads that we should build.
   * <P>
   * Function does not check, if player has a roads building card.
   * This should be checked in {@link #buildOrTradeOrPlayCard() }.
   * <P>
   * @return map, first element is the state value of the best action, second element
   * is the array with numbers of roads that should be built
   */
  protected AbstractMap.SimpleEntry<Float, Integer[]> searchPlayRoads() {
  	
//  	/*DEBUG*/
//  	System.out.println("searchPlayRoads() was called");
  	
  	HashSet<Integer> potentialRoads = (HashSet<Integer>) ourPlayerData.getPotentialRoads().clone();
  	
//  	/*DEBUG*/
//  	System.out.println("In the first run we can check " + potentialRoads.size() +
//  			" of potential roads");
  	
  	SOCState tmpState = state.copySOCState();
  	ArrayList<Float> state_values = new ArrayList<Float>();
  	ArrayList<Integer[]> twoRoadsCoord = new ArrayList<Integer[]>();
  	
  	tmpState.updatePlayedDevCard(SOCDevCardConstants.ROADS);
  	
  	/* for checking the situation after placing the road we use the method built into
		 * SOCGame: SOCGame.putTempPiece */
  	for(Integer posSetCoord : potentialRoads) {
//  		/*DEBUG*/
//  		System.out.println("road1 number " + posSetCoord);
  		
  		SOCRoad tmpRoad = new SOCRoad(ourPlayerData, posSetCoord, board);
  		
  		game.putTempPiece(tmpRoad);
  		
  		Iterator<SOCPlayer> playersIter = players.values().iterator();
  		SOCPlayer ourPn = game.getPlayer(ourPlayerNumber);

	    while (playersIter.hasNext())
	    {   
	    	tmpState.updatePlaceRoad(playersIter.next());
	    }
	    
	    /*potential roads after placing the first road*/
	    HashSet<Integer> potentialSecondRoads = (HashSet<Integer>) ourPn.getPotentialRoads().clone();
	    
//		    /*DEBUG*/
//	    	System.out.println("In the second run we can check " +  potentialSecondRoads.size() +
//	    			" of potential roads");

	    	for(Integer posSecondSetCoord : potentialSecondRoads) {
//	    		/*DEBUG*/
//	    		System.out.println("road2 number " + posSecondSetCoord);
	    		
	    		SOCRoad tmpSecondRoad = new SOCRoad(ourPn, posSecondSetCoord, board);
	    		
	    		game.putTempPiece(tmpSecondRoad);
	      		
	      		Iterator<SOCPlayer> playersSecondIter = players.values().iterator();

	    	    while (playersSecondIter.hasNext())
	    	    {   
	    	    	tmpState.updatePlaceRoad(playersSecondIter.next());
	    	    }
			    
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
  
  /**
   * Checks the value of the new state after trading card with the bank and which
   * resources it is best to trade.
   * <P>
   * Function DOES check, if player has enough resources to trade the bank
   * (unlike other functions this is not done in {@link #buildOrTradeOrPlayCard() }.
   * <P>
   * @return map, first element is the state value of the best action, second element
   * is the trade offer. Trade offer is an array, where first element is the index of the
   * resource we would like to sell, second  - resource we would like to buy, third - 
   * quantity of first resource that we have to give.
   */
  protected AbstractMap.SimpleEntry<Float, int[]> searchTradeBank() {
  	
//  	/*DEBUG*/
//  	System.out.println("searchTradeBank() was called");
  	
  	SOCState tmpState = state.copySOCState();
  	ArrayList<Float> state_values = new ArrayList<Float>();
  	ArrayList<int[]> tradeOffer = new ArrayList<int[]>();
  	
  	boolean[] ports = ourPlayerData.getPortFlags();
  	int[] plRes = tmpState.getPlayerState(ourPlayerData).getResources();
  	int[] plResources = Arrays.copyOf(plRes, plRes.length);
  	
//  	/*DEBUG*/
//  	System.out.println("resource " + Arrays.toString(plResources) + 
//  			" resource in tmpstate " + Arrays.toString(tmpState.getPlayerState(ourPlayerData).getResources()));
//  	
  	//usually to trade with bank we have to give 4 resources of the same type
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
  	for (int j = 0; j < plResources.length; j++) {
  		tmpState.updateResources(ourPlayerData, false);
  		int res = plResources[j];
  		
//  		/*DEBUG*/
//      	System.out.println("resource " + Arrays.toString(plResources) + 
//      			" and j = " + j + " res = " + res);
  		
  		
  		if (res>=4 || (res>=3 && ports[SOCBoard.MISC_PORT])|| (res>=2 && ports[j+1]) ) {
  			
//  			/*DEBUG*/
//  	    	System.out.println("We can trade " + SOCResourceConstants.resName(j+1) +
//  	    			" because we have " + res + " cards of this type");
  			
  			int quantity = howMuchToGive;
  			if (ports[j+1]) {
  				quantity = 2;
  			}
  			
  			for (int i = 0; i < plResources.length; i++) {
  	    		if (i == j) 
  	    			continue;
  	    		int[] resources = new int[] {i, j};
      			int[] amounts = new int[] {1, -quantity};
      			tmpState.updateResources(ourPlayerData, false);
      			tmpState.updateAddSubstractResources(resources, amounts);

  	    		state_values.add(Float.valueOf(getStateValue(tmpState)));	
  	    		tradeOffer.add(new int[] {j, i, quantity});
  	    		
//  	    		/*DEBUG*/
//  	        	System.out.println("resource " + Arrays.toString(plResources) + 
//  	        			" resource in tmpstate " + Arrays.toString(tmpState.getPlayerState(ourPlayerData).getResources()) + 
//  	        			" resource in state " + Arrays.toString(state.getPlayerState(ourPlayerData).getResources()) + 
//  	        			" resource in game " + Arrays.toString(ourPlayerData.getResources().getAmounts(false)));
  	    	}    			
  		}
  	}
  	
  	//if we don't have any possibility to trade
  	if (state_values.isEmpty()) {
  		return(new AbstractMap.SimpleEntry<Float, int[]>(Float.valueOf(-1.0f), null));
  	}

  	/*best resource to trade and best resource to buy are chosen*/
  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
  	int[] trade = tradeOffer.get(maxAndIndex.getValue());
  	
//  	/*DEBUG*/
//  	System.out.println("We decide to trade " + trade[2] + " " + 
//  			SOCResourceConstants.resName(trade[0]+1) +
//  			" for " + SOCResourceConstants.resName(trade[1]+1));
  	
  	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
  			maxAndIndex.getKey(), trade);
  	return result;  
  }
  
  
  /**
   * When 7 is rolled and player has more than 7 cards, he has to discard half of his cards. 
   * This function checks every possible deck of cards, that the player can discard and
   * returns the one with the highest state value.
   * <P>
   * 
   * @param numDiscards - number of cards that one has to discard
   * @return map, first element is the state value of the best action, second element
   * is the array with resources that we should give back
   */
  protected AbstractMap.SimpleEntry<Float, int[]> searchDiscardAfterSevenRolled(int numDiscards) {
  	
//  	/*DEBUG*/
//  	System.out.println("searchDiscardAfterSevenRolled() was called");
  	
  	SOCState tmpState = state.copySOCState();
  	ArrayList<Float> state_values = new ArrayList<Float>();
  	int[] res = new int[] {0, 1, 2, 3, 4};
  	ArrayList<int[]> allCardsDiscard = new ArrayList<int[]>();
  	int[] plResources =  state.getPlayerState(ourPlayerData).getResources();

  	
  	/*find all combinations of given resources of length equal to number of cards we 
  	 * have to discard
  	 */
  	kLengthCombination combinationsFinder = new kLengthCombination(plResources, numDiscards);
  	allCardsDiscard = combinationsFinder.getCombinations();
  	
//  	/*DEBUG*/
//  	System.out.println("There are " + allCardsDiscard.size() + 
//  			" combinations of cards we can discard");
//  	/*DEBUG*/
//  	int combindex = 0;
  	
  	for(int[] cards : allCardsDiscard) {
  		
//  		/*DEBUG*/
//      	combindex += 1;
//      	System.out.println("Checking combination " + combindex + ". " +
//      			Arrays.toString(cards));

  		tmpState.updateResources(ourPlayerData, false);
			tmpState.updateSubstractResources(res, cards);
  		
  		state_values.add(Float.valueOf(getStateValue(tmpState)));    		   		
  	}
  	
  	/*best combination of cards to discard is chosen*/
  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
  	int[] resourceToDiscard = allCardsDiscard.get(maxAndIndex.getValue());
  	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
  			maxAndIndex.getKey(), resourceToDiscard);
  	
//  	/*DEBUG*/
//  	System.out.println("Finished discarding");
//  	System.out.println("Resources" + ourPlayerData.getResources().toFriendlyString());
//  	System.out.println("resources to dicard " + Arrays.toString(resourceToDiscard));
  	
  	return result;  	
  }
  
  /**
   * Checks the value of the new state after rolling the dice. We don't know
   * what number we will roll, therefore we calculate weighted mean from all states.
   * <P>
   * @return expected state value after rolling the dice
   */
  protected float searchRollDice() {
  	
//  	/*DEBUG*/
//  	System.out.println("searchRollDice() was called");
  	
  	SOCState tmpState = state.copySOCState();
  	float state_value = 0.0f;
//  	ArrayList<Float> state_values = new ArrayList<Float>();
  	
  	SOCPlayerNumbers numbers = ourPlayerData.getNumbers();
  	int robberHex = board.getRobberHex();
  	
  	/*probabilities of throwing the number from 0 to 12 */
  	float[] FLOAT_VALUES =
  	    {
  	        0.0f, 0.0f, 0.03f, 0.06f, 0.08f, 0.11f, 0.14f, 0.17f, 0.14f, 0.11f,
  	        0.08f, 0.06f, 0.03f
  	    };
  	
  	for(int i = 2; i <= 12; i++) {
  		/*for now we don't take into account throwing 7 and possibility 
  		 * of discarding and stealing a resource
  		 */
  		if (i==7)
  			continue;
  		
  		tmpState.updateResources(ourPlayerData, false);
  		
  		Vector<Integer> resources = numbers.getResourcesForNumber(i, robberHex);
  		tmpState.updateAddResourcesFromConstants(resources);
  		
//  		state_values.add(Float.valueOf(getStateValue(tmpState)) * FLOAT_VALUES[i]);
  		state_value += getStateValue(tmpState) * FLOAT_VALUES[i];
  	}
  	
  	//For 7 more happens: discard cards and Place robber
  	tmpState.updateResourcesByCopy(ourPlayerData, false);
  	int numCards = ourPlayerData.getResources().getKnownTotal();
  	if (numCards > 7) {
  		AbstractMap.SimpleEntry<Float, int[]> discards = searchDiscardAfterSevenRolled( (int)numCards/2 );
  		int[] cardsDiscard = discards.getValue();
  		int[] res = new int[] {0, 1, 2, 3, 4};
			tmpState.updateSubstractResources(res, cardsDiscard);
  	}
  	
  	//we pass a copy of tmpState, because searchPlaceRobberOrPlayKnight updates resources
		SOCState tmpState2 = tmpState.copySOCState();
  	
  	AbstractMap.SimpleEntry<Float, int[]> resultRobber = searchPlaceRobberOrPlayKnight(tmpState2, false);
  	
  	state_value += resultRobber.getKey()*FLOAT_VALUES[7];
  	
  	//probabilities of dice roll sum up to 1.01
  	return(state_value/1.01f);
  }   
  
  /**
   * From array return the maximum element and its index
   * @param array
   * @return map, first element is the maximum value, second is its index
   */
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
  
  protected abstract float getStateValue(SOCState state);
  
  /**
   * Action function used at the beginning of the turn: 
   * @return
   */
  public AbstractMap.SimpleEntry<Integer, int[]> rollOrPlayKnight() {

//  	/*DEBUG*/
//  	System.out.println("rollOrPlayKnight() was called");
  	
  	updateStateValue();
  	
  	if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.KNIGHT) &&
  			!ourPlayerData.hasPlayedDevCard()) {
  		
  		AbstractMap.SimpleEntry<Float, int[]> playKnight = searchPlaceRobberOrPlayKnight(state, true);
      	float roll = searchRollDice();
      	
//      	/*DEBUG*/
//      	System.out.println("roll value " + roll + "play knight value " + playKnight.getKey());
//      	
      	if (roll<playKnight.getKey()) {
//      		/*DEBUG*/
//          	System.out.println("decided to play knight");
      		
      		return(new AbstractMap.SimpleEntry<Integer, int[]>(PLAY_KNIGHT, playKnight.getValue()));
      	} 
  	}
  	
//  	/*DEBUG*/
//  	System.out.println("decided to roll");
  	
  	return(new AbstractMap.SimpleEntry<Integer, int[]>(ROLL, null));  	
  }
  
  public int[] moveRobber() {
  	
//  	/*DEBUG*/
//  	System.out.println(brain.getName() + " moveRobber() was called");
  	
  	updateStateValue();
  	
  	return searchPlaceRobberOrPlayKnight(state, false).getValue(); 	
  }
  
  public SOCResourceSet discard(int numDiscards) {
  	
//  	/*DEBUG*/
//  	System.out.println("discard() was called. we have to discard " + numDiscards + " cards");
  	
  	updateStateValue();
  	
  	SOCResourceSet resSet = new SOCResourceSet(searchDiscardAfterSevenRolled(numDiscards).getValue());
  	
//  	/*DEBUG*/
//  	System.out.println("resSet send to discard");
  	
  	return resSet;
  }
  
  public AbstractMap.SimpleEntry<Integer, int[]> buildOrTradeOrPlayCard() {
  	
//  	/*DEBUG*/
//  	System.out.println("buildOrTradeOrPlayCard() was called");
  	

		
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
//  			/*DEBUG*/
//  			System.out.println("search discovery");
  			discovery = searchPlayDiscovery();
  			actionValues.add(discovery.getKey());
  			actionNames.add(PLAY_DISC);
  		}
  		
  		if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.MONO)) {
//  			/*DEBUG*/
//  			System.out.println("search monopoly");
  			monopoly = searchPlayMonopoly();
  			actionValues.add(monopoly.getKey());
  			actionNames.add(PLAY_MONO);
  		}
  		
  		if ( (ourPlayerData.getNumPieces(SOCPlayingPiece.ROAD) >= 2) 
  				&& ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.ROADS)
  				&& ourPlayerData.getPotentialRoads().size()>0) {
//  			/*DEBUG*/
//  			System.out.println("search roads card");
  			roads = searchPlayRoads();
  			if (roads!=null) {
  				actionValues.add(roads.getKey());
      			actionNames.add(PLAY_ROADS);
  			}
  		}
  		
  		if (ourPlayerData.getInventory().hasPlayable(SOCDevCardConstants.KNIGHT)) {
//  			/*DEBUG*/
//  			System.out.println("search play knight");
  			knight = searchPlaceRobberOrPlayKnight(state, true);
  			actionValues.add(knight.getKey());
  			actionNames.add(PLAY_KNIGHT);
  		}    		
  	}
  	
//  	/*DEBUG*/
//  	System.out.println("resources " + ourPlayerData.getResources().toFriendlyString());
  	
  	//TO DO: get resources into a variable, because it's used several times
  	
  	if (ourPlayerData.getResources().contains(
  			SOCPlayingPiece.getResourcesToBuild(SOCPlayingPiece.SETTLEMENT)) 
  			&& ourPlayerData.hasPotentialSettlement()
  			&& (ourPlayerData.getNumPieces(SOCPlayingPiece.SETTLEMENT) > 0)) { 
//  		/*DEBUG*/
//  		System.out.println("search settlement");
  		settlement = searchPlaceSettlement();
			actionValues.add(settlement.getKey());
			actionNames.add(PLACE_SETTLEMENT);    		
  	}
  	
  	if (ourPlayerData.getResources().contains(
  			SOCPlayingPiece.getResourcesToBuild(SOCPlayingPiece.ROAD))
  			&& ourPlayerData.hasPotentialRoad() 
  			&& (ourPlayerData.getNumPieces(SOCPlayingPiece.ROAD) > 0)) {
//  		/*DEBUG*/
//  		System.out.println("search road");
  		road = searchPlaceRoad();
			actionValues.add(road.getKey());
			actionNames.add(PLACE_ROAD);    		
  	}
  	
  	if (ourPlayerData.getResources().contains(
  			SOCPlayingPiece.getResourcesToBuild(SOCPlayingPiece.CITY))
  			&& ourPlayerData.hasPotentialCity()
  			&& (ourPlayerData.getNumPieces(SOCPlayingPiece.CITY) > 0)) {
//  		/*DEBUG*/
//  		System.out.println(game.getName() + " search city");
//  		System.out.println(game.getName() + " has potential city " + ourPlayerData.hasPotentialCity());
//  		System.out.println(game.getName() + " has settlements " + ourPlayerData.getSettlements().size());
//  		System.out.println(game.getName() + " has city pieces " + ourPlayerData.getNumPieces(SOCPlayingPiece.CITY));
      	
  		city = searchPlaceCity();
			actionValues.add(city.getKey());
			actionNames.add(PLACE_CITY);    		
  	}
  	
  	//SOCPlayingPiece.MAXPLUSONE is used to get cost of the devcard
  	if (ourPlayerData.getResources().contains(
  			SOCPlayingPiece.getResourcesToBuild(SOCPlayingPiece.MAXPLUSONE)) &&
  			game.getNumDevCards()>0) {
//  		/*DEBUG*/
//  		System.out.println("search development card");
  		devCard = searchBuyDevelopmentCard();
			actionValues.add(devCard);
			actionNames.add(BUY_DEVCARD);    		
  	}
  	
  	AbstractMap.SimpleEntry<Float, int[]> bank = searchTradeBank();
  	if (bank.getKey()>=0) {
//  		/*DEBUG*/
//		System.out.println("search trade bank");
  		actionValues.add(bank.getKey());
  		actionNames.add(TRADE_BANK);  
  	}
  	
//  	System.out.println("actionvalues " + actionValues.isEmpty());
  	if (actionValues.isEmpty()) {
//  		/*DEBUG*/
//  		System.out.println("choose do nothing");
  		return(new AbstractMap.SimpleEntry<Integer, int[]>(END_TURN, null));
  	}
  	
  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(actionValues);
  	AbstractMap.SimpleEntry<Integer, int[]> result;
  	
  	//if state without any action is better: do nothing
  	if (maxAndIndex.getKey() < currentStateValue) {
//  		/*DEBUG*/
//  		System.out.println("choose do nothing");
  		return(new AbstractMap.SimpleEntry<Integer, int[]>(END_TURN, null));
  	}
  	
  	switch (actionNames.get(maxAndIndex.getValue())) {
  	  case PLAY_DISC:
//  		/*DEBUG*/
//  		System.out.println("choose discovery");
  	    result = new AbstractMap.SimpleEntry<Integer, int[]>(PLAY_DISC, discovery.getValue());
  	    break;

  	  case PLAY_MONO:
//  		  /*DEBUG*/
//  		  System.out.println("choose monopoly");
  		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLAY_MONO, new int[]{monopoly.getValue()});
  		  break;
  		  
  	  case PLAY_ROADS:
//  		  /*DEBUG*/
//  		  System.out.println("choose play roads card");
  		  int[] roadsToBuild = Arrays.stream(roads.getValue()).mapToInt(i -> i).toArray();
  		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLAY_ROADS, roadsToBuild);
  		  break;
  		  
  	  case PLAY_KNIGHT:
//  		  /*DEBUG*/
//  		  System.out.println("choose play knight");
  		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLAY_KNIGHT, knight.getValue());
  		  break;
  	
  	  case PLACE_SETTLEMENT:
//  		  /*DEBUG*/
//  		  System.out.println("choose place settlement");
  		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLACE_SETTLEMENT, new int[]{settlement.getValue()});
  		  break;
  	  
  	  case PLACE_CITY:
//  		  /*DEBUG*/
//  		  System.out.println("choose place city");
  		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLACE_CITY, new int[]{city.getValue()});
  		  break;
  		  
  	  case PLACE_ROAD:
//  		  /*DEBUG*/
//  		  System.out.println("choose place road " + ourPlayerData.getPotentialRoads().contains(road.getValue()));
  		  result = new AbstractMap.SimpleEntry<Integer, int[]>(PLACE_ROAD, new int[]{road.getValue()});
  		  break;
  		 
  	  case BUY_DEVCARD:
//  		  /*DEBUG*/
//  		  System.out.println("choose buy devcard");
  		  result = new AbstractMap.SimpleEntry<Integer, int[]>(BUY_DEVCARD, null);
  		  break;
  		  
  	  case TRADE_BANK:
//  		  /*DEBUG*/
//  		  System.out.println("choose trade bank");
  		  result = new AbstractMap.SimpleEntry<Integer, int[]>(TRADE_BANK, bank.getValue());
  		  break;

  	  default:
//  		  /*DEBUG*/
//  		  System.out.println("choose do nothing");
  		  result = new AbstractMap.SimpleEntry<Integer, int[]>(END_TURN, null);
  	}
  	
  	return result;
  }
  
  protected abstract void updateStateValue();
  
//  protected abstract void writeMemory();
  
//  protected abstract void readMemory();
  
//  protected abstract void synchroniseMemory();
  
  public abstract void updateReward();
  
  public void updateReward(int winner) {
	  
  }
  
  public int[] getStateRes(SOCPlayer pn) {
	  return state.getPlayerState(pn).getResources();
  }

  
  protected class CustomPair {
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
  
  protected class kLengthCombination {
  	private int[] elements;
  	private ArrayList<int[]> kCombinations;
  	int k;
  	
  	public kLengthCombination(int[] elements, int k) {
  		this.elements = elements;
  		this.k = k;
  		this.kCombinations = new ArrayList<int[]>();
  	}
  	
  	protected void findCombinations(int[] solution, int idx, int length) {
  		
  		if (length== k) {
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
