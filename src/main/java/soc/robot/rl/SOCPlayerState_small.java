package soc.robot.rl;

import java.util.Arrays;

public class SOCPlayerState_small extends SOCPlayerState {

	public SOCPlayerState_small() {
	}

	public SOCPlayerState_small(SOCPlayerState state) {
		this.resources = Arrays.copyOf(state.resources, state.resources.length);
		this.points = state.points;
		this.devCards =  Arrays.copyOf(state.devCards, state.devCards.length);
		this.resourceProbabilities = Arrays.copyOf(state.resourceProbabilities, 
				state.resourceProbabilities.length);
		this.uniqueAdjacentHexes = state.uniqueAdjacentHexes;
		this.uniqueNumbers = state.uniqueNumbers;
		this.longestRoad = state.longestRoad;
		this.playedKnights = state.playedKnights;
		this.hasLargestArmy = state.hasLargestArmy;
		this.hasLongestRoad = state.hasLongestRoad;
		this.ports = Arrays.copyOf(state.ports, state.ports.length);
		this.blockedByRobber = state.blockedByRobber;
		this.numberOfPotentialSettlements = state.numberOfPotentialSettlements;
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
