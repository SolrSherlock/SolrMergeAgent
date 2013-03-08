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
 * Create a Triple: two actors and a tuple; multiple passes will test merging
 */
public class TripleMergeTest {
	private SolrEnvironment environment;
	private ISolrDataProvider solr;
	private INodeModel model;

	/**
	 * 
	 */
	public TripleMergeTest() {
		environment = new SolrEnvironment();
		//grab the solr database
		solr = environment.getDataProvider();
		model = solr.getNodeModel();
		runTest();
	}
	
	void runTest() {
		Set<String>credentials = new HashSet<String>();
		credentials.add("admin");
		String myLabel1 = "Joe Smith";
		String myLabel2 = "Sara Smith";
		
		IResult r1 = model.newNode(myLabel1, null, "en", "admin", null, null, false);
		INode actor1 = (INode)r1.getResultObject();
	//	solr.putNode(actor1);
		IResult r2 = model.newNode(myLabel2, null, "en", "admin", null, null, false);
		INode actor2 = (INode)r2.getResultObject();
	//	solr.putNode(actor2);
		//NOTICE that we are passing in nodes which are stored, but which
		//do not, at this moment, have their version numbers
		IResult r3 = model.relateNewNodes(actor1, actor2, "MarriedToRelation", "admin", null, null, false, false);
		if (r1.hasError())
			r3.addErrorString(r1.getErrorString());
		if (r2.hasError())
			r3.addErrorString(r2.getErrorString());
		System.out.println("DONE-1 "+r3.getErrorString()+" | "+r3.getResultObject());

	}

}
