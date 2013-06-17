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
import org.topicquests.solr.SolrEnvironment;
import org.topicquests.solr.api.ISolrDataProvider;

/**
 * @author park
 *
 */
public class WebResourceTest {
	private SolrEnvironment environment;
	private ISolrDataProvider solr;
	private INodeModel model;
	private final String myURL = "http://someserver.org/";

	/**
	 * 
	 */
	public WebResourceTest() {
		environment = new SolrEnvironment();
		//grab the solr database
		solr = environment.getDataProvider();
		model = solr.getNodeModel();
		new Worker();
	}

	class Worker extends Thread {
		private Object waitObject = new Object();
		
		public Worker() {
			this.start();
		}
		
		public void run() {
			Set<String>credentials = new HashSet<String>();
			credentials.add("admin");
			String myLabel1 = "My Website"; //"A key point was made that this is true" //"On the other hand, anything is possible"
			
			IResult r1 = model.newInstanceNode(ITopicQuestsOntology.WEB_RESOURCE_TYPE, myLabel1,"First of two in rapid order"  , "en", "admin", null, null, false);
			INode actor1 = (INode)r1.getResultObject();
			actor1.setURL(myURL);
			solr.putNode(actor1);
			// when you let two of the same nodes hit the database at precisely
			// (for all practical purposes) the same time, merge fails
			// So, we mess with a slight delay: 1 second
			// 1 second not enough
	/*		System.out.println(System.currentTimeMillis());
			synchronized(waitObject) {
				try {
					waitObject.wait(10000); // 10 seconds
				} catch (Exception e) {
					e.printStackTrace(); //"Another key point was made that that is false" //"One never knows about such things"
				}
			}
			System.out.println(System.currentTimeMillis()); */
			IResult r2 = model.newInstanceNode(ITopicQuestsOntology.WEB_RESOURCE_TYPE, myLabel1,"Second of two in rapid order" , "en", "admin", null, null, false);
			INode actor2 = (INode)r2.getResultObject();
			actor2.setURL(myURL);
			solr.putNode(actor2);
			if (r2.hasError())
				r1.addErrorString(r2.getErrorString());
			System.out.println("DONE-1 "+r2.getErrorString()+" "+actor1.getLocator()+" "+actor2.getLocator());
			
		}
	}
}
