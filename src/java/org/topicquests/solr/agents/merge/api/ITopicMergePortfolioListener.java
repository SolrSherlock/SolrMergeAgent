/**
 * 
 */
package org.topicquests.solr.agents.merge.api;
import org.topicquests.solr.agents.merge.TopicMergePortfolio;
/**
 * @author park
 *
 */
public interface ITopicMergePortfolioListener {

	/**
	 * 
	 * @param p
	 */
	void acceptPortfolio(TopicMergePortfolio p);
}
