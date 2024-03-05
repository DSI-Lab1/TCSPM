import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class AlgoTCSPM11 {

	/** start time of last algorithm execution */
	private long startTime;
	/** end time of last algorithm execution */
	private long endTime;
	/** number of patterns */
	public int patternCount;

	private int minsup = 0;
	/** Target sequence */
	Sequence querySequence = new Sequence();
	List<Integer> qsArray = new ArrayList<>();
    int count = 0;
	/** object to write to a file */
	BufferedWriter writer = null;

	/** MemoryDB： sequence that contains the query sequence */
	List<int[]> MemoryDB = new ArrayList<int[]>();

	/** AbsDB： sequence that contains the query sequence */
	List<Integer> AbsIndx = new ArrayList<Integer>();

	// record the sequence id for each item
	Map<Integer, List<Integer>> mapSequenceIDs = new HashMap<Integer, List<Integer>>();
	
	// recored the all positions for each item in all sequences
	Map<Integer, List<PseudoSequence>> itemIndexMap = new HashMap<Integer, List<PseudoSequence>>();

	public void runAlgorithm(String input, String outputFilePath, int minsupAbs, Sequence query) throws IOException {
		// initialization
		this.querySequence = query;
		minsup = minsupAbs;

		// create an object to write the file
		writer = new BufferedWriter(new FileWriter(outputFilePath));

		// initialize the number of patterns found
		patternCount = 0;
		// to log the memory used
		MemoryLogger.getInstance().reset();

		// record start time
		startTime = System.currentTimeMillis();
		// RUN THE ALGORITHM
		loadDB(input);
		createSequenceChain();
		//printDB();
		contiguousPatternGrowth();

		// record end time
		endTime = System.currentTimeMillis();
		// close the file
		writer.close();
	}

	/**
	 * read database file and filter the sequence that does not contain query
	 * sequence
	 */
	private void loadDB(String input) throws IOException {
		getArrayofqs();
		try {
			// read the file
			FileInputStream fin = new FileInputStream(new File(input));
			BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
			String thisLine;
			int sid = 0;
			// for each line (sequence) in the file until the end
			while ((thisLine = reader.readLine()) != null) {
				// if the line is a comment, is empty or is a kind of metadata
				if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
						|| thisLine.charAt(0) == '@') {
					continue;
				}
				// split the sequence according to spaces into tokens
				String tokens[] = thisLine.split(" ");

				int[] transactionArray = new int[tokens.length];
				for (int i = 0; i < tokens.length; i++) {
					int item = Integer.parseInt(tokens[i]);
					transactionArray[i] = item;
					if (item < 0)
						continue;
					List<Integer> sequenceIDs = mapSequenceIDs.get(item);
					if (sequenceIDs == null) {
						// if the set does not exist, create one
						sequenceIDs = new ArrayList<Integer>();
						mapSequenceIDs.put(item, sequenceIDs);
					}
					if (sequenceIDs.size() == 0 || sequenceIDs.get(sequenceIDs.size() - 1) != sid) {
						sequenceIDs.add(sid);
					}
				}
				int indexFlag = checkTargertSequence(transactionArray);
				//System.out.println(indexFlag);
				// 返回值为-1代表不包含qs
				if (indexFlag == -1) {
					for (int j = 0; j < transactionArray.length; j++) {
						int item = transactionArray[j];
						if (item < 0)
							continue;
						else {
							List<Integer> sequenceIDs = mapSequenceIDs.get(item);
							if (sequenceIDs.size() > 0 && sequenceIDs.get(sequenceIDs.size() - 1) == sid)
								sequenceIDs.remove(sequenceIDs.size() - 1);
						}
					}
				}else{
					AbsIndx.add(indexFlag);
					MemoryDB.add(transactionArray);
					sid++;
				}
					
				
              
			}
			reader.close(); // close the input file
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 将query sequence转化成数组
	private void getArrayofqs() {
		for (Itemset itemset : querySequence.itemsets) {
			for (int i = 0; i < itemset.size(); i++) {
				qsArray.add(itemset.get(i));
			}
			qsArray.add(-1);
		}
	}


	// 检测序列中是否含有Target sequence
	private int checkTargertSequence(int[] sequence) {
		int index = qsArray.size() - 2;
		// 反向遍历
		for (int i = sequence.length - 1; i >= 0;) {
			//System.out.println("i: "+tokens[i]+" "+qsArray.get(index));
			int item = sequence[i];
			if (item == qsArray.get(index)) {
				//System.out.println("相等"+i+" "+index);
				int temIndex = index;
				boolean match = false;
				while( i >= 0&&sequence[i] != -1) {
					if (sequence[i] == qsArray.get(index)) {
						if(index == 0)
							return i;
						index--;
					}
					i--;
					//说明该项集匹配完
					if (index == -1 || qsArray.get(index) == -1){
						index--;
						match = true;
						break;
					}
					
					
				}
				//改项集匹配失败
				if(match == false)
					index = temIndex;
				
				while( i >= 0 &&sequence[i] != -1){
					i--;
				}
				
			} else
				i--;
			
		}
		return -1;
	}

	/**
	 * Mark infrequent items with 0 construct the itemIndexMap,lastIndexMap
	 * 
	 * @throws IOException
	 */
	private void createSequenceChain() throws IOException {
		 
		for (int i = 0; i < MemoryDB.size(); i++) {
			int[] sequence = MemoryDB.get(i);
			//临时变量，保存item在序列i中出现的位置
			Map<Integer, PseudoSequence> sequenceIndex = new HashMap<Integer, PseudoSequence>();
			for (int j = 0; j <=AbsIndx.get(i); j++) {
				int item = sequence[j];
				if (item > 0) {

					if (mapSequenceIDs.get(item) != null && mapSequenceIDs.get(item).size() >=minsup){
						if (sequenceIndex.get(item) == null) {
							sequenceIndex.put(item, new PseudoSequence(i));
						}
						PseudoSequence temp = sequenceIndex.get(item);
						temp.getIndex().add(j);
					}
				}
			}
			for (Map.Entry<Integer, PseudoSequence> tempPseudo : sequenceIndex.entrySet()) {
				int item = tempPseudo.getKey();
				if (itemIndexMap.get(item) == null) {
					itemIndexMap.put(item, new ArrayList<PseudoSequence>());
				}
				List<PseudoSequence> pseudoList = itemIndexMap.get(item);
				pseudoList.add(tempPseudo.getValue());
				itemIndexMap.put(item, pseudoList);
			}

		}

	}

	private void printDB() {
//		System.out.println("MemeryDB数据：");
//		for (int i = 0; i < MemoryDB.size(); i++) {
//			for (int j = 0; j < MemoryDB.get(i).length; j++) {
//				System.out.print(MemoryDB.get(i)[j] + " ");
//			}
//			System.out.println();
//		}
		for (Map.Entry<Integer, List<Integer>> tem : mapSequenceIDs.entrySet()) {
			System.out.println(tem.getKey() + "  " + tem.getValue());
		}
//		System.out.println("每个item在不同序列中出现的位置");
//		for (Map.Entry<Integer, List<PseudoSequence>> lastIndex : itemIndexMap.entrySet()) {
//			int item = lastIndex.getKey();
//			System.out.println("项" + item);
//			for (PseudoSequence pse : lastIndex.getValue()) {
//				System.out.println(pse.toString());
//			}
//
//		}
		
	}

	/** 遍历所有频繁项，递归进行成长 */
	private void contiguousPatternGrowth() throws IOException {
		MemoryLogger.getInstance().checkMemory();
		// 遍历所有频繁项
		for (Map.Entry<Integer, List<PseudoSequence>> itemMap : itemIndexMap.entrySet()) {
			int sup = itemMap.getValue().size();// 得到item的支持度
			int item = itemMap.getKey();
			int iimatch = 0;
			int imatch = 0;
			if(sup < minsup)
				continue;
			// 更新标记
			if (querySequence.size() != 0 && item == querySequence.itemsets.get(imatch).get(iimatch)) {
				iimatch++;
			}
			// 构建序列，递归地进行增长
			Sequence prefix = new Sequence();
			prefix.addItemset(new Itemset(item));
			dfsPatternGrowth(imatch, iimatch, prefix, itemMap.getValue(), sup);
		}
		MemoryLogger.getInstance().checkMemory();
	}

	/** 递归进行模式增长 */
	private void dfsPatternGrowth(int imatch, int iimatch, Sequence prefix, List<PseudoSequence> itemPseudo, int sup)
			throws IOException {
		count++;
		MemoryLogger.getInstance().checkMemory();
		if (imatch >= querySequence.size()) {
			savePattern(prefix, sup, itemPseudo);
		}else {
			if(iimatch >= querySequence.get(imatch).size()){
				if(imatch +1 == querySequence.size() ){
					savePattern(prefix, sup,itemPseudo);
				}
			}
		}
		// 分别获取S扩展项和I扩展项
		Map<Integer, List<PseudoSequence>> SStepMap = getSStepItems(itemPseudo);
		Map<Integer, List<PseudoSequence>> IStepMap = getIStepItems(itemPseudo);

		// I扩展
		for (Map.Entry<Integer, List<PseudoSequence>> istepItem : IStepMap.entrySet()) {
			int item = istepItem.getKey();
			// i扩展剪枝，如果扩展项支持度小于minsup,则不进行增长
			int support = istepItem.getValue().size();
			if (istepItem.getValue() == null || support < minsup)
				continue;
			// 更新标记
			int imt = imatch;
			int iimt = iimatch;
			if (imt < querySequence.size()) {
				if(iimt <querySequence.get(imt).size()){
					if (item == querySequence.get(imt).get(iimt)) {
					  iimt++;
					}
				}
			}

			Sequence ISequence = prefix.cloneSequence();
			List<PseudoSequence> tem = istepItem.getValue();
			ISequence.getItemsets().get(ISequence.itemsets.size() - 1).addItem(item);
			dfsPatternGrowth(imt, iimt, ISequence, tem, support);
		}
		// S扩展
		for (Map.Entry<Integer, List<PseudoSequence>> sstepItem : SStepMap.entrySet()) {
			int item = sstepItem.getKey();
			// S扩展剪枝
			int support = sstepItem.getValue().size();
			if (sstepItem.getValue() != null && support < minsup)
				continue;
			// 更新qs标记,进行s扩展，iimatch更新为0
			int imt = imatch;
			int iimt = iimatch;
			if(imt < querySequence.size() &&iimt >= querySequence.get(imt).size()){
				imt++;
			}
			iimt = 0;
			if (imt < querySequence.size()) {
				if (iimt < querySequence.get(imt).size()&&item == querySequence.get(imt).get(iimt)) {
					iimt++;

				}
			}
			Sequence SSequence = prefix.cloneSequence();
			SSequence.addItemset(new Itemset(item));
			List<PseudoSequence> tem = sstepItem.getValue();
			dfsPatternGrowth(imt, iimt, SSequence, tem, support);
		}
		MemoryLogger.getInstance().checkMemory();
	}

	/**
	 * 获取I扩展项
	 */
	private Map<Integer, List<PseudoSequence>> getIStepItems(List<PseudoSequence> itemPseudo) {
		MemoryLogger.getInstance().checkMemory();
		Map<Integer, List<PseudoSequence>> istep = new HashMap<Integer, List<PseudoSequence>>();
		for (PseudoSequence pseudoSequence : itemPseudo) {
			int sequenceID = pseudoSequence.getSequenceID();
			List<Integer> indexList = pseudoSequence.getIndex();
			Map<Integer, PseudoSequence> sequenceIndex = new HashMap<Integer, PseudoSequence>();
			int sequence[] = MemoryDB.get(sequenceID);
			for (Integer index : indexList) {

				for (int i = index + 1; i < sequence.length; i++) {
					int item = sequence[i];
					// I扩展只需遍历模式最后一个项集所在的位置
					if (item == -1 || item == -2)
						break;
					else if(mapSequenceIDs.get(item).size()<minsup)
						continue;
					else {
						if (sequenceIndex.get(item) == null) {
							sequenceIndex.put(item, new PseudoSequence(sequenceID));
						}
						PseudoSequence pseudo = sequenceIndex.get(item);
						pseudo.getIndex().add(i);
						sequenceIndex.put(item, pseudo);
					}
				}
			}
			if (sequenceIndex == null)
				continue;
			for (Map.Entry<Integer, PseudoSequence> tempIndex : sequenceIndex.entrySet()) {
				int item = tempIndex.getKey();
				if (istep.get(item) == null) {
					List<PseudoSequence> curIndex = new ArrayList<PseudoSequence>();
					curIndex.add(tempIndex.getValue());
					istep.put(item, curIndex);
				} else {
					List<PseudoSequence> curIndex = istep.get(item);
					curIndex.add(tempIndex.getValue());
					istep.put(item, curIndex);
				}
			}
		}
		MemoryLogger.getInstance().checkMemory();
		return istep;
	}

	/**
	 * 获取S扩展项
	 */
	private Map<Integer, List<PseudoSequence>> getSStepItems(List<PseudoSequence> itemPseudo) {
		MemoryLogger.getInstance().checkMemory();
		Map<Integer, List<PseudoSequence>> sstep = new HashMap<Integer, List<PseudoSequence>>();
		// 遍历所以投影序列
		for (PseudoSequence pseudoSequence : itemPseudo) {
			int sequenceID = pseudoSequence.getSequenceID();
			List<Integer> indexList = pseudoSequence.getIndex();
			int sequence[] = MemoryDB.get(sequenceID);
			Map<Integer, PseudoSequence> sequenceIndex = new HashMap<Integer, PseudoSequence>();
			// 遍历item在序列中的所有出现的位置
			for (Integer index : indexList) {
				// s扩展遍历模式的下一个项集
				int i = index;
				while (sequence[i] != -1) {
					i++;
				}
				i++;
				for (; i < sequence.length; i++) {
					int item = sequence[i];
				 if (item == -2 || item == -1)
						break;
				 else if(mapSequenceIDs.get(item).size()<minsup)
						continue;
					else {
						if (sequenceIndex.get(item) == null) {
							sequenceIndex.put(item, new PseudoSequence(sequenceID));
						}
						PseudoSequence pseudo = sequenceIndex.get(item);
						pseudo.getIndex().add(i);
						sequenceIndex.put(item, pseudo);
					}
				}
			}
			if (sequenceIndex == null)
				continue;
			for (Map.Entry<Integer, PseudoSequence> tempIndex : sequenceIndex.entrySet()) {
				int item = tempIndex.getKey();
				if (sstep.get(item) == null) {
					List<PseudoSequence> curIndex = new ArrayList<PseudoSequence>();
					curIndex.add(tempIndex.getValue());
					sstep.put(item, curIndex);
				} else {
					List<PseudoSequence> curIndex = sstep.get(item);
					curIndex.add(tempIndex.getValue());
					sstep.put(item, curIndex);
				}
			}
		}
		MemoryLogger.getInstance().checkMemory();
		return sstep;
	}
	private void sortIndex(){

		for (Map.Entry<Integer, List<PseudoSequence>> Index : itemIndexMap.entrySet()) {
			int item = Index.getKey();
			for (PseudoSequence pse : Index.getValue()) {
				Collections.sort(pse.getIndex());
			}

		}
	}
	/** Save a pattern of size > 1 to the output file. */
	private void savePattern(Sequence pattern, int sup, List<PseudoSequence> itemPseudo) throws IOException {

		patternCount++;
		StringBuilder r = new StringBuilder("");
		for (Itemset itemset : pattern.getItemsets()) {
			for (Integer item : itemset.getItems()) {
				String string = item.toString();
				r.append(string);
				r.append(' ');
			}
			r.append("-1 ");
		}
		r.append("-2 #SUP:" + sup + " #Sequence ID:");
		for (PseudoSequence pseudo : itemPseudo)
			r.append(pseudo.getSequenceID() + " ");
		//System.out.println(r.toString());
		writer.write(r.toString());
		writer.newLine();
	}

	/**
	 * Print the statistics of the algorithm execution to System.out.
	 */
	public void printStatistics() {
		StringBuilder r = new StringBuilder(200);
		r.append("=============== TCSPM V1 ===============\n Total time: ");
		r.append((endTime - startTime) / 1000.0);
		r.append("s\n");
		r.append(" query sequence:"+querySequence.toString()+"\n");
		r.append(" Target Contiguous sequences count: " + patternCount);
		r.append('\n');
		r.append(" Max memory (mb): ");
		r.append(MemoryLogger.getInstance().getMaxMemory());
		r.append(patternCount);
		r.append('\n');
		r.append(" minsup: " + minsup);
		r.append('\n');
		r.append(" count: " + count+"\n");
		r.append("========================================\n");
		System.out.println(r.toString());
	}
}
