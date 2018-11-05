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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Stream;

import com.sun.security.ntlm.Client;

import soc.game.SOCPlayer;
import soc.robot.SOCRobotBrain;



public class RLStrategyLookupTable extends RLStrategy{

	/** variable to remember previous state of type 1: for all opponents */
    protected HashMap<SOCPlayer, int[]> oldState;
    
    /** variable to remember previous general state (type 2) */
    protected int[] oldState2;
    
    /** Memory for all the states type 1*/
    protected HashMap<int[], Double> states;
    
    /** Memory for all the states type 2 */
    protected HashMap<int[], Double> states2;
	
	public RLStrategyLookupTable(RlbotBrain2 br) {
		super(br);
		// TODO Auto-generated constructor stub
		state = new SOCState(ourPlayerNumber, playerTrackers);
        state.updateAll(playerTrackers, board);   
        
        //TO DO: read these from file - make a function: read memory
        states = new HashMap<int[], Double>();    
        states2 = new HashMap<int[], Double>(); 
        oldState = new HashMap<SOCPlayer, int[]>();
        oldState2 = new int[6];
        readMemory();
        
//        int gamesPlayed = br.getClient().getGamesPlayed();
//        int updateFrequency = br.getClient().getUpdateFrequency();
//        if ((gamesPlayed % updateFrequency)==0) {
//        	synchroniseMemory();
//        } else {
//        	readMemory();
//        }
//       
        ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();

        /* adding to memory the state at the beginning of the game */
    	for (SOCPlayer opp : opponents) {
    		int[] playerState = state.getState(opp);
    		int points = opp.getPublicVP();
    		Double value = Double.valueOf(0.5); //or maybe random?
			states.put(playerState, value);
			oldState.put(opp, playerState);
    		
    		int state_value = Math.round(value.floatValue()*10);	
    		
    		opp_states.add(new CustomPair(Integer.valueOf(points), Integer.valueOf(state_value)));
    		
    	}
    	
    	int[] secondState = new int[6];
    	
    	for(int i = 0; i<opp_states.size(); i++) {
    		secondState[i*2] = opp_states.get(i).getKey().intValue();
    		secondState[i*2 + 1] = opp_states.get(i).getValue().intValue();
    	}
    	
    	Double value = Double.valueOf(0.5); //or maybe random?
		states2.put(secondState, Double.valueOf(value));
		oldState2 = secondState;
		
	}
	

	protected float getStateValue(SOCState tmpState) {
    	ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();

    	for (SOCPlayer opp : opponents) {
    		int[] playerState = tmpState.getState(opp);
    		int points = opp.getPublicVP();
    		Double value = states.get(playerState);
    		if (value==null) {
    			value = new Random().nextGaussian()*0.05 + 0.5;
    			states.put(playerState, Double.valueOf(value));
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
    	
    	Double value = states2.get(secondState);
		if (value!=null) {}
		else {
			value = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
			states2.put(secondState, Double.valueOf(value));
		}
		
		return value.floatValue();

    }
	
	 protected void updateStateValue() {
	    	state.updateAll(playerTrackers, board);
	    	ArrayList<CustomPair> opp_states = new ArrayList<CustomPair>();
	    	
	    	for (SOCPlayer opp : opponents) {
	    		int[] oldPlayerState = oldState.get(opp);
	    		Double oldPlayerStateValue = states.get(oldPlayerState);
	    		
	    		int[] newPlayerState = state.getState(opp);
	    		Double newPlayerStateValue = states.get(newPlayerState);
	    		
	    		if (newPlayerStateValue==null) {
	    			newPlayerStateValue = new Random().nextGaussian()*0.05 + 0.5; //or maybe random?
	    			states.put(newPlayerState, Double.valueOf(newPlayerStateValue));
	    		}
	    		
	    		oldPlayerStateValue = oldPlayerStateValue + alpha * (gamma * newPlayerStateValue - oldPlayerStateValue);
	    		
	    		states.put(oldPlayerState, Double.valueOf(oldPlayerStateValue));
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
	    	Double newStateValue = states2.get(newState);
			if (newStateValue==null) {
//				newStateValue = Double.valueOf(0.5); //or maybe random?
				newStateValue = new Random().nextGaussian()*0.05 + 0.5;
				states2.put(newState, Double.valueOf(newStateValue));
			}
	    	
			Double oldStateValue = states2.get(oldState2);
	    	
	    	oldStateValue = oldStateValue + alpha * (gamma * newStateValue  - oldStateValue);
	    	
	    	states2.put(oldState2, Double.valueOf(oldStateValue));
	    	oldState2 = newState;	    	
	    	currentStateValue = newStateValue;
	    }
	 
	 protected void readMemory() {
		 String nickname = brain.getClient().getNickname();
		 
		 System.out.println("reading memory");
		 
		 HashMap<int[], Double> map = null;
		 
		 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
		            Paths.get("memory"),  "RL_LT_" + nickname + "_states_*")) {
			 
			 for (Path dir : dirStream) {
				 map = readPath(dir);
				 map.forEach(
						    (key, value) -> states.merge(key, value, (v1, v2) -> new Double((v1+v2)/2))
						);
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
						    (key, value) -> states2.merge(key, value, (v1, v2) -> new Double((v1+v2)/2))
						);
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
		 
	 }
	 
	 protected void writeMemory() {

		 String nickname = brain.getClient().getNickname();
		 Path path = Paths.get("memory", "RL_LT_" + nickname + "_states_" + game.getName()); 
		 writePath(path, states);
		 /*DEBUG*/
         System.out.println("Serialized HashMap states data is saved in " + path.toString());
         System.out.println(states.size() + " states saved");
         
         path = Paths.get("memory", "RL_LT_" + nickname + "_states2_" + game.getName()); 
         writePath(path, states2);
         /*DEBUG*/
         System.out.println("Serialized HashMap states2 data is saved in " + path.toString());
         System.out.println(states2.size() + " states2 saved");

	 }
	 
	 protected void writeStats() {

	 }
	 
	 protected HashMap<int[], Double> readPath(Path path){
		 HashMap<int[], Double> map = null;
		 try
	     {
	    	 FileInputStream fis = new FileInputStream(path.toFile());
	         ObjectInputStream ois = new ObjectInputStream(fis);
	         map = (HashMap<int[], Double>) ois.readObject();
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
	 
	 
	 
	 protected void writePath(Path path, HashMap<int[], Double> target){
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

}
