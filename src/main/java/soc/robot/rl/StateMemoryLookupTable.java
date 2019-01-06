package soc.robot.rl;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.lang.Math;


public class StateMemoryLookupTable  {

	 /** Memory for all the states type 1 that will be synchronised among servers*/
    protected HashMap<SOCStateArray, Float[]> states;
    
    /** Memory for all the states type 1*/
    protected HashMap<SOCStateArray, Float[]> statesOrg;
    
    /** Memory for all the states type 2  that will be synchronised among servers*/
    protected HashMap<SOCStateArray, Float[]> states2;
    
    /** Memory for all the states type 2 */
    protected HashMap<SOCStateArray,  Float[]> states2Org;
    
    protected int port;
	
	public StateMemoryLookupTable(int port) {
		 states = new HashMap<SOCStateArray, Float[]>();    
	     states2 = new HashMap<SOCStateArray, Float[]>(); 
	     statesOrg = new HashMap<SOCStateArray, Float[]>();    
	     states2Org = new HashMap<SOCStateArray, Float[]>(); 
	     this.port = port;
	}

	public Float[] getState1Value(SOCStateArray state) {
		return(states.get(state));
	}

//	public void setState1Value(List<Integer> state, Double[] value) {
//		states.put(state,  value);
//	}
	
	public void setState1Value(SOCStateArray state, Float[] value) {
//		Double[] currentValueCount = states.get(state);
//		if (currentValueCount==null) {
//			statesOrg.put(state,   new Double[] {value[0], Double.valueOf(1.0)});
//		} else {
//			currentValueCount[1]++;
//			currentValueCount[0] = value[0];
//			statesOrg.put(state, currentValueCount);
//		}
//		if (port==0) {
//			double val = value[0];
//			double count = value[1];
//			double s = val + count;
//			double f = s;
//		}
		states.put(state,  value);
	}
	
	
	public Float[] getState2Value(SOCStateArray state) {
		return(states2.get(state));
	}

	public void setState2Value(SOCStateArray state, Float[] value) {
//		Double[] currentValueCount = states2Org.get(state);
//		if (currentValueCount==null) {
//			states2Org.put(state,   new Double[] {value[0], Double.valueOf(1.0)});
//		} else {
//			currentValueCount[1]++;
//			currentValueCount[0] = value[0];
//			states2Org.put(state, currentValueCount);
//		}
		states2.put(state,  value);
	}
		
	public void synchronise(String nickname) {
		
		new MemorySynchroniser(this, nickname).start();
	 }
	
//	public void readMemory(String nickname) {
//
//		new MemoryReader(this, nickname).start();
//
//	 }
//	
	public void readMemory(String nick) {
		 /*DEBUG*/
		 System.out.println("reading memory");
		 
		 Path dir = Paths.get("RL_LT_" + nick + "_states_" + port);
		 System.out.println("Reading from path: " + dir.toString());
		 states = readPath(dir);
			 
		 /*DEBUGA*/
        System.out.println(states.size() + " states read");
			 
        Path dir2 = Paths.get("RL_LT_" + nick + "_states2_" + port);
        System.out.println("Reading from path: " + dir2.toString());
        
        /*TEST: should we leave synchronized or not*/
        synchronized(states2) {
       	 states2 = readPath(dir2);
        }
		 			 
		 /*DEBUGA*/
        System.out.println(states2.size() + " states2 read");
	}
	
	public void synchroniseAcrossPlayers(String nickname) {
		
		new MemoryPlayerSynchroniser(this, nickname).start();
	 }
	 
