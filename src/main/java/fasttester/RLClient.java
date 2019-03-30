package fasttester;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soc.disableDebug.D;
import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCGame;
import soc.game.SOCInventory;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.message.SOCDevCardAction;
import soc.message.SOCPlayerElement;
import soc.robot.OpeningBuildStrategy;
import soc.robot.SOCPossiblePiece;
import soc.robot.SOCPossibleRoad;
import soc.robot.rl.RLStrategyLookupTable;
import soc.robot.rl.RLStrategyLookupTable_small;
import soc.robot.rl.RLStrategyLookupTable_small_test;
import soc.robot.rl.RLStrategyLookupTable_test;
import soc.robot.rl.RLStrategyNN;
import soc.robot.rl.RLStrategyNN_opp1;
import soc.robot.rl.RLStrategyNN_opp1_test;
import soc.robot.rl.RLStrategyNN_opp4;
import soc.robot.rl.RLStrategyNN_oppsum;
import soc.robot.rl.RLStrategyNN_oppsum_test;
import soc.robot.rl.RLStrategyNN_test;
import soc.robot.rl.RLStrategyRandom;
import soc.robot.rl.StateMemoryLookupTable;
import soc.robot.rl.StateValueFunction;
import soc.robot.rl.RLStrategy;

public class RLClient {

	/* type of RLClients, type indicates how states values are stored (lookup table, neural network)
	 * and whether bot updates values (training) or only uses them (testing)
	 */
	public static final int RANDOM = 0;
	public static final int TRAIN_LOOKUP_TABLE = 1;
	public static final int TEST_LOOKUP_TABLE= 2;
	public static final int TRAIN_NN = 3;
	public static final int TEST_NN = 4;
	
	protected int playerNumber;
	
	protected String name;
	
	/*used from original game*/
	protected OpeningBuildStrategy openingBuildStrategy;
	
	protected SOCGame game;
	
	/*data that will be used for deriving soc state*/
	protected SOCPlayer ourPlayerData;
	
	protected int strategyType;
	
	/**
	 * memory of states to synchronize between many games at the same time.
	 * All the states are saved here, even if the brain is killed.
	 * TO change type for different RL Strategies look for phrase 
	 * "update type if changed memory"
	 */
//	protected StateMemoryLookupTable memory;
	protected StateValueFunction memory;
	protected StateValueFunction memory2;
	
	/**
	 * Responsible for all logic behind bots moves. Uses Reinforcement learning algorithm to 
	 * learn which movements bring about the best outcomes 
	 */
    protected RLStrategy rlStrategy;
    
    /*game specific variables*/
    protected int[] moveRobber;
    
    protected int[] roadsToBuildCoord;
    
    protected SOCResourceSet resourceChoices;
    
    protected int monopolyChoice;
    
    protected int buildingCoord;
    
    protected SOCResourceSet[] bankTrade;
    
    protected int lastStartingRoadTowardsNode;
	
    /*used for shared memory, had to comment, because of another memory type
     * update type if changed memory
     */
//    public RLClient(int i, int strategyType, int memoryType, StateMemoryLookupTable memory) {
//    	if (memory==null) {
//    		/*update type if changed memory*/
//    		this.memory = new StateMemoryLookupTable(i);
//    	} else {
//    		this.memory = memory;
//    	}    
//    	
//    	playerNumber = i;
//		name = "rlbot" + i;
//		this.strategyType = strategyType;
//		
//		/*DEBUGA*/
//		switch (strategyType) {
//		case RLClient.RANDOM:
//			System.out.println("Random bot created number: " + playerNumber);
//			break;
//		case RLClient.TRAIN_LOOKUP_TABLE:
//			System.out.println("Train lookup table bot created number: " + playerNumber);
//			break;
//		case RLClient.TEST_LOOKUP_TABLE:
//			System.out.println("Test lookup table bot created number: " + playerNumber);
//			break;
//		case RLClient.TRAIN_NN:
//			System.out.println("Train Neural Network bot created number: " + playerNumber);
//			break;
//		}
//		
//    }
    
