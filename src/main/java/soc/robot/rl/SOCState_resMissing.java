package soc.robot.rl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import soc.game.SOCBoard;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCInventory;
import soc.game.SOCPlayer;
import soc.game.SOCPlayerNumbers;
import soc.game.SOCResourceConstants;

public class SOCState_resMissing extends SOCState {

	public SOCState_resMissing(int pln, HashMap<Integer, SOCPlayer> players) {
		super(pln, players);
		
		Iterator<SOCPlayer> playersIter = players.values().iterator();

	       while (playersIter.hasNext())
	       {
	           addPlayerState(playersIter.next().getPlayerNumber());
	       }

	}

	public SOCState_resMissing(int pln) {
		super(pln);
	}
	
	public void addPlayerState(int playerNumber) {
    	SOCPlayerState state = new SOCPlayerState_resMissing();
        playersInfo.put(new Integer(playerNumber), state);
    }
	
	public void updateDevCards(SOCPlayer pn) {	
		int[] devCards = new int[2];
		SOCInventory inventory = pn.getInventory();
    	devCards[0] = inventory.getTotal();
    	PlayerState statepn = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
    	statepn.setDevCards(devCards);
	}
	
	 public void updateMyDevCards(SOCPlayer pn) {
		 int[] devCards = new int[2];
		 SOCInventory inventory = pn.getInventory();
	     devCards[0] = inventory.getTotal();
	   	 PlayerState statepn = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
	   	 statepn.setDevCards(devCards);
	 }
	 
	 /**
	     * For each resource type we calculate probabilities with which the player can take this resource.
	     * Probability increases if player has more than one settlement/city adjacent to this resource
	     * and if he has a port. Being blocked by robber decreases the probability.
	     * {@link SOCPlayerNumbers} from {@link SOCPlayer#getNumbers()} is used.
	     * 
	     * @param pn player for whom to update info
	     * @param robberHex int value of hex on which robber is placed
	     */
	    public void updateResourceProbabilitiesAndUniqueNumbers(SOCPlayer pn, int robberHex) {
	    	float[] resourceNumbersRaw = new float[5];
	    	float[] resourceNumbersFinal = new float[5];
	    	int[] resourceNumbers = new int[5];
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
	            
	            for (Integer number : numbersRes) {
	            	totalProbability += FLOAT_VALUES[number.intValue()];
	            }
	            
	            if (totalProbability != 0.0f)
	            {
	            	//SOCResourceConstants.CLAY = 1, therefore we take -1 to fill the array
//	            	resourceNumbers[resource-1] = Math.round(totalProbability*30);
	            	resourceNumbersRaw[resource-1] = totalProbability;
	            	resourceNumbersFinal[resource-1] = totalProbability;
	            }
	            
	        }
	    	
	    	/*We add to probability if player has ports*/
	    	boolean[] portFlags = pn.getPortFlags();
	    	
	    	if(portFlags[0]) {
	    		for (int res = 0; res<5 ; res ++)
		        {
	    			float resAdd = (float) Math.pow(resourceNumbersRaw[res], 3);
	    			
	    			for (int i = 0; i<5 ; i ++) {
	    				if (i==res)
	    					continue;
	    				resourceNumbersFinal[res] += resAdd;
	    			}
	    			
		        }
	    	}
	    	
	    	for (int j=1; j < portFlags.length; j++) {
	    		if (portFlags[j]) {
	    			//SOCBoard.CLAY_PORT = 1, therefore we take -1 to get the port resource
	    			int res = j-1;
	    			float resAdd = (float) Math.pow(resourceNumbersRaw[res], 2);
	    			
	    			for (int i = 0; i<5 ; i ++) {
	    				if (i==res)
	    					continue;
	    				resourceNumbersFinal[res] += resAdd;
	    			}

	    		}
	    	}
	    	
	    	for (int i = 0; i<5 ; i ++) {
	    		resourceNumbers[i] = Math.round(resourceNumbersFinal[i]*15);
			}
	    	
