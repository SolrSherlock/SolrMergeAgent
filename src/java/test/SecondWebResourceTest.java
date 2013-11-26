/**
 * 
 */
package test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.topicquests.agent.solr.AgentEnvironment;
import org.topicquests.common.api.IResult;
import org.topicquests.common.api.ITopicQuestsOntology;
import org.topicquests.model.api.INode;
import org.topicquests.model.api.INodeModel;
import org.topicquests.solr.SolrEnvironment;
import org.topicquests.solr.api.ISolrDataProvider;

import test.WebResourceTest.Worker;

/**
 * @author park
 *
 */
public class SecondWebResourceTest {
	private AgentEnvironment environment;
	private ISolrDataProvider solr;
	private INodeModel model;
	private final String myURL = "http://xxsomeserver.org/";
	private final String otherURL = "http://xxsomeotherserver.org/";

	/**
	 * 
	 */
	public SecondWebResourceTest() {
		environment = new AgentEnvironment();
		//grab the solr database
		solr = (ISolrDataProvider)environment.getSolrEnvironment().getDataProvider();
		model = solr.getNodeModel();
		new Worker();
	}
	class Worker /*extends Thread*/ {
		private Object waitObject = new Object();
		
		public Worker() {
			//this.start();
			run();
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
			System.out.println(System.currentTimeMillis());
			synchronized(waitObject) {
				try {
					waitObject.wait(2000); // 2 seconds
				} catch (Exception e) {
					e.printStackTrace(); //"Another key point was made that that is false" //"One never knows about such things"
				}
			}
			System.out.println(System.currentTimeMillis()); 
			IResult r2 = model.newInstanceNode(ITopicQuestsOntology.WEB_RESOURCE_TYPE, myLabel1,"Second of two in rapid order" , "en", "admin", null, null, false);
			INode actor2 = (INode)r2.getResultObject();
			actor2.setURL(myURL);
			solr.putNode(actor2);
			if (r2.hasError())
				r1.addErrorString(r2.getErrorString());
			IResult r3 = model.newInstanceNode(ITopicQuestsOntology.WEB_RESOURCE_TYPE, myLabel1,"First of two in rapid order"  , "en", "admin", null, null, false);
			INode actor3 = (INode)r1.getResultObject();
			actor3.setURL(otherURL);
			solr.putNode(actor3);
			// when you let two of the same nodes hit the database at precisely
			// (for all practical purposes) the same time, merge fails
			// So, we mess with a slight delay: 1 second
			// 1 second not enough
			System.out.println(System.currentTimeMillis());
			synchronized(waitObject) {
				try {
					waitObject.wait(4000); // 4 seconds
				} catch (Exception e) {
					e.printStackTrace(); //"Another key point was made that that is false" //"One never knows about such things"
				}
			}
			System.out.println(System.currentTimeMillis()); 
			IResult r4 = model.newInstanceNode(ITopicQuestsOntology.WEB_RESOURCE_TYPE, myLabel1,"Second of two in rapid order" , "en", "admin", null, null, false);
			INode actor4 = (INode)r2.getResultObject();
			actor4.setURL(otherURL);
			solr.putNode(actor4);
			if (r4.hasError())
				r3.addErrorString(r4.getErrorString());
			System.out.println("DONE-1 "+r2.getErrorString()+" "+actor1.getLocator()+" "+actor2.getLocator()+"\n"+
					                     r3.getErrorString()+" "+actor3.getLocator()+" "+actor4.getLocator());
			synchronized(waitObject) {
				try {
					waitObject.wait(10000); // 2 seconds
				} catch (Exception e) {
					e.printStackTrace(); //"Another key point was made that that is false" //"One never knows about such things"
				}
			}
			IResult x = solr.listTrimmedInstanceNodes(ITopicQuestsOntology.WEB_RESOURCE_TYPE, 0, 10, credentials);
			System.out.println("FINAL "+x.getErrorString()+" "+x.getResultObject());
			List<INode>l = (List<INode>)x.getResultObject();
			for (int i=0;i<l.size();i++)
				System.out.println(l.get(i).toXML());
		}
	}
}
