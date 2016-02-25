package kea.main;

/*
 *    KEAKeyphraseExtractor.java
 *    Copyright (C) 2001-2006 Eibe Frank, Olena Medelyan
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import kea.filters.KEAFilter;
import kea.filters.KEAPhraseFilter;
import kea.stemmers.SremovalStemmer;
import kea.stemmers.Stemmer;
import kea.stopwords.Stopwords;
import kea.stopwords.StopwordsEnglish;
import kea.util.Counter;

/**
 * Extracts keyphrases from the documents in a given directory.
 * Assumes that the file names for the documents end with ".txt".
 * Puts extracted keyphrases into corresponding files ending with
 * ".key" (if those are not already present). Optionally an encoding
 * for the documents/keyphrases can be defined (e.g. for Chinese
 * text). Documents for which ".key" exists, are used for evaluation.
 *
 * Valid options are:<p>
 *
 * -l "directory name"<br>
 * Specifies name of directory.<p>
 *
 * -m "model name"<br>
 * Specifies name of model.<p>
 *
 * -v "vocabulary name"<br>
 * Specifies name of vocabulary.<p>
 *
 * -f "vocabulary format"<br>
 * Specifies format of vocabulary (text or skos).<p>
 *
 * -i "document language" <br>
 * Specifies document language (en, es, de, fr).<p>
 * 
 * -e "encoding"<br>
 * Specifies encoding.<p>
 *
 * -n <br>
 * Specifies number of phrases to be output (default: 5).<p>
 *
 * -t "name of class implementing stemmer"<br>
 * Sets stemmer to use (default: SremovalStemmer). <p>
 *
 * -s "name of class implementing stopwords"<br>
 * Sets stemmer to use (default: StopwordsEnglish). <p>
 * 
 * -d<br>
 * Turns debugging mode on.<p>
 *
 * -g<br>
 * Build global dictionaries from the test set.<p>
 *
 * -a<br>
 * Also write stemmed phrase and score into ".key" file.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class KEAKeyphraseExtractor implements OptionHandler {
	
	/** Name of directory */
	String m_dirName = null;
	
	/** Name of model */
	String m_modelName = null;
	
	/** Name of vocabulary */
	String m_vocabulary = null;
	
	/** Vocabulary format */
	String m_vocabularyFormat = null;
	
	/** Document language */
	String m_documentLanguage = "en";
	
	/** Encoding */
	String m_encoding = "default";
	
	
	/** Debugging mode? */
	boolean m_debug = false;
	
	/** The KEA filter object */
	KEAFilter m_KEAFilter = null;
	
	
	/** The number of phrases to extract. */
	int m_numPhrases = 10;
	
	/** The stemmer to be used */
	private Stemmer m_Stemmer = new SremovalStemmer();
	
	
	/** The list of stop words to be used */
	private Stopwords m_Stopwords;
	
	
	/** Also write stemmed phrase and score into .key file. */
	boolean m_AdditionalInfo = true;
	
	
	/** Build global dictionaries from the test set. */
	boolean m_buildGlobal = false;
	
	
	/**
	 * Get the value of AdditionalInfo.
	 *
	 * @return Value of AdditionalInfo.
	 */
	public boolean getAdditionalInfo() {
		
		return m_AdditionalInfo;
	}
	
	/**
	 * Set the value of AdditionalInfo.
	 *
	 * @param newAdditionalInfo Value to assign to AdditionalInfo.
	 */
	public void setAdditionalInfo(boolean newAdditionalInfo) {
		
		m_AdditionalInfo = newAdditionalInfo;
	}
	
	
	/**
	 * Get the value of BuildGlobal.
	 *
	 * @return Value of BuildGlobal.
	 */
	public boolean getBuildGlobal() {
		
		return m_buildGlobal;
	}
	
	/**
	 * Set the value of BuildGlobal.
	 *
	 * @param newBuildGlobal Value to assign to BuildGlobal.
	 */
	public void setBuildGlobal(boolean newBuildGlobal) {
		
		m_buildGlobal = newBuildGlobal;
	}
	
	/**
	 * Get the value of numPhrases.
	 *
	 * @return Value of numPhrases.
	 */
	public int getNumPhrases() {
		
		return m_numPhrases;
	}
	
	/**
	 * Get the Stemmer value.
	 * @return the Stemmer value.
	 */
	public Stemmer getStemmer() {
		
		return m_Stemmer;
	}
	
	/**
	 * Set the Stemmer value.
	 * @param newStemmer The new Stemmer value.
	 */
	public void setStemmer(Stemmer newStemmer) {
		
		this.m_Stemmer = newStemmer;
	}
	
	
	/**
	 * Get the Stopwords value.
	 * @return the Stopwords value.
	 */
	public Stopwords getStopwords() {
		
		return m_Stopwords;
	}
	
	/**
	 * Set the Stopwords value.
	 * @param newStopwords The new Stopwords value.
	 */
	public void setStopwords(Stopwords newStopwords) {
		
		this.m_Stopwords = newStopwords;
	}
	
	
	/**
	 * Set the value of numPhrases.
	 *
	 * @param newnumPhrases Value to assign to numPhrases.
	 */
	public void setNumPhrases(int newnumPhrases) {
		
		m_numPhrases = newnumPhrases;
	}
	
	/**
	 * Get the value of debug.
	 *
	 * @return Value of debug.
	 */
	public boolean getDebug() {
		
		return m_debug;
	}
	
	/**
	 * Set the value of debug.
	 *
	 * @param newdebug Value to assign to debug.
	 */
	public void setDebug(boolean newdebug) {
		
		m_debug = newdebug;
	}
	
	/**
	 * Get the value of encoding.
	 *
	 * @return Value of encoding.
	 */
	public String getEncoding() {
		
		return m_encoding;
	}
	
	
	/**
	 * Set the value of encoding.
	 *
	 * @param newencoding Value to assign to encoding.
	 */
	public void setEncoding(String newencoding) {
		
		m_encoding = newencoding;
	}
	
	/**
	 * Get the value of vocabulary name.
	 *
	 * @return Value of vocabulary name.
	 */
	public String getVocabulary() {
		
		return m_vocabulary;
	}
	
	/**
	 * Set the value of vocabulary name.
	 *
	 * @param newvocabulary Value to assign to vocabulary name.
	 */
	public void setVocabulary(String newvocabulary) {
		
		m_vocabulary = newvocabulary;
	}
	
	/**
	 * Get the value of vocabulary format.
	 *
	 * @return Value of vocabulary format.
	 */
	public String getVocabularyFormat() {
		
		return m_vocabularyFormat;
	}
	
	/**
	 * Set the value of vocabulary format.
	 *
	 * @param newvocabularyFormat Value to assign to vocabularyFormat .
	 */
	public void setVocabularyFormat(String newvocabularyFormat) {
		
		m_vocabularyFormat = newvocabularyFormat;
	}
	/**
	 * Get the value of document language.
	 *
	 * @return Value of document language.
	 */
	public String getDocumentLanguage() {
		
		return m_documentLanguage;
	}
	
	/**
	 * Set the value of document language.
	 *
	 * @param newdocumentLanguage Value to assign to document language.
	 */
	public void setDocumentLanguage(String newdocumentLanguage) {
		
		m_documentLanguage = newdocumentLanguage;
	}
	
	
	/**
	 * Get the value of modelName.
	 *
	 * @return Value of modelName.
	 */
	public String getModelName() {
		
		return m_modelName;
	}
	
	/**
	 * Set the value of modelName.
	 *
	 * @param newmodelName Value to assign to modelName.
	 */
	public void setModelName(String newmodelName) {
		
		m_modelName = newmodelName;
	}
	
	/**
	 * Get the value of dirName.
	 *
	 * @return Value of dirName.
	 */
	public String getDirName() {
		
		return m_dirName;
	}
	
	/**
	 * Set the value of dirName.
	 *
	 * @param newdirName Value to assign to dirName.
	 */
	public void setDirName(String newdirName) {
		
		m_dirName = newdirName;
	}
	
	/**
	 * Parses a given list of options controlling the behaviour of this object.
	 * Valid options are:<p>
	 *
	 * -l "directory name"<br>
	 * Specifies name of directory.<p>
	 *
	 * -m "model name"<br>
	 * Specifies name of model.<p>
	 *
	 * -v "vocabulary name"<br>
	 * Specifies vocabulary name.<p>
	 * 
	 * -f "vocabulary format"<br>
	 * Specifies vocabulary format.<p>
	 * 
	 * -i "document language" <br>
	 * Specifies document language.<p>
	 * 
	 * -e "encoding"<br>
	 * Specifies encoding.<p>
	 *
	 * -n<br>
	 * Specifies number of phrases to be output (default: 5).<p>
	 *
	 * -d<br>
	 * Turns debugging mode on.<p>
	 *
	 * -b<br>
	 * Builds global dictionaries for computing TFxIDF from the test collection.<p>
	 *
	 * -a<br>
	 * Also write stemmed phrase and score into ".key" file.<p>
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		
		String dirName = Utils.getOption('l', options);
		if (dirName.length() > 0) {
			setDirName(dirName);
		} else {
			setDirName(null);
			throw new Exception("Name of directory required argument.");
		}
		String modelName = Utils.getOption('m', options);
		if (modelName.length() > 0) {
			setModelName(modelName);
		} else {
			setModelName(null);
			throw new Exception("Name of model required argument.");
		}
		
		String vocabularyName = Utils.getOption('v', options);
		if (vocabularyName.length() > 0) {
			setVocabulary(vocabularyName);
		} else {
			setVocabulary(null);
			throw new Exception("Name of vocabulary required argument.");
		}
		
		String vocabularyFormat = Utils.getOption('f', options);
		
		if (!getVocabulary().equals("none")) {
			if (vocabularyFormat.length() > 0) {
				if (vocabularyFormat.equals("skos") || vocabularyFormat.equals("text")) {
					setVocabularyFormat(vocabularyFormat);
				} else {
					throw new Exception("Unsupported format of vocabulary. It should be either \"skos\" or \"text\".");
				}
			} else {
				setVocabularyFormat(null);
				throw new Exception("If a controlled vocabulary is used, format of vocabulary required argument (skos or text).");
			}
		} else {
			setVocabularyFormat(null);
		}
		
		
		String encoding = Utils.getOption('e', options);
		if (encoding.length() > 0) {
			setEncoding(encoding);
		} else {
			setEncoding("default");
		}
		
		String documentLanguage = Utils.getOption('i', options);
		if (documentLanguage.length() > 0) {
			setDocumentLanguage(documentLanguage);
		} else {
			setDocumentLanguage("en");
		}
		
		
		String numPhrases = Utils.getOption('n', options);
		if (numPhrases.length() > 0) {
			setNumPhrases(Integer.parseInt(numPhrases));
		} else {
			setNumPhrases(5);
		}
		
		
		String stemmerString = Utils.getOption('t', options);
		if (stemmerString.length() > 0) {
			stemmerString = "kea.stemmers.".concat(stemmerString);
			setStemmer((Stemmer)Class.forName(stemmerString).newInstance());
		}
		
		String stopwordsString = Utils.getOption('s', options);
		if (stopwordsString.length() > 0) {
			stopwordsString = "kea.stopwords.".concat(stopwordsString);
			setStopwords((Stopwords)Class.forName(stopwordsString).newInstance());
		}

		
		setDebug(Utils.getFlag('d', options));
		setBuildGlobal(Utils.getFlag('b', options));
		setAdditionalInfo(Utils.getFlag('a', options));
		Utils.checkForRemainingOptions(options);
	}
	
	/**
	 * Gets the current option settings.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String [] getOptions() {
		
		String [] options = new String [21];
		int current = 0;
		
		options[current++] = "-l"; 
		options[current++] = "" + (getDirName());
		options[current++] = "-m"; 
		options[current++] = "" + (getModelName());
		options[current++] = "-v"; 
		options[current++] = "" + (getVocabulary());
		options[current++] = "-f"; 
		options[current++] = "" + (getVocabularyFormat());
		options[current++] = "-e"; 
		options[current++] = "" + (getEncoding());
		options[current++] = "-i"; 
		options[current++] = "" + (getDocumentLanguage());
		options[current++] = "-n"; 
		options[current++] = "" + (getNumPhrases());
		options[current++] = "-t"; 
		options[current++] = "" + (getStemmer().getClass().getName());		
		options[current++] = "-s"; 
		options[current++] = "" + (getStopwords().getClass().getName());

		if (getDebug()) {
			options[current++] = "-d";
		}
		
		if (getBuildGlobal()) {
			options[current++] = "-b";
		}
		
		if (getAdditionalInfo()) {
			options[current++] = "-a";
		}
		
		while (current < options.length) {
			options[current++] = "";
		}
		return options;
	}
	
	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options
	 */
	public Enumeration listOptions() {
		
		Vector newVector = new Vector(13);
		
		newVector.addElement(new Option(
				"\tSpecifies name of directory.",
				"l", 1, "-l <directory name>"));
		newVector.addElement(new Option(
				"\tSpecifies name of model.",
				"m", 1, "-m <model name>"));
		newVector.addElement(new Option(
				"\tSpecifies vocabulary name.",
				"v", 1, "-v <vocabulary name>"));
		newVector.addElement(new Option(
				"\tSpecifies vocabulary format.",
				"f", 1, "-f <vocabulary format>"));
		newVector.addElement(new Option(
				"\tSpecifies encoding.",
				"e", 1, "-e <encoding>"));		
		newVector.addElement(new Option(
				"\tSpecifies document language (en (default), es, de, fr).",
				"i", 1, "-i <document language>"));
		newVector.addElement(new Option(
				"\tSpecifies number of phrases to be output (default: 5).",
				"n", 1, "-n"));
		newVector.addElement(new Option(
				"\tSet the stemmer to use (default: SremovalStemmer).",
				"t", 1, "-t <name of stemmer class>"));
		newVector.addElement(new Option(
				"\tSet the stopwords class to use (default: EnglishStopwords).",
				"s", 1, "-s <name of stopwords class>"));
		newVector.addElement(new Option(
				"\tTurns debugging mode on.",
				"d", 0, "-d"));
		newVector.addElement(new Option(
				"\tBuilds global dictionaries for computing TFIDF from the test collection.",
				"b", 0, "-b"));
		newVector.addElement(new Option(
				"\tAlso write stemmed phrase and score into \".key\" file.",
				"a", 0, "-a"));
		
		return newVector.elements();
	}
	
	/**
	 * Collects the stems of the file names.
	 */
	public Hashtable collectStems() throws Exception {
		
		Hashtable stems = new Hashtable();
		
		try {
			File dir = new File(m_dirName);
			String[] files = dir.list();
			for (int i = 0; i < files.length; i++) {
				if (files[i].endsWith(".txt")) {
					String stem = files[i].substring(0, files[i].length() - 4);
					if (!stems.containsKey(stem)) {
						stems.put(stem, new Double(0));
					}
				}
			}
		} catch (Exception e) {
			throw new Exception("Problem opening directory " + m_dirName);
		}
		return stems;
	}
	
	public ArrayList<KeyPhrase> extractKeyphrasesToList(String text) {

		m_KEAFilter.setNumPhrases(m_numPhrases);
		m_KEAFilter.setVocabulary(m_vocabulary);
		m_KEAFilter.setVocabularyFormat(m_vocabularyFormat);
		m_KEAFilter.setDocumentLanguage(getDocumentLanguage());
		m_KEAFilter.setStemmer(m_Stemmer);
		m_KEAFilter.setStopwords(m_Stopwords);

		if (getVocabulary().equals("none")) {
			m_KEAFilter.m_NODEfeature = false;
		} else {
			m_KEAFilter.loadThesaurus(m_Stemmer, m_Stopwords);
		}

		FastVector atts = new FastVector(3);
		atts.addElement(new Attribute("doc", (FastVector) null));
		atts.addElement(new Attribute("keyphrases", (FastVector) null));
		atts.addElement(new Attribute("filename", (String) null));
		Instances data = new Instances("keyphrase_training_data", atts, 0);

		if (m_KEAFilter.m_Dictionary == null) {
			// Build dictionary of n-grams with associated
			// document frequencies
			m_KEAFilter.m_Dictionary = new HashMap();

			// Enumeration over all files in the directory (now in the hash):
			KEAPhraseFilter kpf = new KEAPhraseFilter();

			HashMap hash = m_KEAFilter.getPhrasesForDictionary(kpf
					.tokenize(text));
			Iterator it = hash.keySet().iterator();
			while (it.hasNext()) {
				String phrase = (String) it.next();
				Counter counter = (Counter) m_KEAFilter.m_Dictionary
						.get(phrase);
				if (counter == null) {
					m_KEAFilter.m_Dictionary.put(phrase, new Counter());
				} else {
					counter.increment();
				}
			}
		}

		// Extract keyphrases

		double[] newInst = new double[2];

		newInst[0] = (double) data.attribute(0).addStringValue(text);
		newInst[1] = (double) data.attribute(1).addStringValue("");

		data.add(new Instance(1.0, newInst));

		try {
			m_KEAFilter.input(data.instance(0));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		data = data.stringFreeStructure();
		
		Instance[] topRankedInstances = new Instance[m_numPhrases];
		Instance inst;

		// Iterating over all extracted keyphrases (inst)
		while ((inst = m_KEAFilter.output()) != null) {

			int index = (int) inst.value(m_KEAFilter.getRankIndex()) - 1;

			if (index < m_numPhrases) {
				topRankedInstances[index] = inst;
			}
		}
		
		ArrayList<KeyPhrase> output = new ArrayList<KeyPhrase>();
		
		for (int i = 0; i < m_numPhrases; i++) {
			if (topRankedInstances[i] != null){
				
				String next = topRankedInstances[i].stringValue(m_KEAFilter.getStemmedPhraseIndex());
//				System.out.println(topRankedInstances[i].
//								stringValue(m_KEAFilter.getUnstemmedPhraseIndex()));
				KeyPhrase kp = new KeyPhrase(
						next, 
						topRankedInstances[i].
						stringValue(m_KEAFilter.getUnstemmedPhraseIndex()),
						m_KEAFilter.getStemmed2surface().get(next),
						i,
						topRankedInstances[i].
						value(m_KEAFilter.
								getProbabilityIndex())
						);
			output.add(kp);
			}
			
		}
		return output;
	}
	
	/**
	 * Builds the model from the files
	 */
	public void extractKeyphrases(Hashtable stems) throws Exception {
		
		Vector stats = new Vector();
		
		// Check whether there is actually any data
		// = if there any files in the directory
		if (stems.size() == 0) {
			throw new Exception("Couldn't find any data!");
		}
		m_KEAFilter.setNumPhrases(m_numPhrases);    
		m_KEAFilter.setVocabulary(m_vocabulary);
		m_KEAFilter.setVocabularyFormat(m_vocabularyFormat);
		m_KEAFilter.setDocumentLanguage(getDocumentLanguage());
		m_KEAFilter.setStemmer(m_Stemmer);
		m_KEAFilter.setStopwords(m_Stopwords);
		
		if (getVocabulary().equals("none")) {
			m_KEAFilter.m_NODEfeature = false;
		} else {
			m_KEAFilter.loadThesaurus(m_Stemmer,m_Stopwords);
		}
		
		FastVector atts = new FastVector(3);
		atts.addElement(new Attribute("doc", (FastVector) null));
		atts.addElement(new Attribute("keyphrases", (FastVector) null));
		atts.addElement(new Attribute("filename", (String) null));
		Instances data = new Instances("keyphrase_training_data", atts, 0);
		
		if (m_KEAFilter.m_Dictionary == null) {
			buildGlobalDictionaries(stems);
		}
		
		System.err.println("-- Extracting Keyphrases... ");
		// Extract keyphrases
		Enumeration elem = stems.keys();
		// Enumeration over all files in the directory (now in the hash):
		while (elem.hasMoreElements()) {	
			String str = (String)elem.nextElement();
			
			double[] newInst = new double[2];
			try {
				File txt = new File(m_dirName + "/" + str + ".txt");	
				InputStreamReader is;
				if (!m_encoding.equals("default")) {
					is = new InputStreamReader(new FileInputStream(txt), m_encoding);
				} else {
					is = new InputStreamReader(new FileInputStream(txt));
				}
				StringBuffer txtStr = new StringBuffer();
				int c;
				while ((c = is.read()) != -1) {
					txtStr.append((char)c);
				}
				
				newInst[0] = (double)data.attribute(0).addStringValue(txtStr.toString());
				
			} catch (Exception e) {
				if (m_debug) {
					System.err.println("Can't read document " + str + ".txt");
				}
				newInst[0] = Instance.missingValue();
			}
			try {
				File key = new File(m_dirName + "/" + str + ".key");
				InputStreamReader is; 
				if (!m_encoding.equals("default")) {
					is = new InputStreamReader(new FileInputStream(key), m_encoding);
				} else {
					is = new InputStreamReader(new FileInputStream(key));
				}
				StringBuffer keyStr = new StringBuffer();
				int c;
				
				// keyStr = keyphrases in the str.key file
				// Kea assumes, that these keyphrases were assigned by the author
				// and evaluates extracted keyphrases againse these
				
				while ((c = is.read()) != -1) {
					keyStr.append((char)c);
				}      
				
				newInst[1] = (double)data.attribute(1).addStringValue(keyStr.toString());
			} catch (Exception e) {
				if (m_debug) {
					System.err.println("No existing keyphrases for stem " + str + ".");
				}
				newInst[1] = Instance.missingValue();
			}
			
			
			data.add(new Instance(1.0, newInst));
			
			
			m_KEAFilter.input(data.instance(0));
			
			
			data = data.stringFreeStructure();
			if (m_debug) {
				System.err.println("-- Document: " + str);
			}
			Instance[] topRankedInstances = new Instance[m_numPhrases];
			Instance inst;
			
			// Iterating over all extracted keyphrases (inst)
			while ((inst = m_KEAFilter.output()) != null) {
				
				int index = (int)inst.value(m_KEAFilter.getRankIndex()) - 1;
				
				if (index < m_numPhrases) {
					topRankedInstances[index] = inst;
					
				}
			}
			
			if (m_debug) {
				System.err.println("-- Keyphrases and feature values:");
			}
			FileOutputStream out = null;
			PrintWriter printer = null; 
			File key = new File(m_dirName + "/" + str + ".key");
			if (!key.exists()) {
				out = new FileOutputStream(m_dirName + "/" + str + ".key");
				if (!m_encoding.equals("default")) {
					printer = new PrintWriter(new OutputStreamWriter(out, m_encoding));					
					
				} else {
					printer = new PrintWriter(out);
				}
			}
			double numExtracted = 0, numCorrect = 0;
			
			for (int i = 0; i < m_numPhrases; i++) {
				if (topRankedInstances[i] != null) {
					if (!topRankedInstances[i].
							isMissing(topRankedInstances[i].numAttributes() - 1)) {
						numExtracted += 1.0;
					}
					if ((int)topRankedInstances[i].
							value(topRankedInstances[i].numAttributes() - 1) == 1) {
						numCorrect += 1.0;
					}
					if (printer != null) {
						printer.print(topRankedInstances[i].
								stringValue(m_KEAFilter.getUnstemmedPhraseIndex()));
						printer.print("\t");
						if (m_AdditionalInfo) {								
							printer.print(topRankedInstances[i].
									stringValue(m_KEAFilter.getStemmedPhraseIndex()));
							String next = topRankedInstances[i].stringValue(m_KEAFilter.getStemmedPhraseIndex());
							printer.print("\t");
							printer.print(m_KEAFilter.getStemmed2surface().get(next));
							printer.print("\t");
							printer.print(Utils.
									doubleToString(topRankedInstances[i].
											value(m_KEAFilter.
													getProbabilityIndex()), 4));
						}
						printer.println();
					}
					if (m_debug) {
						System.err.println(topRankedInstances[i]);
					}
				}
			}
			if (numExtracted > 0) {
				if (m_debug) {
					System.err.println("-- " + numCorrect + " correct");
				}
				stats.addElement(new Double(numCorrect));
			}
			if (printer != null) {
				printer.flush();
				printer.close();
				out.close();
			}
		}
		double[] st = new double[stats.size()];
		for (int i = 0; i < stats.size(); i++) {
			st[i] = ((Double)stats.elementAt(i)).doubleValue();
		}
		double avg = Utils.mean(st);
		double stdDev = Math.sqrt(Utils.variance(st));
		
		System.err.println("Avg. number of matching keyphrases compared to existing ones : " +
				Utils.doubleToString(avg, 2) + " +/- " + 
				Utils.doubleToString(stdDev, 2));
		System.err.println("Based on " + stats.size() + " documents");
		// m_KEAFilter.batchFinished();
	}
	
	private void buildGlobalDictionaries (Hashtable stems) throws Exception  {
		
		System.err.println("--- Building global dictionaries from the test collection.. ");
		
		
		// Build dictionary of n-grams with associated
		// document frequencies
		
		m_KEAFilter.m_Dictionary = new HashMap();
		
		Enumeration elem = stems.keys();
		
		// Enumeration over all files in the directory (now in the hash):
		while (elem.hasMoreElements()) {	
			String str = (String)elem.nextElement();
			
			File txt = new File(m_dirName + "/" + str + ".txt");	
			InputStreamReader is;
			if (!m_encoding.equals("default")) {
				is = new InputStreamReader(new FileInputStream(txt), m_encoding);
			} else {
				is = new InputStreamReader(new FileInputStream(txt));
			}
			StringBuffer txtStr = new StringBuffer();
			int c;
			while ((c = is.read()) != -1) {
				txtStr.append((char)c);
			}
		
			KEAPhraseFilter kpf = new KEAPhraseFilter();
			
			HashMap hash = m_KEAFilter.getPhrasesForDictionary(kpf.tokenize(txtStr.toString()));
			Iterator it = hash.keySet().iterator();
			while (it.hasNext()) {
				String phrase = (String)it.next();
				Counter counter = (Counter)m_KEAFilter.m_Dictionary.get(phrase);
				if (counter == null) {
					m_KEAFilter.m_Dictionary.put(phrase, new Counter());
				} else {
					counter.increment();
				}
			}
		}
	}
	
	
	/** 
	 * Loads the extraction model from the file.
	 */
	public void loadModel() throws Exception {
		
		BufferedInputStream inStream =
			new BufferedInputStream(new FileInputStream(m_modelName));
		ObjectInputStream in = new ObjectInputStream(inStream);
		m_KEAFilter = (KEAFilter)in.readObject();
		
		// If TFxIDF values are to be computed from the test corpus
		if (m_buildGlobal == true) {
			if (m_debug) {
				System.err.println("-- The global dictionaries will be built from this test collection..");
			}
			m_KEAFilter.m_Dictionary = null;			
		}
		in.close();
	}
	
	/**
	 * The main method.  
	 */
	public static void main(String[] ops) {
		
		KEAKeyphraseExtractor kmb = new KEAKeyphraseExtractor();
		try {
			// Checking and Setting Options selected by the user:
			kmb.setOptions(ops);      
			System.err.print("Extracting keyphrases with options: ");
			
			// Reading Options, which were set above and output them:
			String[] optionSettings = kmb.getOptions();
			for (int i = 0; i < optionSettings.length; i++) {
				System.err.print(optionSettings[i] + " ");
			}
			System.err.println();
			
			// Loading selected Model:
			System.err.println("-- Loading the Model... ");
			kmb.loadModel();
			// Extracting Keyphrases from all files in the selected directory
			// stem == the name of the file without ".txt"
			
			kmb.extractKeyphrases(kmb.collectStems());
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
			System.err.println("\nOptions:\n");
			Enumeration en = kmb.listOptions();
			while (en.hasMoreElements()) {
				Option option = (Option) en.nextElement();
				System.err.println(option.synopsis());
				System.err.println(option.description());
			}
		}
	}
}
