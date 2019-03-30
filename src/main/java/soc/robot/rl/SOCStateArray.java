package soc.robot.rl;

import java.io.Serializable;
import java.util.Arrays;

public class SOCStateArray implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int[] arr;
	
	public SOCStateArray(int[] array) {
		arr = array;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o== null)
			return false;
		if (o == this)
            return true;
        if (!(o instanceof SOCStateArray))
            return false;
        SOCStateArray anotherArr = (SOCStateArray) o;
        if (this.arr.length!= anotherArr.arr.length)
        	return false;
       
        for (int i=0; i<arr.length; i++) {
        	if (this.arr[i]!=anotherArr.arr[i])
        		return false;
        }
        return true;
	}
	
	@Override
	public int hashCode() {
        int hashCode = 1;
        for (int a : arr)
            hashCode = 31*hashCode + a;
        return hashCode;
    }
	
	public int[] getArray() {
		return arr;
	}
	
	public void setArray(int[] array) {
		arr = array;
	}
	
	public String toString() {
		return Arrays.toString(arr);
	}

}
