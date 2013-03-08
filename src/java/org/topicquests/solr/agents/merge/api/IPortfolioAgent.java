/**
 * 
 */
package org.topicquests.solr.agents.merge.api;

import java.util.List;

import org.topicquests.agent.solr.AgentEnvironment;
import org.topicquests.common.api.IResult;
import org.topicquests.model.api.INode;
import org.topicquests.solr.agents.merge.TopicMergePortfolio;

/**
 * @author park
 * <p>Instances of this interface are threaded, and hosted.
 * They are intended to return themselves to their host, where
 * the evaluations are summed and a merge decision is made.</p>
 */
public interface IPortfolioAgent {
	
	
	/**
	 * Initialize a booted agent
	 * @param environment
	 * @param host
	 */
	void init(AgentEnvironment environment, TopicMergePortfolio host);
	
	
	/**
	 * <p>Test <code>newTopic</code> to answer whether this agent
	 * is appropriate for evaluating the node for merge candidates.</p>
	 * <p>Will return <code>true</code> if this agent should perform
	 * merge evaluation on <code>newTopic</code>
	 * 
	 * @param newTopic
	 * @return
	 */
	boolean isAppropriateTopic(INode newTopic);
	
	/**
	 * <p>Start an evaluation of <code>newTopic</code></p>
	 * <p>NOTE: this method is not suitable to <em>concurrent evaluations 
	 * on the same instance of <code>newTopic</code></p>
	 * <p>Internally, this method runs an implemented test which is specific
	 * to the test. That is, some tests are conducted only on certain node types.
	 * If <code>newTopic</code> is inappropriate to the test, then the agent
	 * ignores it and takes itself out of the chain of agents.</p>
	 * @param newTopic
	 */
	void evaluateTopic(INode newTopic);
	
	/**
	 * <p>Start an evaluation of <code>newTopicLocator</code> which entails
	 * fetching the node.</p>
	 * @param topicLocator
	 */
	void evaluateTopic(String newTopicLocator);
	
	/**
	 * Return a list of Exception messages 
	 * @return
	 */
	List<String> getErrorStrings();
}
