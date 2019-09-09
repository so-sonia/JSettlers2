package soc.robot.rl;

public abstract class StateValueFunction {

	protected int ID;
	
	public StateValueFunction() {		
	}	
	
	/**
	 * General class used by RLClient and RLStrategy to store values of states.
	 * Child classes include Lookup Table and Neural Network.
	 * 
	 * @param createMemory indicated whether we should create memory, if not later 
	 * {@link #readMemory()} should be used to read from file or read shared memory.
	 * @param id - number of the player, used to create names of files to write memory
	 */
	public StateValueFunction(boolean createMemory, int id) {		
	}
	
	public abstract void printStats();
	
	public abstract void writeMemory(String name);
	
	public abstract void readMemory(String name);
	
	public abstract void setStateValue();
	
	public abstract float getStateValue();
	
	public abstract void startTraining();
	
	public abstract void startTesting();
	
	public abstract void endTraining();

}
