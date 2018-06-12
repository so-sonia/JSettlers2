package soc.robot.rl;

import soc.game.SOCGame;
import soc.message.SOCMessage;
import soc.robot.SOCRobotBrain;
import soc.robot.SOCRobotClient;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;

public class RlbotClient extends SOCRobotClient {
	
	private static final String RBCLASSNAME_RL = RlbotClient.class.getName();
	
	public RlbotClient(String h, int p, String nn, String pw, String co) {
		super(h, p, nn, pw, co);
		
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
            System.err.println("usage: java " + RBCLASSNAME_RL + " hostname port_number userid password cookie");

            return;
        }

        RlbotClient cli = new RlbotClient(args[0], Integer.parseInt(args[1]), args[2], args[3], args[4]);
        cli.init();

	}

}
