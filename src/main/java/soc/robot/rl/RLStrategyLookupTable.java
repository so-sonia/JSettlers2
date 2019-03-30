package soc.robot.rl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import soc.game.SOCGame;
import soc.game.SOCPlayer;



public class RLStrategyLookupTable extends RLStrategy{

	/** variable to remember previous state of type 1: for all opponents */
    protected HashMap<SOCPlayer, SOCStateArray> oldState;
    
    /** variable to remember previous general state (type 2) */
    protected SOCStateArray oldState2;
    
    /** memory of state value pairs shared by all brain of this player */
    protected StateMemoryLookupTable memory;
    
//    /** Memory for all the states type 1*/
//    protected HashMap<List<Integer>, Double[]> states;
//    
//    /** Memory for all the states type 2 */
//    protected HashMap<List<Integer>, Double[]> states2;
//	
	public RLStrategyLookupTable(SOCGame game, int pn, StateMemoryLookupTable memory) {
		super(game, pn);
		this.memory = memory;
		state = new SOCState(ourPlayerNumber, players);
	    state.updateAll(players, board);      
        
//        states = new HashMap<List<Integer>, Double[]>();    
//        states2 = new HashMap<List<Integer>, Double[]>(); 
        oldState = new HashMap<SOCPlayer, SOCStateArray>();
        
//        readMemory();
//        this.memory = br.getClient().getStateMemory();
        
//        int gamesPlayed = br.getClient().getGamesPlayed();
//        int updateFrequency = br.getClient().getUpdateFrequency();
//        if ((gamesPlayed % updateFrequency)==0) {
//        	synchroniseMemory();
//        } else {
//        	readMemory();
//        }
 
        ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();

        /* adding to memory the state at the beginning of the game */
    	for (SOCPlayer opp : opponents) {
    		SOCStateArray playerState = new SOCStateArray(state.getState(opp));
    		int points = opp.getTotalVP();
    		Float value = (float)(new Random().nextGaussian()*0.05 + 0.5); //or maybe random?
    		memory.setState1Value(playerState, new Float[] {value, Float.valueOf(1)});
			oldState.put(opp, playerState);
    		
    		int state_value = Math.round(value.floatValue()*10);	
    		
    		opp_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(state_value)));
    		
    	}
    	
    	int[] secondState = new int[6];
    	opp_states.sort(new Comparator<CustomPair>() {
		    public int compare(CustomPair o1, CustomPair o2) {
		        return o2.getKey().compareTo(o1.getKey());
		    }
		});
    	
    	for(int i = 0; i<opp_states.size(); i++) {
    		secondState[i*2] = opp_states.get(i).getKey().intValue();
    		secondState[i*2 + 1] = opp_states.get(i).getValue().intValue();
    	}
    	
    	Float value = (float)(new Random().nextGaussian()*0.05 + 0.5); //or maybe random?
    	oldState2 = new SOCStateArray(secondState);
    	memory.setState2Value(oldState2, new Float[] {value, Float.valueOf(1)});
		
	}
	

	protected float getStateValue(SOCState tmpState) {
    	ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();

    	for (SOCPlayer opp : opponents) {
    		SOCStateArray playerState = new SOCStateArray(tmpState.getState(opp));
    		
//    		/*DEBUG*/
//    		System.out.println("opponent state: " + Arrays.toString(playerState.toArray()));
    		
    		int points = opp.getTotalVP();
    		Float value = null;
    		Float[] valueCount = memory.getState1Value(playerState);
    		if (valueCount==null) {
    			value = (float)(new Random().nextGaussian()*0.05 + 0.5);
    			memory.setState1Value(playerState, new Float[] {value, Float.valueOf(1)});
    		} else {
    			value = valueCount[0];
    		}
    		
    		int state_value = Math.round(value.floatValue()*10);	
    		
    		opp_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(state_value)));
    		
    	}
    	
    	int[] secondState = new int[6];
    	opp_states.sort(new Comparator<CustomPair>() {
		    public int compare(CustomPair o1, CustomPair o2) {
		        return o2.getKey().compareTo(o1.getKey());
		    }
		});
    	
    	for(int i = 0; i<opp_states.size(); i++) {
    		secondState[i*2] = opp_states.get(i).getKey().intValue();
    		secondState[i*2 + 1] = opp_states.get(i).getValue().intValue();
    	}
    	
