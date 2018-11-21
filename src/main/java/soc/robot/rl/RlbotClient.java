package soc.robot.rl;

import soc.game.SOCGame;
import soc.message.SOCChangeFace;
import soc.message.SOCDeleteGame;
import soc.message.SOCForceEndTurn;
import soc.message.SOCMessage;
import soc.message.SOCRobotDismiss;
import soc.message.SOCSitDown;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.robot.SOCRobotDM;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;

public class RlbotClient extends SOCRobotClient {
	
	/**
	 * Every how many rounds should memory of states be updated
	 */
	protected int updateFrequency;
	
	/**
	 * memory of states to synchronize between many games at the same time.
	 * All the states are saved here, even if the brain is killed.
	 * TO change type for different RL Strategies look for phrase 
	 * "update type if changed memory"
	 */
	protected StateMemoryLookupTable memory;
	
	private static final String RBCLASSNAME_RL = RlbotClient.class.getName();
	
	public RlbotClient(String h, int p, String nn, String pw, String co, int updateFrequency) {
		super(h, p, nn, pw, co);
		this.updateFrequency = updateFrequency;
		
		/*update type if changed memory*/
		this.memory = new StateMemoryLookupTable();
		rbclass = RBCLASSNAME_RL;
	}
	
	   /**
     * Factory to provide our client's {@link Sample3PBrain} to games instead of the standard brain.
     *<P>
     * Javadocs from original factory:
     *<BR>
     * {@inheritDoc}
     */
    @Override
    public SOCRobotBrain createBrain
        (final SOCRobotParameters params, final SOCGame ga, final CappedQueue<SOCMessage> mq)
    {
        return new RlbotBrain2(this, params, ga, mq);
    }

	public static void main(String[] args) {
		if (args.length < 5)
        {
            System.err.println("Java Settlers sample robotclient");
            System.err.println("usage: java " + RBCLASSNAME_RL + " hostname port_number userid password cookie updateFrequency");

            return;
        }

        RlbotClient cli = new RlbotClient(args[0], Integer.parseInt(args[1]), args[2], args[3], 
        		args[4], Integer.parseInt(args[5]));
        cli.init();

	}
	
	/**
     * Added to calculate compare with {@link #updateFrequency} and decide how
     * often we should update shared memory
     * 
     */
    public int getGamesPlayed() {
		return gamesPlayed;
	}
    
	/**
     * Added to calculate how frequently we should update shared memory.
     * 
     */
    public int getUpdateFrequency() {
		return updateFrequency;
	}
    
    /**
     * memory is staying by the client so it's not destroyed after killing the brain
     * update type if changed memory
     */
    public StateMemoryLookupTable getStateMemory() {
		return memory;
	}
    
    
    /**
     * handle the "someone is sitting down" message
     * @param mes  the message
     */
    @Override
    protected void handleSITDOWN(SOCSitDown mes)
    {
        /**
         * tell the game that a player is sitting
         */
        SOCGame ga = games.get(mes.getGame());

        if (ga != null)
        {
            ga.addPlayer(mes.getNickname(), mes.getPlayerNumber());

            /**
             * set the robot flag
             */
            ga.getPlayer(mes.getPlayerNumber()).setRobotFlag(mes.isRobot(), false);

            /**
             * let the robot brain find our player object if we sat down
             */
            if (nickname.equals(mes.getNickname()))
            {
                SOCRobotBrain brain = robotBrains.get(mes.getGame());

                /**
                 * retrieve the proper face for our strategy
                 */
                int faceId;
                switch (brain.getRobotParameters().getStrategyType())
                {
                case SOCRobotDM.SMART_STRATEGY:
                    faceId = -1;  // smarter robot face
                    break;

                default:
                    faceId = 0;   // default robot face
                }

                brain.setOurPlayerData();
                brain.start();

                /**
                 * change our face to the robot face
                 */
                put(SOCChangeFace.toCmd(ga.getName(), mes.getPlayerNumber(), faceId));
            }
            else
            {
                /**
                 * add tracker for player in previously vacant seat
                 */
                RlbotBrain2 brain = (RlbotBrain2) robotBrains.get(mes.getGame());

                if (brain != null)
                {
                    brain.addPlayerTracker(mes.getPlayerNumber());
                    brain.handleRLStartegyAfterAddingPlayer();
                }
            }
        }
    }
    
    @Override
    protected void handleROBOTDISMISS(SOCRobotDismiss mes) {
    	/*DEBUG*/
    	System.out.println("Robot dismissed " + getNickname());
    	
//    	if ((gamesPlayed % updateFrequency)== (updateFrequency-1)) {
//        	memory.writeMemory(getNickname());
    	if ((gamesPlayed % updateFrequency)== 1) {
        	memory.writeMemory(getNickname());        	 	
        	
        	/*DEBUG*/
        	System.out.println("Memory saved");
        } 
//    	else if ((gamesPlayed % updateFrequency)== 0) {
//        	memory.synchronize();
//        }
//    	
    	super.handleROBOTDISMISS(mes);
    }
    
    @Override
    protected void handleDELETEGAME(SOCDeleteGame mes)
    {
    	/*DEBUG*/
    	System.out.println("Robot dismissed " + getNickname());
//    	System.out.println("gamesPlayed: " + gamesPlayed + "; gamesPlayed % updateFrequency: " +
//    			gamesPlayed % updateFrequency);    	
    	
    	if ((gamesPlayed % updateFrequency)== (updateFrequency-1)) {
        	memory.writeMemory(getNickname());     	 	
        	
        	/*DEBUG*/
        	System.out.println("Memory saved");
        } 
//    	else if ((gamesPlayed % updateFrequency)== 0) {
//        	memory.synchronize();
//        }
//    	   	
    	super.handleDELETEGAME(mes);
    }
    

    


}
