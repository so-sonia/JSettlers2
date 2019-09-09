package soc.robot.rl;

public class StateValueFunctionNN extends StateValueFunction {
	
	protected AproximatorNN states;

	public StateValueFunctionNN() {
		// TODO Auto-generated constructor stub
	}

	public StateValueFunctionNN(boolean createMemory, int id) {
		ID = id;
		
		if(createMemory) {
			states = new AproximatorNN(id, 152);
		}
	}

	@Override
	public void printStats() {
		states.printStats();
	}

	@Override
	public void writeMemory(String name) {
		states.writeMemory(name);
	}

	@Override
	public void readMemory(String name) {
		states.readMemory(name);
	}
	
	public AproximatorNN getStates() {
		return states;
	}

	@Override
	public void setStateValue() {
		// TODO Auto-generated method stub

	}

	@Override
	public float getStateValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void startTraining() {
		if (!states.getIsTraining()) {
			states.setIsTraining(true);
			Thread thread = new Thread(states);
	        thread.start();
		}
	}

	@Override
	public void startTesting() {
		states.setIsTraining(false);
	}

	@Override
	public void endTraining() {
		states.setIsTraining(false);

	}

}
