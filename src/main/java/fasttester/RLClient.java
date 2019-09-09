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
import soc.robot.rl.RLStrategyNN_dialogue;
import soc.robot.rl.RLStrategyNN_dialogue_test;
import soc.robot.rl.RLStrategyNN_opp1;
import soc.robot.rl.RLStrategyNN_opp1_test;
import soc.robot.rl.RLStrategyNN_opp4;
import soc.robot.rl.RLStrategyNN_oppsum;
import soc.robot.rl.RLStrategyNN_oppsum_test;
import soc.robot.rl.RLStrategyNN_test;
import soc.robot.rl.RLStrategyRandom;
import soc.robot.rl.StateMemoryLookupTable;
import soc.robot.rl.StateValueFunction;
import soc.robot.rl.StateValueFunctionLT;
import soc.robot.rl.StateValueFunctionNN;
import soc.robot.rl.RLStrategy;

public class RLClient {

	/* type of RLClients, type indicates how states values are stored (lookup table, neural network)
	 * and whether bot updates values (training) or only uses them (testing)
	 */
	public static final int RANDOM  = 0;
	public static final int TRAIN_LOOKUP_TABLE = 1;
	public static final int TEST_LOOKUP_TABLE= 2;
	public static final int TRAIN_NN = 3;
	public static final int TEST_NN = 4;
	public static final int FAST_BUILTIN = 5;
	public static final int SMART_BUILTIN = 6;

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
//	protected StateValueFunction memory;
//	protected StateValueFunction memory2;
	protected StateValueFunction stateValueFunction;
	
	
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
    
    
    /*Depending on strategy type RLStrategy is created with neural networks or with lookup table*/
	public RLClient(int i, int strategyType, int memoryType) {
		playerNumber = i;
		name = "rlbot" + i;
		
		this.strategyType = strategyType;
		
		/*read memory if filenumber provided*/
		if (memoryType!=-1) {
			if (strategyType==RLClient.TEST_LOOKUP_TABLE || strategyType==RLClient.TRAIN_LOOKUP_TABLE) {
//				memory.readMemory(getName()+ "_" + memoryType);
				this.stateValueFunction = new StateValueFunctionLT(false, i);
				stateValueFunction.readMemory(getName()+ "_" + memoryType);
			}
		} else {
			if (strategyType==RLClient.TEST_LOOKUP_TABLE || strategyType==RLClient.TRAIN_LOOKUP_TABLE) {
//				memory.readMemory(getName()+ "_" + memoryType);
				this.stateValueFunction = new StateValueFunctionLT(true, i);
			} else if (strategyType==RLClient.TEST_NN|| strategyType==RLClient.TRAIN_NN) {
				/*changed type if changed memory*/
				this.stateValueFunction = new StateValueFunctionNN(true, i);
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
		case RLClient.FAST_BUILTIN:
			System.out.println("Fast Built In bot created number: " + playerNumber);
			break;
		case RLClient.SMART_BUILTIN:
			System.out.println("Smart Built In bot created number: " + playerNumber);
			break;
		}
		
	}
	
	/**used in child classes, to play with fullinfo server*/
	public void joinGame(SOCGame game) {
		this.game = game;
		
		ourPlayerData = game.getPlayer(getName());
		openingBuildStrategy = new OpeningBuildStrategy(game, ourPlayerData);
		
		switch (strategyType) {
		
			case RLClient.RANDOM:
	//			System.out.println("Random bot created number: " + playerNumber);
				rlStrategy = new RLStrategyRandom(game, playerNumber);
				break;
			case RLClient.TRAIN_LOOKUP_TABLE:
	//			System.out.println("Train lookup table bot created number: " + playerNumber);
				RLStrategyLookupTable rlStrategyLT = new RLStrategyLookupTable(game, playerNumber);
				rlStrategyLT.setStateValueFunction(stateValueFunction);
				rlStrategy = rlStrategyLT;
				break;
			case RLClient.TEST_LOOKUP_TABLE:
	//			System.out.println("Test lookup table bot created number: " + playerNumber);
				RLStrategyLookupTable rlStrategyLTtest  = new RLStrategyLookupTable_test(game, playerNumber);
				rlStrategyLTtest.setStateValueFunction(stateValueFunction);
				rlStrategy = rlStrategyLTtest;
				break;
			case RLClient.TRAIN_NN:
//				RLStrategyNN rlStrategyTemp = new RLStrategyNN(game, playerNumber);
//				RLStrategyNN_opp1 rlStrategyTemp = new RLStrategyNN_opp1(game, playerNumber);
//				RLStrategyNN_oppsum rlStrategyTemp = new RLStrategyNN_oppsum(game, playerNumber);
//				RLStrategyNN_opp4 rlStrategyTemp = new RLStrategyNN_opp4(game, playerNumber);
				RLStrategyNN_dialogue rlStrategyTemp = new RLStrategyNN_dialogue(game, playerNumber);				
				rlStrategyTemp.setStateValueFunction(stateValueFunction);
				rlStrategy = rlStrategyTemp;
				break;
			case RLClient.TEST_NN:
//				RLStrategyNN_test rlStrategyTempTest = new RLStrategyNN_test(game, playerNumber);
//				RLStrategyNN_opp1_test rlStrategyTempTest = new RLStrategyNN_opp1_test(game, playerNumber);
//				RLStrategyNN_oppsum_test rlStrategyTempTest = new RLStrategyNN_oppsum_test(game, playerNumber);
				RLStrategyNN_dialogue_test rlStrategyTempTest= new RLStrategyNN_dialogue_test(game, playerNumber);
				rlStrategyTempTest.setStateValueFunction(stateValueFunction);
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
	
	/**RlStrategies are created and {@link RLClient#stateValueFunction} is passed, which
	 * has memory from all previous games.
	 * 
	 * @param name of the player
	 * @param botNumbers - used to add opponents to our version of game
	 * @param botNames
	 */
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
		
		joinGame(game);
		
	}
	
	/**
	 * Board is randomly created in the instant of the game at server, 
	 * information about the board must be passed to all players
	 * @param hexes - order of hexes on the board
	 * @param numbers - number belonging to each hex
	 * @param robber - where is desert hex (robber at the beginning of the game 
	 * in on the desert hex.
	 */
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

	/**
	 * Used at the beginning of the game, from the game at server we
	 * pass places, where clients can put their settlements.
	 * @param psList - list of potential settlements
	 */
	public void handlePotentialSettlements(HashSet<Integer> psList) {
		for (int pn = game.maxPlayers - 1; pn >= 0; --pn)
        {
            SOCPlayer pl = game.getPlayer(pn);
            pl.setPotentialAndLegalSettlements(psList, true, null);
        }
		
	}
	
	/**
	 * Set the player who is making the first move
	 * @param pn - player number
	 */
	public void handleFirstPlayer(int pn) {
		game.setFirstPlayer(pn);
	}
	
	
	/**
	 * Game state is set on client's version of the game
	 * @param newState
	 */
	public void handleGAMESTATE(int newState) {
		if (newState == 0)
            return;
        game.setGameState(newState);
	}
	
	/**
	 * Place the piece of the given type on client's version of the game
	 * @param pn player who puts the piece
	 * @param pieceType type of the piece: road, settlement, city
	 * @param coord coordinations where the piece should be put
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
    
    /**
     * Game state is set on client's version of the game.
     * Current player is set and {@link SOCGame#updateAtTurn()} is invoked.
     * 
     * @param pn - current player number
     * @param gs - game state
     */
    public void handleTURN(int pn, int gs) {
    	 handleGAMESTATE(gs);

         game.setCurrentPlayerNumber(pn);
         game.updateAtTurn();
    }
	
    /**
     * All possible settlements are searched and the best one is chosen
     * as the place, where client should build
     * @return id of the first settlement, where client wants to build
     */
	public int getFirstSettlement() {
		final int firstSettleNode = openingBuildStrategy.planInitialSettlements();
		return firstSettleNode;
	}
	
	/**
	 * Road must be adjacent to the settlement chosen at the beginning of the game.
	 * Method is called both after first and second settlement placement:
	 * {@link RLClient#getFirstSettlement()} or {@link RLClient#getSecondSettlement()}
	 * @return id of the road
	 */
	public int getInitRoad() {
        final int roadEdge = openingBuildStrategy.planInitRoad();
        lastStartingRoadTowardsNode = openingBuildStrategy.getPlannedInitRoadDestinationNode();

//	        lastStartingPieceCoord = roadEdge;
//	        lastStartingRoadTowardsNode = openingBuildStrategy.getPlannedInitRoadDestinationNode();
        return roadEdge;
    }
	
	/**
	 * Again search is invoked and player choses the best spot for the second settlement
	 * @return id of the second settlement
	 */
	public int getSecondSettlement() {
		final int secondSettleNode = openingBuildStrategy.planSecondSettlement();
		if (secondSettleNode == -1) {
			System.err.println("robot assert failed: initSecondSettlement -1, "
	                + ourPlayerData.getName() + " leaving game " + game.getName());
		}
//		lastStartingPieceCoord = initSettlement;
		return secondSettleNode;
	}
	
	/**
	 * At the beginning of each turn if player has a knight card chose whether to roll or
	 * play the card
	 * @return id of the action chosen by the player
	 */
	public int rollOrPlayKnight() {
		AbstractMap.SimpleEntry<Integer, int[]> action = rlStrategy.rollOrPlayKnight();
		if(action.getKey() == RLStrategy.PLAY_KNIGHT) {

    		//if knight card is chosen save where we want to move the robber
    		moveRobber = action.getValue();
    	} 
		return action.getKey();		
	}
	
	/**
	 * Inventory of the player is changed according to the action.
	 * @param pn - player who played the dev card
	 * @param ctype - type of the dev card
	 * @param action - as in {@link SOCDevCardAction#PLAY}
	 */
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
	
	/**
	 * Not used
	 * @param pn
	 * @param action
	 * @param type
	 * @param amount
	 */
	public void handlePLAYERELEMENT(int pn, int action, int type, int amount) {
		
	}
	
	/**
	 * Given amount of knight cards is added to the current number of played knight cards.
	 * Then {@link SOCGame#updateLargestArmy()} is invoked.
	 * @param pn player for whom tu update number of knights
	 * @param action currently not used, number of knights is always increased
	 * @param amount number of knights added to the current number
	 */
	public void handlePLAYERELEMENT_numKnights(int pn, int action, int amount) {
		SOCPlayer player = game.getPlayer(pn);
		player.setNumKnights(player.getNumKnights() + amount);
		game.updateLargestArmy();
	}
	
	/**
	 * All actions regarding resources like gaining, setting or losing are handled here.
	 * If opponent lost resources of type unknown, first all his resources are converted
	 * to unknown and then the specified number of resources is subtracted.
	 * @param pn player for whom we change number of resources
	 * @param type of the resource
	 * @param action like in {@link SOCPlayerElement#SET}
	 * @param amount
	 */
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
	
	/**
	 * Set the value of the flag
	 * @param pn - player for  whom to set the playedDevCardFlag
	 * @param value - of the flag
	 */
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
	
	/**
	 * Set the new location of the robber
	 * @param coord - id of hex where robber is being moved
	 */
	public void handleRobberMoved(int coord) {
		game.getBoard().setRobberHex(coord, true);
	}
	
	/**
	 * Returns the set of cards that will be discarde. Called after 7 was rolled.
	 * 
	 * @param numCards number of cards that must be discarded
	 * @return set of cards that can be discarded
	 */
	public SOCResourceSet handleDiscard(int numCards) {
		return rlStrategy.discard(numCards);
	}
	
	/**
	 * get location where to move robber and the victim
	 * from whom we want to steal
	 * @return array with two elements. First is coordination of the hex
	 * where we want to move the robber. Second is victim from whom we want to steal.
	 * If there's only one victim, second element is -1, because server will not ask 
	 * us for the victim.
	 */
	public int moveRobber() {
		moveRobber = rlStrategy.moveRobber();
		return moveRobber[0];
	}
	
	/**
	 * During main game after roll, tell server what action client wants to perform
	 * @return type of action as in {@link RLStrategy#PLACE_CITY}.
	 */
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
	
	/**
	 * After playing the road development card we have to give coordinates
	 * of the roads we want to build.
	 * @return array of length 2 containing coordinations of 2 roads
	 */
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
	
	/**
	 * Set number of development card left in the game. 
	 * Used when someone bought a card.
	 * @param value number of development cards left in the game
	 */
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
	
	public void writeMemory(int n) {
		stateValueFunction.writeMemory(getName() + "_" + n);
	}
	
	public void stateValueFunctionStats() {
		stateValueFunction.printStats();
		
//		memory.memoryStats();
//		memory.stats();
	}
	
	/**
	 * Invoke {@link RLStrategy#updateReward(winner)} and reinforce the state value
	 * @param winner player number of the winner
	 */
	public void handleEndGame(int winner) {
		rlStrategy.updateReward(winner);
	}
	
	public void changeLR(double alpha) {
		rlStrategy.changeLR(alpha);
	}
	
	public void startTraining() {
		if (stateValueFunction!=null) {
			strategyType = RLClient.TRAIN_NN;
			stateValueFunction.startTraining();
		}
//        memory.printStats();
	}
	
	public void startTesting() {
		if (stateValueFunction!=null) {
			stateValueFunction.startTesting();
			strategyType = RLClient.TEST_NN;
		}
//		memory.printStats();
	}
	
	public void endTraining() {
		if (stateValueFunction!=null)
			stateValueFunction.endTraining();
	}

	public void setStateValueFunction(StateValueFunction sharedSVFunction) {
		stateValueFunction = sharedSVFunction;		
	}
	
	public StateValueFunction getStateValueFunction() {
		return stateValueFunction;		
	}

}
