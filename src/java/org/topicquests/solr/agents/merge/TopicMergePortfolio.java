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
package org.topicquests.solr.agents.merge;

import java.util.*;

import org.topicquests.agent.solr.AgentEnvironment;
import org.topicquests.common.api.IResult;
import org.topicquests.common.api.ITopicQuestsOntology;
//import org.topicquests.model.api.IMergeImplementation;
import org.topicquests.model.api.INode;
import org.topicquests.model.api.INodeModel;
import org.topicquests.solr.agents.merge.agents.DetailsMergeAgent;
import org.topicquests.solr.agents.merge.agents.LabelMergeAgent;
import org.topicquests.solr.agents.merge.api.IPortfolioAgent;
import org.topicquests.solr.agents.merge.api.ITopicMergePortfolioListener;
//import org.topicquests.solr.agents.merge.api.ITopicMergeTestAgent;
import org.topicquests.solr.api.ISolrDataProvider;

/**
 * @author park
 *
 */
public class TopicMergePortfolio {
	private TopicMergePortfolio instance;
	private AgentEnvironment agentEnvironment;
	private ITopicMergePortfolioListener listener;
	private ISolrDataProvider database;
	private INodeModel nodeModel;
	private INode myNode;
	private List<IPortfolioAgent>agents;
	private List<Double>votes;
	private List<String>reasons;
	private List<String>errorMessages;
	private Set<String>credentials;

	private String mergeAgentLocator;
	///////////////////////////////////////////////////////
	// What Merging Needs:
	//   for each mergable node
	//	 	What was the merge reason
	//   	How much was the vote associated with that reason
	//   This gives rise to a common Map structure here:
	private Map<String,Map<String,Double>> nodeMergeMap;
	//    primary key: nodeLocator
	//    primary value: a Map
	//        secondary key: reason
	//        secondary value: vote
	/////////////////////////////////////////////////////////

	/**
	 * 
	 */
	public TopicMergePortfolio(AgentEnvironment e, ITopicMergePortfolioListener l) {
		agentEnvironment = e;
		database = agentEnvironment.getSolrEnvironment().getDataProvider();
		nodeModel = database.getNodeModel();
		listener = l;
		agents = new ArrayList<IPortfolioAgent>();
		votes = new ArrayList<Double>();
		reasons = new ArrayList<String>();
		credentials = new HashSet<String>();
		credentials.add("admin");
		mergeAgentLocator = ITopicQuestsOntology.MERGE_AGENT_TYPE;
		nodeMergeMap = new HashMap<String,Map<String,Double>>();
		instance = this;
		try {
			IPortfolioAgent merger;
			List<List<String>>agl = (List<List<String>>)agentEnvironment.getProperties().get("PortfolioAgents");
			if (agl == null)
				throw new RuntimeException("Missing PortfolioAgents property");
			Iterator<List<String>>itr = agl.iterator();
			String cp = null;
			Class o = null; 
			String name;
			List<String>hit;
			while (itr.hasNext()) {
				hit = itr.next();
				name = hit.get(0);
				cp = hit.get(1);
				o = Class.forName(cp);
				merger = (IPortfolioAgent)o.newInstance();
				merger.init(agentEnvironment,this);
				agents.add(merger);
			}
		} catch (Exception x) {
			agentEnvironment.logError(x.getMessage(), x);
			x.printStackTrace();
			throw new RuntimeException(x);
		}

	}

	/**
	 * Callback from {@link IPortfolioAgent} instances
	 * @param nodeLocator
	 * @param reason
	 * @param vote
	 */
	public void assignVote(String nodeLocator, String reason, Double vote) {
		synchronized(nodeMergeMap) {
			Map<String,Double>m = nodeMergeMap.get(nodeLocator);
			if (m == null) {
				m = new HashMap<String,Double>();
				nodeMergeMap.put(nodeLocator, m);
			}
			m.put(reason, vote);
		}
	}
	/**
	 * Starts the process
	 * @param newTopic
	 */
	public void studyNode(INode newTopic) {
		myNode = newTopic;
		agentEnvironment.logDebug("TopicMergePortfolio.studyNode "+newTopic.getLocator());
		new MergeWorker();
	}
	
	public List<Double> getVotes() {
		return votes;
	}
	
	public List<String> getReasons() {
		return reasons;
	}
	
	public List<String> getErrorMessages() {
		return errorMessages;
	}
	
	public INode getMyNode() {
		return myNode;
	}
	
	void workerDone() {
		listener.acceptPortfolio(this);
	}
	
