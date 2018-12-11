package soc.robot.rl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.robot.rl.RLStrategy.CustomPair;

public class RLStrategyLookupTable_small extends RLStrategyLookupTable {

	public RLStrategyLookupTable_small(SOCGame game, int pn, StateMemoryLookupTable memory) {
		super(game, pn, memory);
		
		state = new SOCState_small(ourPlayerNumber, players);
	    state.updateAll(players, board);      

        /* adding to memory the state at the beginning of the game */
	    ArrayList<CustomPair> pn_states = new ArrayList<CustomPair>();
    	int[] secondState = new int[8];

    	Iterator<SOCPlayer> playersIter = players.values().iterator();
    	
	    while (playersIter.hasNext()) {
	    	SOCPlayer player = playersIter.next();
    		List<Integer> playerState = Arrays.stream(state.getState(player)).boxed().collect(Collectors.toList());
    		int points = player.getTotalVP();
    		Double value = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
    		memory.setState1Value(playerState, new Double[] {value, Double.valueOf(1.0)});
			oldState.put(player, playerState);
    		
    		int state_value = Math.round(value.floatValue()*10);	
    		

    		if (player.getPlayerNumber()==ourPlayerNumber) {
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
    	
    	Double value = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
    	oldState2 = Arrays.stream(secondState).boxed().collect(Collectors.toList());
    	memory.setState2Value(oldState2, new Double[] {value, Double.valueOf(1.0)});
	}
	
	protected float getStateValue(SOCState tmpState) {
    	ArrayList<CustomPair> pn_states = new ArrayList<CustomPair>();
    	int[] secondState = new int[8];

    	Iterator<SOCPlayer> playersIter = players.values().iterator();
    	
	    while (playersIter.hasNext()) {
	    	SOCPlayer pn = playersIter.next();
	    	
    		List<Integer> playerState = Arrays.stream(tmpState.getState(pn)).boxed().collect(Collectors.toList());
    		
//    		/*DEBUG*/
//    		System.out.println("In pn" + ourPlayerNumber + ". player " 
//    					+ pn.getPlayerNumber() + " state: " 
//    					+ Arrays.toString(playerState.toArray()));
//    		pn.stats();
    		
    		int points = pn.getTotalVP();
    		Double value = null;
    		Double[] valueCount = memory.getState1Value(playerState);
    		if (valueCount==null) {
    			value = new Random().nextGaussian()*0.05 + 0.5;
    			memory.setState1Value(playerState, new Double[] {value, Double.valueOf(1.0)});
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
    	
    	List<Integer> secondStateList = Arrays.stream(secondState).boxed().collect(Collectors.toList());
    	
    	Double value = null;
    	Double[] valueCount = memory.getState2Value(secondStateList);
		if (valueCount==null) {
			value = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
			memory.setState2Value(secondStateList, new Double[] {value, Double.valueOf(1.0)});
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
	    		List<Integer> oldPlayerState = oldState.get(pn);
	    		
	    		Double[] oldPlayerStateValueCount = memory.getState1Value(oldPlayerState);
	    		Double oldPlayerStateValue = null;
	    		Double oldPlayerStateCount = Double.valueOf(1.0);
	    		/*obvious mistake, should never be null, but error sometimes thrown*/
	    		if (oldPlayerStateValueCount==null) {
	    			oldPlayerStateValue = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
	    			memory.setState1Value(oldPlayerState, new Double[] {oldPlayerStateValue, Double.valueOf(1.0)});
	    		} else {
	    			oldPlayerStateValue = oldPlayerStateValueCount[0];
	    			oldPlayerStateCount = oldPlayerStateValueCount[1];
	    		}
	    		
	    		List<Integer> newPlayerState = Arrays.stream(state.getState(pn)).boxed().collect(Collectors.toList());
	    		
//	    		/*DEBUG*/
//	    		System.out.println("In pn" + ourPlayerNumber + ". player " 
//	    					+ pn.getPlayerNumber() + " state: " 
//	    					+ Arrays.toString(newPlayerState.toArray()));
//	    		pn.stats();
	    		
	    		
	    		Double newPlayerStateValue = null;
	    		Double[] newPlayerStateValueCount = memory.getState1Value(newPlayerState);
	    		if (newPlayerStateValueCount==null) {
	    			newPlayerStateValue = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
	    			memory.setState1Value(newPlayerState, new Double[] {newPlayerStateValue, Double.valueOf(1.0)});
	    		} else {
	    			newPlayerStateValue = newPlayerStateValueCount[0];
	    		}
	    		
	    		oldPlayerStateValue = oldPlayerStateValue + alpha * (gamma * newPlayerStateValue - oldPlayerStateValue);
	    		
	    		memory.setState1Value(oldPlayerState, new Double[] {oldPlayerStateValue, ++oldPlayerStateCount});
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
	    	List<Integer> newStateList = Arrays.stream(newState).boxed().collect(Collectors.toList());
	    	
//	    	/*DEBUG*/
//    		System.out.println("In pn" + ourPlayerNumber + " state2: " 
//    					+ Arrays.toString(newStateList.toArray()));	    	
	    	
	    	Double newStateValue = null;
	    	Double[] newStateValueCount = memory.getState2Value(newStateList);
			if (newStateValueCount==null) {
//				newStateValue = Double.valueOf(0.5); //or maybe random?
				newStateValue = new Random().nextGaussian()*0.05 + 0.5;
				memory.setState2Value(newStateList, new Double[] {newStateValue, Double.valueOf(1.0)});
			} else {
				newStateValue = newStateValueCount[0];
			}
	    	
			Double oldStateValue = null;
			Double oldStateCount = Double.valueOf(1.0);
			Double[] oldStateValueCount = memory.getState2Value(oldState2);
			if (oldStateValueCount==null) {
				oldStateValue = new Random().nextGaussian()*0.05 + 0.5;
			} else {
				oldStateValue = oldStateValueCount[0];
				oldStateCount = oldStateValueCount[1];	
			}

	    	
	    	oldStateValue = oldStateValue + alpha * (gamma * newStateValue  - oldStateValue);
	    	
	    	memory.setState2Value(oldState2, new Double[] {oldStateValue, ++oldStateCount});
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
	    		List<Integer> oldPlayerState = oldState.get(pn);

	    		Double[] oldPlayerStateValueCount = memory.getState1Value(oldPlayerState);
	    		Double oldPlayerStateValue = null;
	    		Double oldPlayerStateCount = Double.valueOf(1.0);
	    		/*obvious mistake, should never be null, but error sometimes thrown*/
	    		if (oldPlayerStateValueCount==null) {
	    			oldPlayerStateValue = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
	    			memory.setState1Value(oldPlayerState, new Double[] {oldPlayerStateValue, Double.valueOf(1.0)});
	    		} else {
	    			oldPlayerStateValue = oldPlayerStateValueCount[0];
	    			oldPlayerStateCount = oldPlayerStateValueCount[1];
	    		}
	    		
	    		List<Integer> newPlayerState = Arrays.stream(state.getState(pn)).boxed().collect(Collectors.toList());
	    		
//	    		/*DEBUG*/
//	    		System.out.println("In pn" + ourPlayerNumber + ". player " 
//	    					+ pn.getPlayerNumber() + " state: " 
//	    					+ Arrays.toString(newPlayerState.toArray()));
//	    		pn.stats();
	    		
	    		if (winner == pn.getPlayerNumber()) {
	    			reward = 1;
	    		} else {
	    			reward = 0;
	    		}
	    		
	    		Double newPlayerStateValue = Double.valueOf(reward);
	    		Double[] newPlayerStateValueCount = memory.getState1Value(newPlayerState);
	    		if (newPlayerStateValueCount==null) {
	    			memory.setState1Value(newPlayerState, new Double[] {newPlayerStateValue, Double.valueOf(1.0)});
	    		} else {
	    			memory.setState1Value(newPlayerState, new Double[] {newPlayerStateValue, ++newPlayerStateValueCount[1]});
	    		}
	    		
	    		oldPlayerStateValue = oldPlayerStateValue + alpha * (gamma * newPlayerStateValue - oldPlayerStateValue);
	    		
	    		memory.setState1Value(oldPlayerState, new Double[] {oldPlayerStateValue, ++oldPlayerStateCount});
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
	    	List<Integer> newStateList = Arrays.stream(newState).boxed().collect(Collectors.toList());
	    	
//	    	/*DEBUG*/
//    		System.out.println("In pn" + ourPlayerNumber + " state2: " 
//    					+ Arrays.toString(newStateList.toArray()));	    	
	    	
    		if (winner == ourPlayerNumber) {
    			reward = 1;
    		} else {
    			reward = 0;
    		}
	    	Double newStateValue = Double.valueOf(reward);
	    	Double[] newStateValueCount = memory.getState2Value(newStateList);
			if (newStateValueCount==null) {
				memory.setState2Value(newStateList, new Double[] {newStateValue, Double.valueOf(1.0)});
			} else {
				memory.setState2Value(newStateList, new Double[] {newStateValue, ++newStateValueCount[1]});
			}

			Double oldStateValue = null;
			Double oldStateCount = Double.valueOf(1.0);
			Double[] oldStateValueCount = memory.getState2Value(oldState2);
			if (oldStateValueCount==null) {
				oldStateValue = new Random().nextGaussian()*0.05 + 0.5;
			} else {
				oldStateValue = oldStateValueCount[0];
				oldStateCount = oldStateValueCount[1];	
			}

	    	oldStateValue = oldStateValue + alpha * (gamma * newStateValue  - oldStateValue);
	    	
	    	memory.setState2Value(oldState2, new Double[] {oldStateValue, ++oldStateCount});
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

}
