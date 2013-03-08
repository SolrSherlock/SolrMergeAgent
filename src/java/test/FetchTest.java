/**
 * 
 */
package test;

import java.util.HashSet;
import java.util.Set;

import org.topicquests.common.ResultPojo;
import org.topicquests.common.api.IResult;
import org.topicquests.common.api.ITopicQuestsOntology;
import org.topicquests.solr.QueryUtil;
import org.topicquests.solr.SolrEnvironment;
import org.topicquests.solr.api.ISolrDataProvider;

/**
 * @author park
 *
 */
public class FetchTest {
	private SolrEnvironment environment;
	private ISolrDataProvider solr;

	/**
	 * 
	 */
	public FetchTest() {
		//create an environment without a desktop window
		environment = new SolrEnvironment();
		//grab the solr database
		solr = environment.getDataProvider();
		runTest();
	}

	void runTest() {
		Set<String>credentials = new HashSet<String>();
		credentials.add("admin");
		String query = ITopicQuestsOntology.LABEL_PROPERTY+":"+ QueryUtil.escapeQueryCulprits("Node Type");
		IResult x = solr.runQuery(query, 0, 10, credentials);
		System.out.println("DONE "+x.getErrorString()+" | "+x.getResultObject());

	}
}
