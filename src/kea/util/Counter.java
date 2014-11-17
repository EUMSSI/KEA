package kea.util;


import java.io.*;

/**
 * Class that implements a simple counter.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class Counter implements Serializable {
  
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
/** Integer value stored */
    private int m_val = 1;
  
  /**
   * Initializes the counter to 1
   */
  public Counter() {
    
    m_val = 1;
  }
  
  /**
   * Initializes the counter to the given value
   */
  public Counter(int val) {
    
    m_val = val;
  }
  
  /**
   * Increments the counter.
   */
  public void increment() {
    
    m_val++;
  }
  
  /**
   * Gets the value.
   * @return the value
   */
  public int value() {
    
    return m_val;
  }
  
  /**
   * Returns string containing value.
   */
  public String toString() {
    
    return String.valueOf(m_val);
  }
}
