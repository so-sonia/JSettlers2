package soc.robot.rl;

/*for now always needs to update resources, resource probabilities and ports*/

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCInventory;
import soc.game.SOCPlayer;
import soc.game.SOCPlayerNumbers;
import soc.game.SOCResourceConstants;
import soc.game.SOCSettlement;

public class SOCState {
	
	protected HashMap<Integer, PlayerState> playersInfo;
	
    /**
     * {@link #ourPlayerData}'s player number.
     * @since 2.0.00
     */
    protected final int ourPlayerNumber;
    
	
	/**
	 * Constructor for the state object. Data is updated through 
	 * {@link #updateAll(HashMap) updateAll(players)} method.
	 * @param pln our player number
	 */
	public SOCState(int pln, HashMap<Integer,SOCPlayer> players){
		playersInfo = new HashMap<Integer, PlayerState>();	
		ourPlayerNumber = pln;
		
		Iterator<SOCPlayer> playersIter = players.values().iterator();

	       while (playersIter.hasNext())
	       {
	           addPlayerState(playersIter.next().getPlayerNumber());
	       }
	}	
	
	public SOCState(int pln){
		playersInfo = new HashMap<Integer, PlayerState>();	
		ourPlayerNumber = pln;
	}	
	
	public void addPlayerState(int playerNumber) {
    	SOCPlayerState state = new SOCPlayerState();
        playersInfo.put(new Integer(playerNumber), state);
    }
    
    /**
     * From {@link SOCInventory} get number of development cards by type and state (old/new)
     * Following cards will be checked: VP cards, road building card, discovery card,
     * monopoly card, Knight card
     * {@link SOCInventory}{@link SOCInventory#getAmount(int, int) .getAmount(state, type)} is not very efficient
     * method, uses linear search to get amounts of development cards by type => possibly change in future
     * @return array of length 9 with development cards by type and state
     */
    public void updateMyDevCards(SOCPlayer pn) {
    	int[] myDevCards = new int[9];
    	SOCInventory inventory = pn.getInventory();
    	myDevCards[0] = inventory.getNumVPItems();
    	myDevCards[1] = inventory.getAmount(SOCInventory.OLD, SOCDevCardConstants.ROADS);
    	myDevCards[2] = inventory.getAmount(SOCInventory.NEW, SOCDevCardConstants.ROADS);
    	myDevCards[3] = inventory.getAmount(SOCInventory.OLD, SOCDevCardConstants.DISC);
    	myDevCards[4] = inventory.getAmount(SOCInventory.NEW, SOCDevCardConstants.DISC);
    	myDevCards[5] = inventory.getAmount(SOCInventory.OLD, SOCDevCardConstants.MONO);
    	myDevCards[6] = inventory.getAmount(SOCInventory.NEW, SOCDevCardConstants.MONO);
    	myDevCards[7] = inventory.getAmount(SOCInventory.OLD, SOCDevCardConstants.KNIGHT);
    	myDevCards[8] = inventory.getAmount(SOCInventory.NEW, SOCDevCardConstants.KNIGHT);     	
    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setDevCards(myDevCards);
    }
    
    /**
     * From {@link SOCInventory} get number of development cards by type and state (old/new)
     * For other players all the cards have type {@link SOCDevCardConstants#UNKNOWN}.
     * Array of length 2 with development cards by type and state is updated.
     * <P>
     * {@link SOCInventory}{@link SOCInventory#getAmount(int, int) .getAmount(state, type)} is not very efficient
     * method, uses linear search to get amounts of development cards by type => possibly change in future
     */
    public void updateDevCards(SOCPlayer pn) {
    	int[] devCards = new int[2];
    	SOCInventory inventory = pn.getInventory();
    	devCards[0] = inventory.getAmount(SOCInventory.OLD, SOCDevCardConstants.UNKNOWN);
    	devCards[1] = inventory.getAmount(SOCInventory.NEW, SOCDevCardConstants.UNKNOWN);   	
    	/*DEBUG*/
    	PlayerState statepn = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
    	statepn.setDevCards(devCards);
//    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setDevCards(devCards);
    }
    