	 public void writeMemory(String nick, boolean original) {
		 /*DEBUGA*/
		 System.out.println("writing memory");
		 
//		 Path path = Paths.get("memory", "RL_LT_" + nick + "_states_" + port); 
		 Path path = Paths.get("RL_LT_" + nick + "_states_" + port); 
		 
		 if (!original) {
			 writePath(path, states);
			 /*DEBUGA*/
	         System.out.println(states.size() + " states written");
		 } else
		 {
			 writePath(path, statesOrg);
			 /*DEBUGA*/
	         System.out.println(statesOrg.size() + " statesOrg written");
	         statesOrg=new HashMap<SOCStateArray, Float[]>();
		 }
		 		 
//		 /*DEBUG*/
//        System.out.println("Serialized HashMap states data is saved in " + path.toString());
//        System.out.println(states.size() + " states saved");
//        states.forEach((key, value) -> System.out.println(
//       		 key.toString() + " : " + Arrays.toString(value)));

//        path = Paths.get("memory", "RL_LT_" + nick + "_states2_" + port); 
        path = Paths.get("RL_LT_" + nick + "_states2_" + port); 
        
        if (!original) {
			 writePath(path, states2);
			 /*DEBUGA*/
		     System.out.println(states2.size() + " states2 written");
		 } else
		 {
			 writePath(path, states2Org);
			 /*DEBUGA*/
		     System.out.println(states2Org.size() + " states2Org written");
		     states2Org=new HashMap<SOCStateArray, Float[]>();
		 }
		        
        
//        /*DEBUG*/
//        System.out.println("Serialized HashMap states2 data is saved in " + path.toString());
//        System.out.println(states2.size() + " states2 saved");
//        states2.forEach((key, value) -> System.out.println(
//       		 key.toString() + " : " + Arrays.toString(value)));
		 

	 }
	 
//	 public void writeMemory(String nickname, boolean org) {
//		 
//		 new MemorySaver(this, nickname, org).start();
//
//	 }
	
