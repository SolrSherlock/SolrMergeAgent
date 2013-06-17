/*
 * Copyright 2013, TopicQuests
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.topicquests.solr.agents.merge.agents;

import java.util.*;

import org.apache.commons.collections.CollectionUtils;
import org.topicquests.agent.solr.AgentEnvironment;
import org.topicquests.common.ResultPojo;
import org.topicquests.common.api.IResult;
import org.topicquests.common.api.ITopicQuestsOntology;
import org.topicquests.model.api.INode;
import org.topicquests.solr.SolrEnvironment;
import org.topicquests.solr.agents.merge.BasePortfolioAgent;
import org.topicquests.solr.agents.merge.TopicMergePortfolio;
import org.topicquests.solr.agents.merge.api.IPortfolioAgent;
import org.topicquests.solr.api.ISolrModel;
import org.topicquests.solr.api.ISolrQueryIterator;

/**
 * @author park
 * <p>Label testing is a very <em>weak</em> heuristic, so we do not
 * use a particularly high vote value.
 * <p>This class exists in the context of a single {@link INode} and
 * does not have to be thread safe. It will die when done.</p>
 */
public class LabelMergeAgent extends BasePortfolioAgent {
	
	class Worker extends Thread {
		//HAH! What we want is, for each INode, how many of the same labels does
		// it have with this one. To do that, we must intersect the label lists
		// and grab the cardinality of that intersection as weight!
		// that should be easier than what this is trying to do
		Worker() {
			this.start();
		}
		
		public void run() {
			List<String> labels = theNode.listLabels(language);
			agentEnvironment.logDebug("LabelMergeAgent- "+labels);
			ISolrQueryIterator itr = null;
			Iterator<String>labelItr = labels.iterator();
			String testLabel;
			List<INode> theHits;
			IResult itrResult;
			Iterator<INode>nodeItr;
			INode theHit;
			while (labelItr.hasNext()) {
				testLabel = labelItr.next();
				agentEnvironment.logDebug("LabelMergeAgent-0 "+testLabel);
				if (!testLabel.equals("")) {
					itr = solrModel.listNodesByLabel(testLabel, language, 10, credentials);
					itrResult = itr.next();
					theHits = (List<INode>)itrResult.getResultObject();
					agentEnvironment.logDebug("LabelMergeAgent-1 "+theHits);
					while (theHits != null && !theHits.isEmpty()) {
						if (itrResult.hasError())
							errorMessages.add(itrResult.getErrorString());
						agentEnvironment.logDebug("LabelMergeAgent-2 "+itrResult.getErrorString()+" "+itrResult.getResultObject());
						theHits = (List<INode>)itrResult.getResultObject();
						if (itrResult.hasError())
							errorMessages.add(itrResult.getErrorString());
						if (theHits.size() > 0) {
							nodeItr = theHits.iterator();
							while (nodeItr.hasNext()) {
								theHit = nodeItr.next();
								if (!isSameNode(theHit)) {
									agentEnvironment.logDebug("LabelMergeAgent-3 "+theHit);
									if (compareNodeTypes(theHit))
										addToHits(theHit, labels,language, ITopicQuestsOntology.LABEL_PROPERTY);
								}
							}
						}
						itrResult = itr.next();
						theHits = (List<INode>)itrResult.getResultObject();
					}
				}
			}
			//then return to the host
			done();
		}
		
	}

	@Override
	protected void startWorker() {
		new Worker();
	}

	@Override
	protected boolean doWeCare(INode newTopic) {
		//TODO become more sophisticated here. We need to test;
		//there will be some nodes for which looking at labels is
		//not indicated, such as tuples
		myReason = "Same label";
		if (newTopic.isTuple())
			return false;
		return true;
	}

}