//    	/*DEBUG*/
//		System.out.println("state2: " + Arrays.toString(secondState));
    	
    	SOCStateArray secondStateList = new SOCStateArray(secondState);
    	
    	Float value = null;
    	Float[] valueCount = memory.getState2Value(secondStateList);
		if (valueCount==null) {
			value = (float)(new Random().nextGaussian()*0.05 + 0.5); //or maybe random?
			memory.setState2Value(secondStateList, new Float[] {value, Float.valueOf(1)});
		} else {
			value = valueCount[0];
		}
		
		return value.floatValue();

    }
	
	 protected void updateStateValue() {
	    	state.updateAll(players, board);
	    	ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();
	    	
	    	for (SOCPlayer opp : opponents) {
	    		SOCStateArray oldPlayerState = oldState.get(opp);
	    		
	    		Float[] oldPlayerStateValueCount = memory.getState1Value(oldPlayerState);
	    		Float oldPlayerStateValue = null;
	    		Float oldPlayerStateCount = Float.valueOf(1);
	    		/*obvious mistake, should never be null, but error sometimes thrown*/
	    		if (oldPlayerStateValueCount==null) {
	    			oldPlayerStateValue = (float)(new Random().nextGaussian()*0.05 + 0.5); //or maybe random?
	    			memory.setState1Value(oldPlayerState, new Float[] {oldPlayerStateValue, Float.valueOf(1)});
	    		} else {
	    			oldPlayerStateValue = oldPlayerStateValueCount[0];
	    			oldPlayerStateCount = oldPlayerStateValueCount[1];
	    		}
	    		
	    		SOCStateArray newPlayerState = new SOCStateArray(state.getState(opp));
	    		Float newPlayerStateValue = null;
	    		Float[] newPlayerStateValueCount = memory.getState1Value(newPlayerState);
	    		if (newPlayerStateValueCount==null) {
	    			newPlayerStateValue = (float)(new Random().nextGaussian()*0.05 + 0.5); //or maybe random?
	    			memory.setState1Value(newPlayerState, new Float[] {newPlayerStateValue, Float.valueOf(1)});
	    		} else {
	    			newPlayerStateValue = newPlayerStateValueCount[0];
	    		}
	    		
	    		oldPlayerStateValue = (float)(oldPlayerStateValue + alpha * (gamma * newPlayerStateValue - oldPlayerStateValue));
	    		
	    		memory.setState1Value(oldPlayerState, new Float[] {oldPlayerStateValue, ++oldPlayerStateCount});
	    		oldState.put(opp, newPlayerState);
	    		
	    		//calculation to get new state array
	    		int points = opp.getTotalVP();
	    		int roundedPlayerStateValue = Math.round(newPlayerStateValue.floatValue()*10);
	    		opp_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(roundedPlayerStateValue)));
	    	}
	    	
	    	int[] newState = new int[6];
	    	opp_states.sort(new Comparator<CustomPair>() {
			    public int compare(CustomPair o1, CustomPair o2) {
			        return o2.getKey().compareTo(o1.getKey());
			    }
			});
	    	for(int i = 0; i<opp_states.size(); i++) {
	    		newState[i*2] = opp_states.get(i).getKey().intValue();
	    		newState[i*2 + 1] = opp_states.get(i).getValue().intValue();
	    	}
	    	SOCStateArray newStateList = new SOCStateArray(newState);
	    	
	    	Float newStateValue = null;
	    	Float[] newStateValueCount = memory.getState2Value(newStateList);
			if (newStateValueCount==null) {
//				newStateValue = Float.valueOf(0.5); //or maybe random?
				newStateValue = (float)(new Random().nextGaussian()*0.05 + 0.5);
				memory.setState2Value(newStateList, new Float[] {newStateValue, Float.valueOf(1)});
			} else {
				newStateValue = newStateValueCount[0];
			}
	    	
			Float oldStateValue = null;
			Float oldStateCount = Float.valueOf(1);
			Float[] oldStateValueCount = memory.getState2Value(oldState2);
			if (oldStateValueCount==null) {
				oldStateValue = (float)(new Random().nextGaussian()*0.05 + 0.5);
			} else {
				oldStateValue = oldStateValueCount[0];
				oldStateCount = oldStateValueCount[1];	
			}

	    	
	    	oldStateValue = (float)(oldStateValue + alpha * (gamma * newStateValue  - oldStateValue));
	    	
	    	memory.setState2Value(oldState2, new Float[] {oldStateValue, ++oldStateCount});
	    	oldState2 = newStateList;	    	
	    	currentStateValue = newStateValue;
	    }
	 
	 protected void updateStateValue(int reward) {
	    	state.updateAll(players, board);
	    	ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();
	    	
	    	for (SOCPlayer opp : opponents) {
	    		SOCStateArray oldPlayerState = oldState.get(opp);

	    		Float[] oldPlayerStateValueCount = memory.getState1Value(oldPlayerState);
	    		Float oldPlayerStateValue = null;
	    		Float oldPlayerStateCount = Float.valueOf(1);
	    		/*obvious mistake, should never be null, but error sometimes thrown*/
	    		if (oldPlayerStateValueCount==null) {
	    			oldPlayerStateValue = (float)(new Random().nextGaussian()*0.05 + 0.5); //or maybe random?
	    			memory.setState1Value(oldPlayerState, new Float[] {oldPlayerStateValue, Float.valueOf(1)});
	    		} else {
	    			oldPlayerStateValue = oldPlayerStateValueCount[0];
	    			oldPlayerStateCount = oldPlayerStateValueCount[1];
	    		}
	    		
	    		SOCStateArray newPlayerState = new SOCStateArray(state.getState(opp));
	    		Float newPlayerStateValue = Float.valueOf(reward);
	    		Float[] newPlayerStateValueCount = memory.getState1Value(newPlayerState);
	    		if (newPlayerStateValueCount==null) {
	    			memory.setState1Value(newPlayerState, new Float[] {newPlayerStateValue, Float.valueOf(1)});
	    		} else {
	    			memory.setState1Value(newPlayerState, new Float[] {newPlayerStateValue, ++newPlayerStateValueCount[1]});
	    		}
	    		
	    		oldPlayerStateValue = (float)(oldPlayerStateValue + alpha * (gamma * newPlayerStateValue - oldPlayerStateValue));
	    		
	    		memory.setState1Value(oldPlayerState, new Float[] {oldPlayerStateValue, ++oldPlayerStateCount});
	    		oldState.put(opp, newPlayerState);
	    		
	    		//calculation to get new state array
	    		int points = opp.getTotalVP();
	    		int roundedPlayerStateValue = Math.round(newPlayerStateValue.floatValue()*10);
	    		opp_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(roundedPlayerStateValue)));
	    	}
	    	
	    	int[] newState = new int[6];
	    	opp_states.sort(new Comparator<CustomPair>() {
			    public int compare(CustomPair o1, CustomPair o2) {
			        return o2.getKey().compareTo(o1.getKey());
			    }
			});
	    	for(int i = 0; i<opp_states.size(); i++) {
	    		newState[i*2] = opp_states.get(i).getKey().intValue();
	    		newState[i*2 + 1] = opp_states.get(i).getValue().intValue();
	    	}
	    	SOCStateArray newStateList = new SOCStateArray(newState);
	    	
	    	Float newStateValue = Float.valueOf(reward);
	    	Float[] newStateValueCount = memory.getState2Value(newStateList);
			if (newStateValueCount==null) {
				memory.setState2Value(newStateList, new Float[] {newStateValue, Float.valueOf(1)});
			} else {
				memory.setState2Value(newStateList, new Float[] {newStateValue, ++newStateValueCount[1]});
			}

			Float oldStateValue = null;
			Float oldStateCount = Float.valueOf(1);
			Float[] oldStateValueCount = memory.getState2Value(oldState2);
			if (oldStateValueCount==null) {
				oldStateValue = (float)(new Random().nextGaussian()*0.05 + 0.5);
			} else {
				oldStateValue = oldStateValueCount[0];
				oldStateCount = oldStateValueCount[1];	
			}

	    	oldStateValue = (float)(oldStateValue + alpha * (gamma * newStateValue  - oldStateValue));
	    	
	    	memory.setState2Value(oldState2, new Float[] {oldStateValue, ++oldStateCount});
	    }
	 
