package soc.robot.rl;

public abstract class PlayerState implements Cloneable {

	public PlayerState() {
		// TODO Auto-generated constructor stub
	}
	
	public PlayerState clone()
    {
		PlayerState pn = null;

        try {
        	pn = (PlayerState) super.clone();
        }
        catch (CloneNotSupportedException e)
        {  System.out.println("CloneNotSupportedException!"); }
        
        return pn;
    }
	
	public int[] getResources() {
		return new int[2];
	}
	public void setResources(int[] resources) {
	}
	public void copyResources(int[] resources) {
	}
	
	public int getPoints() {
		return 0;
	}
	public void setPoints(int points) {
	}
	
	public int[] getDevCards() {
		return new int[2];
	}
	public void setDevCards(int[] devCards) {
	}
	
	public int[] getResourceProbabilities() {
		return new int[2];
	}
	public void setResourceProbabilities(int[] resourceProbabilities) {
	}
	
	public float[] getResourceProbabilitiesFloat() {
		return new float[2];
	}
	public void setResourceProbabilitiesFloat(float[] resourceProbabilities) {
	}
	
	public int[] getResourceAdjacentBuildings() {
		return new int[2];
	}
	public void setResourceAdjacentBuildings(int[] resourceAdjacentBuildings) {
	}
	
	public int getUniqueAdjacentHexes() {
		return 0;
	}
	public void setUniqueAdjacentHexes(int uniqueAdjacentHexes) {
	}
	
	public int getUniqueNumbers() {
		return 0;
	}
	public void setUniqueNumbers(int uniqueNumbers) {
	}
	
	public int getLongestRoad() {
		return 0;
	}
	public void setLongestRoad(int longestRoad) {
	}
	
	public int getPlayedKnights() {
		return 0;
	}
	public void setPlayedKnights(int playedKnights) {
	}
	
	public int getHasLongestRoad() {
		return 0;
	}
	public void setHasLongestRoad(int hasLongestRoad) {
	}
	
	public int getHasLargestArmy() {
		return 0;
	}
	public void setHasLargestArmy(int hasLargestArmy) {
	}
	
	public int[] getPorts() {
		return new int[2];
	}
	public void setPorts(int[] ports) {
	}
	
	public int getBlockedByRobber() {
		return 0;
	}
	public void setBlockedByRobber(int blockedByRobber) {
	}
	
	public int getNumberOfPotentialSettlements() {
		return 0;
	}
	public void setNumberOfPotentialSettlements(int numberOfPotentialSettlements) {
	}
	
	public void addPoints(int points) {
	}
	public void substractPoints(int points) {
	}
	
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
	
	public int stateLength() {
		return 0;
	}
	public int[] getStateArray() {
		return new int[2];
	}
	
	public float[] getNormalizedStateArray() {
		return new float[2];
	}

}