	/**
	 * Callback from {@link IPortfolioAgent} when it has a vote.
	 * @param agent
	 */
	public void acceptPortfolioAgent(IPortfolioAgent agent) {
		agentEnvironment.logDebug("AgentPortfolio.acceptPortfolio "+agent+" "+agents);
		List<String>errorMessages = agent.getErrorStrings();
		agentEnvironment.logDebug("AgentPortfolio.acceptPortfolio-1 "+errorMessages+" | "+this.nodeMergeMap);
		boolean isEmpty = false;
		synchronized(agents) {
			System.out.println("TopicMergePortfolio.acceptPortfolioAgent");
			agents.remove(agent);
			isEmpty = agents.isEmpty();
		}
		if (isEmpty)
			dealWithMerge();

	}
	////////////////////////////////////////////////////////////////
	// The BigPicture(tm)
	// We are given a node, "myNode", and asked to find all the nodes in the topic map
	//   with which it can merge, the reasons given, and votes.
	//   reasons go with merge tests; some merge tests, e.g. label tests, might have
	//     several hits on the same test, e.g. several labels which are the same
	//   reasons, votes, and nodes are a triple
	//   { reasons, vote, node }
	//   which must be federated along the lines of reason-node pairs
	//   var totalVotes
	//   for each merge candidate node
	//     for each reason given
	//        totalVotes += votes for the reason
	//     submit those results {all reasons, total vote, node} to merge decision process
	//
	//	When a merge happens, the merge tuple (topic which connects merged nodes)
	//    is given the total vote, and list of votes.
	//    Might be interesting to give the {vote, reason} pair
	/////////////////////////////////////////////////////////////////
	/**
	 * <p>Here, we must deal with potentially many nodes which are
	 * candidates for merge with <code>myNode</code></p>
	 */
	void dealWithMerge() {
		agentEnvironment.logDebug("TopicMergePortfolio.dealWithMerge- "+nodeMergeMap);
		//keep this node in the log for studying the results
		agentEnvironment.logDebug("MY NODE: "+myNode.toXML());
		String nodeLocator;
		Map<String,Double> hitDetails;
		Iterator<String>hititr = nodeMergeMap.keySet().iterator();
		while (hititr.hasNext()) {
			nodeLocator = hititr.next();
			hitDetails = nodeMergeMap.get(nodeLocator);
			performMerge(nodeLocator,hitDetails);
		}
	}
	
	/**
	 * This might consider a merge or a possible merge
	 * @param targetNodeLocator for an existing node to merge <em>myNode</em> with.
	 * @param details
	 */
	void performMerge(String targetNodeLocator, Map<String,Double>details) {
		agentEnvironment.logDebug("TopicMergePortfolio.performMerge- "+targetNodeLocator+" "+details);
		boolean isOkToMerge = true;
		//TODO calculate isOkToMerge
		if (isOkToMerge) {
			IResult x = nodeModel.assertMerge(myNode, targetNodeLocator, details, 0, ITopicQuestsOntology.MERGE_AGENT_TYPE);
			agentEnvironment.logDebug("TopicMergePortfolio.performMerge+ "+x.getErrorString());			
		}
	}
	
	class MergeWorker extends Thread {
		MergeWorker() {
			this.start();
		}
		
		/**
		 * Here is where the merge action happens!
		 */
		public void run() {
			agentEnvironment.logDebug("TopicMergePortfolio starting: "+myNode.getLocator());
			//Do a bunch of stuff
			buildAgenda();
			//go do it
			fireTheAgenda();
			//then die
			agentEnvironment.logDebug("TopicMergePortfolio resting: "+myNode.getLocator());
		}
				
		void fireTheAgenda() {
			agentEnvironment.logDebug("TopicMergePortfolio.fireTheAgenda "+agents);
			String locator = myNode.getLocator();
			synchronized(agents) {
				Iterator<IPortfolioAgent>itr = agents.iterator();
				//There is a concurrency issue going on here in relations to
				// nodes being processed while this is running.
				//we have many agents playing with myNode
				//some of them are opening it up to look at the guts,
				// building iterators, etc.
				// the solution to that was to trade on locators, not on nodes
				///////////////////////////////////
				//there is a local ConcurrentModificationException going on
				// at "a = itr.next()"
				// so all visible references to the agents list are synchronized.
				// that's not stopping the exception
				IPortfolioAgent a;
				while (itr.hasNext()) {
					System.out.println("TopicMergePortfolio.firing");
					a = itr.next();
					if (a != null) // strange null values
						a.evaluateTopic(locator);
				}
			}
				
		}
		
		/**
		 * Figure out what to do on <code>myNode</code>
		 */
		void buildAgenda() {
			List<IPortfolioAgent>toRemove = new ArrayList<IPortfolioAgent>();
			IPortfolioAgent labels=null;
			IPortfolioAgent details=null;
			synchronized(agents) {
				Iterator<IPortfolioAgent>itr = agents.iterator();
				IPortfolioAgent x;
				//prune agents list
				while (itr.hasNext()) {
					x = itr.next();
					if (x instanceof DetailsMergeAgent) {
						if (!x.isAppropriateTopic(myNode))
							toRemove.add(x);
						else
							details = x;
					} else if (x instanceof LabelMergeAgent) {
						if (!x.isAppropriateTopic(myNode))
							toRemove.add(x);
						else
							labels = x;
					} if (!x.isAppropriateTopic(myNode))
						toRemove.add(x); //this agent doesn't want to look at myNode
				}
				if (!toRemove.isEmpty()) {
					itr = toRemove.iterator();
					while (itr.hasNext())
						agents.remove(itr.next());
				}
			
				//if agents is empty, add back labels and details as defaults
				if (agents.isEmpty()) {
					if (agents != null)
						agents.add(labels);
					if (details != null)
						agents.add(details);
				}
			}
				
		}
	}
}
