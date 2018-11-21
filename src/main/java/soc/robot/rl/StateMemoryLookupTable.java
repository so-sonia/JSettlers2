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
		states2.put(state,  value);
	}
		
//	protected void synchronise() {
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
	 
	 protected void writeMemory(String nickname) {
		 Path path = Paths.get("memory", "RL_LT_" + nickname + "_states"); 
		 writePath(path, states);
		 
//		 /*DEBUG*/
//        System.out.println("Serialized HashMap states data is saved in " + path.toString());
//        System.out.println(states.size() + " states saved");
//        states.forEach((key, value) -> System.out.println(
//       		 Arrays.toString(key.toArray()) + " : " + Arrays.toString(value)));

        path = Paths.get("memory", "RL_LT_" + nickname + "_states2" ); 
        writePath(path, states2);
        
//        /*DEBUG*/
//        System.out.println("Serialized HashMap states2 data is saved in " + path.toString());
//        System.out.println(states2.size() + " states2 saved");
//        states2.forEach((key, value) -> System.out.println(
//       		 Arrays.toString(key.toArray()) + " : " + Arrays.toString(value)));

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

}