//	 protected void readMemory() {
//		 String nickname = brain.getClient().getNickname();
//		 
//		 System.out.println("reading memory");
//		 
//		 HashMap<List<Integer>, Double[]> map = null;
//		 
//		 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
//		            Paths.get("memory"),  "RL_LT_" + nickname + "_states_*")) {
//			 
//			 for (Path dir : dirStream) {
//				 
//				 /*DEBUG*/
//				 System.out.println("reading from path: " + dir.toString());
//				 map = readPath(dir);
//				 map.forEach(
//						    (key, value) -> states.merge(key, value, (v1, v2) -> 
//						    {Double[] val = new Double[] {
//						    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
//						    		v1[1] + v2[1]}; 
//						    		return val; }) );
//				 dir.toFile().delete();
//			 }
//			 
//			 /*DEBUG*/
//	         System.out.println(states.size() + " states read");
//			 
//	      }catch(IOException ioe)
//	      {
//	         ioe.printStackTrace();
//	         return;
//	      }
//		 
//		 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
//		            Paths.get("memory"),  "RL_LT_" + nickname + "_states2_*")) {
//			 
//			 for (Path dir : dirStream) {
//				 map = readPath(dir);
//				 map.forEach(
//						    (key, value) -> states2.merge(key, value, (v1, v2) -> 
//						    {Double[] val = new Double[] {
//						    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
//						    		v1[1] + v2[1]}; 
//						    		return val; }) );
//				 dir.toFile().delete();
//			 }
//			 
//			 /*DEBUG*/
//	         System.out.println(states2.size() + " states2 read");
//			 
//	      }catch(IOException ioe)
//	      {
//	         ioe.printStackTrace();
//	         return;
//	      }
//		 
//		 writeMemory();
//		
//	 }
//	 
//	 protected void synchroniseMemory() {
//		 
//		 /*DEBUG*/
//		 System.out.println("reading memory");
//		 
//		 HashMap<List<Integer>, Double[]> map = null;
//		 
//		 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
//		            Paths.get("memory"),  "RL_LT_*_states_*")) {
//			 
//			 for (Path dir : dirStream) {
//				 map = readPath(dir);
//				 map.forEach(
//						    (key, value) -> states.merge(key, value, (v1, v2) -> 
//						    {Double[] val = new Double[] {
//						    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
//						    		v1[1] + v2[1]}; 
//						    		return val; }) );
//				 dir.toFile().delete();
//			 }
//			 
//			 /*DEBUG*/
//	         System.out.println(states.size() + " states read");
//			 
//	      }catch(IOException ioe)
//	      {
//	         ioe.printStackTrace();
//	         return;
//	      }
//		 
//		 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
//		            Paths.get("memory"),  "RL_LT_*_states2_*")) {
//			 
//			 for (Path dir : dirStream) {
//				 map = readPath(dir);
//				 map.forEach(
//						    (key, value) -> states2.merge(key, value, (v1, v2) -> 
//						    {Double[] val = new Double[] {
//						    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
//						    		v1[1] + v2[1]}; 
//						    		return val; }) );
//				 dir.toFile().delete();
//			 }
//			 
//			 /*DEBUG*/
//	         System.out.println(states2.size() + " states2 read");
//			 
//	      }catch(IOException ioe)
//	      {
//	         ioe.printStackTrace();
//	         return;
//	      }
//		 
//		 writeMemory();
//	 }
//	 
//	 protected void writeMemory() {
//		 String nickname = brain.getClient().getNickname();
//		 Path path = Paths.get("memory", "RL_LT_" + nickname + "_states_" + game.getName()); 
//		 writePath(path, states);
//		 
//		 /*DEBUG*/
//         System.out.println("Serialized HashMap states data is saved in " + path.toString());
//         System.out.println(states.size() + " states saved");
//         states.forEach((key, value) -> System.out.println(
//        		 Arrays.toString(key.toArray()) + " : " + Arrays.toString(value)));
//
//         path = Paths.get("memory", "RL_LT_" + nickname + "_states2_" + game.getName()); 
//         writePath(path, states2);
//         
//         /*DEBUG*/
//         System.out.println("Serialized HashMap states2 data is saved in " + path.toString());
//         System.out.println(states2.size() + " states2 saved");
//         states2.forEach((key, value) -> System.out.println(
//        		 Arrays.toString(key.toArray()) + " : " + Arrays.toString(value)));
//
//	 }
	

	protected void writeStats() {

	 }
	
	public void updateReward() {
		
		SOCPlayer winPn = game.getPlayerWithWin();
		int reward = 0;
		
		if (winPn!=null) {
			/*DEBUG*/
			System.out.println(game.getName() + " winner is: " + game.getPlayerWithWin().getName());

			int winPnNum = winPn.getPlayerNumber();
			if (winPnNum==ourPlayerNumber) {
				reward = 1;
			}
		}

		updateStateValue(reward);
	}
	
	@Override
	public void updateStateAfterAddingPlayer(){
		super.updateStateAfterAddingPlayer();
		
		for (SOCPlayer opp : opponents) {
			SOCStateArray playerState = new SOCStateArray(state.getState(opp));
    		Float[] valueCount = memory.getState1Value(playerState);
    		if (valueCount==null) {
    			Float value = (float)(new Random().nextGaussian()*0.05 + 0.5);
    			memory.setState1Value(playerState, new Float[] {value, Float.valueOf(1)});
    		} 
			oldState.put(opp, playerState);
    	}
	}
	 
