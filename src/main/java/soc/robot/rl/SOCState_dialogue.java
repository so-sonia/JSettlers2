package soc.robot.rl;
//STATE SIZE: 152

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCRoutePiece;
import soc.game.SOCSettlement;

/**
 * State has the same data as in work 
 * "Strategic Dialogue Management via Deep Reinforcement Learning"
 * Contains information about resources of the main player, hexes on the board,
 * nodes (whether there's a building and whether it belongs to us or the opponent),
 * edges (whether it has a road and to whom it belongs),
 * type of hex on which robber stands,
 * number of turns of the game so far
 * 
 * @author Sonia
 */
public class SOCState_dialogue extends SOCState {

	int[] resources;
	float[] hexes;
	int[] nodes;
	int[] edges;
	int robber;
	int turn;
	SOCPlayer ourPlayer;
	
	int[] legalNodesCoord;
	int[] legalEdgesCoord;
	SOCBoard board;
	
	/**
	 * index of legalNodesCoord, where last node was placed
	 */
	int lastPlacedSettlement;
	int lastPlacedCity;
	/**
	 * index of legalEdgesCoord, where last road was placed
	 */
	int lastPlacedRoad;
	
	HashMap<Integer, SOCPlayer> players;
	
	/**
	 * Information from board is extracted about hex layout, legal nodes and legal edges
	 * @param pln number of the main player
	 * @param players set of all the players in the game
	 */
	public SOCState_dialogue(int pln, HashMap<Integer, SOCPlayer> players) {
		super(pln);
		this.players = players;
		ourPlayer = players.get(ourPlayerNumber);
		board = ourPlayer.getGame().getBoard();
		int[] hexcoords = board.getLandHexCoords();
		hexes = new float[hexcoords.length];
		for (int i=0; i<hexcoords.length; i++) {
			hexes[i] = (float)board.getHexTypeFromCoord(hexcoords[i])/5.0f;
			//desert hex was 0 before, now it's 6 so we need to change it
			if (hexes[i]==1.2f)
				hexes[i]=0.0f;
		}
		legalNodesCoord = board.initPlayerLegalSettlements().stream().mapToInt(x -> x).toArray();
		legalEdgesCoord = board.initPlayerLegalRoads().stream().mapToInt(x -> x).toArray();
		nodes = new int[legalNodesCoord.length];
		edges = new int[legalEdgesCoord.length];
				
	}

	/**
	 * contructor used in {@link #copySOCState()}
	 * @param pln our player number
	 */
	public SOCState_dialogue(int pln) {
		super(pln);
	}
	
	/**
	 * all arrays are copied with deep copy
	 */
	public SOCState copySOCState() {
		   SOCState_dialogue copy = new SOCState_dialogue(ourPlayerNumber);
		   copy.resources =  Arrays.copyOf(resources, resources.length);
		   copy.hexes =  Arrays.copyOf(hexes, hexes.length);
		   copy.edges =  Arrays.copyOf(edges, edges.length);
		   copy.nodes =  Arrays.copyOf(nodes, nodes.length);
		   copy.robber = robber;
		   copy.turn = turn;
		   copy.ourPlayer = ourPlayer;
		   copy.legalNodesCoord =  Arrays.copyOf(legalNodesCoord, legalNodesCoord.length);
		   copy.legalEdgesCoord =  Arrays.copyOf(legalEdgesCoord, legalEdgesCoord.length);
		   copy.board = board;
		   copy.lastPlacedSettlement = lastPlacedSettlement;
		   copy.lastPlacedCity = lastPlacedCity;
		   copy.lastPlacedRoad = lastPlacedRoad;
		   copy.players = players;
		   
		   return copy;
	 }
	
	/**
	 * used only in Client-Server architecture
	 */
	public void addPlayerState(int playerNumber) {
    }
	
	/**
	 * We keep track only of resources of our player, saved in State, not in PlayerState
	 */
	public void updateResources(SOCPlayer pn, boolean otherPlayer) {
    	resources = pn.getResources().getAmounts(otherPlayer);
    }
	
	/**
	 * here the same as {@link SOCState_dialogue#updateResources(SOCPlayer, boolean)}
	 */
	public void updateResourcesByCopy(SOCPlayer pn, boolean otherPlayer) {
    	resources = pn.getResources().getAmounts(otherPlayer);    	    	
    }
	
	/**
	 * From the board we take information about settlements and cities and
	 * we update given nodes to indicate whether there's something built 
	 * on them and whether it belongs to our player or to the opponent.
	 * @param board
	 */	
	public void updateNodes(SOCBoard board) {
		Vector<SOCSettlement> settlements = board.getSettlements();
		nodes = new int[legalNodesCoord.length];
		for (SOCSettlement set: settlements) {
			int coord = set.getCoordinates();
			for (int i=0; i<legalNodesCoord.length; i++) {
				if (coord == legalNodesCoord[i]) {
					if (set.getPlayerNumber()==ourPlayerNumber) {
						nodes[i] = 3;
					} else {
						nodes[i] = 1;
					}
					break;
				}
			}
		}
		
		Vector<SOCCity> cities = board.getCities();
		for (SOCCity city: cities) {
			int coord = city.getCoordinates();
			for (int i=0; i<legalNodesCoord.length; i++) {
				if (coord == legalNodesCoord[i]) {
					if (city.getPlayerNumber()==ourPlayerNumber) {
						nodes[i] = 4;
					} else {
						nodes[i] = 2;
					}
					break;
				}
			}
		}
	}
	
