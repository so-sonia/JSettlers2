package soc.robot.rl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;


import org.nd4j.linalg.primitives.Pair;

import soc.game.SOCGame;
import soc.game.SOCPlayer;

public class RLStrategyNN_opp1 extends RLStrategyNN {

	public RLStrategyNN_opp1(SOCGame game, int pn) {
		super(game, pn);
		
		state = new SOCStateNN_opp1(ourPlayerNumber, players);
	    state.updateAll(players, board);
	    
	    oldState = new HashMap<SOCPlayer, float[]>();
	    oldState2 = new float[7];
	    
	    float[] ourPlayerArray = state.getPlayerState(ourPlayerData).getNormalizedStateArray();
	    
    	for (SOCPlayer opp : opponents) {   		
    		float[] oppArray = state.getPlayerState(opp).getNormalizedStateArray();
    		float[] result = new float[ourPlayerArray.length + oppArray.length];
    		System.arraycopy(ourPlayerArray, 0, result, 0, ourPlayerArray.length);
    		System.arraycopy(oppArray, 0, result, ourPlayerArray.length, oppArray.length);
    		oldState.put(opp, result);
    	}      
	}

	
protected float getStateValue(SOCState state) {
		
		ArrayList<Pair<Integer, Float>> pn_states = new ArrayList< Pair<Integer, Float> >();
		float[] secondState = new float[7];
		secondState[0]=ourPlayerData.getTotalVP()/10.0f;
		
		float[] ourPlayerArray = state.getPlayerState(ourPlayerData).getNormalizedStateArray();
		
		for (SOCPlayer opp : opponents) {   		
    		float[] oppArray = state.getPlayerState(opp).getNormalizedStateArray();
    		float[] result = new float[ourPlayerArray.length + oppArray.length];
    		System.arraycopy(ourPlayerArray, 0, result, 0, ourPlayerArray.length);
    		System.arraycopy(oppArray, 0, result, ourPlayerArray.length, oppArray.length);
    		int points = opp.getTotalVP();
    		
//    		/*DEBUG*/
//    		System.out.println("In pn" + ourPlayerNumber + ". player " 
//    					+ opp.getPlayerNumber() + " state: " 
//    					+ Arrays.toString(result));
////    		System.out.println(Arrays.toString(state.getPlayerState(ourPlayerData).getStateArray()));
////    		System.out.println(Arrays.toString(state.getPlayerState(opp).getStateArray()));
//    		System.out.println("Arrays lenght: " + result.length);
//    		opp.stats();
    		
    		if (result.length>72) {
    			System.out.println("we have mistake");
    		}
    		
    		float value = stateValueFunction.getStates().getStateValue(result);
    		pn_states.add(new Pair<Integer, Float>(Integer.valueOf(points), Float.valueOf(value)));
    	}   
    	
    	pn_states.sort(new Comparator<Pair<Integer, Float>>() {
		    public int compare(Pair<Integer, Float> o1, Pair<Integer, Float> o2) {
		        return o2.getKey().compareTo(o1.getKey());
		    }
		});
    	
    	for(int i = 0; i<pn_states.size(); i++) {
    		secondState[i*2 + 1] = pn_states.get(i).getKey().floatValue()/10.0f;
    		secondState[i*2 + 2] = pn_states.get(i).getValue().floatValue();
    	}
    	
    	/*DEBUG*/
//		System.out.println("In pn" + ourPlayerNumber + " state2: " 
//					+ Arrays.toString(secondState));
//		System.out.println("");
    	
    	float value =  stateValueFunction.getStates2().getStateValue(secondState);
		
		return value;
	}

