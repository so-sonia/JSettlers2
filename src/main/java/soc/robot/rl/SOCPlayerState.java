/*
 * other classes use: resources and resource probabilities
 */

package soc.robot.rl;

import java.util.Arrays;

/**
 * In case the field in {@link SOCPlayerState} will be changed. Method {@link #stateLength()}
 * should be changed accordingly;
 */
public class SOCPlayerState extends PlayerState {
	
	protected int[] resources;
	protected int points;
	
	/**In order: VP cards, road building cards, discovery cards, monopoly cards, Knight cards. 
     * Except for VP cards all the other cards are counted separately by state: old, new*/
	protected int[] devCards;
	protected int[] resourceProbabilities;
	protected int[] resourceAdjacentBuildings;
	protected int uniqueAdjacentHexes;
	protected int uniqueNumbers;
	protected int longestRoad;
	protected int playedKnights;
	protected int hasLongestRoad;
	protected int hasLargestArmy;
	protected int[] ports;
	protected int blockedByRobber;
	protected int numberOfPotentialSettlements;
	
	public SOCPlayerState(){
		
	}
	
	public SOCPlayerState clone() {
		SOCPlayerState pn = null;

		pn = (SOCPlayerState) super.clone();
		pn.resources = resources.clone();
		pn.devCards = devCards.clone();
		pn.resourceProbabilities = resourceProbabilities.clone();
		pn.resourceAdjacentBuildings = resourceAdjacentBuildings.clone();
		pn.ports = ports.clone();
        
        return pn;
	}
	
	public SOCPlayerState cloneVanilla() {
		SOCPlayerState pn = null;

		pn = (SOCPlayerState) super.clone();
        
        return pn;
	}
	
	public int[] getResources() {
		return resources;
	}
	public void setResources(int[] resources) {
		this.resources = resources;
	}
	public void copyResources(int[] resources) {
		for (int i=0; i< this.resources.length ; i++) {
			this.resources[i] = resources[i];
		}
	}
	public int getPoints() {
		return points;
	}
	public void setPoints(int points) {
		this.points = points;
	}
	public int[] getDevCards() {
		return devCards;
	}
	public void setDevCards(int[] devCards) {
		this.devCards = devCards;
	}
	public int[] getResourceProbabilities() {
		return resourceProbabilities;
	}
	public void setResourceProbabilities(int[] resourceProbabilities) {
		this.resourceProbabilities = resourceProbabilities;
	}
	public int[] getResourceAdjacentBuildings() {
		return resourceAdjacentBuildings;
	}
	public void setResourceAdjacentBuildings(int[] resourceAdjacentBuildings) {
		this.resourceAdjacentBuildings = resourceAdjacentBuildings;
	}
	public int getUniqueAdjacentHexes() {
		return uniqueAdjacentHexes;
	}
	public void setUniqueAdjacentHexes(int uniqueAdjacentHexes) {
		this.uniqueAdjacentHexes = uniqueAdjacentHexes;
	}
	public int getUniqueNumbers() {
		return uniqueNumbers;
	}
	public void setUniqueNumbers(int uniqueNumbers) {
		this.uniqueNumbers = uniqueNumbers;
	}
	public int getLongestRoad() {
		return longestRoad;
	}
	public void setLongestRoad(int longestRoad) {
		this.longestRoad = longestRoad;
	}
	public int getPlayedKnights() {
		return playedKnights;
	}
	public void setPlayedKnights(int playedKnights) {
		this.playedKnights = playedKnights;
	}
	public int getHasLongestRoad() {
		return hasLongestRoad;
	}
	public void setHasLongestRoad(int hasLongestRoad) {
		this.hasLongestRoad = hasLongestRoad;
	}
	public int getHasLargestArmy() {
		return hasLargestArmy;
	}
	public void setHasLargestArmy(int hasLargestArmy) {
		this.hasLargestArmy = hasLargestArmy;
	}
	public int[] getPorts() {
		return ports;
	}
	public void setPorts(int[] ports) {
		this.ports = ports;
	}
	public int getBlockedByRobber() {
		return blockedByRobber;
	}
	public void setBlockedByRobber(int blockedByRobber) {
		this.blockedByRobber = blockedByRobber;
	}

	public int getNumberOfPotentialSettlements() {
		return numberOfPotentialSettlements;
	}
	public void setNumberOfPotentialSettlements(int numberOfPotentialSettlements) {
		this.numberOfPotentialSettlements = numberOfPotentialSettlements;
	}
	public void addPoints(int points) {
		this.points += points;
	}
	public void substractPoints(int points) {
		this.points -= points;
	}
	
	/*added just not to make new object for Player State*/
	public int getResMissingCity() {
		return 0;
	}

	public void setResMissingCity(int resMissingCity) {
	}

	public int getResMissingSettlement() {
		return 0;
	}

	public void setResMissingSettlement(int resMissingSettlement) {
	}

	public int getResMissingRoad() {
		return 0;
	}

	public void setResMissingRoad(int resMissingRoad) {
	}

	public int getResMissingDevCard() {
		return 0;
	}

	public void setResMissingDevCard(int resMissingDevCard) {
	}

	public int getCardsMissingLA() {
		return 0;
	}

	public void setCardsMissingLA(int cardsMissingLA) {
	}

	public int getRoadsMissngLR() {
		return 0;
	}

	public void setRoadsMissngLR(int roadsMissngLR) {
	}
	
	/**For now length of the array that will be created by the state is calculated by hand.
	 * TO DO: automate it, so it won't be necessary to update it every time that state will be changing. 
	 * @return
	 */
	public int stateLength() {
		int size = 0;
		size += resources.length + devCards.length + resourceProbabilities.length + 
				resourceAdjacentBuildings.length + ports.length + 9;			
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
		int i = 9;
		System.arraycopy(resources, 0, result, i, resources.length);
		i += resources.length;
		System.arraycopy(devCards, 0, result, i, devCards.length);
		i += devCards.length;
		System.arraycopy(resourceProbabilities, 0, result, i, resourceProbabilities.length);
		i += resourceProbabilities.length;
		System.arraycopy(resourceAdjacentBuildings, 0, result, i, resourceAdjacentBuildings.length);
		i += resourceAdjacentBuildings.length;
		System.arraycopy(ports, 0, result, i, ports.length);
		i += ports.length;
		return result;
	}
	
}

