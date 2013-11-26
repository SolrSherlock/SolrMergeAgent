/**
 * 
 */
package test;

import java.util.HashSet;
import java.util.Set;

import org.topicquests.common.api.IResult;
import org.topicquests.common.api.ITopicQuestsOntology;
import org.topicquests.model.api.INode;
import org.topicquests.model.api.INodeModel;
import org.topicquests.solr.QueryUtil;
import org.topicquests.solr.SolrEnvironment;
import org.topicquests.solr.api.ISolrDataProvider;

/**
 * @author park
 *
 */
public class SecondFetchTest {
	private SolrEnvironment environment;
	private ISolrDataProvider solr;
	private INodeModel model;

	/**
	 * 
	 */
	public SecondFetchTest() {
		environment = new SolrEnvironment();
		//grab the solr database
		solr = (ISolrDataProvider)environment.getDataProvider();
		model = solr.getNodeModel();
		runTest();
	}

	void runTest() {
		Set<String>credentials = new HashSet<String>();
		credentials.add("admin");
		String myLabel = "My Node!";
		String myDetails = "How's that for a node?";
		IResult r = model.newNode(myLabel, myDetails, "en", "admin", null, null, false);
		INode n = (INode)r.getResultObject();
		System.out.println("XXXX "+n);
		solr.putNode(n);
		String locator = n.getLocator();
		IResult x = solr.getNode(locator, credentials);
		n = (INode)x.getResultObject();
		System.out.println("DOING "+x.getErrorString()+" | "+n.toXML());
		
		String query = ITopicQuestsOntology.LABEL_PROPERTY+":"+ QueryUtil.escapeQueryCulprits(myLabel);
		x = solr.runQuery(query, 0, 10, credentials);
		System.out.println("DONE-1 "+x.getErrorString()+" | "+x.getResultObject());
		query = ITopicQuestsOntology.DETAILS_PROPERTY+":"+ QueryUtil.escapeQueryCulprits(myDetails);
		x = solr.runQuery(query, 0, 10, credentials);
		System.out.println("DONE-1 "+x.getErrorString()+" | "+x.getResultObject());

	}

}