	    	PlayerState state = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
	    	state.setResourceProbabilities(resourceNumbers);
	    }
	    
	    public void updateResMissing(SOCPlayer pn) {
	    	updateResMissing(getPlayerState(pn));
	    }
	    
	    public void updateResMissing(PlayerState pnState) {
	    	int[] ourResources = pnState.getResources();
	    	int cityMissing = 0;
	    	if (ourResources[SOCResourceConstants.ORE -1]<3) {
	    		cityMissing += 3 - ourResources[SOCResourceConstants.ORE -1];
	    	}
	    	if (ourResources[SOCResourceConstants.WHEAT -1]<2) {
	    		cityMissing += 2 - ourResources[SOCResourceConstants.WHEAT -1];
	    	}
	    	pnState.setResMissingCity(cityMissing);
	    	
	    	int settlementMissing = 0;
	    	int roadMissing = 0;
	    	int devCardMissing = 0;
	    	if (ourResources[SOCResourceConstants.SHEEP -1]<1) {
	    		settlementMissing++;
	    		devCardMissing++;
	    	}
	    	if (ourResources[SOCResourceConstants.WHEAT -1]<1) {
	    		settlementMissing++;
	    		devCardMissing++;
	    	}
	    	if (ourResources[SOCResourceConstants.WOOD -1]<1) {
	    		settlementMissing++;
	    		roadMissing++;
	    	}
	    	if (ourResources[SOCResourceConstants.CLAY -1]<1) {
	    		settlementMissing++;
	    		roadMissing++;
	    	}
	    	if (ourResources[SOCResourceConstants.ORE -1]<1) {
	    		devCardMissing++;
	    	}
	    	pnState.setResMissingSettlement(settlementMissing);
	    	pnState.setResMissingRoad(roadMissing);
	    	pnState.setResMissingDevCard(devCardMissing);	    	
	    }
	    
	    
	   public void updateRoadsMissngLR(SOCPlayer pn) {
		   SOCPlayer pnLR = pn.getGame().getPlayerWithLongestRoad();
		   int firstLR = 4;
		   if (pnLR!=null) {
			   pnLR.calcLongestRoad2();
			   firstLR = pnLR.getLongestRoadLength();
		   }
		   int secondLR = 0;
		   int pnLongestRoad = 0;
		   int[] roadsMissngLR = new int[4];
//		   SOCPlayer[] players = pn.getGame().getPlayers();
		   for (SOCPlayer player : pn.getGame().getPlayers()) {
			   int pnNumber = player.getPlayerNumber();
			   player.calcLongestRoad2();
			   pnLongestRoad = player.getLongestRoadLength();
			   roadsMissngLR[pnNumber] = firstLR - pnLongestRoad + 1;
			   
			   if (pnLR!=null) {
				   if (pnNumber!=pnLR.getPlayerNumber()) {
					   if (pnLongestRoad>secondLR)
						   secondLR = pnLongestRoad;
				   }
			   }
			   
		   }
		   
		   if (pnLR!=null)
			   roadsMissngLR[pnLR.getPlayerNumber()] = secondLR - firstLR;
		   
		   Iterator<Integer> playersIter = playersInfo.keySet().iterator();

		   while (playersIter.hasNext())
	       {    
	           Integer pnNumber = playersIter.next();
	           playersInfo.get(pnNumber)
	           		.setRoadsMissngLR(roadsMissngLR[pnNumber.intValue()]);
	       }
		   
	   }
	   
	   public void updateCardsMissingLA(SOCPlayer pn) {
		   SOCPlayer pnLA = pn.getGame().getPlayerWithLargestArmy();
		   int firstLA = 2;
		   if (pnLA!=null) {
			   firstLA = pnLA.getNumKnights();
		   }
		   int secondLA = 0;
		   int pnLargestArmy = 0;
		   int[] cardsMissingLA = new int[4];
//		   SOCPlayer[] players = pn.getGame().getPlayers();
		   for (SOCPlayer player : pn.getGame().getPlayers()) {
			   int pnNumber = player.getPlayerNumber();
			   pnLargestArmy = player.getNumKnights();
			   cardsMissingLA[pnNumber] = firstLA - pnLargestArmy + 1;
			   
			   if (pnLA!=null) {
				   if (pnNumber!=pnLA.getPlayerNumber()) {
					   if (pnLargestArmy>secondLA)
						   secondLA = pnLargestArmy;
				   }
			   }
			   
		   }
		   
		   if (pnLA!=null)
			   cardsMissingLA[pnLA.getPlayerNumber()] = secondLA - firstLA;
		   
		   Iterator<Integer> playersIter = playersInfo.keySet().iterator();

		   while (playersIter.hasNext())
	       {    
	           Integer pnNumber = playersIter.next();
	           playersInfo.get(pnNumber)
	           		.setCardsMissingLA(cardsMissingLA[pnNumber.intValue()]);
	       }
	   }
	  
	   
	 /**
	    * After placing a settlement information on resource probabilities, points, 
	    * Longest road and player with longest road award (placing a settlement may destroy 
	    * somebody's longest road and award will change ownership).
	    * @param pn
	    * @param board
	    */
	   public void updatePlaceSettlement(SOCPlayer pn,  SOCBoard board) {
		   updateResourceProbabilitiesAndUniqueNumbers(pn, board.getRobberHex());
		   updatePoints(pn);
		   
		   if (pn.getPlayerNumber()==ourPlayerNumber) {
			   updateRoadsMissngLR(pn);
		   }
	   }
	   
	   /**
	    * After placing the road, information about points, longest road length, player
	    * with the longest road must be updated
	    * @param pn - player number for whom the information has to be updated
	    */
	   public void updatePlaceRoad(SOCPlayer pn) {
		   updatePoints(pn);
		   if (pn.getPlayerNumber()==ourPlayerNumber) {
			   updateRoadsMissngLR(pn);
		   }
	   }
	   
	   public void updatePlaceCity(SOCPlayer pn,  SOCBoard board) {
		   updateResourceProbabilitiesAndUniqueNumbers(pn, board.getRobberHex());
		   updatePoints(pn);
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
			    * unknown type (we check that opponent ha resources of unknown type before calling this function)
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
		   
		   updateResMissing(op);
		   updateResMissing(ourPlayer);
		   
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
		   devCards[0]++;
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
			 devCards[0]--;
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
			devCards[0]--;
			/*change the owner of the largest army award if that's a case */
			if (pn.getPlayerNumber()==ourPlayerNumber) {
				updateCardsMissingLA(pn);
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
		   devCards[0]--;		  
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
		   
		   updateResMissing(playersInfo.get(Integer.valueOf(ourPlayerNumber)));   
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
		   
		   updateResMissing(playersInfo.get(Integer.valueOf(ourPlayerNumber)));
	   }
	   
	   
	 public void updateSubstractResources(int[] resources, int[] amounts) {
		   int[] ourResources = playersInfo.get(Integer.valueOf(ourPlayerNumber)).getResources();
		   for (int i=0; i <resources.length; i++) {
			   ourResources[resources[i]] -= amounts[i];
		   }
		   
		   updateResMissing(playersInfo.get(Integer.valueOf(ourPlayerNumber)));
		   
	   }
	   
	 public void updateResources(SOCPlayer pn, boolean otherPlayer) {
	    	int[] resources = pn.getResources().getAmounts(true);
//	    	/*DEBUG*/
//	    	System.out.println("resource " + Arrays.toString(resources));  	
	    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setResources(
	    			Arrays.copyOf(resources, resources.length));
	 }
	 
	 
	 public void updateResourcesByCopy(SOCPlayer pn, boolean otherPlayer) {
	    	int[] resources = pn.getResources().getAmounts(true);    	    	
	    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).copyResources(resources);
	  }
	 
	 public void updatePlayer(SOCPlayer pn,  SOCBoard board) {
		   /* development cards and resources are updated differently for our and other player */
		   if (pn.getPlayerNumber() == ourPlayerNumber) {
			   updateMyDevCards(pn);
			   updateResources(pn, false);
			   updateCardsMissingLA(pn);
			   updateRoadsMissngLR(pn);
		   }
		   else {   
			   updateDevCards(pn);
			   updateResources(pn, true);
		   }
		   
		   updateResMissing(pn);
		   /*all the other state fields are updated in the same way*/
		   updateResourceProbabilitiesAndUniqueNumbers(pn, board.getRobberHex());
		   updateBoardInfoOnBuildings(pn, board, board.getRobberHex());
		   updatePoints(pn);
	   }

	 public int[] getState(SOCPlayer pn) {
		   PlayerState pnState = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
		   int[] stateArray = pnState.getStateArray();
	      
	       return stateArray;
	   }
	 
	   public SOCState copySOCState() {
		   SOCState copy = new SOCState_resMissing(ourPlayerNumber);
		   HashMap<Integer, PlayerState> copyPlayersInfo = copy.getPlayersInfo();
		   Iterator<Integer> pnIter = playersInfo.keySet().iterator();

	       while (pnIter.hasNext())
	       {
	    	   Integer key = pnIter.next();
	    	   copyPlayersInfo.put(key, playersInfo.get(key).clone());
	       }
		   
		   return copy;
	   }

}
