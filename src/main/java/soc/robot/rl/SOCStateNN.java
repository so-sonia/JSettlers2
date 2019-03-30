package soc.robot.rl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import soc.game.SOCInventory;
import soc.game.SOCPlayer;
import soc.game.SOCPlayerNumbers;
import soc.game.SOCResourceConstants;

public class SOCStateNN extends SOCState {

	public SOCStateNN(int pln, HashMap<Integer, SOCPlayer> players) {
		super(pln, players);

		Iterator<SOCPlayer> playersIter = players.values().iterator();

	       while (playersIter.hasNext())
	       {
	           addPlayerState(playersIter.next().getPlayerNumber());
	       }
	}

	public SOCStateNN(int pln) {
		super(pln);
	}
	
	public void addPlayerState(int playerNumber) {
    	PlayerState state = new SOCPlayerStateNN();
        playersInfo.put(new Integer(playerNumber), state);
    }
	
	public SOCState copySOCState() {
		   SOCState copy = new SOCStateNN(ourPlayerNumber);
		   HashMap<Integer, PlayerState> copyPlayersInfo = copy.getPlayersInfo();
		   Iterator<Integer> pnIter = playersInfo.keySet().iterator();

	       while (pnIter.hasNext())
	       {
	    	   Integer key = pnIter.next();
	    	   copyPlayersInfo.put(key, playersInfo.get(key).clone());
	       }
		   
		   return copy;
	 }
	
	public SOCPlayerStateNN getPlayerState(SOCPlayer pn) {
		   return((SOCPlayerStateNN) playersInfo.get(Integer.valueOf(pn.getPlayerNumber())));
	   }
	
	
	public void updateDevCards(SOCPlayer pn) {	
		int[] devCards = new int[2];
		SOCInventory inventory = pn.getInventory();
    	devCards[0] = inventory.getOld();
    	devCards[1] = inventory.getTotal();
    	PlayerState statepn = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
    	statepn.setDevCards(devCards);
	}
	
	 public void updateMyDevCards(SOCPlayer pn) {
		 updateDevCards(pn);
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
	 
	 
	 public void updateBuyDevCard(SOCPlayer pn, int boughtCard) {
		 int[] devCards = playersInfo.get(ourPlayerNumber).getDevCards();
		  devCards[1]++;
		  if (boughtCard == 0) {		   
			  playersInfo.get(ourPlayerNumber).addPoints(1);
		  }	
	 }
	 
	 public void undoUpdateBuyDevCard(SOCPlayer pn, int boughtCard) {
		 int[] devCards = playersInfo.get(ourPlayerNumber).getDevCards();
		 devCards[1]--;
		 if (boughtCard == 0) {		   
		   playersInfo.get(ourPlayerNumber).substractPoints(1);
		 }	
	 }	 
	 
	 public void updatePlayedDevCard(int cardPlayed) {
		 int[] devCards = playersInfo.get(Integer.valueOf(ourPlayerNumber)).getDevCards();
		 devCards[0]--;
		 devCards[1]--;
	 }
	 
	 public void updatePlayedKnightCard(SOCPlayer pn, boolean willGetLA, SOCPlayer currentPlayerWithLA) {
		   PlayerState pnState = getPlayerState(pn);
		   int[] devCards = pnState.getDevCards();
			//decrease the number of old knight cards in our hand
			devCards[0]--;
			devCards[1]--;
			pnState.setPlayedKnights(pnState.getPlayedKnights() + 1);
			/*change the owner of the largest army award if that's a case */
			if (willGetLA) {
				pnState.setHasLargestArmy(1);
				if (currentPlayerWithLA!=null) {
					getPlayerState(currentPlayerWithLA).setHasLargestArmy(0);
				}
			}
	   }
	 
	  public void updateResourceProbabilitiesAndUniqueNumbers(SOCPlayer pn, int robberHex) {
	    	float[] resourceNumbers = new float[5];
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
	            
	          //SOCResourceConstants.CLAY = 1, therefore we take -1 to fill the array
            	resourceNumbers[resource-1] = totalProbability;
	            
	        }
	    	
	    	PlayerState state = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
	    	state.setResourceProbabilitiesFloat(resourceNumbers);
	    	state.setUniqueNumbers(uniqueNumbers.size());
	    }

	 
	 

}
