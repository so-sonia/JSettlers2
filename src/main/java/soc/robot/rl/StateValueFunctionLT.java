package soc.robot.rl;

public class StateValueFunctionLT extends StateValueFunction {

	protected StateMemoryLookupTable memory;
	
	public StateValueFunctionLT(boolean createMemory, int id) {
		ID = id;
		
		if(createMemory)
			memory = new StateMemoryLookupTable(id);
	}

	@Override
	public void printStats() {
		memory.memoryStats();
	}

	public StateMemoryLookupTable getMemory() {
		return memory;
	}
	
	@Override
	public void writeMemory(String name) {
		memory.writeMemory(name, false);
	}
	
	@Override
	public void readMemory(String name) {
		memory.readMemory(name);
		
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
		// TODO Auto-generated method stub

	}

	@Override
	public void startTesting() {
		// TODO Auto-generated method stub

	}

	@Override
	public void endTraining() {
	}

}
