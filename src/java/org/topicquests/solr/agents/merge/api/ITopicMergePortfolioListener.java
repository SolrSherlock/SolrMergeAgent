/**
 * 
 */
package org.topicquests.solr.agents.merge.api;
import java.util.List;

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
	
	List<String> listNodesInMerge();
}
