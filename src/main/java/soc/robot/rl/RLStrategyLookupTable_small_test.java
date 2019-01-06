package soc.robot.rl;

import java.util.Random;

import soc.game.SOCGame;

public class RLStrategyLookupTable_small_test extends RLStrategyLookupTable_small {

	public RLStrategyLookupTable_small_test(SOCGame game, int pn, StateMemoryLookupTable memory) {
		super(game, pn, memory);

	}
	
	@Override
	protected void updateStateValue() {
		state.updateAll(players, board);
		currentStateValue = Float.valueOf(getStateValue(state));
	}
	
	@Override
	public void updateReward() {
	}
	
	@Override
	public void updateReward(int winner) {
	}


}