    /**
     * For each resource type we calculate probabilities with which the player can take this resource.
     * Having more than one village/city adjacent to the same hex does not increase the probability 
     * (however number of adjacent hexes is taken into account in 
     * {@link #updateBoardInfoOnBuildings(SOCPlayer pn, SOCBoard board)}).
     * <P>
     * {@link SOCPlayerState#resourceProbabilities} and {@link SOCPlayerState#uniqueNumbers} are 
     * updated at the same time, because the same information is accessed for these two.
     * <P>
     * {@link SOCPlayerNumbers} from {@link SOCPlayer#getNumbers()} is used.
     * 
     * @param pn player for whom to update info
     * @param robberHex int value of hex on which robber is placed
     */
    public void updateResourceProbabilitiesAndUniqueNumbers(SOCPlayer pn, int robberHex) {
    	int[] resourceNumbers = new int[5];
    	Set<Integer> uniqueNumbers = new HashSet<Integer>();
    	SOCPlayerNumbers numbers = pn.getNumbers();
    	float[] FLOAT_VALUES =
    	    {
    	        0.0f, 0.0f, 0.03f, 0.06f, 0.08f, 0.11f, 0.14f, 0.17f, 0.14f, 0.11f,
    	        0.08f, 0.06f, 0.03f
    	    };
    	    	
    	for (int resource = SOCResourceConstants.CLAY;
                resource <= SOCResourceConstants.WOOD; resource++)
        {
            //D.ebugPrintln("resource: " + resource);

            float totalProbability = 0.0f;

            /** we add each number to probability only once, that's why we use set - to get unique values */
            Vector<Integer> numbersRes =
                (robberHex != -1)
                   ? numbers.getNumbersForResource(resource, robberHex)
                   : numbers.getNumbersForResource(resource);
                   
            
            Set<Integer> uniqueRes = new HashSet<Integer>();
            uniqueNumbers.addAll(numbersRes);
            uniqueRes.addAll(numbersRes);
            for (Integer number : uniqueRes) {
            	totalProbability += FLOAT_VALUES[number.intValue()];
            }
            
            if (totalProbability != 0.0f)
            {
            	//SOCResourceConstants.CLAY = 1, therefore we take -1 to fill the array
            	resourceNumbers[resource-1] = Math.round(totalProbability*30);
            }
            
        }
    	
    	PlayerState state = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
    	state.setResourceProbabilities(resourceNumbers);
    	state.setUniqueNumbers(uniqueNumbers.size());
    }
    
    
    /**
     * For each resource type number of adjacent settlements and cities (counted twice) is calculated.
     * Settlements and cities adjacent to the hex blocked by the robber are not taken into account.
     * Also number of unique hexes to which settlements and cities of the player are adjacent is calculated.
     * (this number includes the hex blocked by the robber, if that's the case)
     * And the information, if player is blocked by robber (robber stands on a hex, that's adjacent to
     * player's settlement or city) is updated
     * {@link #resourceAdjacentBuildings}, {@link #uniqueHexes} and {@link #blockedByRobber} are updated.
     * 
     * @param pn player for whom to update info
     * @param board on which game is played
     */
    public void updateBoardInfoOnBuildings(SOCPlayer pn, SOCBoard board, int robberHex) {
    	int[] resourceAdjacentBuildings = new int[5];
    	Set<Integer> uniqueHexes = new HashSet<Integer>();
    	int blockedByRobber = 0;
    	
    	Vector<SOCSettlement> settlements = pn.getSettlements();
    	
    	for (SOCSettlement settle : settlements) 
    	{
    		Vector<Integer> hexes = settle.getAdjacentHexes();
    		
    		for (Integer hexInt : hexes)
    	        {
    				if (board.isHexOnLand(hexInt))
    					uniqueHexes.add(hexInt);
    	
//    				int hex = hexInt.intValue();
    				
    				if (hexInt == robberHex) {
    					blockedByRobber = 1;
    					continue;
    				}
  			
    	            final int resource = board.getHexTypeFromCoord(hexInt);
    	            
    	            if ((resource >= SOCResourceConstants.CLAY) && (resource <= SOCResourceConstants.WOOD))
    	            {
    	            	//SOCResourceConstants.CLAY = 1, therefore we take -1 to fill the array
    	            	resourceAdjacentBuildings[resource-1]++;
    	            }
    				
    	        }
    	}
    	
    	Vector<SOCCity> cities = pn.getCities();
    	for (SOCCity city : cities) 
    	{
    		Vector<Integer> hexes = city.getAdjacentHexes();
    		
    		for (Integer hexInt : hexes)
    	        {
    				uniqueHexes.add(hexInt);
    				int hex = hexInt.intValue();
    				
    				if (hex == robberHex) {
    					blockedByRobber = 1;
    					continue;
    				}
  			
    	            final int resource = board.getHexTypeFromCoord(hex);
    	            
    	            if ((resource >= SOCResourceConstants.CLAY) && (resource <= SOCResourceConstants.WOOD))
    	            {
    	            	//SOCResourceConstants.CLAY = 1, therefore we take -1 to fill the array
    	            	//for city we add +2
    	            	resourceAdjacentBuildings[resource-1]+=2;
    	            }
    				
    	        }
    	}
    	
    	PlayerState state = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
    	state.setResourceAdjacentBuildings(resourceAdjacentBuildings);
    	state.setUniqueAdjacentHexes(uniqueHexes.size());    
    	state.setBlockedByRobber(blockedByRobber); 
    }
    
