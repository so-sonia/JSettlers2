package fasttester;

import soc.game.SOCCity;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.robot.OpeningBuildStrategy;
import soc.robot.rl.RLStrategyLookupTable;
import soc.robot.rl.RLStrategyLookupTable_test;
import soc.robot.rl.RLStrategyNN_dialogue;
import soc.robot.rl.RLStrategyNN_dialogue_test;
import soc.robot.rl.RLStrategyNN_oppsum;
import soc.robot.rl.RLStrategyNN_oppsum_test;
import soc.robot.rl.RLStrategyRandom;
import soc.robot.rl.StateMemoryLookupTable;

/**
 * Class compatible with BotServer_fullInfo.
 * Used for training bots, where full information is provided.
 * In the constructor game object is provided. Therefore all clients have 
 * the same game object as the one at the server. Therefore clients should
 * never modify game object, they can only read from it.
 * @author Sonia
 *
 */
public class RLClient_fullInfo extends RLClient {

	public RLClient_fullInfo(int i, int strategyType, int memoryType) {
		super(i, strategyType, memoryType);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Server will still send {@link BotServer#sendPutPiece(int, int, int)}
	 * because we need this in {@link RLClient_buildIn} to update playerTrackers.
	 * @see fasttester.RLClient#handlePUTPIECE(int, int, int)
	 */
	public void handlePUTPIECE(int pn, int pieceType, int coord)
    {
    }

}
