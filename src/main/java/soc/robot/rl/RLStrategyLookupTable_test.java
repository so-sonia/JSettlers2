package soc.robot.rl;

import soc.game.SOCGame;

public class RLStrategyLookupTable_test extends RLStrategyLookupTable {

	public RLStrategyLookupTable_test(SOCGame game, int pn, StateMemoryLookupTable memory) {
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