    /*Depending on strategy type RLStrategy is created with neural networks or with lookup table*/
	public RLClient(int i, int strategyType, int memoryType) {
		playerNumber = i;
		name = "rlbot" + i;
		
		/*update type if changed memory*/
//		this.memory = new StateMemoryLookupTable(i);
//		this.memory = new StateValueFunction(i, 33);
//		this.memory2 = new StateValueFunction(i, 8);
		//for RLStrategyNN_opp1
		this.memory = new StateValueFunction(i, 72);
//		this.memory2 = new StateValueFunction(i, 7);
        
		
		this.strategyType = strategyType;
		
		/*read memory if filenumber provided*/
		if (memoryType!=-1) {
			if (strategyType==RLClient.TEST_LOOKUP_TABLE || strategyType==RLClient.TRAIN_LOOKUP_TABLE) {
				memory.readMemory(getName()+ "_" + memoryType);
			}
		}
		
		/*DEBUGA*/
		switch (strategyType) {
		case RLClient.RANDOM:
			System.out.println("Random bot created number: " + playerNumber);
			break;
		case RLClient.TRAIN_LOOKUP_TABLE:
			System.out.println("Train lookup table bot created number: " + playerNumber);
			break;
		case RLClient.TEST_LOOKUP_TABLE:
			System.out.println("Test lookup table bot created number: " + playerNumber);
			break;
		case RLClient.TRAIN_NN:
			System.out.println("Train Neural Network bot created number: " + playerNumber);
			break;
		case RLClient.TEST_NN:
			System.out.println("Test Neural Network bot created number: " + playerNumber);
			break;
			
		}
		
	}
	
	public void joinGame(String name, int[] botNumbers, String[] botNames) {
		game = new SOCGame(name);
		
		for (int i=0; i<botNumbers.length; i++) {
			game.addPlayer(botNames[i], botNumbers[i]);
			
             /**
              * set the robot flag
              */
//			game.getPlayer(botNumbers[i]).setRobotFlag(true, false);
		}
		game.isBotsOnly = true;
		
		
		ourPlayerData = game.getPlayer(getName());
		openingBuildStrategy = new OpeningBuildStrategy(game, ourPlayerData);
		
		switch (strategyType) {
//		    /*update type if changed memory
//		had to comment out becaouse of changed to NN*/
			case RLClient.RANDOM:
//				System.out.println("Random bot created number: " + playerNumber);
				rlStrategy = new RLStrategyRandom(game, playerNumber);
				break;
//			case RLClient.TRAIN_LOOKUP_TABLE:
////				System.out.println("Train lookup table bot created number: " + playerNumber);
//				rlStrategy = new RLStrategyLookupTable_small(game, playerNumber, memory);
//				break;
//			case RLClient.TEST_LOOKUP_TABLE:
////				System.out.println("Test lookup table bot created number: " + playerNumber);
//				rlStrategy = new RLStrategyLookupTable_small_test(game, playerNumber, memory);
//				break;
			case RLClient.TRAIN_NN:
//				RLStrategyNN rlStrategyTemp = new RLStrategyNN(game, playerNumber);
//				RLStrategyNN_opp1 rlStrategyTemp = new RLStrategyNN_opp1(game, playerNumber);
				RLStrategyNN_oppsum rlStrategyTemp = new RLStrategyNN_oppsum(game, playerNumber);
//				RLStrategyNN_opp4 rlStrategyTemp = new RLStrategyNN_opp4(game, playerNumber);
				rlStrategyTemp.setStates(memory);
//				rlStrategyTemp.setStates2(memory2);
				rlStrategy = rlStrategyTemp;
				break;
			case RLClient.TEST_NN:
//				RLStrategyNN_test rlStrategyTempTest = new RLStrategyNN_test(game, playerNumber);
//				RLStrategyNN_opp1_test rlStrategyTempTest = new RLStrategyNN_opp1_test(game, playerNumber);
				RLStrategyNN_oppsum_test rlStrategyTempTest = new RLStrategyNN_oppsum_test(game, playerNumber);
				rlStrategyTempTest.setStates(memory);
//				rlStrategyTempTest.setStates2(memory2);
				rlStrategy = rlStrategyTempTest;
				break;
		}
		moveRobber = null;
		roadsToBuildCoord = null;
		resourceChoices = new SOCResourceSet();
		monopolyChoice = 0;
		buildingCoord = 0;
		bankTrade = new SOCResourceSet[2];
		
//		ourTurn = false;
	}
	
