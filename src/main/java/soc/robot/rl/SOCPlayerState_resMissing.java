/*
 * res missing to build: city, settle, road, dev card
 * cards missing to LA, roads missing to LR, points, 
 * res prob: if port add to pow^2 to others, if port 3:1 pow^3 add to others, 
 * take into account buildings 
 * blocked by robber 
 * dev cards
 */
package soc.robot.rl;

import java.util.Arrays;

public class SOCPlayerState_resMissing extends SOCPlayerState {

	protected int resMissingCity;
	
	protected int resMissingSettlement;
	
	protected int resMissingRoad;
	
	protected int resMissingDevCard;
	
	protected int cardsMissingLA;
	
	protected int roadsMissngLR;
	
	public SOCPlayerState_resMissing() {
	}
	
	public SOCPlayerState_resMissing clone() {
		SOCPlayerState_resMissing pn = null;

		pn = (SOCPlayerState_resMissing) super.clone();
        
        return pn;
	}

//	public SOCPlayerState_resMissing(SOCPlayerState state) {
//		this.resources = Arrays.copyOf(state.resources, state.resources.length);
//		this.points = state.points;
//		this.devCards =  Arrays.copyOf(state.devCards, state.devCards.length);
//		this.resourceProbabilities = Arrays.copyOf(state.resourceProbabilities, 
//				state.resourceProbabilities.length);
//		this.blockedByRobber = state.blockedByRobber;
//		this.resMissingCity = state.getResMissingCity();
//		this.resMissingSettlement = state.getResMissingSettlement();
//		this.resMissingRoad = state.getResMissingRoad();
//		this.resMissingDevCard = state.getResMissingDevCard();
//		this.cardsMissingLA = state.getCardsMissingLA();
//		this.roadsMissngLR = state.getRoadsMissngLR();
//	}

	public int getResMissingCity() {
		return resMissingCity;
	}

	public void setResMissingCity(int resMissingCity) {
		this.resMissingCity = resMissingCity;
	}

	public int getResMissingSettlement() {
		return resMissingSettlement;
	}

	public void setResMissingSettlement(int resMissingSettlement) {
		this.resMissingSettlement = resMissingSettlement;
	}

	public int getResMissingRoad() {
		return resMissingRoad;
	}

	public void setResMissingRoad(int resMissingRoad) {
		this.resMissingRoad = resMissingRoad;
	}

	public int getResMissingDevCard() {
		return resMissingDevCard;
	}

	public void setResMissingDevCard(int resMissingDevCard) {
		this.resMissingDevCard = resMissingDevCard;
	}

	public int getCardsMissingLA() {
		return cardsMissingLA;
	}

	public void setCardsMissingLA(int cardsMissingLA) {
		this.cardsMissingLA = cardsMissingLA;
	}

	public int getRoadsMissngLR() {
		return roadsMissngLR;
	}

	public void setRoadsMissngLR(int roadsMissngLR) {
		this.roadsMissngLR = roadsMissngLR;
	}
	
	public int stateLength() {
		int size = resourceProbabilities.length + 3 + 6;			
		return size;
	}
	
	public int[] getStateArray() {
		int[] result = new int[stateLength()];
		result[0] = points;
		result[1] = devCards[0];
		result[2] = blockedByRobber;
		result[3] = resMissingCity;
		result[4] = resMissingSettlement;
		result[5] = resMissingRoad;
		result[6] = resMissingDevCard;
		result[7] = cardsMissingLA;
		result[8] = roadsMissngLR;
		int i = 9;
		System.arraycopy(resourceProbabilities, 0, result, i, resourceProbabilities.length);
		i += resourceProbabilities.length;
		return result;
	}

}
