/**
 * 
 */
package org.topicquests.solr.agents.merge.agents;

import org.topicquests.model.api.INode;
import org.topicquests.solr.agents.merge.BasePortfolioAgent;
import org.topicquests.solr.agents.merge.api.IStructuredTreeMergeAgent;

/**
 * @author park
 *
 */
public class IBISProArgumentMergeAgent extends BasePortfolioAgent 
		implements IStructuredTreeMergeAgent {

	/* (non-Javadoc)
	 * @see org.topicquests.solr.agents.merge.BasePortfolioAgent#startWorker()
	 */
	@Override
	protected void startWorker() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.topicquests.solr.agents.merge.BasePortfolioAgent#doWeCare(org.topicquests.model.api.INode)
	 */
	@Override
	protected boolean doWeCare(INode newTopic) {
		// TODO Auto-generated method stub
		return false;
	}

}
