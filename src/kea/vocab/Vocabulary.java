package kea.vocab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import kea.stemmers.SpanishStemmerSB;
import kea.stemmers.Stemmer;
import kea.stopwords.Stopwords;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;


/**
* Builds an index with the content of the controlled vocabulary.
* Accepts vocabularies as rdf files (SKOS format) and in plain text format:
* vocabulary_name.en (with "ID TERM" per line) - descriptors & non-descriptors
* vocabulary_name.use (with "ID_NON-DESCR \t ID_DESCRIPTOR" per line)
* vocabulary_name.rel (with "ID \t RELATED_ID1 RELATED_ID2 ... " per line)
* See KEA's homepage for more details.
* @author Olena Medelyan
*/

public class Vocabulary implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/** Location of the rdf version of the controlled vocabulary
	 * it needs to be in the SKOS format! */
	public static File SKOS;
	/** Location of the vocabulary's *.en file
	 * containing all terms of the vocabularies and their ids.*/
	public static File EN;
	/** Location of the vocabulary's *.use file
	 * containing ids of non-descriptor with the corresponding ids of descriptors.*/
	public static File USE;
	/** Location of the vocabulary's *.rel file
	 * containing semantically related terms for each descriptor in the vocabulary.*/
	public static File REL;
	
	// if the type of the semantic relation will be required later 
	// this could be a file containing
	// this information
	// public static File RT;
	
	/** 
	 * Boolean describing which vocabulary format has been chosen:
	 * true if SKOS, false if text.
	 */
	private boolean useSkos;
	
	/** <i>Vocabulary</i> index */
	private HashMap<String,String> VocabularyEN = null;
	/** <i>Vocabulary</i> reverse index */
	private HashMap<String,String> VocabularyENrev = null;
	/** <i>Vocabulary</i> non-descriptors - descriptors list */
	private HashMap<String,String> VocabularyUSE = null;
	/** <i>Vocabulary</i> related terms */
	private HashMap<String,Vector<String>> VocabularyREL = null;
	private HashMap<String,String> VocabularyRT = null;
	
	/** The document language */
	private String m_language;
	
	/** The default stemmer to be used */
	private Stemmer m_Stemmer;
	
	/** The list of stop words to be used */
	private Stopwords m_Stopwords;
	
	
	
	/** Vocabulary constructor. 
	 * 
	 * Given the name of the vocabulary and the format it first checks whether
	 * the VOCABULARIES directory contains the specified files:
	 * - vocabularyName.rdf if skos format is selected
	 * - or a set of 3 flat files starting with vocabularyName and with extensions
	 * .en (id term)
	 * .use (non-descriptor \t descriptor)
	 * .rel (id \t related_id1 related_id2 ...)
	 * If the required files exist, the vocabulary index is built.
	 * 
	 * @param vocabularyName The name of the vocabulary file (before extension).
	 * @param vocabularyFormat The format of the vocabulary (skos or text).
	 * */
	
	public Vocabulary(String vocabularyName, String vocabularyFormat, String documentLanguage) {
		m_language = documentLanguage;
		if (vocabularyFormat.equals("skos")) {
			SKOS = new File("VOCABULARIES/" + vocabularyName + ".rdf");
			if (!SKOS.exists()){
				System.err.println("File VOCABULARIES/" + vocabularyName + ".rdf does not exist.");
				System.exit(1);
			} 
			useSkos = true;
			
		} else if (vocabularyFormat.equals("text")) {
			EN = new File("VOCABULARIES/" + vocabularyName + ".en");
			USE = new File("VOCABULARIES/" + vocabularyName + ".use");
			REL = new File("VOCABULARIES/" + vocabularyName + ".rel");
		//	RT = new File("VOCABULARIES/" + vocabularyName + ".pairs.p1");
			
			if (!EN.exists()) {
				System.err.println("File VOCABULARIES/" + vocabularyName + ".en does not exist.");
				System.exit(1);
			}		
			if (!USE.exists()) {
				System.err.println("File VOCABULARIES/" + vocabularyName + ".list.use does not exist.");
				System.exit(1);
			}
			if (!REL.exists()) {
				System.err.println("File VOCABULARIES/" + vocabularyName + ".rel.p1 does not exist.");
				System.exit(1);
			}
//			if (!RT.exists()) {
//				System.err.println("File VOCABULARIES/" + vocabularyName + ".pairs.p1 does not exist.");
//				System.exit(1);
//			}

		}

	}
	
	/**
	 * Starts initialization of the vocabulary.
	 *
	 */
	public void initialize() {
		
		System.err.println("-- Loading the Index...");
		if (useSkos) {
			try {
				buildSKOS();
			} catch(Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else {
			try {
				build();
			} catch(Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}	
	}
	
	/**
	 * Set the Stemmer value.
	 * @param newStemmer The new Stemmer value.
	 */
	public void setStemmer(Stemmer newStemmer) {	

			this.m_Stemmer = newStemmer;
		
	}
	/**
	 * Set the M_Stopwords value.
	 * @param newM_Stopwords The new M_Stopwords value.
	 */
	public void setStopwords(Stopwords newM_Stopwords) {	
		this.m_Stopwords = newM_Stopwords;
	}
	
	
	/**
	 * Builds the vocabulary indexes from SKOS file.
	 */
	public void buildSKOS() throws Exception {
		
		
		System.err.println("-- Building the Vocabulary index from SKOS file");
		
		VocabularyEN = new HashMap<String,String>();
		VocabularyENrev = new HashMap<String,String>();
		VocabularyUSE = new HashMap<String,String>();
		VocabularyREL = new HashMap<String,Vector<String>>();
		VocabularyRT = new HashMap<String,String>();
		
		
        // create an empty model
        Model model = ModelFactory.createDefaultModel();
        
        
        try {

    	    model.read(new InputStreamReader(new FileInputStream(SKOS),"UTF-8"), "");
            
    	    StmtIterator iter;
    	    Statement stmt;
    	    Property relation;
    	    Resource concept;
    	    RDFNode value;
    	    
    	    
    	    
    	    int count = 1;
    	    // Iterating over all statements in the SKOS file
    	    iter = model.listStatements();
    	    
    	    while (iter.hasNext()) {
    	    	stmt = iter.nextStatement();

    	    	// id of the concept (Resource), e.g. "c_4828"
    	    	concept = stmt.getSubject();    	    
    	    	String id = concept.getURI();
    	    	
    	    	// relation or Property of the concept, e.g. "narrower"
    	    	relation = stmt.getPredicate();
    	    	String rel = relation.getLocalName();
    	    	
    	    	// value of the property, e.g. c_4828 has narrower term "c_4829"
    	    	value = stmt.getObject();  	    	
    	    	String val = value.toString();
    	    	
    	    	//System.out.println("Concept " + concept);
    	    	//System.out.println("Relation " + rel);    	    	
    	    	//System.out.println("Value " + val);
    	    	
    	    	if (rel.equals("prefLabel")) {
    	    		
    	    		String descriptor;
    	    		
    	    		if (val.contains("@")) {
    	    			String[] val_components = val.split("@");
    	    			// System.err.println(val_components[1] + " " + m_language);
    	    			if (val_components[1].equals(m_language)) {
    	    			//	System.err.println("Yes");
    	    				descriptor = val_components[0];
    	    			} else {
    	    				continue;
    	    			}
    	    		} else {
    	    			descriptor = val;
    	    		}
        	    	
    	    		String avterm = pseudoPhrase(descriptor); 
    	    		if (avterm.equals("")) {
    	    			avterm = descriptor;
    	    		}
    	    		
    	    		// System.out.println(descriptor + " ==> " + avterm);
    	    		
    				if (avterm.length() > 1) {    					
    					VocabularyEN.put(avterm, id);
    					VocabularyENrev.put(id,descriptor);
    				}	
    				
    	    		// fill here the index hash
    	    		
    	    		// id => descriptor
    	    		//if (id.equals("http://www.fao.org/aos/agrovoc#c_4314")) {
    	    		//	System.out.println("Descriptor " + descriptor + " (" + id + ")");
    	    		//}
    	    		
    	    	} else if (rel.equals("altLabel") || (rel.equals("hiddenLabel"))) {
    	    		
    	    		String non_descriptor;
    	    		
    	    		if (val.contains("@")) {
    	    			String[] val_components = val.split("@");
    	    			// System.err.println(val_components[1] + " " + m_language);
    	    			if (val_components[1].equals(m_language)) {
    	    			//	System.err.println("Yes");
    	    				non_descriptor = val_components[0];
    	    			} else {
    	    				continue;
    	    			}
    	    		} else {
    	    			non_descriptor = val;
    	    		}
    	    		// System.out.println("Descriptor " + non_descriptor);
    	    		// first add the non_descriptor to the index hash	
    	    		// then fill here non-descriptor hash
    	    		
    	    		// id => id_non_descriptor
    	    		 addNonDescriptor (count, id, non_descriptor);                   
                     count++;
    				
    	    		//System.out.println("Descriptor " + VocabularyENrev.get(id) + " with id (" + id + ")" +
    	    			//	" has a non-descriptor " + non_descriptor + " (" + id_non_descriptor + ")");    	    		
    	    		
    	    	} else if (rel.equals("broader") 
    	    		|| rel.equals("narrower") 
    	    		|| rel.equals("composite")
    	    		|| rel.equals("compositeOf")
    	    		|| rel.equals("hasTopConcept")
    	    		|| rel.equals("related")) {
    	    		    	    	
        	    	String id_related = val;
        	    	
        	    	// System.out.println("Descriptor " + VocabularyENrev.get(id) + " with id " + id + 
    	    		//		" has a " + rel + " term " + VocabularyENrev.get(id_related) + " with id (" + id_related + ")");    
        	    	
        	    	// fill here semantic relations hash
        	    	// id => id_related
        	    	
        	    	if (VocabularyREL.get(id) == null) {
        	    		Vector rt = new Vector();
        	    		rt.add(id_related);         	    		
        	    		VocabularyREL.put(id,rt);
        	    	}	else {      	    		
        	    		Vector rt = (Vector)VocabularyREL.get(id);
        	    		rt.add(id_related);         	    		
        	    		VocabularyREL.put(id,rt);        	    		        	    		
        	    	}
        	    	
        	    	VocabularyRT.put(id + "-" + id_related,rel);
        	    	if (rel.equals("related")) {
        	    		VocabularyRT.put(id_related + "-" + id,rel);
        	    	}
    				
        	    	
        	    	// VocabularyRT.put("id-id_related","1");
        	    	// VocabularyRT.put("id_related-id","1");
    				
    	    	}
    	    	
    	    }
    	   
    	    // Some statistics:
    	 //    System.out.println(VocabularyEN.size() + " terms in total");
    	 //   System.out.println(VocabularyUSE.size() + " non-descriptors");
    	  //   System.out.println(VocabularyREL.size() + " terms have related terms");
    	    
    	} catch (Exception e) {
    	   e.printStackTrace(); 
    	}
	}
	
	

   
    private void addNonDescriptor (int count, String id_descriptor, String non_descriptor) {
        //     id => id_non_descriptor
        String id_non_descriptor = "d_" + count;
        count++;
   
        String avterm = pseudoPhrase(non_descriptor);
        if (avterm.length() > 2) {                       
            VocabularyEN.put(avterm, id_non_descriptor);
            VocabularyENrev.put(id_non_descriptor,non_descriptor);
        }   
        VocabularyUSE.put(id_non_descriptor,id_descriptor);
    }
   
    public String remove (String[] words, int i) {

        String result = "";
        for (int j = 0; j < words.length; j++) {
            if ((j != i) && (!m_Stopwords.isStopword(words[j]))) {
               
                result = result + words[j];
               
                if ((j+1) != words.length) {
                    result = result + " ";
                }
            }
             
        }
        return result;
    }
    
	/**
	 * Builds the vocabulary index from the text files.
	 */
	public void build() throws Exception {
		
		System.err.println("-- Building the Vocabulary index");
		
		VocabularyEN = new HashMap<String,String>();
		VocabularyENrev = new HashMap<String,String>();
		
		String readline;
		String term;
		String avterm;
		String id;
		
		try {	  			
			InputStreamReader is = new InputStreamReader(new FileInputStream(EN));     
			BufferedReader br = new BufferedReader(is);
			while((readline=br.readLine()) != null) {
				int i = readline.indexOf(' ');
				term = readline.substring(i+1);
				
				avterm = pseudoPhrase(term);
				
				if (avterm.length() > 2) {
					id = readline.substring(0,i); 
					VocabularyEN.put(avterm, id);
					VocabularyENrev.put(id,term);
				}				
			}
		} catch (Exception e) {
			e.printStackTrace();	
		}
		
	}   
	
	
	/**
	 * Builds the vocabulary index with descriptors/non-descriptors relations.
	 */
	public void buildUSE() throws Exception {
		if (!useSkos) {
			VocabularyUSE = new HashMap<String,String>();
			String readline;
			String[] entry;
			
			try {	  
				
				InputStreamReader is = new InputStreamReader(new FileInputStream(USE));     
				BufferedReader br = new BufferedReader(is);
				while((readline=br.readLine()) != null) {
					entry = readline.split("\t");
					
//					if more than one descriptors for
//					one non-descriptors are used, ignore it!
//					probably just related terms (cf. latest edition of Agrovoc)
					
					if ((entry[1].indexOf(" ")) == -1) {
						VocabularyUSE.put(entry[0],entry[1]);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();				 	
			}
		}
		
	}
	
	/**
	 * Builds the vocabulary index with semantically related terms.
	 */
	public void buildREL() throws Exception {
		if (!useSkos) {
			
			System.err.println("-- Building the Vocabulary index with related pairs");
			
			VocabularyREL = new HashMap();
			
			String readline;
			String[] entry;
			
			
			try {	  
				
				InputStreamReader is = new InputStreamReader(new FileInputStream(REL));     
				BufferedReader br = new BufferedReader(is);
				while((readline=br.readLine()) != null) {
					entry = readline.split("\t");
					String[] temp = entry[1].split(" ");
					Vector<String> rt = new Vector<String>();
					for (int i = 0; i < temp.length; i++) {
						rt.add(temp[i]);
					}
					VocabularyREL.put(entry[0],rt);
				}
			} catch (Exception e) {
				e.printStackTrace(); 	
			}
		}
		
	}	
	
// Might be useful later, when the kind of relation is important
// or wether two terms are related or not
//	public void buildRT() throws Exception {
//		
//		VocabularyRT = new HashMap();
//		
//		String[] entry;
//		String readline;
//		try {	  				
//			InputStreamReader is2 = new InputStreamReader(new FileInputStream(RT));     
//			BufferedReader br2 = new BufferedReader(is2);	  
//			while((readline=br2.readLine()) != null) {
//				entry = split(readline,"\t");
//				String pair = entry[0] + "-" + entry[1];
//				VocabularyRT.put(pair,"1");
//				
//			}
//		} catch (Exception e) {
//			System.err.println("You need to put the .pairs file into KEA directory"); 	
//		}
//		
//	}
//	
	
	

	/**
	 * Checks whether a normalized version of a phrase (pseudo phrase)
	 * is a valid vocabulary term.
	 * 
	 * @param phrase
	 * @return true if phrase is in the vocabulary
	 */
	public boolean containsEntry(String phrase) {
		return VocabularyEN.containsKey(phrase);
	}
	
	/**
	 * Given a phrase returns its id in the vocabulary.
	 * @param phrase
	 * @return id of the phrase in the vocabulary index
	 */
	public String getID(String phrase) {
		String pseudo = pseudoPhrase(phrase);
		String id = null;
		if (pseudo != null) {
			id = (String)VocabularyEN.get(pseudo);
			if (VocabularyUSE.containsKey(id)) {
				id = (String)VocabularyUSE.get(id);
			}
		}
		return id;
	}
	
	/**
	 * Given id, gets the original version of vocabulary term.
	 * @param id
	 * @return original version of the vocabulary term
	 */
	public String getOrig(String id) {
		return (String)VocabularyENrev.get(id);
	}
	
	/**
	 * Given id of the non-descriptor returs the id of the corresponding descriptor
	 * @param id of the non-descriptor
	 * @return id of the descriptor
	 */
	public String getDescriptor(String id) {
		return (String)VocabularyUSE.get(id);
	}
	
	/**
	 * Given id of a term returns the list with ids of terms related to this term.
	 * @param id
	 * @return a vector with ids related to the input id
	 */
	public Vector<String> getRelated(String id) {
		return VocabularyREL.get(id);
	}
	
	
	/**
	 * Given an ID of a term gets the list of all IDs of terms
	 * that are semantically related to the given term
	 * with a specific relation
	 * @param id, relation
	 * @return a vector with ids related to the input id by a specified relation
	 */
	public Vector getRelated (String id, String relation) {
		Vector related = new Vector(); 
		Vector all_related = (Vector)VocabularyREL.get(id);
		if (all_related != null) {
    	
			for (int d = 0; d < all_related.size(); d++) {
				String rel_id = (String)all_related.elementAt(d);	
			
				String rel = (String)VocabularyRT.get(id + "-" + rel_id);

				if (rel != null) { 
					if (rel.equals(relation)) {
						related.add(rel_id);
					}
				} else {
					System.err.println("Problem with " + getOrig(id) + " and " + getOrig(rel_id));	
				}
			}
    	}
    	return related;
	}
	
	
	/** 
	 * Generates the preudo phrase from a string.
	 * A pseudo phrase is a version of a phrase
	 * that only contains non-stopwords,
	 * which are stemmed and sorted into alphabetical order. 
	 */
	public String pseudoPhrase(String str) {
		// System.err.print(str + "\t");
		String[] pseudophrase;
		String[] words;
		String str_nostop;
		String stemmed;
		
		
		str = str.toLowerCase();
		
		
		// This is often the case with Mesh Terms,
		// where a term is accompanied by another specifying term
		// e.g. Monocytes/*immunology/microbiology
		// we ignore everything after the "/" symbol.
		if (str.matches(".+?/.+?")) {
			String[] elements = str.split("/");		
			str = elements[0];
		}	
				
		// removes scop notes in brackets
		// should be replaced with a cleaner solution !!
		if (str.matches(".+?\\(.+?")) {
			String[] elements = str.split("\\(");		
			str = elements[0];			
		}	

	
		
		// Remove some non-alphanumeric characters
		
		// str = str.replace('/', ' ');
		str = str.replace('-', ' ');
		str = str.replace('&', ' ');
		

		str = str.replaceAll("\\*", "");
		str = str.replaceAll("\\, "," ");
		str = str.replaceAll("\\. "," ");
		str = str.replaceAll("\\:","");
	
		
		str = str.trim();
		
		// Stem string
		words = str.split(" ");
		str_nostop = "";
	
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			if (!m_Stopwords.isStopword(word)) {
				
				if (word.matches(".+?\\'.+?")) {
					String[] elements = word.split("\\'");		
					word = elements[1];			
				}	

				
				if (str_nostop.equals("")) {
					str_nostop = word;
				} else {
					str_nostop = str_nostop + " " + word;
				}
			}
		}

		stemmed = m_Stemmer.stemString(str_nostop);
		// System.err.println(stemmed + "\t" + str_nostop + "\t"+ str);
		pseudophrase = stemmed.split(" ");
		Arrays.sort(pseudophrase);
		//System.err.println(join(pseudophrase));
		return join(pseudophrase);
	}
	
	/** 
	 * Joins an array of strings to a single string.
	 */
	private static String join(String[] str) {
		String result = "";
		for(int i = 0; i < str.length; i++) {
			if (result != "") {
				result = result + " " + str[i];
			} else {
				result = str[i];
			}
		}
		return result;
	}	
	
}


