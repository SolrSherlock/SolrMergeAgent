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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.topicquests.common.api.IResult;
import org.topicquests.common.api.ITopicQuestsOntology;
import org.topicquests.model.api.INode;
import org.topicquests.solr.api.ISolrQueryIterator;
import org.topicquests.solr.agents.merge.BasePortfolioAgent;

/**
 * @author park
 * <p>Details testing is a <em>weak</em> heuristic
 * <p>This class exists in the context of a single {@link INode} and
 * does not have to be thread safe. It will die when done.</p>
 */
public class DetailsMergeAgent extends BasePortfolioAgent {

	
	class Worker extends Thread {
		//HAH! What we want is, for each INode, how many of the same labels does
		// it have with this one. To do that, we must intersect the label lists
		// and grab the cardinality of that intersection as weight!
		// that should be easier than what this is trying to do
		Worker() {
			this.start();
		}
		
		public void run() {
			List<String> details = theNode.listDetails(language);
			//NOTE: this can return an empty list
			// which means we need to look at other details as well
			agentEnvironment.logDebug("DetailsMergeAgent- "+details);
			ISolrQueryIterator itr = null;
			Iterator<String>labelItr = details.iterator();
			String testDetails;
			List<INode> theHits;
			IResult itrResult;
			Iterator<INode>nodeItr;
			INode theHit;
			while (labelItr.hasNext()) {
				testDetails = labelItr.next();
				agentEnvironment.logDebug("DetailsMergeAgent-0 "+testDetails);
				if (!testDetails.equals("")) {
					itr = solrModel.listNodesByDetails(testDetails, language, 10, credentials);
					itrResult = itr.next();
					if (itrResult.hasError())
						errorMessages.add(itrResult.getErrorString());
					theHits = (List<INode>)itrResult.getResultObject();
					agentEnvironment.logDebug("DetailsMergeAgent-1 "+theHits);
					while (theHits != null && !theHits.isEmpty()) {
						if (itrResult.hasError())
							errorMessages.add(itrResult.getErrorString());
						agentEnvironment.logDebug("DetailsMergeAgent-2 "+itrResult.getErrorString()+" "+itrResult.getResultObject());
						if (theHits.size() > 0) {
							nodeItr = theHits.iterator();
							while (nodeItr.hasNext()) {
								theHit = nodeItr.next();
								if (!isSameNode(theHit)) {
									if (compareNodeTypes(theHit))
										addToHits(theHit, details,language, ITopicQuestsOntology.DETAILS_PROPERTY);
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
		//there will be some nodes for which looking at details is
		//not indicated such as tuples
		myReason = "Same details";
		List<String>dets = newTopic.listDetails();
		if (dets == null || dets.size() == 0)
			return false;
		if (newTopic.isTuple())
			return false;
		return true;
	}
}
