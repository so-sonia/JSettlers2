package soc.robot.rl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import soc.game.SOCPlayer;
import soc.game.SOCPlayerNumbers;
import soc.game.SOCResourceConstants;

public class SOCStateNN_opp1 extends SOCState {

	public SOCStateNN_opp1(int pln, HashMap<Integer, SOCPlayer> players) {
		super(pln, players);

		Iterator<SOCPlayer> playersIter = players.values().iterator();

       while (playersIter.hasNext())
       {
           addPlayerState(playersIter.next().getPlayerNumber());
       }
	}

	public SOCStateNN_opp1(int pln) {
		super(pln);
	}
	
	public void addPlayerState(int playerNumber) {
    	PlayerState state = new SOCPlayerStateNN();
        playersInfo.put(new Integer(playerNumber), state);
    }
	
	public SOCState copySOCState() {
		   SOCState copy = new SOCStateNN_opp1(ourPlayerNumber);
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