	public void setBoardLayout(int[] hexes, int[] numbers, int robber) {
		if (game != null) {
			SOCBoard bd = game.getBoard();
	        bd.setHexLayout(hexes);
	        bd.setNumberLayout(numbers);
	        bd.setRobberHex(robber, false);
//	        game.updateAtBoardLayout()
		}
	}
	
	public String getName() {
		return name;
	}
	
	public int getPlayerNumber() {
		return playerNumber;
	}
	
	public void setPlayerNumber(int n) {
		playerNumber = n;
	}
	
	public SOCGame getGame() {
		return game;
	}

	public void handlePotentialSettlements(HashSet<Integer> psList) {
		for (int pn = game.maxPlayers - 1; pn >= 0; --pn)
        {
            SOCPlayer pl = game.getPlayer(pn);
            pl.setPotentialAndLegalSettlements(psList, true, null);
        }
		
	}
	
	public void handleFirstPlayer(int pn) {
		game.setFirstPlayer(pn);
	}
	
	public void handleGAMESTATE(int newState) {
		if (newState == 0)
            return;
        game.setGameState(newState);
	}
	
	/**
	 * 
	 * @param pn
	 * @param pieceType
	 * @param coord
	 */
    public void handlePUTPIECE(int pn, int pieceType, int coord)
    {
    	final SOCPlayer pl =  game.getPlayer(pn);
    	
    	switch (pieceType)
        {
        case SOCPlayingPiece.ROAD:
        	game.putPiece(new SOCRoad(pl, coord, null));
            break;

        case SOCPlayingPiece.SETTLEMENT:
        	game.putPiece(new SOCSettlement(pl, coord, null));
            break;

        case SOCPlayingPiece.CITY:
        	game.putPiece(new SOCCity(pl, coord, null));
            break;

        default:
            System.err.println
                ("handlePUTPIECE: player " + getName() + ": Unknown pieceType " + pieceType);
        }
    }
    
    public void handleTURN(int pn, int gs) {
    	 handleGAMESTATE(gs);

         game.setCurrentPlayerNumber(pn);
         game.updateAtTurn();
    }
	
	public int getFirstSettlement() {
		final int firstSettleNode = openingBuildStrategy.planInitialSettlements();
		return firstSettleNode;
	}
	
	public int getInitRoad() {
        final int roadEdge = openingBuildStrategy.planInitRoad();
        lastStartingRoadTowardsNode = openingBuildStrategy.getPlannedInitRoadDestinationNode();

//	        lastStartingPieceCoord = roadEdge;
//	        lastStartingRoadTowardsNode = openingBuildStrategy.getPlannedInitRoadDestinationNode();
        return roadEdge;
    }
	
	public int getSecondSettlement() {
		final int secondSettleNode = openingBuildStrategy.planSecondSettlement();
		if (secondSettleNode == -1) {
			System.err.println("robot assert failed: initSecondSettlement -1, "
	                + ourPlayerData.getName() + " leaving game " + game.getName());
		}
//		lastStartingPieceCoord = initSettlement;
		return secondSettleNode;
	}
	
	public int rollOrPlayKnight() {
		AbstractMap.SimpleEntry<Integer, int[]> action = rlStrategy.rollOrPlayKnight();
		if(action.getKey() == RLStrategy.PLAY_KNIGHT) {

    		
    		moveRobber = action.getValue();
    	} 
		return action.getKey();		
	}
	
	public void handleDEVCARDACTION(int pn, int ctype, int action)
    {
		SOCPlayer player = game.getPlayer(pn);
		
//		/*DEBUG*/
//		System.out.println("In pn" + playerNumber + " player" + pn + " action: " + action + " type: " + ctype +
//				" dev cards before new: " + 
//				player.getInventory().getByState(SOCInventory.NEW).size() +
//				" playable: " + player.getInventory().getByState(SOCInventory.PLAYABLE).size() +
//				" kept: " + player.getInventory().getByState(SOCInventory.KEPT).size()
//				);

        switch (action)
        {
        case SOCDevCardAction.DRAW:
            player.getInventory().addDevCard(1, SOCInventory.NEW, ctype);
            break;

        case SOCDevCardAction.PLAY:
            player.getInventory().removeDevCard(SOCInventory.OLD, ctype);
            break;

        case SOCDevCardAction.ADD_OLD:
            player.getInventory().addDevCard(1, SOCInventory.OLD, ctype);
            break;

        case SOCDevCardAction.ADD_NEW:
            player.getInventory().addDevCard(1, SOCInventory.NEW, ctype);
            break;
        } 
        
//        /*DEBUG*/
//		System.out.println("In pn" + playerNumber + " player" + pn + " action: " + action +  " type: " + ctype +
//				" dev cards after new: " + 
//				player.getInventory().getByState(SOCInventory.NEW).size() +
//				" playable: " + player.getInventory().getByState(SOCInventory.PLAYABLE).size() +
//				" kept: " + player.getInventory().getByState(SOCInventory.KEPT).size()
//				);
    }
	