//	 protected HashMap<List<Integer>, Double[]> readPath(Path path){
//		 HashMap<List<Integer>, Double[]> map = null;
//		 try
//	     {
//	    	 FileInputStream fis = new FileInputStream(path.toFile());
//	         ObjectInputStream ois = new ObjectInputStream(fis);
//	         map = (HashMap<List<Integer>, Double[]>) ois.readObject();
//	         ois.close();
//	         fis.close();
//	      }catch(IOException ioe)
//	      {
//	         ioe.printStackTrace();
//	         return null;
//	      }catch(ClassNotFoundException c)
//	      {
//	         System.out.println("Class not found");
//	         c.printStackTrace();
//	         return null;
//	      }
//		 return map;
//	 }
//	 
//	 
//	 
//	 protected void writePath(Path path, HashMap<List<Integer>, Double[]> target){
//		 try
//         {
//			 FileOutputStream fos =
//                   new FileOutputStream(path.toFile());
//                ObjectOutputStream oos = new ObjectOutputStream(fos);
//                oos.writeObject(target);
//                oos.close();
//                fos.close();
//                                
//         }catch(IOException ioe)
//          {
//                ioe.printStackTrace();
//          }
//	 }
	 
//	 protected Double[] mergeFunc(Double[] a, Double[] b) {
//		 Double sumCount = a[1]+b[1];
//		 return(new Double[] {(
//		    		Double.valueOf(a[0]*a[1]+b[0]*b[1])/sumCount), sumCount});
//	 }
	 
//	 protected void whatever() {
////		 List<Integer> list = Arrays.stream(ints).boxed().collect(Collectors.toList());
//		 HashMap<int[], Double[]> map1 = new HashMap<int[], Double[]>();
//		 HashMap<int[], Double[]> map2 = new HashMap<int[], Double[]>();
//		 int[] list1 = new int[] {1, 2};
//		 List<Integer> list2 = Arrays.asList(1, 4, 6);
//		 Collections.unmodifiableList(Arrays.stream(list1).boxed().collect(Collectors.toList()));
//		 map1.put(new int[] {1,  2}, new Double[] {1.0, 1.0});
//		 map1.put(new int[] {1,  3}, new Double[] {1.0, 2.0});
//		 map2.put(new int[] {1,  3}, new Double[] {1.0, 4.0});
//		 map2.put(new int[] {1,  4}, new Double[] {1.0, 2.0});
//		 map1.forEach(
//				    (key, value) -> map2.merge(key, value, (v1, v2) -> 
//				    {Double[] val = new Double[] {
//				    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
//				    		v1[1] + v2[1]}; 
//				    		return val; }) );
//		 map2.forEach((key, value) -> System.out.println("key " + key + " value " + value));
//	 }

}
