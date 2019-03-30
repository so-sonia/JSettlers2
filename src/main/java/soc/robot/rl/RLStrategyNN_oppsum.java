package soc.robot.rl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import soc.game.SOCBoard;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCResourceConstants;

public class RLStrategyNN_oppsum extends RLStrategy {

	INDArray oldState;
	
	StateValueFunction states;
	
	
	public RLStrategyNN_oppsum(SOCGame game, int pn) {
		super(game, pn);
		state = new SOCStateNN_opp1(ourPlayerNumber, players);
	    state.updateAll(players, board);
	    
	    INDArray ourPlayerArray = Nd4j.create(state.getPlayerState(ourPlayerData).getNormalizedStateArray());
	    INDArray opps = Nd4j.create(state.getPlayerState(opponents.get(0)).stateLength());
	    for (SOCPlayer opp : opponents) {   		
	    	opps.addi(Nd4j.create(state.getPlayerState(opp).getNormalizedStateArray()));
	    }
	    
	    opps.divi(opponents.size());
	    oldState = Nd4j.concat(1, ourPlayerArray, opps);	
	}

	@Override
	protected float getStateValue(SOCState state) {
		INDArray ourPlayerArray = Nd4j.create(state.getPlayerState(ourPlayerData).getNormalizedStateArray());
	    INDArray opps = Nd4j.create(state.getPlayerState(opponents.get(0)).stateLength());
	    for (SOCPlayer opp : opponents) {   		
	    	opps.addi(Nd4j.create(state.getPlayerState(opp).getNormalizedStateArray()));
	    }
	    
	    opps.divi(opponents.size());
	    INDArray oppsum = Nd4j.concat(1, ourPlayerArray, opps);
	    
//	    /*DEBUG*/;
//    System.out.println("In pn" + ourPlayerNumber +  " state: " 
//			+ oppsum);
////	System.out.println(Arrays.toString(state.getPlayerState(ourPlayerData).getStateArray()));
////	System.out.println(Arrays.toString(state.getPlayerState(opp).getStateArray()));
//    ourPlayerData.stats();
//    System.out.println(states.getStateValue(oppsum));
	    
	    return states.getStateValue(oppsum);		
	}

	@Override
	protected void updateStateValue() {
		state.updateAll(players, board);
		
		INDArray ourPlayerArray = Nd4j.create(state.getPlayerState(ourPlayerData).getNormalizedStateArray());
	    INDArray opps = Nd4j.create(state.getPlayerState(opponents.get(0)).stateLength());
	    for (SOCPlayer opp : opponents) {   		
	    	opps.addi(Nd4j.create(state.getPlayerState(opp).getNormalizedStateArray()));
	    }
	    
	    opps.divi(opponents.size());
	    INDArray oppsum = Nd4j.concat(1, ourPlayerArray, opps);
  
//	    /*DEBUG*/
//	    System.out.println(oppsum);
	    
	    states.store(oldState, oppsum, 0.);
		oldState = oppsum;
		currentStateValue = states.getStateValue(oppsum);
	}

	@Override
	public void updateReward() {
	}
	
	
	
	public void updateReward(int winner) {
		 state.updateAll(players, board);
		 INDArray ourPlayerArray = Nd4j.create(state.getPlayerState(ourPlayerData).getNormalizedStateArray());
		 double reward = 0.;
		 if (winner == ourPlayerNumber) {
 			reward = 1.;
 		 } else {
 			 reward = ourPlayerData.getTotalVP()*0.5;
 		 }
		 
	    INDArray opps = Nd4j.create(state.getPlayerState(opponents.get(0)).stateLength());
	    for (SOCPlayer opp : opponents) {   		
	    	opps.addi(Nd4j.create(state.getPlayerState(opp).getNormalizedStateArray()));
	    }
	    
	    opps.divi(opponents.size());
	    INDArray oppsum = Nd4j.concat(1, ourPlayerArray, opps);
	    
	    states.store(oldState, oppsum, reward); 
	}
	
	public void setStates(StateValueFunction states) {
		this.states = states;
	}
	
