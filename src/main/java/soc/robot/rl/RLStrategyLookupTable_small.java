package soc.robot.rl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import soc.game.SOCGame;
import soc.game.SOCInventory;
import soc.game.SOCPlayer;
import soc.robot.rl.RLStrategy.CustomPair;

public class RLStrategyLookupTable_small extends RLStrategyLookupTable {
	
	int counter=0;

	public RLStrategyLookupTable_small(SOCGame game, int pn) {
		super(game, pn);
		
		/*RES MISSING*/
		state = new SOCState_resMissing(ourPlayerNumber, players);
	    state.updateAll(players, board); 

	    //With StateValueFunction we don't add states at the beginning, 
	    //the svf will be set in another function
	    
//        /* adding to memory the state at the beginning of the game */
//	    ArrayList<CustomPair> pn_states = new ArrayList<CustomPair>();
//    	int[] secondState = new int[8];
//
//    	Iterator<SOCPlayer> playersIter = players.values().iterator();
//    	
//	    while (playersIter.hasNext()) {
//	    	SOCPlayer player = playersIter.next();
//    		SOCStateArray playerState = new SOCStateArray(state.getState(player));
//    		int points = player.getTotalVP();
//    		Float value =  (float)(rnd.nextGaussian()*0.05 + 0.5); //or maybe random?
//    		memory.setState1Value(playerState, new Float[] {value, Float.valueOf(1)});
//			oldState.put(player, playerState);
//    		
//    		int state_value = Math.round(value.floatValue()*10);	
//    		
//
//    		if (player.getPlayerNumber()==ourPlayerNumber) {
//    			secondState[0] = Integer.valueOf(points);
//    			secondState[1] = Integer.valueOf(state_value);
//    		} else {
//    			pn_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(state_value)));
//    		}    		
//    	}
//    	
//    	pn_states.sort(new Comparator<CustomPair>() {
//		    public int compare(CustomPair o1, CustomPair o2) {
//		        return o2.getKey().compareTo(o1.getKey());
//		    }
//		});
//    	
//    	for(int i = 0; i<pn_states.size(); i++) {
//    		secondState[(i+1)*2] = pn_states.get(i).getKey().intValue();
//    		secondState[(i+1)*2 + 1] = pn_states.get(i).getValue().intValue();
//    	}
//    	
//    	Float value =  (float)(rnd.nextGaussian()*0.05 + 0.5); //or maybe random?
//    	oldState2 = new SOCStateArray(secondState);
//    	memory.setState2Value(oldState2, new Float[] {value, Float.valueOf(1)});
	}
	
	protected float getStateValue(SOCState tmpState) {
//		counter++;
    	ArrayList<CustomPair> pn_states = new ArrayList<CustomPair>();
    	int[] secondState = new int[8];

    	Iterator<SOCPlayer> playersIter = players.values().iterator();
    	
	    while (playersIter.hasNext()) {
	    	SOCPlayer pn = playersIter.next();
	    	
    		SOCStateArray playerState = new SOCStateArray(tmpState.getState(pn));
    		
//    		/*DEBUG*/
//    		System.out.println("In pn" + ourPlayerNumber + ". player " 
//    					+ pn.getPlayerNumber() + " state: " 
//    					+ Arrays.toString(playerState.toArray()));
//    		pn.stats();
    		
    		int points = pn.getTotalVP();
    		Float value = null;
    		Float[] valueCount = stateValueFunction.getMemory().getState1Value(playerState);
    		if (valueCount==null) {
    			value = (float)(rnd.nextGaussian()*0.05 + 0.5);
    			/*NO SINGLE STATES in memory*/
    			stateValueFunction.getMemory().setState1Value(playerState, new Float[] {value, Float.valueOf(1)});
    		} else {
    			value = valueCount[0];
    		}
    		
    		int state_value = Math.round(value.floatValue()*10);
    		
    		if (pn.getPlayerNumber()==ourPlayerNumber) {
    			secondState[0] = Integer.valueOf(points);
    			secondState[1] = Integer.valueOf(state_value);
    		} else {
    			pn_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(state_value)));
    		}
    	}
    	
    	
    	pn_states.sort(new Comparator<CustomPair>() {
		    public int compare(CustomPair o1, CustomPair o2) {
		        return o2.getKey().compareTo(o1.getKey());
		    }
		});
    	
    	for(int i = 0; i<pn_states.size(); i++) {
    		secondState[(i+1)*2] = pn_states.get(i).getKey().intValue();
    		secondState[(i+1)*2 + 1] = pn_states.get(i).getValue().intValue();
    	}
    	
