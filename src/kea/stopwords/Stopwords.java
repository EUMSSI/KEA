package kea.stopwords;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that can test whether a given string is a stop word.
 * Lowercases all words before the test.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class Stopwords implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	/** The hashtable containing the list of stopwords */
	private Set<String> m_Stopwords = null;

	/**
	 * default constructor, do not use except for derived classes.
	 */
	protected Stopwords() {
	}
	
	public Stopwords(String path) throws FileNotFoundException {
		File txt = new File(path);
		loadStopwords(new FileInputStream(txt));
	}

	public Stopwords(URL url) throws IOException {
		loadStopwords(url.openStream());
	}

	public Stopwords(InputStream is){
		loadStopwords(is);
	}

	public void loadStopwords(InputStream is) {

		if (m_Stopwords == null) {
			m_Stopwords = new HashSet<String>();
			InputStreamReader isr;
			String sw = null;
			try {
				isr = new InputStreamReader(is, "UTF-8");
				BufferedReader br = new BufferedReader(isr);				
				while ((sw=br.readLine()) != null)  {
					m_Stopwords.add(sw);   
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/** 
	 * Returns true if the given string is a stop word.
	 */
	public boolean isStopword(String str) {
		return m_Stopwords.contains(str.toLowerCase());
	}
}


