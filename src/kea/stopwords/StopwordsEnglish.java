package kea.stopwords;


/**
 * Class that can test whether a given string is a stop word.
 * Lowercases all words before the test.
 *
 * @version 1.0
 */
public class StopwordsEnglish extends Stopwords {

	public StopwordsEnglish(){
		loadStopwords(this.getClass().getResourceAsStream("/com/iai/uima/kea/data/stopwords/stopwords_" + "en" + ".txt"));
	}
}