//    	/*DEBUG*/
//		System.out.println("In pn" + ourPlayerNumber + " state2: " 
//					+ Arrays.toString(secondState));
    	
    	SOCStateArray secondStateList = new SOCStateArray(secondState);
    	
    	Float value = null;
    	Float[] valueCount = stateValueFunction.getMemory().getState2Value(secondStateList);
		if (valueCount==null) {
			value = (float)(rnd.nextGaussian()*0.05 + 0.5); //or maybe random?
			/*NO SINGLE STATES in memory*/
			stateValueFunction.getMemory().setState2Value(secondStateList, new Float[] {value, Float.valueOf(1)});
		} else {
			value = valueCount[0];
		}
		
		return value.floatValue();

    }
	
	 protected void updateStateValue() {
	    	state.updateAll(players, board);
	    	ArrayList<CustomPair> pn_states = new ArrayList<CustomPair>();
	    	int[] newState = new int[8];

	    	Iterator<SOCPlayer> playersIter = players.values().iterator();
	    	
		    while (playersIter.hasNext()) {
		    	SOCPlayer pn = playersIter.next();
	    		SOCStateArray oldPlayerState = oldState.get(pn);
	    		
	    		Float[] oldPlayerStateValueCount = stateValueFunction.getMemory().getState1Value(oldPlayerState);
	    		Float oldPlayerStateValue = null;
	    		Float oldPlayerStateCount = Float.valueOf(1);
	    		/*obvious mistake, should never be null, but error sometimes thrown*/
	    		if (oldPlayerStateValueCount==null) {
	    			oldPlayerStateValue = (float)(rnd.nextGaussian()*0.05 + 0.5); //or maybe random?
	    			stateValueFunction.getMemory().setState1Value(oldPlayerState, new Float[] {oldPlayerStateValue, Float.valueOf(1)});
	    		} else {
	    			oldPlayerStateValue = oldPlayerStateValueCount[0];
	    			oldPlayerStateCount = oldPlayerStateValueCount[1];
	    		}
	    		
	    		SOCStateArray newPlayerState = new SOCStateArray(state.getState(pn));
	    		
//	    		/*DEBUG*/
//	    		System.out.println("In pn" + ourPlayerNumber + ". player " 
//	    					+ pn.getPlayerNumber() + " state: " 
//	    					+ newPlayerState.toString());
//	    		pn.stats();
	    		
	    		
	    		Float newPlayerStateValue = null;
	    		Float[] newPlayerStateValueCount = stateValueFunction.getMemory().getState1Value(newPlayerState);
	    		if (newPlayerStateValueCount==null) {
	    			newPlayerStateValue = (float)(rnd.nextGaussian()*0.05 + 0.5); //or maybe random?
	    			stateValueFunction.getMemory().setState1Value(newPlayerState, new Float[] {newPlayerStateValue, Float.valueOf(1)});
	    		} else {
	    			newPlayerStateValue = newPlayerStateValueCount[0];
	    		}
	    		
	    		oldPlayerStateValue = (float)(oldPlayerStateValue + alpha * (gamma * newPlayerStateValue - oldPlayerStateValue));
	    		
//	    		/*DEBUG*/
//	    		if (ourPlayerData.getName().equals("rlbot0")) {
////	    			System.out.println("-----------");
//	    			System.out.println("In pn" + ourPlayerNumber + ". player " 
//	    					+ pn.getPlayerNumber() + " state: " 
//	    					+ oldPlayerState.toString());
//		    		System.out.println("In pn" + ourPlayerNumber + ". player " 
//		    					+ pn.getPlayerNumber() + " state: " 
//		    					+ newPlayerState.toString());
//		    		System.out.println("new state val: " + newPlayerStateValue + 
//		    				" old state val: " + oldPlayerStateValue + " count: " + oldPlayerStateCount);
//		    		
//	    		}
	    		
	    		
	    		stateValueFunction.getMemory().setState1Value(oldPlayerState, new Float[] {oldPlayerStateValue, ++oldPlayerStateCount});
	    		oldState.put(pn, newPlayerState);
	    		
	    		//calculation to get new state array
	    		int points = pn.getTotalVP();
	    		int roundedPlayerStateValue = Math.round(newPlayerStateValue.floatValue()*10);
	    		
	    		if (pn.getPlayerNumber()==ourPlayerNumber) {
	    			newState[0] = Integer.valueOf(points);
	    			newState[1] = Integer.valueOf(roundedPlayerStateValue);
	    		} else {
	    			pn_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(roundedPlayerStateValue)));
	    		}
	    	}
	    	
	    	pn_states.sort(new Comparator<CustomPair>() {
			    public int compare(CustomPair o1, CustomPair o2) {
			        return o2.getKey().compareTo(o1.getKey());
			    }
			});
	    	for(int i = 0; i<pn_states.size(); i++) {
	    		newState[(i+1)*2] = pn_states.get(i).getKey().intValue();
	    		newState[(i+1)*2 + 1] = pn_states.get(i).getValue().intValue();
	    	}
	    	SOCStateArray newStateList = new SOCStateArray(newState);
	    	
	    	/*DEBUG*/
