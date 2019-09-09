package soc.robot.rl;

import soc.game.SOCGame;

public class RLStrategyNN_dialogue_test extends RLStrategyNN_dialogue {

	public RLStrategyNN_dialogue_test(SOCGame game, int pn) {
		super(game, pn);
	}
	
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
