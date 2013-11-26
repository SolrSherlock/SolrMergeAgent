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

import java.util.Iterator;
import java.util.List;

import org.topicquests.common.api.IResult;
import org.topicquests.common.api.ITopicQuestsOntology;
import org.topicquests.common.api.INodeTypes;
import org.topicquests.model.api.INode;
import org.topicquests.solr.agents.merge.BasePortfolioAgent;
import org.topicquests.solr.agents.merge.agents.DetailsMergeAgent.Worker;
import org.topicquests.solr.api.ISolrQueryIterator;

/**
 * @author park
 *
 */
public class WebResourceMergeAgent extends BasePortfolioAgent {
	String queryString="";
	String webURL = "";
	/* (non-Javadoc)
	 * @see org.topicquests.solr.agents.merge.BasePortfolioAgent#startWorker()
	 */
	@Override
	protected void startWorker() {
		new Worker();
	}

	/* (non-Javadoc)
	 * @see org.topicquests.solr.agents.merge.BasePortfolioAgent#doWeCare(org.topicquests.model.api.INode)
	 */
	@Override
	protected boolean doWeCare(INode newTopic) {
		webURL = (String)newTopic.getProperty(ITopicQuestsOntology.RESOURCE_URL_PROPERTY);
		myReason = "Same Web URL "+webURL;
			String typ = newTopic.getNodeType();
		String locator = newTopic.getLocator();
		agentEnvironment.logDebug("WebResourceMergeAgent.doWeCare- "+typ+" "+webURL+" "+locator);
		//quick test: we don't care if it doesn't have a URL
		//note: in earlier code (below) we decided that a WebResource of any type
		//without a URL is an error
		if (webURL == null)
			return false;
		//look at three different types
		//TODO do we really want to look at these?
		//Awefully constraining, no room for other types to look at
		//Perhaps config.xml types?
		boolean isIt = ITopicQuestsOntology.WEB_RESOURCE_TYPE.equals(typ);
		if (!isIt)
			isIt = INodeTypes.BLOG_TYPE.equals(typ);
		if (!isIt)
			isIt = INodeTypes.BOOKMARK_TYPE.equals(typ);
		if (!isIt)
			return false;
//		Object o = newTopic.getProperty(ITopicQuestsOntology.RESOURCE_URL_PROPERTY);
//		if (o == null) {//sanity check -- would be nice to alert user that a bad node came in
//			solrEnvironment.logError("WebResourceMergeAgent.doWeCare missing URL for "+newTopic.getLocator(), null);
//			return false;
//		}
		//TODO revise this query to allow for using the webURL of the node.
		//DID
		//THIS query narrows the field, and pre-screens for a particular URL, since
		//that is what we are merging on
		//TODO add to exclude this locator
		queryString = "resourceURL:\""+webURL+"\" AND "+
			//	"(("+ITopicQuestsOntology.IS_VIRTUAL_PROXY+":true AND "+ITopicQuestsOntology.INSTANCE_OF_PROPERTY_TYPE+":"+ITopicQuestsOntology.WEB_RESOURCE_TYPE+") OR " +
			//	"("+ITopicQuestsOntology.IS_VIRTUAL_PROXY+":true AND "+ITopicQuestsOntology.INSTANCE_OF_PROPERTY_TYPE+":"+INodeTypes.BLOG_TYPE+") OR " +
			//	"("+ITopicQuestsOntology.IS_VIRTUAL_PROXY+":true AND "+ITopicQuestsOntology.INSTANCE_OF_PROPERTY_TYPE+":"+INodeTypes.BOOKMARK_TYPE+") OR " +
				 "("+ITopicQuestsOntology.INSTANCE_OF_PROPERTY_TYPE+":"+INodeTypes.BOOKMARK_TYPE+" OR "+
				 ITopicQuestsOntology.INSTANCE_OF_PROPERTY_TYPE+":"+INodeTypes.BLOG_TYPE+" OR "+
                 ITopicQuestsOntology.INSTANCE_OF_PROPERTY_TYPE+":"+ITopicQuestsOntology.WEB_RESOURCE_TYPE+")";
		return true;
	}

	
	class Worker extends Thread {
		//HAH! What we want is, for each INode, how many of the same labels does
		// it have with this one. To do that, we must intersect the label lists
		// and grab the cardinality of that intersection as weight!
		// that should be easier than what this is trying to do
		Worker() {
			this.start();
		}
		
		/**
		 * NOTE: this algorithm pulls in all kinds of web resources;
		 * it could, instead, pull in all nodes that use the <code>webURL</code>
		 * found in this node, which would greatly simplify the code.
		 */
		public void run() {
			agentEnvironment.logDebug("WebResourceMergeAgent- "+webURL);
			ISolrQueryIterator itr = solrEnvironment.getQueryIterator();
			List<INode> theHits;
			IResult itrResult;
			Iterator<INode>nodeItr;
			INode theHit;
			itr.start(queryString, 10, credentials);
			itrResult = itr.next();
			if (itrResult.hasError())
				errorMessages.add(itrResult.getErrorString());
			theHits = (List<INode>)itrResult.getResultObject();
			agentEnvironment.logDebug("WebResourceMergeAgent-1 "+theHits);
			String hitURL;
			while (theHits != null && !theHits.isEmpty()) {
				if (itrResult.hasError())
					errorMessages.add(itrResult.getErrorString());
				agentEnvironment.logDebug("WebResourceMergeAgent-2 "+itrResult.getErrorString()+" "+itrResult.getResultObject());
				nodeItr = theHits.iterator();
				while (nodeItr.hasNext()) {
					theHit = nodeItr.next();
					hitURL = (String)theHit.getProperty(ITopicQuestsOntology.RESOURCE_URL_PROPERTY);
					if (hitURL != null && hitURL.equals(webURL)) {
						addToHits(theHit, 1.0);
					}
				}
				itrResult = itr.next();
				if (itrResult.hasError())
					errorMessages.add(itrResult.getErrorString());
				theHits = (List<INode>)itrResult.getResultObject();
			}
			//then return to the host
			done();
		}
	}
}