//    		System.out.println("In pn" + ourPlayerNumber + " state2: " 
//    					+ newStateList.toString());	    	
	    	
	    	Float newStateValue = null;
	    	Float[] newStateValueCount = stateValueFunction.getMemory().getState2Value(newStateList);
			if (newStateValueCount==null) {
//				newStateValue = Float.valueOf(0.5); //or maybe random?
				newStateValue = (float)(rnd.nextGaussian()*0.05 + 0.5);
				stateValueFunction.getMemory().setState2Value(newStateList, new Float[] {newStateValue, Float.valueOf(1)});
			} else {
				newStateValue = newStateValueCount[0];
			}
	    	
			Float oldStateValue = null;
			Float oldStateCount = Float.valueOf(1);
			Float[] oldStateValueCount = stateValueFunction.getMemory().getState2Value(oldState2);
			if (oldStateValueCount==null) {
				oldStateValue = (float)(rnd.nextGaussian()*0.05 + 0.5);
			} else {
				oldStateValue = oldStateValueCount[0];
				oldStateCount = oldStateValueCount[1];	
			}

	    	
	    	oldStateValue = (float)(oldStateValue + alpha * (gamma * newStateValue  - oldStateValue));
	    	
	    	stateValueFunction.getMemory().setState2Value(oldState2, new Float[] {oldStateValue, ++oldStateCount});
	    	oldState2 = newStateList;	    	
	    	currentStateValue = newStateValue;
	    }
	 
	 protected void updateStateValue(int winner) {
	    	state.updateAll(players, board);
	    	ArrayList<CustomPair> pn_states = new ArrayList<CustomPair>();
	    	int[] newState = new int[8];
	    	int reward = 0;

	    	Iterator<SOCPlayer> playersIter = players.values().iterator();
	    	
		    while (playersIter.hasNext()) {
		    	SOCPlayer pn = playersIter.next();
	    		SOCStateArray oldPlayerState = oldState.get(pn);

	    		Float[] oldPlayerStateValueCount = stateValueFunction.getMemory().getState1Value(oldPlayerState);
	    		Float oldPlayerStateValue = null;
	    		Float oldPlayerStateCount = Float.valueOf(1);
	    		/*obvious mistake, should never be null, but error sometimes thrown*/
	    		if (oldPlayerStateValueCount==null) {
	    			oldPlayerStateValue = (float)(rnd.nextGaussian()*0.05 + 0.5); //or maybe random?
	    			stateValueFunction.getMemory().setState1Value(oldPlayerState, new Float[] {oldPlayerStateValue, Float.valueOf(1)});
	    		} else {
	    			oldPlayerStateValue = oldPlayerStateValueCount[0];
	    			oldPlayerStateCount = oldPlayerStateValueCount[1];
	    		}
	    		
	    		SOCStateArray newPlayerState = new SOCStateArray(state.getState(pn));
	    		
//	    		/*DEBUG*/
//	    		System.out.println("-----------");
//	    		System.out.println("In pn" + ourPlayerNumber + ". player " 
//	    					+ pn.getPlayerNumber() + " state: " 
//	    					+ newPlayerState.toString());
//	    		pn.stats();
//	    		int rob = board.getRobberHex();
//	    		System.out.println("numbers: " + pn.getNumbers().toString() + " rob hex:" + rob + 
//	    				" num: " + board.getNumberOnHexFromCoord(rob) + 
//	    				" res:" + board.getHexTypeFromCoord(rob));
//	    		System.out.println("dev cards new: " + 
//	    				pn.getInventory().getByState(SOCInventory.NEW).size() +
//	    				" playable: " + pn.getInventory().getByState(SOCInventory.PLAYABLE).size() +
//	    				" kept: " + pn.getInventory().getByState(SOCInventory.KEPT).size()
//	    				);
	    		
	    		if (winner == pn.getPlayerNumber()) {
	    			reward = 1;
	    		} else {
	    			reward = 0;
	    		}
	    		
	    		Float newPlayerStateValue = Float.valueOf(reward);
	    		Float[] newPlayerStateValueCount = stateValueFunction.getMemory().getState1Value(newPlayerState);
	    		if (newPlayerStateValueCount==null) {
	    			stateValueFunction.getMemory().setState1Value(newPlayerState, new Float[] {newPlayerStateValue, Float.valueOf(1)});
	    		} else {
	    			stateValueFunction.getMemory().setState1Value(newPlayerState, new Float[] {newPlayerStateValue, ++newPlayerStateValueCount[1]});
	    		}
	    		
	    		oldPlayerStateValue = (float)(oldPlayerStateValue + alpha * (gamma * newPlayerStateValue - oldPlayerStateValue));
	    		
	    		stateValueFunction.getMemory().setState1Value(oldPlayerState, new Float[] {oldPlayerStateValue, ++oldPlayerStateCount});
	    		oldState.put(pn, newPlayerState);
	    		
//	    		/*DEBUG*/
//	    		if (ourPlayerData.getName().equals("rlbot0")) {
//	    			System.out.println("-----------");
//	    			System.out.println("In pn" + ourPlayerNumber + ". player " 
//	    					+ pn.getPlayerNumber() + " state: " 
//	    					+ oldPlayerState.toString());
//		    		System.out.println("In pn" + ourPlayerNumber + ". player " 
//		    					+ pn.getPlayerNumber() + " state: " 
//		    					+ newPlayerState.toString());
//		    		System.out.println("new state val: " + newPlayerStateValue + 
//		    				" old state val: " + oldPlayerStateValue + " count: " + oldPlayerStateCount);
//	    		}
//	    		System.out.println("new state val: " + newPlayerStateValue + " old state val: " + oldPlayerStateValue);
	    		
	    		//calculation to get new state array
	    		int points = pn.getTotalVP();
	    		int roundedPlayerStateValue = Math.round(newPlayerStateValue.floatValue()*10);
	    		
	    		if (pn.getPlayerNumber()==ourPlayerNumber) {
	    			newState[0] = Integer.valueOf(points);
	    			newState[1] = Integer.valueOf(roundedPlayerStateValue);
	    		} else {
	    			pn_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(roundedPlayerStateValue)));
	    		}
	    		
	    	}

	    	pn_states.sort(new Comparator<CustomPair>() {
			    public int compare(CustomPair o1, CustomPair o2) {
			        return o2.getKey().compareTo(o1.getKey());
			    }
			});
	    	for(int i = 0; i<pn_states.size(); i++) {
	    		newState[(i+1)*2] = pn_states.get(i).getKey().intValue();
	    		newState[(i+1)*2 + 1] = pn_states.get(i).getValue().intValue();
	    	}
	    	SOCStateArray newStateList = new SOCStateArray(newState);
	    	
