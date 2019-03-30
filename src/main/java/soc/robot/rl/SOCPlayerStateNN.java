package soc.robot.rl;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SOCPlayerStateNN extends SOCPlayerState {
	
	protected float[] resourceProbabilitiesFloat;
	
//	protected int resMissingCity;
//	protected int resMissingSettlement;
//	protected int resMissingRoad;
//	protected int resMissingDevCard;
//	protected int cardsMissingLA;
//	protected int roadsMissngLR;
	
	/*MIN_VALUES given in order as in getStateArray()
	 * points 2-10  
		uniqueAdjacentHexes 6 - 15
		uniqueNumbers 3 - 12
		longestRoad 1 - 15
		playedKnights 0 - 7
		hasLongestRoad 0-1
		hasLargestArmy 0-1
		blockedByRobber 0-1
		numberOfPotentialSettlements 0-10
		resources 0-10
		devCards 0-7
		resourceProbabilities 0-1
		resourceAdjacentBuildings 0-5
		ports 0-1
	 *NORMALIZE_MODULE
	 */
	public static final float[] MIN_VALUES =
	    {
	        2.0f, 6.0f, 3.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 
	        0.0f, 0.0f
	    };
	
	public static final float[] MAX_VALUES =
	    {
	        8.0f, 10.0f, 9.0f, 15.0f, 7.0f, 1.0f, 1.0f, 1.0f, 10.0f, 10.0f, 7.0f, 1.0f, 
	        5.0f, 1.0f
	    };
	

	public SOCPlayerStateNN() {	
	}
	
	public SOCPlayerStateNN clone() {
		SOCPlayerStateNN pn = null;

		pn = (SOCPlayerStateNN) super.cloneVanilla();
		pn.resources = resources.clone();
		pn.devCards = devCards.clone();
		pn.resourceProbabilitiesFloat = resourceProbabilitiesFloat.clone();
		pn.resourceAdjacentBuildings = resourceAdjacentBuildings.clone();
		pn.ports = ports.clone();
        
        return pn;
	}
	
	public int stateLength() {
		int size = 0;
		size += resources.length + devCards.length + resourceProbabilitiesFloat.length + 
				resourceAdjacentBuildings.length + ports.length + 9;	
		return size;
	}
	
	/*When changing getStateArray() change all places with NORMALIZE_MODULE adnotation*/
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
		int i = 9;
		System.arraycopy(resources, 0, result, i, resources.length);
		i += resources.length;
		System.arraycopy(devCards, 0, result, i, devCards.length);
		i += devCards.length;
		for (int k=0; k<resourceProbabilitiesFloat.length; k++) {
			result[i+k] = Math.round(resourceProbabilitiesFloat[k]*30);
		}	
		i += resourceProbabilitiesFloat.length;
		System.arraycopy(resourceAdjacentBuildings, 0, result, i, resourceAdjacentBuildings.length);
		i += resourceAdjacentBuildings.length;
		System.arraycopy(ports, 0, result, i, ports.length);
		return result;
	}
	
	/*TODO: vector operation to normalize*/
	public float[] getNormalizedStateArray() {
		float[] result = new float[stateLength()];
		result[0] = (points - MIN_VALUES[0])/MAX_VALUES[0];
		result[1] = (uniqueAdjacentHexes - MIN_VALUES[1])/MAX_VALUES[1];
		result[2] = (uniqueNumbers - MIN_VALUES[2])/MAX_VALUES[2];
		result[3] = (longestRoad - MIN_VALUES[3])/MAX_VALUES[3];
		result[4] = (playedKnights - MIN_VALUES[4])/MAX_VALUES[4];
		result[5] = (hasLongestRoad - MIN_VALUES[5])/MAX_VALUES[5];
		result[6] = (hasLargestArmy - MIN_VALUES[6])/MAX_VALUES[6];
		result[7] = (blockedByRobber - MIN_VALUES[7])/MAX_VALUES[7];
		result[8] = (numberOfPotentialSettlements - MIN_VALUES[8])/MAX_VALUES[8];
		int k = 9;
		for (int i=0; i< resources.length; i++) {
			result[i+k] = (resources[i] - MIN_VALUES[9])/MAX_VALUES[9];
		}
		k+=resources.length;
		for (int i=0; i< devCards.length; i++) {
			result[i+k] = (devCards[i] - MIN_VALUES[10])/MAX_VALUES[10];
		}
		k+=devCards.length;
		for (int i=0; i< resourceProbabilitiesFloat.length; i++) {
			result[i+k] = (resourceProbabilitiesFloat[i] - MIN_VALUES[11])/MAX_VALUES[11];
		}
		k+=resourceProbabilitiesFloat.length;
		for (int i=0; i< resourceAdjacentBuildings.length; i++) {
			result[i+k] = (resourceAdjacentBuildings[i] - MIN_VALUES[12])/MAX_VALUES[12];
		}
		k+=resourceAdjacentBuildings.length;
		for (int i=0; i< ports.length; i++) {
			result[i+k] = (ports[i] - MIN_VALUES[13])/MAX_VALUES[13];
		}
		return result;
	}
	
	

}
