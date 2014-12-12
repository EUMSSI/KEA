package kea.main;

import java.util.HashSet;

public class KeyPhrase {

	private final String stemmed;
	private final String unstemmed;
	private final HashSet<String> surfaceForms;
	private final int rank;
	private final double probability;
	
	public KeyPhrase(String stemmed, String unstemmed, HashSet<String> surfaceForms, int rank, double probability){
		this.stemmed = stemmed;
		this.unstemmed = unstemmed;
		this.surfaceForms = surfaceForms;
		this.rank = rank;
		this.probability = probability;
	}

	public String getStemmed() {
		return stemmed;
	}

	public String getUnstemmed() {
		return unstemmed;
	}

	public HashSet<String> getSurfaceForms() {
		return surfaceForms;
	}

	public int getRank() {
		return rank;
	}

	public double getProbability() {
		return probability;
	}
	
}
