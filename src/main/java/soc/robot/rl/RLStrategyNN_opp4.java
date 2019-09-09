package soc.robot.rl;

import java.util.Comparator;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import soc.game.SOCGame;
import soc.game.SOCPlayer;

public class RLStrategyNN_opp4 extends RLStrategyNN_oppsum {

	public RLStrategyNN_opp4(SOCGame game, int pn) {
		super(game, pn);
		
		INDArray stateArr = Nd4j.create(state.getPlayerState(ourPlayerData).getNormalizedStateArray());

		opponents.get(0).setSpecialVP(1);
		opponents.get(1).setSpecialVP(2);
		opponents.get(2).setSpecialVP(4);
		
		opponents.sort(new Comparator<SOCPlayer>() {
		    public int compare(SOCPlayer o1, SOCPlayer o2) {
		        return (o2.getPublicVP() - o1.getPublicVP());
		    }
		});
		
		
	    for (SOCPlayer opp : opponents) {  
	    	System.out.println("opp points" + opp.getPublicVP());
	    	stateArr =  Nd4j.concat(1, stateArr, Nd4j.create(state.getPlayerState(opp).getNormalizedStateArray()));
	    }
	    
	    oldState = stateArr;	
	}
	
	@Override
	protected float getStateValue(SOCState state) {
		INDArray stateArr = Nd4j.create(state.getPlayerState(ourPlayerData).getNormalizedStateArray());

		opponents.sort(new Comparator<SOCPlayer>() {
		    public int compare(SOCPlayer o1, SOCPlayer o2) {
		        return (o2.getPublicVP() - o1.getPublicVP());
		    }
		});
		
	    for (SOCPlayer opp : opponents) {   		
	    	stateArr =  Nd4j.concat(1, stateArr, Nd4j.create(state.getPlayerState(opp).getNormalizedStateArray()));
	    }
	    
//	    /*DEBUG*/;
//    System.out.println("In pn" + ourPlayerNumber +  " state: " 
//			+ oppsum);
////	System.out.println(Arrays.toString(state.getPlayerState(ourPlayerData).getStateArray()));
////	System.out.println(Arrays.toString(state.getPlayerState(opp).getStateArray()));
//    ourPlayerData.stats();
//    System.out.println(states.getStateValue(oppsum));
	    
	    return stateValueFunction.getStates().getStateValue(stateArr);		
	}

	@Override
	protected void updateStateValue() {
		state.updateAll(players, board);
		
		INDArray stateArr = Nd4j.create(state.getPlayerState(ourPlayerData).getNormalizedStateArray());

		opponents.sort(new Comparator<SOCPlayer>() {
		    public int compare(SOCPlayer o1, SOCPlayer o2) {
		        return (o2.getPublicVP() - o1.getPublicVP());
		    }
		});
		
	    for (SOCPlayer opp : opponents) {   		
	    	stateArr =  Nd4j.concat(1, stateArr, Nd4j.create(state.getPlayerState(opp).getNormalizedStateArray()));
	    }
  
//	    /*DEBUG*/
//	    System.out.println(oppsum);
	    
	    stateValueFunction.getStates().store(oldState, stateArr, 0.);
		oldState = stateArr;
		currentStateValue = stateValueFunction.getStates().getStateValue(stateArr);
	}
	
	
	public void updateReward(int winner) {
		 state.updateAll(players, board);
		 INDArray ourPlayerArray = Nd4j.create(state.getPlayerState(ourPlayerData).getNormalizedStateArray());
		 double reward = 0.;
		 if (winner == ourPlayerNumber) {
 			reward = 1.;
 		 } else {
 			 reward = ourPlayerData.getTotalVP()*0.5;
 		 }
		 
	    INDArray opps = Nd4j.create(state.getPlayerState(opponents.get(0)).stateLength());
	    for (SOCPlayer opp : opponents) {   		
	    	opps.addi(Nd4j.create(state.getPlayerState(opp).getNormalizedStateArray()));
	    }
	    
	    opps.divi(opponents.size());
	    INDArray oppsum = Nd4j.concat(1, ourPlayerArray, opps);
	    
	    stateValueFunction.getStates().store(oldState, oppsum, reward); 
	}
	

		

}