	public void handlePLAYERELEMENT(int pn, int action, int type, int amount) {
		
	}
	
	public void handlePLAYERELEMENT_numKnights(int pn, int action, int amount) {
		SOCPlayer player = game.getPlayer(pn);
		player.setNumKnights(player.getNumKnights() + amount);
		game.updateLargestArmy();
	}
	
	public void handlePlayerElement_numRscs(int pn, int type, int action, int amount) {
		
		SOCPlayer pl = game.getPlayer(pn);
		switch (action)
        {
        case SOCPlayerElement.SET:
            pl.getResources().setAmount(amount, type);
            break;

        case SOCPlayerElement.GAIN:
            pl.getResources().add(amount, type);
            break;

        case SOCPlayerElement.LOSE:
            if (type != SOCResourceConstants.UNKNOWN)
            {
                int playerAmt = pl.getResources().getAmount(type);
                if (playerAmt >= amount)
                {
                    pl.getResources().subtract(amount, type);
                }
                else
                {
                    pl.getResources().subtract(amount - playerAmt, SOCResourceConstants.UNKNOWN);
                    pl.getResources().setAmount(0, type);
                }
            }
            else
            {
                SOCResourceSet rs = pl.getResources();

                /**
                 * first convert player's known resources to unknown resources,
                 * then remove mes's unknown resources from player
                 */
                rs.convertToUnknown();
                pl.getResources().subtract(amount, SOCResourceConstants.UNKNOWN);
            }

            break;
        }
	}
	
	public void handlePlayedDevCardFlag(int pn, boolean value) {
		SOCPlayer player = game.getPlayer(pn);
		player.setPlayedDevCard(value);
	}
	
	public int getRobberPlace() {
		return moveRobber[0];
	}
	
	public int getRobberVictim() {
		return moveRobber[1];
	}
	
	public void handleRobberMoved(int coord) {
		game.getBoard().setRobberHex(coord, true);
	}
	
	public SOCResourceSet handleDiscard(int numCards) {
		return rlStrategy.discard(numCards);
	}
	
	public int moveRobber() {
		moveRobber = rlStrategy.moveRobber();
		return moveRobber[0];
	}
	