	/**
	 * From the board we take information about roads and
	 * we update given edges to indicate whether there's a road
	 * on them and whether it belongs to our player or to the opponent.
	 * @param board
	 */	
	public void updateEdges(SOCBoard board) {
		Vector<SOCRoutePiece> roads = board.getRoadsAndShips();
		edges = new int[legalEdgesCoord.length];
		for (SOCRoutePiece road: roads) {
			int coord = road.getCoordinates();
			for (int i=0; i<legalEdgesCoord.length; i++) {
				if (coord == legalEdgesCoord[i]) {
					if (road.getPlayerNumber()==ourPlayerNumber) {
						edges[i] = 2;
					} else {
						edges[i] = 1;
					}
					break;
				}
			}
		}
	}
	
	/**
	 * information about type of hex on which robber is located is updated
	 * @param board
	 */
	public void updateRobber(SOCBoard board) {
		robber = board.getHexTypeFromCoord(board.getRobberHex());
		//in previous version DESERT type was 0, now it's 6, so we change it back to 0 as in original paper
		if (robber==6)
			robber=0;
	}
		
			
	public void updateAll(HashMap<Integer,SOCPlayer> players, SOCBoard board) {
		updateResources(ourPlayer, false);
		updateNodes(board);
		updateEdges(board);
		updateRobber(board);
		turn = ourPlayer.getGame().getRoundCount();
	}
	
	/**
	 * After placing settlement we need to update the node at which settlement was placed.
	 * We update all the nodes, because when looking for the settlement we were changing 
	 * nodes values.
	 */
	public void updatePlaceSettlement(SOCPlayer pn,  SOCBoard board) {
		SOCPlayingPiece piece = board.settlementAtNode(legalNodesCoord[lastPlacedSettlement]);
		if (piece==null) {
			nodes[lastPlacedSettlement] = 0;
		} else {
			int owner = piece.getPlayerNumber();
			if (piece.getType()==SOCPlayingPiece.SETTLEMENT) {
				if (owner==ourPlayerNumber) {
					nodes[lastPlacedSettlement] = 3;
				} else {
					nodes[lastPlacedSettlement] = 1;
				}
			} else {
				if (owner==ourPlayerNumber) {
					nodes[lastPlacedSettlement] = 4;
				} else {
					nodes[lastPlacedSettlement] = 2;
				}
			}
		}
		
		SOCSettlement set = board.getSettlements().lastElement();
		int coord = set.getCoordinates();
		for (int i=0; i<legalNodesCoord.length; i++) {
			if (coord == legalNodesCoord[i]) {
				lastPlacedSettlement = i;
				if (set.getPlayerNumber()==ourPlayerNumber) {
					nodes[i] = 3;
				} else {
					nodes[i] = 1;
				}
				break;
			}
		}
	}
	
	/**
	 * After placing the road we need to update information about the edge 
	 * on which it was placed. Because we're not passing the road piece
	 * as an argument we take from the board the last piece that was put.
	 */
	public void updatePlaceRoad(SOCPlayer pn) {
		SOCRoutePiece piece = board.roadOrShipAtEdge(legalEdgesCoord[lastPlacedRoad]);
		if (piece==null) {
			edges[lastPlacedRoad] = 0;
		} else if (piece.getPlayerNumber()==ourPlayerNumber) {
			edges[lastPlacedRoad] = 2;
		} else {
			edges[lastPlacedRoad] = 1;
		}
		
		SOCRoutePiece road = board.getRoadsAndShips().lastElement();
		int coord = road.getCoordinates();
		for (int i=0; i<legalEdgesCoord.length; i++) {
			if (coord == legalEdgesCoord[i]) {
				lastPlacedRoad = i;
				if (road.getPlayerNumber()==ourPlayerNumber) {
					edges[i] = 2;
				} else {
					edges[i] = 1;
				}
				break;
			}
		}

	}
	
