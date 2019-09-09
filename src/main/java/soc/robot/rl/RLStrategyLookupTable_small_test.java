package soc.robot.rl;

import java.util.Random;

import soc.game.SOCGame;

public class RLStrategyLookupTable_small_test extends RLStrategyLookupTable_small {

	public RLStrategyLookupTable_small_test(SOCGame game, int pn) {
		super(game, pn);

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