	/* we need to override this method, because in RLStrategyNN 
	 * we use resourceProbabilities as float[] and not int[].
	 * @see soc.robot.rl.RLStrategy#searchPlaceRobberOrPlayKnight(soc.robot.rl.SOCState, boolean)
	 */
	 protected AbstractMap.SimpleEntry<Float, int[]> 
	  searchPlaceRobberOrPlayKnight(SOCState currentState, boolean playKnight) {
	  	
//	  	/*DEBUG*/
//	  	System.out.println("searchPlaceRobberOrPlayKnight() was called");
	  	
	  	int[] landHexes = board.getLandHexCoords();
//	  	SOCState tmpState = state.copySOCState();
	  	ArrayList<Float> state_values = new ArrayList<Float>();
	  	ArrayList<Integer> robPositions = new ArrayList<Integer>();
	  	ArrayList<Integer> playersToRob = new ArrayList<Integer>();
	  	ArrayList<Float> state_values_no_victims = new ArrayList<Float>();
	  	ArrayList<Integer> robPositions_no_victims = new ArrayList<Integer>();
	  	ArrayList<Integer> playersToRob_no_victims = new ArrayList<Integer>();
	  	ArrayList<Float> state_values_no_stealing = new ArrayList<Float>();
	  	ArrayList<Integer> robPositions_no_stealing = new ArrayList<Integer>();
	  	ArrayList<Integer> playersToRob_no_stealing = new ArrayList<Integer>();
	  	int[] prevResources =  currentState.getPlayerState(ourPlayerData).getResources();

	  	
	  	/*checking if the player will get largest army (in case he's playing
	  	 * the knight card
	  	 */
	  	boolean willGetLA = false;
	  	SOCPlayer playerWithLA = game.getPlayerWithLargestArmy();
	  	int playedKnights = ourPlayerData.getNumKnights();   	
	  	if (playKnight) {
	      	if (playerWithLA == null) {
	      		/*player need minimum 3 knights to get largest army */
	      		willGetLA = playedKnights + 1 > 2;
	      	} else {
	      		if (playerWithLA.getPlayerNumber() != ourPlayerNumber) {
	      			int opPlayedKnights = playerWithLA.getNumKnights();
	      			willGetLA = playedKnights + 1 > opPlayedKnights;
	      		}
	      	}
	      	
	      	
//	      	/*DEBUG*/
//	      	System.out.println("Will get largest army: " + willGetLA);	
	  	}
	  	    	
	  	/*go through all land hexes except the desert and the current robber position
	  	 * (cannot put robber back there)
	  	 */
	  	for(int posRob : landHexes) {
	  		
	  		if (board.getHexTypeFromCoord(posRob) != SOCBoard.DESERT_HEX &&
	  				posRob != board.getRobberHex()){
	  			
	  			/*get all nodes adjacent to the hex and check if there's a building */
	  			List<SOCPlayer> candidates = game.getPlayersOnHex(posRob);
	  			
	  			
	  			/*we add hex, even if there's no player affected, because sometimes may 
	  			 * happen that on every hex with other player we have our own building
	  			 * therefore it's better to place the robber on the hex with no players
	  			 * then to block ourselves
	  			 */
	  			if (candidates.size()==0) {
	  				SOCState tmpState = currentState.copySOCState();
					if (playKnight) {					
	  					tmpState.updatePlayedKnightCard(ourPlayerData, willGetLA, playerWithLA);
	  		    	}
	  				
	  				tmpState.updatePlaceRobberAll(players, board, posRob);
	  				
	  				state_values_no_victims.add(Float.valueOf(getStateValue(tmpState)));
					robPositions_no_victims.add(Integer.valueOf(posRob));
					playersToRob_no_victims.add(Integer.valueOf(-1));
					
	  			} else {
	  				
	  				boolean our = false;
	  	  			boolean[] hasRes = new boolean[candidates.size()];
	  	  			boolean anyRes = false;
	  				String candres = "";
	  	  			
	  				for (int i=0; i< candidates.size(); i++) {
	  					hasRes[i] = candidates.get(i).getResources().getTotal() > 0;
	  					candres += " player: " + candidates.get(i).getPlayerNumber() + " res: " +
	  							Arrays.toString(candidates.get(i).getResources().getAmounts(true));
	  					if (hasRes[i]) {
	  						anyRes = true;
	  					}
	  					if (candidates.get(i).getPlayerNumber()==ourPlayerNumber) {
	  						our = true;
	  						break;
	  					}
	  				}
	  				
	  				
	  				
//	  				/*DEBUGA*/
//	  				System.out.println("Player: " + ourPlayerNumber + " hex: " + posRob + 
//	  							" victims size " + candidates.size() + " anyRes: " 
//	  							+ anyRes + " our: " + our + candres);
	  				
	  				if (our)
	  					continue;
	  				
	  				if (candidates.size()==1 && !hasRes[0]) {
	  					SOCState tmpState = currentState.copySOCState();
	  					if (playKnight) {					
	  	  					tmpState.updatePlayedKnightCard(ourPlayerData, willGetLA, playerWithLA);
	  	  		    	}
	  	  				
	  	  				tmpState.updatePlaceRobberAll(players, board, posRob);
	  	  				
	  	  				state_values_no_stealing.add(Float.valueOf(getStateValue(tmpState)));
						robPositions_no_stealing.add(Integer.valueOf(posRob));
						playersToRob_no_stealing.add(Integer.valueOf(-1));
	  				
	  				} else if (anyRes) {
	  					
	  					for (int i=0; i< candidates.size(); i++) {
	  						
	  						if (!hasRes[i])
	  							continue;
	  						
	  						SOCPlayer player = candidates.get(i);
	  						
	  						ArrayList<Float> state_values_res = new ArrayList<Float>();
	  	      				SOCState tmpState = currentState.copySOCState();
	  	      				int[] plResources = currentState.getPlayerState(player).getResources();
	  	      				float[] plResourcesProb = currentState.getPlayerState(player).getResourceProbabilitiesFloat();

	  	      				if (playKnight) {					
	  	      					tmpState.updatePlayedKnightCard(ourPlayerData, willGetLA, playerWithLA);
	  	      		    	}
	  	      				
	  	      				tmpState.updatePlaceRobberAll(players, board, posRob);
	  	      				
	  	      				/* resource array in tmpState starts from 0 not from 1 like in 
	  	      		    	 * SOCResourceConstants. We don't know what resource we will steal from the other player.
	  	      		    	 * Therefore for every resource we check if that player possesses this resource
	  	      		    	 * or if he has any unknown resources and non zero probability of getting this resource.
	  	      		    	 * If that's the case we calculate state value we would have after stealing this
	  	      		    	 * resource. After checking all resources we calculate average of these state values,
	  	      		    	 * which give us a state value of robbing this particular player. 
	  	      		    	 */
	  	      				for (int resource = SOCResourceConstants.CLAY -1;
	  	      						resource <= SOCResourceConstants.WOOD -1; resource++) {
	  	      					
	  	      					tmpState.updateResources(player, true);
	  	      					
	  	      					/* if search is done after 7 was rolled first we were discarding,
	  	      					 * so instead of updating resources to true values, we have update them 
	  	      					 * to values after discard (case when this function is called
	  	      					 * from searchRollDice())*/
	  	      					if (!playKnight) {
	  	      						tmpState.updateSetResources(ourPlayerData, prevResources);
	  	      					} else {
	  	      						tmpState.updateResources(ourPlayerData, false);
	  	      					}
	  	      					
	  	      					if (plResources[resource]>0 || 
	  	      							(plResourcesProb[resource] > 0 && plResources[SOCResourceConstants.UNKNOWN-1] > 0 )) {
	  	      						
	  	      						tmpState.updateSteal(player, resource, false);
	  	      						
	  	      						//TO TEST: maybe calculate weighted mean
	  	      						state_values_res.add(Float.valueOf(getStateValue(tmpState)));    						
	  	      					} 
	  	      				}
	  	      				
	  	      				/*we get a state value of placing robber on this particular hex and robbing the
	  	      				 * particular player
	  	      				 */
	  	      				if (state_values_res.size()>0 && !our) {
	  	      					double average = state_values_res.stream().mapToDouble(a -> a).average().orElse(0.);
	  	      					state_values.add(Float.valueOf((float) average));
	  	      					robPositions.add(Integer.valueOf(posRob));
	  	      					playersToRob.add(Integer.valueOf(player.getPlayerNumber()));
	  	      					}
	  					}
	  				}	
	  			}
	  		}
	  	}
	  	
		if (state_values.size() == 0) {
	  		if (state_values_no_stealing.size() !=0) {
	  			state_values = state_values_no_stealing;
	  			robPositions = robPositions_no_stealing;
	  			playersToRob = playersToRob_no_stealing;
	  		} else if (state_values_no_victims.size() !=0 ){
	  			state_values = state_values_no_victims;
	  			robPositions = robPositions_no_victims;
	  			playersToRob = playersToRob_no_victims;
	  		} else {
	  			/* added hard coded nonsense values, 
	  			 * because 1 in 50 thousand games throws mistake
	  			 */
	  			state_values.add(Float.valueOf(0));
	  			robPositions.add(landHexes[0]);
	  			playersToRob.add(0);
	  		}
	  	}
	  	
	  	/*we chose the hex, where we should place the robber and the player to rob
	  	 * with highest expected state value
	  	 */
	  	AbstractMap.SimpleEntry<Float, Integer> maxAndIndex = getMaxAndIndex(state_values);
	  	int[] placeAndPlayerToRob = new int[2];
	  	
//	  	/*DEBUG*/
//		    System.out.println(game.getName() + " maxAndIndex key " + maxAndIndex.getKey() + " maxAndIndex value " + 
//		    		maxAndIndex.getValue() + " robPositions size " + robPositions.size() + 
//		    		" playersToRob size " + playersToRob.size());
//		    System.out.println("player to rob " + Arrays.toString(playersToRob.toArray()));
		    
	  	placeAndPlayerToRob[0] = robPositions.get(maxAndIndex.getValue());
	  	placeAndPlayerToRob[1] = playersToRob.get(maxAndIndex.getValue());
	  	AbstractMap.SimpleEntry<Float, int[]> result = new AbstractMap.SimpleEntry<Float, int[]>(
	  			maxAndIndex.getKey(), placeAndPlayerToRob);
	  	return(result);  	
	  }

}
