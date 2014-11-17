package kea.stopwords;


import java.io.*;

/**
 * Class that can test whether a given string is a stop word.
 * Lowercases all words before the test.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public abstract class Stopwords implements Serializable {
  
  /** 
   * Returns true if the given string is a stop word.
   */
  public abstract boolean isStopword(String str);
}