//	    	/*DEBUG*/
//    		System.out.println("In pn" + ourPlayerNumber + " state2: " 
//    					+ Arrays.toString(newStateList.toArray()));	    	
	    	
    		if (winner == ourPlayerNumber) {
    			reward = 1;
    		} else {
    			reward = 0;
    		}
	    	Float newStateValue = Float.valueOf(reward);
	    	Float[] newStateValueCount = stateValueFunction.getMemory().getState2Value(newStateList);
			if (newStateValueCount==null) {
				stateValueFunction.getMemory().setState2Value(newStateList, new Float[] {newStateValue, Float.valueOf(1)});
			} else {
				stateValueFunction.getMemory().setState2Value(newStateList, new Float[] {newStateValue, ++newStateValueCount[1]});
			}

			Float oldStateValue = null;
			Float oldStateCount = Float.valueOf(1);
			Float[] oldStateValueCount = stateValueFunction.getMemory().getState2Value(oldState2);
			if (oldStateValueCount==null) {
				oldStateValue = (float)(rnd.nextGaussian()*0.05 + 0.5);
			} else {
				oldStateValue = oldStateValueCount[0];
				oldStateCount = oldStateValueCount[1];	
			}

	    	oldStateValue = (float)(oldStateValue + alpha * (gamma * newStateValue  - oldStateValue));
	    	
	    	stateValueFunction.getMemory().setState2Value(oldState2, new Float[] {oldStateValue, ++oldStateCount});
	   }
	 
	 public void updateReward() {
			
			SOCPlayer winPn = game.getPlayerWithWin();
			int reward = 0;
			
			if (winPn!=null) {
				/*DEBUG*/
				System.out.println(game.getName() + " winner is: " + game.getPlayerWithWin().getName());

				int winPnNum = winPn.getPlayerNumber();
				if (winPnNum==ourPlayerNumber) {
					reward = 1;
				}
			}

			updateStateValue(reward);
		}
	 
	 public void updateReward(int winner) {
		 updateStateValue(winner);
	 }
	 
	 public void printCounter() {
		 System.out.println("In pn" + ourPlayerNumber + " counter: " + counter);
	 }

}
