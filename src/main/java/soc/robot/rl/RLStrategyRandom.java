package soc.robot.rl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import soc.game.SOCPlayer;

public class RLStrategyRandom extends RLStrategy {

	public RLStrategyRandom(RlbotBrain2 br) {
		super(br);	
		state = new SOCState(ourPlayerNumber, playerTrackers);
        state.updateAll(playerTrackers, board);   
	}

	@Override
	protected float getStateValue(SOCState state) {
		Double stateVal = new Random().nextGaussian()*0.05 + 0.5;
		return stateVal.floatValue();
	}

	@Override
	protected void updateStateValue() {
//		state.updateResources(ourPlayerData, false);
//		for (SOCPlayer opp : opponents) {
//			state.updateResources(opp, true);
//		}		
		state.updateAll(playerTrackers, board);
		currentStateValue = new Random().nextGaussian()*0.05 + 0.5;
	}

	@Override
	protected void writeMemory() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void readMemory() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void synchroniseMemory() {
		// TODO Auto-generated method stub

	}
	
	@Override
	protected void updateReward() {
		// TODO Auto-generated method stub

	}
	
	protected void writeStats() {
		SOCPlayer[] players = game.getPlayers();
		String nickname = brain.getClient().getNickname();
		
		BufferedWriter writer = null;
        try {
        	Path path = Paths.get("log", "RL_RND_stats_" + nickname);
            writer = new BufferedWriter(new FileWriter(path.toFile()));
            
            for (SOCPlayer pn : players) {
            	writer.write(game.getName() + ", " 
            			+ pn.getPlayerNumber() + "," 
            			+ pn.getName() + ","
//            			+ pn.
            			);
            	
            	pn.getTotalVP();
            }
            

            writer.write("Hello world!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            }
        }
		
//		game.getPlayer(player number), game.getName() for game name (or make ID for it),
//		 * 		player.getName() for player name, 
//		 * 		SOCRobotBrain.getRobotParameters().getStrategyType() (0 = SMART_STRATEGY, 1 = FAST_STRATEGY)
//		 * 		in each robot client we will have: gamesPlayed, gamesFinished = 0, gamesWon
//		 * 		from robot client we can get brain by SOCRobotBrain brain = robotBrains.get(mes.getGame());
		
	}
	

}
