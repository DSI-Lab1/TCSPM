import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.BitSet;

public class TestTCSPM {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		// load a sequence database
		String input = fileToPath("syn60K.txt");
		String output = ".//output1.txt";

		// build a query pattern (842, 4616)(7752)>
		Itemset is = new Itemset();
		is.addItem(842);
		is.addItem(4616);
		Sequence Query = new Sequence();

		//Query.addItemset(new Itemset(196));

		Query.addItemset(is);
		Query.addItemset(new Itemset(7752));
		//System.out.println("Query sequene: " + Query);

		// create an instance of the algorithm

		//TCSPM
        //AlgoTCSPM11 algo1 = new AlgoTCSPM11();
        //algo1.runAlgorithm(input, output, 45, Query);
        //algo1.printStatistics();
                 
        //TCSPM+

//		AlgoTCSPM22 algo2 = new AlgoTCSPM22();
//		algo2.runAlgorithm(input, output,155, Query);
//		algo2.printStatistics();

	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException {
		URL url = TestTCSPM.class.getResource(filename);
		return java.net.URLDecoder.decode(url.getPath(), "UTF-8");
	}
}