	public int buildOrTradeOrPlayCard() {
		AbstractMap.SimpleEntry<Integer, int[]> action = rlStrategy.buildOrTradeOrPlayCard();
    	
    	switch(action.getKey()){
    	
    	case RLStrategy.END_TURN:
    		return RLStrategy.END_TURN;
    		
    	case RLStrategy.PLAY_ROADS:
    		roadsToBuildCoord = action.getValue();
    		return RLStrategy.PLAY_ROADS;
    		
    	case RLStrategy.PLAY_DISC:
    		
    		int[] resourcesWanted = action.getValue();
    		resourceChoices.clear();
    		//in RLStrategy resources are counted starting with 0, so adding +1 to get numbers
    		//as in SOCResourceSet
    		resourceChoices.add(1, resourcesWanted[0] + 1);
    		resourceChoices.add(1, resourcesWanted[1] + 1);
    		return RLStrategy.PLAY_DISC;
    		
    	case RLStrategy.PLAY_MONO:
    		
    		int[] resourceWanted = action.getValue();
    		monopolyChoice = resourceWanted[0]+1;
    		return RLStrategy.PLAY_MONO;
            
    	case RLStrategy.PLAY_KNIGHT:
    		moveRobber = action.getValue();
    		return RLStrategy.PLAY_KNIGHT;
    	
    	case RLStrategy.TRADE_BANK:
    		
    		int[] tradeOffer = action.getValue();
    		
    		 SOCResourceSet give = new SOCResourceSet();
             SOCResourceSet get = new SOCResourceSet();
             
           //in RLStrategy resources are counted starting with 0, so adding +1 to get numbers
     		//as in SOCResourceSet
             give.add(tradeOffer[2], tradeOffer[0]+1);
             get.add(1, tradeOffer[1]+1);
             
             bankTrade[0] = give;
             bankTrade[1] = get;

             return RLStrategy.TRADE_BANK;
   		
    	case RLStrategy.PLACE_SETTLEMENT:
    		
    		int[] settlementToBuildCoord = action.getValue();
    		buildingCoord = settlementToBuildCoord [0];
    		
    		return RLStrategy.PLACE_SETTLEMENT;
    		
    	case RLStrategy.PLACE_ROAD:
    		
    		int[] roadToBuildCoord = action.getValue();
    		buildingCoord = roadToBuildCoord [0];
    		
    		return RLStrategy.PLACE_ROAD;
            
    	case RLStrategy.PLACE_CITY:
    		
    		int[] cityToBuildCoord = action.getValue();
    		buildingCoord = cityToBuildCoord [0];
    		
    		return RLStrategy.PLACE_CITY;

    	case RLStrategy.BUY_DEVCARD:
    		return RLStrategy.BUY_DEVCARD;
    	
    	default:
    		return -1;
    	}
	}
	
	public int[] getRoadsToBuildCoord() {
		return roadsToBuildCoord;
	}
	
	public SOCResourceSet getDicoveryResources() {
		return resourceChoices;
	}
	
	public int getMonopolyChoice() {
		return monopolyChoice;
	}
	
	public int getBuildingCoord() {
		return buildingCoord;
	}
	
	public void handleDevCardCount(int value) {
		game.setNumDevCards(value);
	}

	public SOCResourceSet[] getBankTradeOffer() {
		return bankTrade;
	}
	
	public SOCPlayer getOurPlayerData() {
		return ourPlayerData;
	}
	
	public void cancelBuildRoadAtInit() {
		ourPlayerData.clearPotentialSettlement(lastStartingRoadTowardsNode);
	}
	
	public int[] getVictims(int posRob) {
		List<SOCPlayer> victims = game.getPlayersOnHex(posRob);
		if (victims.size()==0) {
			return null;
		} else {
			int[] pnNums = new int[victims.size()];
			for (int i=0; i< victims.size(); i++) {
				pnNums[i] = victims.get(i).getPlayerNumber();
			}
			return pnNums;
		}
		
	}
	
	public int getSet2Away() {
		return lastStartingRoadTowardsNode;
	}
	
	public RLStrategy getRLStrategy() {
		return rlStrategy;
	}
	
	public void writeMemory(int n, boolean org) {
		memory.writeMemory(getName() + "_" + n, org);
//		memory2.writeMemory(getName() + "_mem2_" + n, org);
	}
	
	public void memoryStats() {
		memory.memoryStats();
//		memory.stats();
	}
	
	public void handleEndGame(int winner) {
		rlStrategy.updateReward(winner);
	}
	
	public void changeLR(double alpha) {
		rlStrategy.changeLR(alpha);
	}
	
	/*update type if changed memory*/
//	public StateMemoryLookupTable getMemory() {
//		return memory;
//	}
	
	public StateValueFunction getMemory1() {
		return memory;
	}
	
	public StateValueFunction getMemory2() {
		return memory2;
	}
	
	public void startTraining() {
		strategyType = RLClient.TRAIN_NN;
		memory.setIsTraining(true);
//		memory2.setIsTraining(true);
		Thread thread = new Thread(memory);
        thread.start();
//        Thread thread2 = new Thread(memory2);
//        thread2.start();
//        memory.printStats();
	}
	
	public void startTesting() {
		memory.setIsTraining(false);
//		memory2.setIsTraining(false);
		strategyType = RLClient.TEST_NN;
//		memory.printStats();
	}
	
	public void endTraining() {
		memory.setIsTraining(false);
//		memory2.setIsTraining(false);
//		memory.printCounter();
//		memory2.printCounter();
	}

}