    /**
     * For each port type it is marked if player has access to it or not
     * @param pn player for whom to update ports
     */
    public void updatePorts(SOCPlayer pn){
    	int[] ports = new int[6];
    	boolean[] portFlags = pn.getPortFlags();
    	
    	for (int i=0; i < ports.length; i++) {
    		ports[i] = portFlags[i] ? 1 : 0 ;
    	}
    	
    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setPorts(ports);
    }
    
    /**
     * Update what resource cards player has in hand.
     * For other players also type unknown is included
     * @param pn player for whom to update resources
     * @param otherPlayer if true also unknown resource type is included
     */
    public void updateResources(SOCPlayer pn, boolean otherPlayer) {
    	int[] resources = pn.getResources().getAmounts(otherPlayer);
//    	if ((pn.getPlayerNumber()==ourPlayerNumber) & resources.length > 5 ) {
////        	/*DEBUG*/
//        	System.out.println("resource " + Arrays.toString(resources)); 
//    	}
//    	/*DEBUG*/
//    	System.out.println("resource " + Arrays.toString(resources));  	
    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setResources(
    			Arrays.copyOf(resources, resources.length));
    }
    
    /**
     * Update what resource cards player has in hand, but instead of assigning the array
     * like in {@link #updateResources(SOCPlayer, otherPlayer)} we copy the values.
     * For other players also type unknown is included
     * @param pn player for whom to update resources
     * @param otherPlayer if true also unknown resource type is included
     */
    public void updateResourcesByCopy(SOCPlayer pn, boolean otherPlayer) {
    	int[] resources = pn.getResources().getAmounts(otherPlayer);    	    	
    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).copyResources(resources);
    }
    
    /**
     * Update number of points that player has
     * @param pn player for whom to update points
     */
    public void updatePoints(SOCPlayer pn) {
    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setPoints(pn.getTotalVP());
    }
    
    /**
     * Update the number of played knights for player
     * @param pn player for whom to update played knights
     */
    public void updatePlayedKnights(SOCPlayer pn) {
    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setPlayedKnights(pn.getNumKnights());
    }
    
    /**
     * Update the longest road length
     * @param pn player for whom to update played the longest road length
     */
    public void updateLongestRoad(SOCPlayer pn) {
    	pn.calcLongestRoad2();
    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setLongestRoad(pn.getLongestRoadLength());
    }
    
    /**
     * Update if the player has the longest road in the game
     * @param pn player for whom to update 
     */
    public void updateHasLongestRoad(SOCPlayer pn) {
    	int hasLongestRoad = pn.hasLongestRoad() ? 1 : 0;
    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setHasLongestRoad(hasLongestRoad);
    }
    
    /**
     * Update if the player has the largest army in the game
     * Uses info from SOCPlayer
     * @param pn player for whom to update 
     */
    public void updateHasLargestArmy(SOCPlayer pn) {
    	int hasLargestArmy = pn.hasLargestArmy() ? 1 : 0;
    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setHasLargestArmy(hasLargestArmy);
    }
    
    /**
     * Update number of places, where we can potentially build a settlement
     * Uses info from SOCPlayer
     * @param pn player for whom to update
     */
    public void updateNumberOfPotentialSettlements(SOCPlayer pn) {
    	int settlements = pn.getPotentialSettlements_arr()==null? 0 : pn.getPotentialSettlements_arr().length;
    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setNumberOfPotentialSettlements(settlements);
    }
   
   public void updateAll(HashMap<Integer,SOCPlayer> players, SOCBoard board) {
	   Iterator<SOCPlayer> playersIter = players.values().iterator();
	   
       while (playersIter.hasNext())
       {    
           updatePlayer(playersIter.next(), board);
       }
   }
    
   public void updatePlayer(SOCPlayer pn,  SOCBoard board) {
	   /* development cards and resources are updated differently for our and other player */
	   if (pn.getPlayerNumber() == ourPlayerNumber) {
		   updateMyDevCards(pn);
		   updateResources(pn, false);
	   }
	   else {   
		   updateDevCards(pn);
		   updateResources(pn, true);
	   }
	   
	   /*all the other state fields are updated in the same way*/
	   updateResourceProbabilitiesAndUniqueNumbers(pn, board.getRobberHex());
	   updateBoardInfoOnBuildings(pn, board, board.getRobberHex());
	   updatePorts(pn);
	   updatePoints(pn);
	   updatePlayedKnights(pn);
	   updateLongestRoad(pn);
	   updateHasLargestArmy(pn);
	   updateHasLongestRoad(pn);
	   updateNumberOfPotentialSettlements(pn);
   }
   
   public int[] getState(SOCPlayer pn) {
	   PlayerState opponentState = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
	   PlayerState ourState = playersInfo.get(Integer.valueOf(ourPlayerNumber));
	   int[] ourPlayerArray = ourState.getStateArray();
	   int[] opponentArray = opponentState.getStateArray();
	   
	   int length = ourPlayerArray.length + opponentArray.length;
       int[] result = new int[length];
       System.arraycopy(ourPlayerArray, 0, result, 0, ourPlayerArray.length);
       System.arraycopy(opponentArray, 0, result, ourPlayerArray.length, opponentArray.length);
       
       return result;
   }
   
   /**
    * After placing a settlement information on resource probabilities, unique numbers, 
    * number od adjacent buildings, ports, points, number of potential settlements,
    * Longest road and player with longest road award (placing a settlement may destroy 
    * somebody's longest road and award will change ownership.
    * @param pn
    * @param board
    */
   public void updatePlaceSettlement(SOCPlayer pn,  SOCBoard board) {
	   updateResourceProbabilitiesAndUniqueNumbers(pn, board.getRobberHex());
	   updateBoardInfoOnBuildings(pn, board, board.getRobberHex());
	   updatePorts(pn);
	   updatePoints(pn);
	   updateNumberOfPotentialSettlements(pn);
	   updateHasLongestRoad(pn);
	   updateLongestRoad(pn);
   }
   
   /**
    * After placing the road, information about points, longest road length, player
    * with the longest road and number of potential settlements should be updated
    * @param pn - player number for whom the information has to be updated
    */
   public void updatePlaceRoad(SOCPlayer pn) {
	   updatePoints(pn);
	   updateLongestRoad(pn);
	   updateHasLongestRoad(pn);
	   updateNumberOfPotentialSettlements(pn);
   }
   
   /**
    * After placing the city information about number of adjacent buildings, 
    * points and potential settlements is updated. Resource probabilities are not changed 
    * when the city is built.
    * @param pn
    * @param board
    */
   public void updatePlaceCity(SOCPlayer pn,  SOCBoard board) {
	   updateBoardInfoOnBuildings(pn, board, board.getRobberHex());
	   updatePoints(pn);
	   updateNumberOfPotentialSettlements(pn);
   }
   
   /**After placing the robber we have to change the probabilities of obtaining the 
    * given resource, number of unique numbers (from 2 to 12) that are placed 
    * on player hexes (through {@link #updateResourceProbabilitiesAndUniqueNumbers}).
    * We also have to update, if the player is blocked by the robber and number of
    * buildings of the player adjacent to the given resource (through 
    * {@link #updateBoardInfoOnBuildings})
    */
   public void updatePlaceRobber(SOCPlayer pn,  SOCBoard board, int robberHex) {
	   updateResourceProbabilitiesAndUniqueNumbers(pn, robberHex);
	   updateBoardInfoOnBuildings(pn, board, robberHex);
   }
   
   
   public void updatePlaceRobberAll(HashMap<Integer,SOCPlayer> players, 
		   		SOCBoard board, int robberHex) {
	   
	   Iterator<SOCPlayer> playersIter = players.values().iterator();
	   while (playersIter.hasNext())
       {    
           SOCPlayer pn = playersIter.next();
           updateResourceProbabilitiesAndUniqueNumbers(pn, robberHex);
    	   updateBoardInfoOnBuildings(pn, board, robberHex);
       }
   }
   
   /**
    * Resources may be stolen in two cases: when robber is moved, then one
    * resource is stolen and when the monopoly card is played - then all
    * resources of the given type are stolen.
    * 
    * @param pn - player number from whom resources are being stolen.
    * @param res - resource that will be stolen
    * @param all - if all resources of given type will be stolen
    */
   public void updateSteal(SOCPlayer pn, int res, boolean all) {
	   PlayerState op = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
	   int[] resOp = op.getResources();
	   PlayerState ourPlayer = playersInfo.get(ourPlayerNumber);
	   int[] resOur = ourPlayer.getResources();
	   if (resOp[res] == 0) {
		   /*if the opponent has no resources of the given type we decrease the resources of the
		    * unknown type (we check that opponent has resources of unknown type before calling this function)
		    * in searchPlaceRobberOrPlayKnight()
		    */
		   if(!all) {
			   resOp[SOCResourceConstants.UNKNOWN-1]--;
			   op.setResources(resOp); //TO TEST: I think this one is not needed, the array is the same thing
			   resOur[res]++;
			   ourPlayer.setResources(resOur); //TO TEST: same as above
		   }
	   } else {
		   
		   if(all) {
			   resOur[res]+=resOp[res];
			   resOp[res]= 0;
		   } else {
			   resOur[res]++;
			   resOp[res]--;
		   }
		   
		   op.setResources(resOp); //TO TEST: same as above
		   ourPlayer.setResources(resOur); //TO TEST: same as above
	   }
   }
   
   /**
    * Function used in {@link RLStrategy#searchBuyDevelopmentCard()} to account for changes
    * that will result after buying a specific type of development card.
    * After buying a development card we increase the number of cards in our hand.
    * If VP card was bought we also increase the number of VP points.
    * @param pn - player number who bought the development card
    * @param boughtCard - index of the card in devCards
    */
   public void updateBuyDevCard(SOCPlayer pn, int boughtCard) {
	   int[] devCards = playersInfo.get(ourPlayerNumber).getDevCards();
	   devCards[boughtCard]++;
	   if (boughtCard == 0) {		   
		   playersInfo.get(ourPlayerNumber).addPoints(1);
	   }	    	
   }
   
   /**
    * Function used in {@link RLStrategy#searchBuyDevelopmentCard()} to reestablish the original
    * state after it was changed by {@link #updateBuyDevCard(SOCPlayer, boughtCard)}.
    * To return to previous state we decrease the number of cards in our hand.
    * If VP card was bought we also decrease the number of VP points.
    * @param pn - player number who bought the development card
    * @param boughtCard - index of the card in devCards
    */
   public void undoUpdateBuyDevCard(SOCPlayer pn, int boughtCard) {
	   int[] devCards = playersInfo.get(ourPlayerNumber).getDevCards();
	   devCards[boughtCard]--;
	   if (boughtCard == 0) {		   
		   playersInfo.get(ourPlayerNumber).substractPoints(1);
	   }	    	
   }
   
   /**
    * Function used in {@link RLStrategy#searchPlaceRobberOrPlayKnight()} when
    * knight card was played. Updates development cards
    * @param pn - player number who played the knight card
    * @param willGetLA - if pn will get Largest Army award
    * @param currentPlayerWithLA - (LA -Largest Army award)
    */
   public void updatePlayedKnightCard(SOCPlayer pn, boolean willGetLA, SOCPlayer currentPlayerWithLA) {
	   PlayerState pnState = getPlayerState(pn);
	   int[] devCards = pnState.getDevCards();
		//decrease the number of old knight cards in our hand
		devCards[7]--;
		pnState.setPlayedKnights(pnState.getPlayedKnights() + 1);
		/*change the owner of the largest army award if that's a case */
		if (willGetLA) {
			pnState.setHasLargestArmy(1);
			if (currentPlayerWithLA!=null) {
				getPlayerState(currentPlayerWithLA).setHasLargestArmy(0);
			}
		}
   }
   
   /**
    * Reduces amount of old development cards possessed by our player.
    * Used for roads, discovery and monopoly card. For Knight card use
    * {@link #updatePlayedKnightCard(SOCPlayer pn,boolean willGetLA, SOCPlayer currentPlayerWithLA)}
    * @param cardPlayed - type of development card played as in {@link SOCDevCardConstants}
    */
   public void updatePlayedDevCard(int cardPlayed) {
	   int[] devCards = playersInfo.get(Integer.valueOf(ourPlayerNumber)).getDevCards();
	   switch(cardPlayed) {
	   	case SOCDevCardConstants.ROADS:
	   		devCards[1]--;
	   		break;
	   	case SOCDevCardConstants.DISC:
	   		devCards[3]--;
	   		break;
	   	case SOCDevCardConstants.MONO:
	   		devCards[5]--;
	   		break;
		   
	   }		  
   }
   
   /**
    * Change amount of resource by given amounts
    * 
    * @param resources - index of resources to update starting with 0
    * @param amounts - positive or negative amounts by which th resource change
    */
   public void updateAddSubstractResources(int[] resources, int[] amounts) {
	   int[] ourResources = playersInfo.get(Integer.valueOf(ourPlayerNumber)).getResources();
	   for (int i=0; i <resources.length; i++) {
		   ourResources[resources[i]] += amounts[i];
	   }
	   
   }
   
   /**
    * Used in {@link RLStrategy#searchRollDice()} to add 1 resource for each building
    * that we would receive with the given roll result
    * 
    * @param resources - index of resources to update starting with 1
    */
   public void updateAddResourcesFromConstants(Vector<Integer> resources) {
	   int[] ourResources = playersInfo.get(Integer.valueOf(ourPlayerNumber)).getResources();
	   for (int res : resources){
			//numbers.getResourcesForNumber(i, robberHex) returns resources, where CLAY = 1,
			//so we have to decrease each resource by 1
			ourResources[res-1]++;
		}
   }
   
   
   public void updateSubstractResources(int[] resources, int[] amounts) {
	   int[] ourResources = playersInfo.get(Integer.valueOf(ourPlayerNumber)).getResources();
	   for (int i=0; i <resources.length; i++) {
		   ourResources[resources[i]] -= amounts[i];
	   }
	   
   }
   
   /**
    * used in {@link RLStrategy#searchPlaceRobberOrPlayKnight(SOCState, boolean)} in case
    * when search is done after 7 was rolled to account for changes in resources done
    * while discarding
    * @param pn - player for which to set resources
    * @param resources - set of resources
    */
   public void updateSetResources(SOCPlayer pn, int[] resources) {
	   playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setResources(resources);
   }
   
   
   
   public HashMap<Integer, PlayerState> getPlayersInfo() {
	   return playersInfo;
   }
   
   public SOCState copySOCState() {
	   SOCState copy = new SOCState(ourPlayerNumber);
	   HashMap<Integer, PlayerState> copyPlayersInfo = copy.getPlayersInfo();
	   Iterator<Integer> pnIter = playersInfo.keySet().iterator();

       while (pnIter.hasNext())
       {
    	   Integer key = pnIter.next();
    	   copyPlayersInfo.put(key, playersInfo.get(key).clone());
       }
	   
	   return copy;
   }
   
   public PlayerState getPlayerState(SOCPlayer pn) {
	   return(playersInfo.get(Integer.valueOf(pn.getPlayerNumber())));
   }
    
}