	/**
	 * After placing the city, we need to update information about the 
	 * node on which it was placed. As we don't pass the city piece 
	 * in the function we take from the board the last city that was placed
	 */
	public void updatePlaceCity(SOCPlayer pn,  SOCBoard board) {
		SOCPlayingPiece piece = board.settlementAtNode(legalNodesCoord[lastPlacedCity]);
		if (piece==null) {
			nodes[lastPlacedCity] = 0;
		} else {
			int owner = piece.getPlayerNumber();
			if (piece.getType()==SOCPlayingPiece.SETTLEMENT) {
				if (owner==ourPlayerNumber) {
					nodes[lastPlacedCity] = 3;
				} else {
					nodes[lastPlacedCity] = 1;
				}
			} else {
				if (owner==ourPlayerNumber) {
					nodes[lastPlacedCity] = 4;
				} else {
					nodes[lastPlacedCity] = 2;
				}
			}
		}
		
		SOCCity city = board.getCities().lastElement();
		int coord = city.getCoordinates();
		for (int i=0; i<legalNodesCoord.length; i++) {
			if (coord == legalNodesCoord[i]) {
				lastPlacedCity = i;
				if (city.getPlayerNumber()==ourPlayerNumber) {
					nodes[i] = 4;
				} else {
					nodes[i] = 2;
				}
				break;
			}
		}

    }
	
	/**
	 * We update the type of hex on which robber stands
	 */
	public void updatePlaceRobber(SOCPlayer pn,  SOCBoard board, int robberHex) {
		robber = board.getHexTypeFromCoord(robberHex);
		//in previous version DESERT type was 0, now it's 6, so we change it back to 0 as in original paper
		if (robber==6)
			robber=0;
	}
	
	/**
	 * same as {@link #updatePlaceRobber(SOCPlayer, SOCBoard, int)}
	 */
	public void updatePlaceRobberAll(HashMap<Integer,SOCPlayer> players, 
		   		SOCBoard board, int robberHex) {
		robber = board.getHexTypeFromCoord(robberHex);
		//in previous version DESERT type was 0, now it's 6, so we change it back to 0 as in original paper
		if (robber==6)
			robber=0;
	 }
	
	/**
	 * Only our player's resources are changed, we don't track resources of other players
	 */
	public void updateSteal(SOCPlayer pn, int res, boolean all) {
		int[] resOp = pn.getResources().getAmounts(true);
		if (resOp[res] == 0) {
		   /*if the opponent has no resources of the given type we decrease the resources of the
		    * unknown type (we check that opponent has resources of unknown type before calling this function)
		    */
			if(!all) {
				resources[res]++;
			}
		} else {   
			if(all) {
				resources[res]+=resOp[res];
			} else {
				resources[res]++;
			}
		}
	}
	
	/**
	 * we don't track information about bought dev cards
	 */
	public void updateBuyDevCard(SOCPlayer pn, int boughtCard) {	    	
	}

	public void undoUpdateBuyDevCard(SOCPlayer pn, int boughtCard) {	
	}
	
	public void updatePlayedKnightCard(SOCPlayer pn, boolean willGetLA, SOCPlayer currentPlayerWithLA) {
	}
	
	public void updatePlayedDevCard(int cardPlayed) {
	}
	 
	public void updateAddSubstractResources(int[] resources, int[] amounts) {
	   for (int i=0; i <resources.length; i++) {
		   this.resources[resources[i]] += amounts[i];
	   }
		   
	}
	
	public void updateAddResourcesFromConstants(Vector<Integer> resources) {
	   for (int res : resources){
			//numbers.getResourcesForNumber(i, robberHex) returns resources, where CLAY = 1,
			//so we have to decrease each resource by 1
		   this.resources[res-1]++;
		}
	}
	
	public void updateSubstractResources(int[] resources, int[] amounts) {
	   for (int i=0; i <resources.length; i++) {
		   this.resources[resources[i]] -= amounts[i];
	   }   
   }
	
	public void updateSetResources(SOCPlayer pn, int[] resources) {
		this.resources=resources;
   }
	
	public int stateLength() {
		int size = 0;
		size += resources.length + hexes.length + nodes.length + 
				edges.length + 2;	
		return size;
	}
	
	public float[] getNormalizedStateArray() {
////		/*DEBUG*/
//		System.out.println("Resources length: " + resources.length + " : " + Arrays.toString(resources));
//		System.out.println("Hexes length: " + hexes.length + " : " + Arrays.toString(hexes));
//		System.out.println("Nodes length: " + nodes.length + " : " + Arrays.toString(nodes));
//		System.out.println("Edges length: " + edges.length + " : " + Arrays.toString(edges));
//		System.out.println("robber: " + robber + " turn: " + turn);
		
		float[] result = new float[stateLength()];
		int k = 0;
		for (int i=0; i< resources.length; i++) {
			result[i] = resources[i]/10.0f;
		}
		k+=resources.length;
		for (int i=0; i< hexes.length; i++) {
			result[i+k] = hexes[i];
		}
		k+=hexes.length;
		for (int i=0; i< nodes.length; i++) {
			result[i+k] = nodes[i]/4.0f;
		}
		k+=nodes.length;
		for (int i=0; i< edges.length; i++) {
			result[i+k] = edges[i]/2.0f;
		}
		k+=edges.length;
		result[k] = robber/5.0f;
		result[k+1] = turn/100.0f;
		
		/*DEBUG*/
//		System.out.println("result: " + Arrays.toString(result));
		
		return result;
	}
	
	public int[] getResources() {
		return resources;
	}
 
}
	
	
	
	 
	 


