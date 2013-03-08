/**
 * 
 */
package test;

import org.topicquests.agent.solr.AgentEnvironment;
import org.topicquests.solr.agents.merge.SolrMergeAgent;
import org.topicquests.solr.agents.merge.SolrMergeThread;

/**
 * @author park
 *
 */
public class FirstMergeAgentTest {
	private AgentEnvironment agentEnvironment;
	private SolrMergeAgent mergeAgent;
	private SolrMergeThread mergeThread = null;
	private final String testDocument = 
			"{\"locator\":\"123456\",\"label\":\"hello world\"}";

	/**
	 * 
	 */
	public FirstMergeAgentTest() {
		agentEnvironment = new AgentEnvironment();
		mergeAgent = new SolrMergeAgent();
		mergeAgent.init(agentEnvironment, "TestAgent");
		mergeThread = mergeAgent.getMergeThread();
		mergeThread.addDocument(testDocument);
	}

}