	protected void writePath(Path path, HashMap<SOCStateArray, Float[]> target){
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
	
	public void memoryStats() {
//		float sum = 0;
//		for (Float[] arr : states.values()) {
//			sum+=arr[1].floatValue();
//		}
//		
//		float sum2 = 0;
//		for (Float[] arr : states2.values()) {
//			sum2+=arr[1].floatValue();
//		}
//		
//		System.out.println("rlbot" + port + " there are " + states.size() 
//				+ " states and " + states2.size() + " states2"
//				+ " total states: " + sum + " total states2: " + sum2);
		
		System.out.println("rlbot" + port + " there are " + states.size() 
		+ " states and " + states2.size() + " states2");
		
//		 /*DEBUG*/
//      states.forEach((key, value) -> System.out.println(
//     		 key.toString() + " : " + Arrays.toString(value)));
//      
//      states2.forEach((key, value) -> System.out.println(
//    		 key.toString() + " : " + Arrays.toString(value)));
		
	}
	
	
	
	 protected HashMap<SOCStateArray, Float[]> readPath(Path path){
		 HashMap<SOCStateArray, Float[]> map = null;
		 try
	     {
	    	 FileInputStream fis = new FileInputStream(path.toFile());
	         ObjectInputStream ois = new ObjectInputStream(fis);
	         map = (HashMap<SOCStateArray, Float[]>) ois.readObject();
	         ois.close();
	         fis.close();
	      } catch(EOFException eof) {
	    	  eof.printStackTrace();
	    	  return map;
	      } catch(IOException ioe)
	      {
	         ioe.printStackTrace();
	         return null;
	      } 
		 catch(ClassNotFoundException c)
	      {
	         System.out.println("Class not found");
	         c.printStackTrace();
	         return null;
	      }
		 return map;
	 }
	 
	 public void stats() {
		 /*DEBUG*/
       System.out.println(states.size() + " states in memory");
       states.forEach((key, value) -> 
       			{
//       				System.out.println(Arrays.toString(value));
//       				if(Math.abs(value[0]-0)<1e-6 || Math.abs(value[0]-1)<1e-6) 
       					System.out.println(
       							key.toString() + " : " + Arrays.toString(value) ); 
       					} );
	 }
	 
	 class MemorySaver extends Thread {
		 
		 protected String nick;
		 
		 protected boolean original;
		 
		 protected StateMemoryLookupTable memory;
		 
		 public MemorySaver(StateMemoryLookupTable memory, String nickname, boolean org) {
			 this.memory = memory;
			 this.nick = nickname;
			 this.original = org;
		 }
		 
		 @Override
	    public void run()
	    {
			 /*DEBUGA*/
			 System.out.println("writing memory");
			 
//			 Path path = Paths.get("memory", "RL_LT_" + nick + "_states_" + port); 
			 Path path = Paths.get("RL_LT_" + nick + "_states_" + port); 
			 
			 if (!original) {
				 writePath(path, states);
				 /*DEBUGA*/
		         System.out.println(states.size() + " states written");
			 } else
			 {
				 writePath(path, statesOrg);
				 /*DEBUGA*/
		         System.out.println(statesOrg.size() + " statesOrg written");
		         statesOrg=new HashMap<SOCStateArray, Float[]>();
			 }
			 		 
//			 /*DEBUG*/
//	        System.out.println("Serialized HashMap states data is saved in " + path.toString());
//	        System.out.println(states.size() + " states saved");
//	        states.forEach((key, value) -> System.out.println(
//	       		 Arrays.toString(key.toArray()) + " : " + Arrays.toString(value)));

//	        path = Paths.get("memory", "RL_LT_" + nick + "_states2_" + port); 
	        path = Paths.get("RL_LT_" + nick + "_states2_" + port); 
	        
	        if (!original) {
				 writePath(path, states2);
				 /*DEBUGA*/
			     System.out.println(states2.size() + " states2 written");
			 } else
			 {
				 writePath(path, states2Org);
				 /*DEBUGA*/
			     System.out.println(states2Org.size() + " states2Org written");
			     states2Org=new HashMap<SOCStateArray, Float[]>();
			 }
			        
	        
//	        /*DEBUG*/
//	        System.out.println("Serialized HashMap states2 data is saved in " + path.toString());
//	        System.out.println(states2.size() + " states2 saved");
//	        states2.forEach((key, value) -> System.out.println(
//	       		 Arrays.toString(key.toArray()) + " : " + Arrays.toString(value)));
	    }
	 }
	 
	 class MemorySynchroniser extends Thread {
		 
		 protected String nick;
		 
		 protected StateMemoryLookupTable memory;
		 
		 public MemorySynchroniser(StateMemoryLookupTable memory, String nickname) {
			 this.memory = memory;
			 this.nick = nickname;
		 }
		 
		 @Override
		 public void run() {
			 /*DEBUGA*/
			 System.out.println("reading memory");
			 
			 HashMap<SOCStateArray, Float[]> map = null;
			 
			 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
			            Paths.get("memory"),  "RL_LT_" + nick + "_states_*")) {			 
				 
				 for (Path dir : dirStream) {
					 
					 /*DEBUGA*/
					 System.out.println("path from which reading " + dir.toString());
					 Path our = Paths.get("memory", "RL_LT_" + nick + "_states_" + port);
					 
					 if(dir.equals(our))
						 continue;
					 				 
					 map = readPath(dir);
					 map.forEach(
							    (key, value) -> statesOrg.merge(key, value, (v1, v2) -> 
							    {Float[] val = new Float[] {
							    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
							    		v1[1] + v2[1]}; 
							    		return val; }) );
					 map.forEach(
							    (key, value) -> states.merge(key, value, (v1, v2) -> 
							    {Float[] val = new Float[] {
							    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
							    		v1[1] + v2[1]}; 
							    		return val; }) );
				 }
				 
				 
				 /*DEBUGA*/
		         System.out.println(states.size() + " states read");
				 
		      }catch(IOException ioe)
		      {
		         ioe.printStackTrace();
		         return;
		      }
			 
			 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
			            Paths.get("memory"),  "RL_LT_" + nick + "_states2_*")) {
							 
				 for (Path dir : dirStream) {
					 
					 Path our = Paths.get("memory", "RL_LT_" + nick + "_states_" + port);
					 if(dir.equals(our))
						 continue;
					 
					 map = readPath(dir);
					 map.forEach(
							    (key, value) -> states2Org.merge(key, value, (v1, v2) -> 
							    {Float[] val = new Float[] {
							    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
							    		v1[1] + v2[1]}; 
							    		return val; }) );
					 
					 map.forEach(
							    (key, value) -> states2.merge(key, value, (v1, v2) -> 
							    {Float[] val = new Float[] {
							    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
							    		v1[1] + v2[1]}; 
							    		return val; }) );
//					 dir.toFile().delete();
				 }
				 
				 /*DEBUGA*/
		         System.out.println(states2.size() + " states2 read");
				 
		      }catch(IOException ioe)
		      {
		         ioe.printStackTrace();
		         return;
		      }
			 writeMemory(nick, false);
		 }
	 }
	 
	 class MemoryReader extends Thread {
			 
		 protected String nick;

		 protected StateMemoryLookupTable memory;
		 
		 public MemoryReader(StateMemoryLookupTable memory, String nickname) {
			 this.memory = memory;
			 this.nick = nickname;
		 }
		 
		 @Override
		 public void run() {
			 /*DEBUG*/
			 System.out.println("reading memory");
			 
			 Path dir = Paths.get("memory", "RL_LT_" + nick + "_states_8888");
			 states = readPath(dir);
				 
			 /*DEBUGA*/
	         System.out.println(states.size() + " states read");
				 
	         Path dir2 = Paths.get("memory", "RL_LT_" + nick + "_states2_8888");
	         
	         /*TEST: should we leave synchronized or not*/
	         synchronized(states2) {
	        	 states2 = readPath(dir2);
	         }
			 			 
			 /*DEBUGA*/
	         System.out.println(states2.size() + " states2 read");
		 }
	 }
	 
	 class MemoryPlayerSynchroniser extends Thread {
		 
		 protected String nick;

		 protected StateMemoryLookupTable memory;
		 
		 public MemoryPlayerSynchroniser(StateMemoryLookupTable memory, String nickname) {
			 this.memory = memory;
			 this.nick = nickname;
		 }
		 
		 @Override
		 public void run() {
			 /*DEBUGA*/
			 System.out.println("synchronise Across Players");
			 
			 HashMap<SOCStateArray, Float[]> map = null;
			 
			 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
			            Paths.get("memory"),  "RL_LT_*org_states_8888")) {			 
				 
				 for (Path dir : dirStream) {
					 
					 /*DEBUGA*/
					 System.out.println("path from which reading " + dir.toString());
					 Path our = Paths.get("memory", "RL_LT_" + nick + "org_states_" + port);
					 
					 if(dir.equals(our))
						 continue;
					 				 
					 map = readPath(dir);
					 map.forEach(
							    (key, value) -> states.merge(key, value, (v1, v2) -> 
							    {Float[] val = new Float[] {
							    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
							    		v1[1] + v2[1]}; 
							    		return val; }) );
				 }
				 
				 /*DEBUGA*/
		         System.out.println(states.size() + " states read");
				 
		      }catch(IOException ioe)
		      {
		         ioe.printStackTrace();
		         return;
		      }
			 
			 try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
			            Paths.get("memory"),  "RL_LT_*org_states2_8888")) {
							 
				 for (Path dir : dirStream) {
					 
					 Path our = Paths.get("memory", "RL_LT_" + nick + "org_states2_" + port);
					 if(dir.equals(our))
						 continue;
					 
					 map = readPath(dir);
					 map.forEach(
							    (key, value) -> states2.merge(key, value, (v1, v2) -> 
							    {Float[] val = new Float[] {
							    		(v1[0]*v1[1]+v2[0]*v2[1])/(v1[1]+v2[1]), 
							    		v1[1] + v2[1]}; 
							    		return val; }) );
				 }
				 
				 /*DEBUGA*/
		         System.out.println(states2.size() + " states2 read");
				 
		      }catch(IOException ioe)
		      {
		         ioe.printStackTrace();
		         return;
		      }
			 writeMemory(nick, false);
		 }
	 }
	 
}
