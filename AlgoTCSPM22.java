import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;

public class AlgoTCSPM22 {

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
    int qsLength = 0;
    /** object to write to a file */
    BufferedWriter writer = null;
    int count = 0;
    /** MemoryDB： sequence that contains the query sequence */
    List<int[]> MemoryDB = new ArrayList<int[]>();

    /** AbsDB： sequence that contains the query sequence */
    List<Integer> AbsIndx = new ArrayList<Integer>();
    //用于存储对应qi的位置 sid qs对应位置
    Map<Integer,List<Integer>> pruningMap = new HashMap<>();
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

        // removeInfrequentItem();
        // record start time
        startTime = System.currentTimeMillis();
        // RUN THE ALGORITHM
        loadDB(input);
        createSequenceChain();
        sortIndex();
        //printPruningMap();
        //printDB();
        contiguousPatternGrowth();

        // record end time
        endTime = System.currentTimeMillis();
        // close the file
        writer.close();
    }

    public void printPruningMap() {
    	for(Entry<Integer,List<Integer>> entry : pruningMap.entrySet()) {
    		int id = entry.getKey();
    		ArrayList<Integer> list = (ArrayList<Integer>) entry.getValue();
    		System.out.println("id = "+id+"  value="+list.toString());
    	}
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
            int cnt = 0;
            // for each line (sequence) in the file until the end
            while ((thisLine = reader.readLine()) != null) {
                // if the line is a comment, is empty or is a kind of metadata
                if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#' || thisLine.charAt(0) == '%'
                        || thisLine.charAt(0) == '@') {
                    continue;
                }
                cnt++;
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
                int indexFlag = checkTargetSequence(transactionArray,sid);
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
                    pruningMap.remove(sid);
                }else{
                    AbsIndx.add(indexFlag);
                    MemoryDB.add(transactionArray);
                    Collections.sort(pruningMap.get(sid));
                   // System.out.println(pruningMap.get(sid).toString()+"  cnt"+ cnt);
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
                qsLength++;
                qsArray.add(itemset.get(i));
            }
            qsArray.add(-1);
        }
    }
    // 检测序列中是否含有Target sequence
    private int checkTargetSequence(int[] sequence,int sid) {
        int index = qsArray.size() - 2;
        // 反向遍历
        for (int i = sequence.length - 1; i >= 0;) {
            //System.out.println("i: "+tokens[i]+" "+qsArray.get(index));
            int item = sequence[i];
            if (item == qsArray.get(index)) {
                //System.out.println("相等"+i+" "+index);
                int temIndex = index;
                int cnt = 0;
                boolean match = false;
                while( i >= 0&&sequence[i] != -1) {

                    if (sequence[i] == qsArray.get(index)) {
                        if(pruningMap.get(sid) == null){
                            pruningMap.put(sid, new ArrayList<Integer>(qsLength));
                        }
                        List<Integer> indexList =pruningMap.get(sid);
                        indexList.add(i);
                        cnt++;
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
                if(match == false){
                    index = temIndex;
                    List<Integer> indexList =pruningMap.get(sid);
                    while(cnt>0){
                        cnt--;
                        if(indexList.size()!=0)
                        indexList.remove(indexList.size()-1);
                    }
                }


                while( i >= 0 &&sequence[i] != -1){
                    i--;
                }

            } else
                i--;
        }
        return -1;
    }

    private void createSequenceChain() {
        for (int i = 0; i < MemoryDB.size(); i++) {
            //临时变量，保存item在序列i中出现的位置
            Map<Integer, PseudoSequence> sequenceIndex = new HashMap<Integer, PseudoSequence>();
            int sequence[] = MemoryDB.get(i);
            // 从序列最后一个项开始匹配qs实例
            for (int j = sequence.length - 2; j >= 0;) {
                int item = sequence[j];
                if (item < 0)
                    j--;
                else if (mapSequenceIDs.get(item).size() < minsup) {
                    j--;
                } else {// 如果该项为频繁项， 匹配包含qs的频繁项序列
                    int index[] = devideSequence(j, sequence);
                    //System.out.println(i+"  返回结果：j的值"+j+" "+index[0]+"  "+index[1]);
                    //说明匹配失败，改项集都为非频繁项
                    j = index[1];
                    if(index[0] == -1){
                        j--;
                    }else{
                        while(j>=0){
                            if(sequence[j]==-1){
                                j--;
                                continue;
                            }
                            //频繁项
                            if(mapSequenceIDs.get(sequence[j]).size()>=minsup){
                                while(j>=0 && sequence[j]!=-1){
                                    int ans = sequence[j];
                                    if (mapSequenceIDs.get(ans) != null
                                            && mapSequenceIDs.get(ans).size() >= minsup) {
                                        if (sequenceIndex.get(ans) == null) {
                                            sequenceIndex.put(ans, new PseudoSequence(i));
                                        }
                                        PseudoSequence temp = sequenceIndex.get(ans);
                                        temp.getIndex().add(j);
                                    }
                                    j--;
                                }
                            }else{//非频繁项
                                boolean flag = false;
                                int kk = j;
                                while(kk>=0 && sequence[kk]!=-1){
                                    int ans = sequence[kk];
                                    if (mapSequenceIDs.get(ans) != null
                                            && mapSequenceIDs.get(ans).size() >= minsup) {
                                        flag= true;
                                        break;
                                    }
                                    kk--;
                                }
                                //System.out.println(j+" "+kk+"  "+ flag);
                                if(flag == true){
                                    while(j>=0 && sequence[j]!=-1){
                                        int ans = sequence[j];
                                        if (mapSequenceIDs.get(ans) != null
                                                && mapSequenceIDs.get(ans).size() >= minsup) {
                                            if (sequenceIndex.get(ans) == null) {
                                                sequenceIndex.put(ans, new PseudoSequence(i));
                                            }
                                            PseudoSequence temp = sequenceIndex.get(ans);
                                            temp.getIndex().add(j);
                                        }
                                        j--;
                                    }
                                }else{
                                    j = kk;
                                    break;
                                }
                            }
                        }
                    }

                }
            }
            for (Map.Entry<Integer, PseudoSequence> tempPseudo : sequenceIndex.entrySet()) {
                int item = tempPseudo.getKey();
                //System.out.println(item+" "+tempPseudo.getValue().getIndex().toString());
                if (itemIndexMap.get(item) == null) {
                    itemIndexMap.put(item, new ArrayList<PseudoSequence>());
                }
                List<PseudoSequence> pseudoList = itemIndexMap.get(item);
                pseudoList.add(tempPseudo.getValue());
                itemIndexMap.put(item, pseudoList);
            }
        }
    }

    //Suff(qi)剪枝
    private boolean checkPruning(List<PseudoSequence> itemPseudo, int imatch, int iimatch){
        int cnt = 0;
       for(int i = 0;i<imatch;i++){
           cnt = cnt+querySequence.get(i).size();
       }
       cnt += iimatch;
       if(querySequence.get(imatch).size()<=iimatch)
           cnt--;
        int sup = 0;
       // System.out.println(imatch+"  "+iimatch);
        for (PseudoSequence pseudoSequence : itemPseudo) {
            int sequenceID = pseudoSequence.getSequenceID();
           // System.out.println(pruningMap.get(sequenceID).size()+" size");
            int lastIndex = pruningMap.get(sequenceID).get(cnt);
            int itemIndex = pseudoSequence.getIndex().get(0);
            //System.out.println(pseudoSequence.getIndex().toString());
            //System.out.println("id:"+sequenceID+"  剪枝位置："+lastIndex+"   项所在位置："+itemIndex);
            if (itemIndex <= lastIndex)
                sup++;
        }
        if(sup>=minsup)
            return true;
        else
            return false;
    }
    //从j的位置开始 反向匹配qs
    private int[] devideSequence(int j, int sequence[]){
        int qsIndex = qsArray.size() - 2;
        int[] index = new int[3];
        index[0] = -1;
        boolean isMatch = false;
        //反向匹配
        for(int i = j;i >=0;){
            if(sequence[i]<0){
                i--;
                continue;
            }
            //该项为非频繁项，判断对应项集是否全为非频繁项
            if(mapSequenceIDs.get(sequence[i]).size() < minsup){
                boolean flag = false;
                for(int k = i; k>=0&&sequence[k]!=-1;k--){
                    if(mapSequenceIDs.get(sequence[k]).size() >= minsup){
                        flag =true;
                        break;
                    }
                }
                if(flag == false){
                    for(int k = i+1;sequence[k]!=-1;k++){
                        if(mapSequenceIDs.get(sequence[k]).size() >= minsup){
                            flag =true;
                            break;
                        }
                    }
                }
                //说明改项集的元素全为非频繁项的，该项集为一个分割符
                if(flag == false){
                    index[0] = -1;
                    index[1] = i;
                    return index;
                }
            }
            //说明i位置对应的项集 不可能全部都为非频繁的。
            if(sequence[i] == qsArray.get(qsIndex)){
                int temIndex = qsIndex;
                boolean match =  false;
                while(i>=0 && sequence[i]!=-1){
                    //System.out.println("item:"+sequence[i]+" "+qsArray.get(qsIndex));
                    if(sequence[i] == qsArray.get(qsIndex)){
                        if(qsIndex == 0){
                            index[0] = 1;
                            index[1] = i;
                            return index;
                        }
                        qsIndex --;
                    }
                    i--;

                    //说明匹配完
                    if(qsIndex < 0 || qsArray.get(qsIndex)==-1){
                        match = true;
                        break;
                    }
                }
                if(match == false){
                    qsIndex = temIndex;
                }else{
                    qsIndex --;
                }
                while(i>=0 && sequence[i]!=-1)
                    i--;
            }else{
                i--;
            }

        }
        index[0] = -1;
        index[1] = 0;
        return index;
    }


    private void printDB() {
        System.out.println("MemeryDB数据：");
        for (int i = 0; i < MemoryDB.size(); i++) {
            for (int j = 0; j < MemoryDB.get(i).length; j++) {
                System.out.print(MemoryDB.get(i)[j] + " ");
            }
            System.out.println();
        }
        for (Map.Entry<Integer, List<Integer>> tem : mapSequenceIDs.entrySet()) {
            System.out.println("item:" + tem.getKey() + "  " + tem.getValue());
        }

        for(Map.Entry<Integer,List<Integer>> tem : pruningMap.entrySet()){
            System.out.println(tem.getKey()+"  "+tem.getValue().toString());
        }
//		System.out.println("每个item在不同序列中出现的位置");
//		for (Map.Entry<Integer, List<PseudoSequence>> Index : itemIndexMap.entrySet()) {
//			int item = Index.getKey();
//			System.out.println("item位置: "+item);
//			for (PseudoSequence pse : Index.getValue()) {
//				System.out.println(pse.toString());
//			}
//
//		}

    }
    private void sortIndex(){

        for (Map.Entry<Integer, List<PseudoSequence>> Index : itemIndexMap.entrySet()) {
			int item = Index.getKey();
			for (PseudoSequence pse : Index.getValue()) {
                Collections.sort(pse.getIndex());
			}
		}

    }


    /** 遍历所有频繁项，递归进行成长 */
    private void contiguousPatternGrowth() throws IOException {
        // 遍历所有频繁项
        MemoryLogger.getInstance().checkMemory();

        for (Map.Entry<Integer, List<PseudoSequence>> itemMap : itemIndexMap.entrySet()) {
            int sup = itemMap.getValue().size();// 得到item的支持度
            int item = itemMap.getKey();
            int iimatch = 0;
            int imatch = 0;

            if(sup < minsup){
                continue;
            }

            // 更新标记
            if (querySequence.size() != 0&&item == querySequence.itemsets.get(imatch).get(iimatch)) {
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
        MemoryLogger.getInstance().checkMemory();
        if (imatch == querySequence.size()) {
            savePattern(prefix, sup, itemPseudo);
        }else {
            if(iimatch >= querySequence.get(imatch).size()){
                if(imatch +1 == querySequence.size() ){
                    savePattern(prefix, sup,itemPseudo);
                }
            }else{
                if(checkPruning(itemPseudo,imatch,iimatch)==false){
                   //System.out.println("剪枝： "+prefix.toString()+"  imatch: "+imatch+"  iimatch: "+iimatch);
                    return ;
                }

            }
        }
        count++;
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
                    else if (mapSequenceIDs.get(item).size()<minsup)
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
       // System.out.println(r.toString());
        writer.write(r.toString());
        writer.newLine();
    }

    /**
     * Print the statistics of the algorithm execution to System.out.
     */
    public void printStatistics() {
        StringBuilder r = new StringBuilder(200);
        r.append("=============== TCSPM V2 ===============\n Total time: ");
        r.append((endTime - startTime) / 1000.0);
        r.append("s\n");
        r.append(" query sequence:"+querySequence.toString()+"\n");
        r.append(" Target Contiguous sequences count: " + patternCount);
        r.append('\n');
        r.append(" Max memory (mb): ");
        r.append(MemoryLogger.getInstance().getMaxMemory());
        r.append(patternCount);
        r.append('\n');
        r.append(" minsup: " + minsup+"\n");
        r.append(" count: " + count);
        r.append('\n');
        r.append("========================================\n");
        System.out.println(r.toString());
    }
}
