package soc.robot.rl;

import java.util.Arrays;

public class SOCPlayerState_small extends SOCPlayerState {

	public SOCPlayerState_small() {
	}
	
	public SOCPlayerState_small clone() {
		SOCPlayerState_small pn = null;

		pn = (SOCPlayerState_small) super.clone();
        
        return pn;
	}
	
	public int stateLength() {
		int size = resourceProbabilities.length + ports.length + 10;			
		return size;
	}
	
	public int[] getStateArray() {
		int length = stateLength();
		int[] result = new int[length];
		result[0] = points;
		result[1] = uniqueAdjacentHexes;
		result[2] = uniqueNumbers;
		result[3] = longestRoad;
		result[4] = playedKnights;
		result[5] = hasLongestRoad;
		result[6] = hasLargestArmy;
		result[7] = blockedByRobber;
		result[8] = numberOfPotentialSettlements;
		result[9] = devCards[0];
		int i = 10;
		System.arraycopy(resourceProbabilities, 0, result, i, resourceProbabilities.length);
		i += resourceProbabilities.length;
		System.arraycopy(ports, 0, result, i, ports.length);
		i += ports.length;
		return result;
	}

}
