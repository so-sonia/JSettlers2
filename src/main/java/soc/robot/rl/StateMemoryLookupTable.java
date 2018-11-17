package soc.robot.rl;

import java.util.HashMap;
import java.util.List;

public class StateMemoryLookupTable  {

	 /** Memory for all the states type 1*/
    protected HashMap<List<Integer>, Double[]> states;
    
    /** Memory for all the states type 2 */
    protected HashMap<List<Integer>, Double[]> states2;
	
	public StateMemoryLookupTable() {
		 states = new HashMap<List<Integer>, Double[]>();    
	     states2 = new HashMap<List<Integer>, Double[]>(); 
	}

	public Double[] getState1Value(List<Integer> state) {
		return(states.get(state));
	}

	public void setState1Value(List<Integer> state, Double[] value) {
		states.put(state,  value);
	}
	
	public Double[] getState2Value(List<Integer> state) {
		return(states2.get(state));
	}

	public void setState2Value(List<Integer> state, Double[] value) {
		states.put(state,  value);
	}

}
