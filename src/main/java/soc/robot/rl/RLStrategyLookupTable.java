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

import soc.game.SOCPlayer;



public class RLStrategyLookupTable extends RLStrategy{

	/** variable to remember previous state of type 1: for all opponents */
    protected HashMap<SOCPlayer, List<Integer>> oldState;
    
    /** variable to remember previous general state (type 2) */
    protected List<Integer> oldState2;
    
    /** Memory for all the states type 1*/
    protected HashMap<List<Integer>, Double[]> states;
    
    /** Memory for all the states type 2 */
    protected HashMap<List<Integer>, Double[]> states2;
	
	public RLStrategyLookupTable(RlbotBrain2 br) {
		super(br);
		state = new SOCState(ourPlayerNumber, playerTrackers);
        state.updateAll(playerTrackers, board);   
        
        states = new HashMap<List<Integer>, Double[]>();    
        states2 = new HashMap<List<Integer>, Double[]>(); 
        oldState = new HashMap<SOCPlayer, List<Integer> >();
        readMemory();
        
        int gamesPlayed = br.getClient().getGamesPlayed();
        int updateFrequency = br.getClient().getUpdateFrequency();
        if ((gamesPlayed % updateFrequency)==0) {
        	synchroniseMemory();
        } else {
        	readMemory();
        }
       
        ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();

        /* adding to memory the state at the beginning of the game */
    	for (SOCPlayer opp : opponents) {
    		List<Integer> playerState = Arrays.stream(state.getState(opp)).boxed().collect(Collectors.toList());
    		int points = opp.getPublicVP();
    		Double value = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
			states.put(playerState, new Double[] {value, Double.valueOf(1.0)});
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
    	
    	Double value = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
    	oldState2 = Arrays.stream(secondState).boxed().collect(Collectors.toList());
		states2.put(oldState2, new Double[] {value, Double.valueOf(1.0)});
		
	}
	

	protected float getStateValue(SOCState tmpState) {
    	ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();

    	for (SOCPlayer opp : opponents) {
    		List<Integer> playerState = Arrays.stream(tmpState.getState(opp)).boxed().collect(Collectors.toList());
    		
    		int points = opp.getPublicVP();
    		Double value = null;
    		Double[] valueCount = states.get(playerState);
    		if (valueCount==null) {
    			value = new Random().nextGaussian()*0.05 + 0.5;
    			states.put(playerState, new Double[] {value, Double.valueOf(1.0)});
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
    	
    	List<Integer> secondStateList = Arrays.stream(secondState).boxed().collect(Collectors.toList());
    	
    	Double value = null;
    	Double[] valueCount = states2.get(secondStateList);
		if (valueCount==null) {
			value = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
			states2.put(secondStateList, new Double[] {value, Double.valueOf(1.0)});
		} else {
			value = valueCount[0];
		}
		
		return value.floatValue();

    }
	
	 protected void updateStateValue() {
	    	state.updateAll(playerTrackers, board);
	    	ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();
	    	
	    	for (SOCPlayer opp : opponents) {
	    		List<Integer> oldPlayerState = oldState.get(opp);
	    		Double oldPlayerStateValue = states.get(oldPlayerState)[0];
	    		Double oldPlayerStateCount = states.get(oldPlayerState)[1];
	    		
	    		List<Integer> newPlayerState = Arrays.stream(state.getState(opp)).boxed().collect(Collectors.toList());
	    		Double newPlayerStateValue = null;
	    		Double[] newPlayerStateValueCount = states.get(newPlayerState);
	    		if (newPlayerStateValueCount==null) {
	    			newPlayerStateValue = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
	    			states.put(newPlayerState, new Double[] {newPlayerStateValue, Double.valueOf(1.0)});
	    		} else {
	    			newPlayerStateValue = newPlayerStateValueCount[0];
	    		}
	    		
	    		oldPlayerStateValue = oldPlayerStateValue + alpha * (gamma * newPlayerStateValue - oldPlayerStateValue);
	    		
	    		states.put(oldPlayerState, new Double[] {oldPlayerStateValue, ++oldPlayerStateCount});
	    		oldState.put(opp, newPlayerState);
	    		
	    		//calculation to get new state array
	    		int points = opp.getPublicVP();
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
	    	List<Integer> newStateList = Arrays.stream(newState).boxed().collect(Collectors.toList());
	    	
	    	Double newStateValue = null;
	    	Double[] newStateValueCount = states2.get(newStateList);
			if (newStateValueCount==null) {
//				newStateValue = Double.valueOf(0.5); //or maybe random?
				newStateValue = new Random().nextGaussian()*0.05 + 0.5;
				states2.put(newStateList, new Double[] {newStateValue, Double.valueOf(1.0)});
			} else {
				newStateValue = newStateValueCount[0];
			}
	    	
			Double oldStateValue = states2.get(oldState2)[0];
			Double oldStateCount = states2.get(oldState2)[1];
	    	
	    	oldStateValue = oldStateValue + alpha * (gamma * newStateValue  - oldStateValue);
	    	
	    	states2.put(oldState2, new Double[] {oldStateValue, ++oldStateCount});
	    	oldState2 = newStateList;	    	
	    	currentStateValue = newStateValue;
	    }
	 
	 protected void updateStateValue(int reward) {
	    	state.updateAll(playerTrackers, board);
	    	ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();
	    	
	    	for (SOCPlayer opp : opponents) {
	    		List<Integer> oldPlayerState = oldState.get(opp);
	    		Double oldPlayerStateValue = states.get(oldPlayerState)[0];
	    		Double oldPlayerStateCount = states.get(oldPlayerState)[1];
	    		
	    		List<Integer> newPlayerState = Arrays.stream(state.getState(opp)).boxed().collect(Collectors.toList());
	    		Double newPlayerStateValue = Double.valueOf(reward);
	    		Double[] newPlayerStateValueCount = states.get(newPlayerState);
	    		if (newPlayerStateValueCount==null) {
	    			states.put(newPlayerState, new Double[] {newPlayerStateValue, Double.valueOf(1.0)});
	    		} else {
	    			states.put(newPlayerState, new Double[] {newPlayerStateValue, ++newPlayerStateValueCount[1]});
	    		}
	    		
	    		oldPlayerStateValue = oldPlayerStateValue + alpha * (gamma * newPlayerStateValue - oldPlayerStateValue);
	    		
	    		states.put(oldPlayerState, new Double[] {oldPlayerStateValue, ++oldPlayerStateCount});
	    		oldState.put(opp, newPlayerState);
	    		
	    		//calculation to get new state array
	    		int points = opp.getPublicVP();
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
	    	List<Integer> newStateList = Arrays.stream(newState).boxed().collect(Collectors.toList());
	    	
	    	Double newStateValue = Double.valueOf(reward);
	    	Double[] newStateValueCount = states2.get(newStateList);
			if (newStateValueCount==null) {
				states2.put(newStateList, new Double[] {newStateValue, Double.valueOf(1.0)});
			} else {
				states2.put(newStateList, new Double[] {newStateValue, ++newStateValueCount[1]});
			}
			Double oldStateValue = states2.get(oldState2)[0];
			Double oldStateCount = states2.get(oldState2)[1];
	    	
	    	oldStateValue = oldStateValue + alpha * (gamma * newStateValue  - oldStateValue);
	    	
	    	states2.put(oldState2, new Double[] {oldStateValue, ++oldStateCount});
	    }
	 
	 protected void readMemory() {
		 String nickname = brain.getClient().getNickname();
		 
		 System.out.println("reading memory");
		 
		 HashMap<List<Integer>, Double[]> map = null;
		 
		 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
		            Paths.get("memory"),  "RL_LT_" + nickname + "_states_*")) {
			 
			 for (Path dir : dirStream) {
				 map = readPath(dir);
				 map.forEach(
						    (key, value) -> states.merge(key, value, (v1, v2) -> 
						    {Double[] val = new Double[] {
						    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
						    		v1[1] + v2[1]}; 
						    		return val; }) );
				 dir.toFile().delete();
			 }
			 
			 /*DEBUG*/
	         System.out.println(states.size() + " states read");
			 
	      }catch(IOException ioe)
	      {
	         ioe.printStackTrace();
	         return;
	      }
		 
		 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
		            Paths.get("memory"),  "RL_LT_" + nickname + "_states2_*")) {
			 
			 for (Path dir : dirStream) {
				 map = readPath(dir);
				 map.forEach(
						    (key, value) -> states2.merge(key, value, (v1, v2) -> 
						    {Double[] val = new Double[] {
						    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
						    		v1[1] + v2[1]}; 
						    		return val; }) );
				 dir.toFile().delete();
			 }
			 
			 /*DEBUG*/
	         System.out.println(states2.size() + " states2 read");
			 
	      }catch(IOException ioe)
	      {
	         ioe.printStackTrace();
	         return;
	      }
		 
		 writeMemory();
		
	 }
	 
	 protected void synchroniseMemory() {
		 String nickname = brain.getClient().getNickname();
		 
		 System.out.println("reading memory");
		 
		 HashMap<List<Integer>, Double[]> map = null;
		 
		 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
		            Paths.get("memory"),  "RL_LT_*_states_*")) {
			 
			 for (Path dir : dirStream) {
				 map = readPath(dir);
				 map.forEach(
						    (key, value) -> states.merge(key, value, (v1, v2) -> 
						    {Double[] val = new Double[] {
						    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
						    		v1[1] + v2[1]}; 
						    		return val; }) );
				 dir.toFile().delete();
			 }
			 
			 /*DEBUG*/
	         System.out.println(states.size() + " states read");
			 
	      }catch(IOException ioe)
	      {
	         ioe.printStackTrace();
	         return;
	      }
		 
		 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
		            Paths.get("memory"),  "RL_LT_*_states2_*")) {
			 
			 for (Path dir : dirStream) {
				 map = readPath(dir);
				 map.forEach(
						    (key, value) -> states2.merge(key, value, (v1, v2) -> 
						    {Double[] val = new Double[] {
						    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
						    		v1[1] + v2[1]}; 
						    		return val; }) );
				 dir.toFile().delete();
			 }
			 
			 /*DEBUG*/
	         System.out.println(states2.size() + " states2 read");
			 
	      }catch(IOException ioe)
	      {
	         ioe.printStackTrace();
	         return;
	      }
		 
		 writeMemory();
	 }
	 
	 protected void writeMemory() {

		 String nickname = brain.getClient().getNickname();
		 Path path = Paths.get("memory", "RL_LT_" + nickname + "_states_" + game.getName()); 
		 writePath(path, states);
		 /*DEBUG*/
         System.out.println("Serialized HashMap states data is saved in " + path.toString());
         System.out.println(states.size() + " states saved");
//         states.forEach((key, value) -> System.out.println(
//        		 Arrays.toString(key.toArray()) + " : " + Arrays.toString(value)));

         path = Paths.get("memory", "RL_LT_" + nickname + "_states2_" + game.getName()); 
         writePath(path, states2);
         /*DEBUG*/
         System.out.println("Serialized HashMap states2 data is saved in " + path.toString());
         System.out.println(states2.size() + " states2 saved");
//         states2.forEach((key, value) -> System.out.println(
//        		 Arrays.toString(key.toArray()) + " : " + Arrays.toString(value)));

	 }
	

	protected void writeStats() {

	 }
	
	protected void updateReward() {
		int winPn = game.getPlayerWithWin().getPlayerNumber();
		
		/*DEBUG*/
		System.out.println(game.getName() + " winner is: " + game.getPlayerWithWin().getName());
		
		int reward = 0;
		if (winPn==ourPlayerNumber) {
			reward = 1;
		}
		updateStateValue(reward);
	}
	 
	 protected HashMap<List<Integer>, Double[]> readPath(Path path){
		 HashMap<List<Integer>, Double[]> map = null;
		 try
	     {
	    	 FileInputStream fis = new FileInputStream(path.toFile());
	         ObjectInputStream ois = new ObjectInputStream(fis);
	         map = (HashMap<List<Integer>, Double[]>) ois.readObject();
	         ois.close();
	         fis.close();
	      }catch(IOException ioe)
	      {
	         ioe.printStackTrace();
	         return null;
	      }catch(ClassNotFoundException c)
	      {
	         System.out.println("Class not found");
	         c.printStackTrace();
	         return null;
	      }
		 return map;
	 }
	 
	 
	 
	 protected void writePath(Path path, HashMap<List<Integer>, Double[]> target){
		 try
         {
			 FileOutputStream fos =
                   new FileOutputStream(path.toFile());
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(target);
                oos.close();
                fos.close();
                                
         }catch(IOException ioe)
          {
                ioe.printStackTrace();
          }
	 }
	 
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
