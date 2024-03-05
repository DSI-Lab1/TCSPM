import java.util.ArrayList;
import java.util.List;


class Sequence{
	
	final List<Itemset> itemsets = new ArrayList<Itemset>();
	
	/**
	 * Default constructor
	 */
	public Sequence(){
	}
	
	/**
	 * Add an itemset to that sequence
	 * @param itemset
	 */
	public void addItemset(Itemset itemset) {
		itemsets.add(itemset);
	}
	
	/**
	 * Make a copy of that sequence
	 * @return a copy of that sequence
	 */
	public Sequence cloneSequence(){
		// create a new empty sequence
		Sequence sequence = new Sequence();
		// for each itemset
		for(Itemset itemset : itemsets){
			// copy the itemset
			sequence.addItemset(itemset.cloneItemSet());
		}
		return sequence; // return the sequence
	}

	/**
	 * Print this sequence to System.out.
	 */
	public void print() {
		System.out.print(toString());
	}
	
	/**
	 * Return a string representation of this sequence
	 */
	public String toString() {
		// create a StringBuilder
		StringBuilder r = new StringBuilder("");
		// for each itemset
		for(Itemset itemset : itemsets){
			// for each item in the current itemset
			for(Integer item : itemset.getItems()){
				// append the item to the StringBuilder
				String string = item.toString();
				r.append(string);
				r.append(' ');
			}
			// add a right parenthesis to indicate the end of the itemset
			r.append("-1 "); 
		}
		return r.toString(); // return the string
	}

	/**
	 * Get the list of itemsets in this sequence
	 * @return A list of itemsets.
	 */
	public List<Itemset> getItemsets() {
		return itemsets;
	}
	
	/**
	 * Get the itemset at a given position
	 * @param index the position
	 * @return the itemset
	 */
	public Itemset get(int index) {
		return itemsets.get(index);
	}
	
	/**
	 * Get the ith item in this sequence (no matter in which itemset)
	 * @param i  the position
	 * @return the item
	 */
	public Integer getIthItem(int i) { 
		// make a for loop through all itemset
		for(int j=0; j< itemsets.size(); j++){
			// if the position that we look for is in this itemset
			if(i < itemsets.get(j).size()){
				// we return the position in this itemset
				return itemsets.get(j).get(i);
			}
			// otherwise we substract the size of the current itemset
			// from the position that we are searching for
			i = i- itemsets.get(j).size();
		}
		return null;
	}
	
	/**
	 * Get the number of elements in this sequence
	 * @return the number of elements.
	 */
	public int size(){
		return itemsets.size();
	}
	
	/**
	 * Return the sum of the total number of items in this sequence
	 */
	public int getItemOccurencesTotalCount(){
		// variable to count
		int count =0;
		// for each itemset
		for(Itemset itemset : itemsets){
			count += itemset.size();  // add the size of the current itemset
		}
		return count; // return the total
	}
	
	// NEW FOR MAX...
	public boolean containsItem(Integer item) {
		for(Itemset itemset : itemsets) {
			if(itemset.getItems().contains(item)) {
				return true;
			}
		}
		return false;
	}

	public Integer getLastItem() {
		Itemset a = itemsets.get(itemsets.size()-1);
		return a.get(a.size()-1);
	}

	public Integer getFirstItem() {
		Itemset a = itemsets.get(0);
		return a.get(0);
	}
	
	
	public Sequence getSequenceByIndex(Sequence sequence,int imatch,int iimatch){
		Sequence tem = new Sequence();
		Itemset itemset = new Itemset();
		for(int i = iimatch; i < sequence.getItemsets().get(imatch).size(); i++){
			
			itemset.addItem(sequence.getItemsets().get(imatch).get(i));
		}
		tem.addItemset(itemset);
		for(int i = imatch + 1; i < sequence.getItemsets().size(); i++){
			
			tem.addItemset(sequence.getItemsets().get(i));
		}
		
		return  tem;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((itemsets == null) ? 0 : itemsets.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sequence other = (Sequence) obj;
		if (itemsets == null) {
			if (other.itemsets != null)
				return false;
		} else if (!itemsets.equals(other.itemsets))
			return false;
		return true;
	}

}