	@Override
	protected void updateStateValue() {
		state.updateAll(players, board);
		ArrayList<Pair<Integer, Float>> pn_states = new ArrayList< Pair<Integer, Float> >();
		
		float[] secondState = new float[7];
		secondState[0]=ourPlayerData.getTotalVP()/10.0f;
		
		float[] ourPlayerArray = state.getPlayerState(ourPlayerData).getNormalizedStateArray();
		
		for (SOCPlayer opp : opponents) {   		
    		float[] oppArray = state.getPlayerState(opp).getNormalizedStateArray();
    		float[] result = new float[ourPlayerArray.length + oppArray.length];
    		System.arraycopy(ourPlayerArray, 0, result, 0, ourPlayerArray.length);
    		System.arraycopy(oppArray, 0, result, ourPlayerArray.length, oppArray.length);
    		float[] oldpnState = oldState.get(opp);		   	
    		 stateValueFunction.getStates().store(oldpnState, result, 0.);
    		oldState.put(opp, result);
    		
    		
//    		/*DEBUG*/
//    		System.out.println("In pn" + ourPlayerNumber + ". player " 
//    					+ opp.getPlayerNumber() + " state: " 
//    					+ Arrays.toString(result));
////    		System.out.println(Arrays.toString(state.getPlayerState(ourPlayerData).getStateArray()));
////    		System.out.println(Arrays.toString(state.getPlayerState(opp).getStateArray()));
//    		System.out.println("Arrays lenght: " + result.length);
//    		opp.stats();
    		
    		int points = opp.getTotalVP();    		
    		float value = stateValueFunction.getStates().getStateValue(result);
    		pn_states.add(new Pair<Integer, Float>(Integer.valueOf(points), Float.valueOf(value)));
    		
//    		/*DEBUG*/
//    		System.out.println("opponent state: " + Arrays.toString(pnState));
		}   
  
		pn_states.sort(new Comparator<Pair<Integer, Float>>() {
		    public int compare(Pair<Integer, Float> o1, Pair<Integer, Float> o2) {
		        return o2.getKey().compareTo(o1.getKey());
		    }
		});
    	
    	for(int i = 0; i<pn_states.size(); i++) {
    		secondState[i*2 + 1] = pn_states.get(i).getKey().floatValue()/10.0f;
    		secondState[i*2 + 2] = pn_states.get(i).getValue().floatValue();
    	}
    	
    	/*DEBUG*/
//		System.out.println("In pn" + ourPlayerNumber + " state2: " 
//					+ Arrays.toString(secondState));
    	
    	stateValueFunction.getStates2().store(oldState2, secondState, 0.);
    	oldState2 = secondState;	    	
    	currentStateValue = stateValueFunction.getStates2().getStateValue(secondState);
	}

	@Override
	public void updateReward() {
	}
	
	 public void updateReward(int winner) {
		 state.updateAll(players, board);
			ArrayList<Pair<Integer, Float>> pn_states = new ArrayList< Pair<Integer, Float> >();
			double reward = 0.;
			
			float[] secondState = new float[7];
			secondState[0]=ourPlayerData.getTotalVP()/10.0f;
			
			float[] ourPlayerArray = state.getPlayerState(ourPlayerData).getNormalizedStateArray();
			
			for (SOCPlayer opp : opponents) {   		
	    		float[] oppArray = state.getPlayerState(opp).getNormalizedStateArray();
	    		float[] result = new float[ourPlayerArray.length + oppArray.length];
	    		System.arraycopy(ourPlayerArray, 0, result, 0, ourPlayerArray.length);
	    		System.arraycopy(oppArray, 0, result, ourPlayerArray.length, oppArray.length);
	    		float[] oldpnState = oldState.get(opp);	
	    		int points = opp.getTotalVP(); 
	    		
	    		if (winner == ourPlayerNumber) {
	    			reward = 1.;
	    		} else if (winner == opp.getPlayerNumber()) {
	    			reward = 0.;
	    		} else if (points< ourPlayerData.getTotalVP()){
	    			reward = 0.5;
	    		}
	    		
	    		stateValueFunction.getStates().store(oldpnState, result, reward);
		
	    		float value = stateValueFunction.getStates().getStateValue(result);
	    		pn_states.add(new Pair<Integer, Float>(Integer.valueOf(points), Float.valueOf(value)));
	    		
//	    		/*DEBUG*/
//	    		System.out.println("opponent state: " + Arrays.toString(pnState));
			}   
			
			if (winner == ourPlayerNumber) {
    			reward = 1;
    		} else {
    			reward = 0;
    		}
			
			pn_states.sort(new Comparator<Pair<Integer, Float>>() {
			    public int compare(Pair<Integer, Float> o1, Pair<Integer, Float> o2) {
			        return o2.getKey().compareTo(o1.getKey());
			    }
			});
	    	
	    	for(int i = 0; i<pn_states.size(); i++) {
	    		secondState[i*2 + 1] = pn_states.get(i).getKey().floatValue()/10.0f;
	    		secondState[i*2 + 2] = pn_states.get(i).getValue().floatValue();
	    	}
	    	
	    	/*DEBUG*/
//			System.out.println("In pn" + ourPlayerNumber + " state2: " 
//						+ Arrays.toString(secondState));
	    	
	    	stateValueFunction.getStates2().store(oldState2, secondState, reward);
	 }
}
