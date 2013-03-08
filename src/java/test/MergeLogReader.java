/**
 * 
 */
package test;
import java.io.*;
import java.util.*;

import org.topicquests.util.TextFileHandler;
/**
 * @author park
 * <p>Read and organize the log around Debug lines in MergeBean</p>
 */
public class MergeLogReader {
	private TextFileHandler handler;
	private Map<String,Map<String,String>> result;
	private Map<Long,String> v1;
	private Map<Long,String> v1a;
	private Map<Long,String> v2;
	private Map<Long,String> v3;
	private Map<Long,String> v4;
	private Map<Long,String> assertmerge; // start of a document
	private Map<Long,String> mergebean1;
	private Map<Long,String> mergebean1a;
	private Map<Long,String> mergebean2; // end of a document
	private Map<Long,String> tupquery;
	
	/**
	 * 
	 */
	public MergeLogReader() {
		handler = new TextFileHandler();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
