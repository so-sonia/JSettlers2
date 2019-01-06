package soc.robot.rl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import soc.game.SOCDevCardConstants;
import soc.game.SOCInventory;
import soc.game.SOCPlayer;

public class SOCState_small extends SOCState {

	public SOCState_small(int pln, HashMap<Integer, SOCPlayer> players) {
		super(pln, players);

		Iterator<SOCPlayer> playersIter = players.values().iterator();

	       while (playersIter.hasNext())
	       {
	           addPlayerState(playersIter.next().getPlayerNumber());
	       }
	}

	public SOCState_small(int pln) {
		super(pln);
		// TODO Auto-generated constructor stub
	}
	
	public void addPlayerState(int playerNumber) {
    	SOCPlayerState state = new SOCPlayerState_small();
        playersInfo.put(new Integer(playerNumber), state);
    }
	
	public void updateDevCards(SOCPlayer pn) {	
		int[] devCards = new int[2];
		SOCInventory inventory = pn.getInventory();
    	devCards[0] = inventory.getTotal();
    	SOCPlayerState statepn = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
    	statepn.setDevCards(devCards);
	}
	
	 public void updateMyDevCards(SOCPlayer pn) {
		 int[] devCards = new int[2];
		 SOCInventory inventory = pn.getInventory();
	     devCards[0] = inventory.getTotal();
	   	 SOCPlayerState statepn = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
	   	 statepn.setDevCards(devCards);
	 }
	 
	 public void updateBuyDevCard(SOCPlayer pn, int boughtCard) {
		 int[] devCards = playersInfo.get(ourPlayerNumber).getDevCards();
		  devCards[0]++;
		  if (boughtCard == 0) {		   
			  playersInfo.get(ourPlayerNumber).addPoints(1);
		  }	
	 }
	 
	 public void undoUpdateBuyDevCard(SOCPlayer pn, int boughtCard) {
		 int[] devCards = playersInfo.get(ourPlayerNumber).getDevCards();
		 devCards[0]--;
		 if (boughtCard == 0) {		   
		   playersInfo.get(ourPlayerNumber).substractPoints(1);
		 }	
	 }	 
	 
	 public void updatePlayedDevCard(int cardPlayed) {
		 int[] devCards = playersInfo.get(Integer.valueOf(ourPlayerNumber)).getDevCards();
		 devCards[0]--;
	 }
	 
	 public void updatePlayedKnightCard(SOCPlayer pn, boolean willGetLA, SOCPlayer currentPlayerWithLA) {
		   SOCPlayerState pnState = getPlayerState(pn);
		   int[] devCards = pnState.getDevCards();
			//decrease the number of old knight cards in our hand
			devCards[0]--;
			pnState.setPlayedKnights(pnState.getPlayedKnights() + 1);
			/*change the owner of the largest army award if that's a case */
			if (willGetLA) {
				pnState.setHasLargestArmy(1);
				if (currentPlayerWithLA!=null) {
					getPlayerState(currentPlayerWithLA).setHasLargestArmy(0);
				}
			}
	   }
	   
	 
	 public void updatePorts(SOCPlayer pn){
	    	int[] ports = new int[2];
	    	boolean[] portFlags = pn.getPortFlags();
	    	ports[0] = portFlags[0] ? 1 : 0 ;
	    	ports[1] = 0;
	    	
	    	for (int i=1; i < portFlags.length; i++) {
	    		if (portFlags[i]) {
	    			ports[1] = 1;
	    			break;
	    		}
	    	}
	    	
	    	playersInfo.get(Integer.valueOf(pn.getPlayerNumber())).setPorts(ports);
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
	 
	 public int[] getState(SOCPlayer pn) {
		   SOCPlayerState pnState = playersInfo.get(Integer.valueOf(pn.getPlayerNumber()));
	      
	       return pnState.getStateArray();
	   }
	 
	   public SOCState copySOCState() {
		   SOCState copy = new SOCState_small(ourPlayerNumber);
		   HashMap<Integer, SOCPlayerState> copyPlayersInfo = copy.getPlayersInfo();
		   Iterator<Integer> pnIter = playersInfo.keySet().iterator();

	       while (pnIter.hasNext())
	       {
	    	   Integer key = pnIter.next();
	    	   copyPlayersInfo.put(key, new SOCPlayerState_small(playersInfo.get(key)));
	       }
		   
		   return copy;
	   }
}
