package kea.stopwords;


/**
 * Class that can test whether a given string is a stop word.
 * Lowercases all words before the test.
 *
 * @version 1.0
 */
public class StopwordsFrench extends Stopwords {

	public StopwordsFrench(){
		loadStopwords(this.getClass().getResourceAsStream("/com/iai/uima/kea/data/stopwords/stopwords_" + "fr" + ".txt"));
	}
}


