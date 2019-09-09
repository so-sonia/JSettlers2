package soc.robot.rl;

public class StateValueFunctionNNDouble extends StateValueFunction {

	protected AproximatorNN states;
	
	protected AproximatorNN states2;
	
	public StateValueFunctionNNDouble() {
	}

	public StateValueFunctionNNDouble(boolean createMemory, int id) {
		ID = id;
		
		if(createMemory) {
			states = new AproximatorNN(id, 33);
			states2 = new AproximatorNN(id, 8);
		}
	}

	@Override
	public void printStats() {
		System.out.println("States1 are printed");
		states.printStats();
		System.out.println("States2 are printed");
		states2.printStats();

	}

	@Override
	public void writeMemory(String name) {
		System.out.println("States1 are written");
		states.writeMemory(name);
		System.out.println("States2 are written");
		states2.writeMemory(name);

	}

	@Override
	public void readMemory(String name) {
		System.out.println("States1 are read");
		states.readMemory(name);
		System.out.println("States2 are read");
		states2.readMemory(name); 
	}
	
	public AproximatorNN getStates() {
		return states;
	}
	
	public AproximatorNN getStates2() {
		return states2;
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
		if (!states2.getIsTraining()) {
	        states2.setIsTraining(true);
			Thread thread2 = new Thread(states2);
	        thread2.start();
		}
	}

	@Override
	public void startTesting() {
		states.setIsTraining(false);
		states2.setIsTraining(false);

	}

	@Override
	public void endTraining() {
		states.setIsTraining(false);
		states2.setIsTraining(false);
	}

}
