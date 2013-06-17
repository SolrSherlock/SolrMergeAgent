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

import org.apache.commons.collections.CollectionUtils;
import org.topicquests.agent.solr.AgentEnvironment;
import org.topicquests.common.ResultPojo;
import org.topicquests.common.api.IResult;
import org.topicquests.common.api.ITopicQuestsOntology;
import org.topicquests.model.api.INode;
import org.topicquests.model.api.ITuple;
import org.topicquests.model.api.ITupleQuery;
import org.topicquests.solr.SolrEnvironment;
import org.topicquests.solr.agents.merge.api.IPortfolioAgent;
import org.topicquests.solr.api.ISolrDataProvider;
import org.topicquests.solr.api.ISolrModel;

/**
 * @author park
 * <p>Serves as a base for any {@link IPortfolioAgent}
 * each of which is booted by Class.forName from configuration properties.</p>
 * <p>Each agent is designed for a particular test seeking an answer to the question
 * <em>have I seen this before</em>.</p>
 * <p>Some agents will have no interest in particular node types, so they take
 * themselves out of the agent queue</p>
 */
public abstract class BasePortfolioAgent implements IPortfolioAgent {
	public AgentEnvironment agentEnvironment;
	public TopicMergePortfolio host;
	public SolrEnvironment solrEnvironment;
	public ISolrDataProvider database;
	public ISolrModel solrModel;
	public INode theNode;
	public Set<String>credentials;
	public String myReason;
	public List<String>errorMessages;
	private ITupleQuery tupleQuery;
	
	/** 
	 * <p>all development work is in English; will be a while before others</p> 
	 * <p>Technically speaking, the INode api lets us look at <em>all</em>
	 * labels rather than language specific. Consider switching to that.</p>
	 */
	public final String language = "en";
//	public String myReason = "Same details";
	/**
	 * Outer key is node   //'s locator
	 * 	Value  key is name of object: numHits, vote
	 * 	
	 */
	public Map<INode, Map<String,Object>> hits;
	private int numVotes = 0;
	
	
	/* (non-Javadoc)
	 * @see org.topicquests.solr.agents.merge.api.IPortfolioAgent#init(org.topicquests.agent.solr.AgentEnvironment, org.topicquests.solr.agents.merge.TopicMergePortfolio)
	 */
	@Override
	public void init(AgentEnvironment environment, TopicMergePortfolio host) {
		this.agentEnvironment = environment;
		this.solrEnvironment = agentEnvironment.getSolrEnvironment();
		this.host = host;
		solrModel = solrEnvironment.getSolrModel();
		database = solrEnvironment.getDataProvider();
		tupleQuery = database.getTupleQuery();
		hits = new HashMap<INode, Map<String,Object>>();
		credentials = new HashSet<String>();
		credentials.add("admin");
		errorMessages = new ArrayList<String>();
		numVotes = 0;
	}

	/**
	 * Fire up a Worker thread which performs the merge test
	 */
	protected abstract void startWorker();
	
	/**
	 * Should never consider a MergeAssertion tuple for merging
	 * @param newTopic
	 * @return <code>true</code> IFF is tuple and is one of the merge assertion types
	 * / // now trapped upstream
	private boolean isMergeTuple(INode newTopic) {
		//DO NOT MERGE MERGE ASSERTION TUPLES!!!
		String ttype = newTopic.getNodeType();
		if (ttype != null) {
			if (ttype.equals(ITopicQuestsOntology.MERGE_ASSERTION_TYPE) ||
				ttype.equals(ITopicQuestsOntology.POSSIBLE_MERGE_ASSERTIONTYPE) ||
				ttype.equals(ITopicQuestsOntology.UNMERGE_ASSERTION_TYPE))
				return newTopic.isTuple();
		}
		return false;
	}
	*/
	
	@Override
	public boolean isAppropriateTopic(INode newTopic) {
		//the semantics of this is that newTopic was tested,
		//with this reason given as a default.
		//This reason should never show up in a trace because
		//the extending class will set it to an appropriate value
		myReason = "Bad topic "+ newTopic.getLocator();
	//	if (!isMergeTuple(newTopic)) {
			IResult x = hasVirtualProxy(newTopic);
			//if this is an already merged topic, ignore it;
			//in theory, there's a virtual proxy around for it
			if (x.getResultObject() != null)
				return false;
		//}
		return doWeCare(newTopic);
	}
	
	public List<String> getErrorStrings() {
		return errorMessages;
	}
	/**
	 * <p>As the agent itself if it wants to examine <code>newTopic</code></p>
	 * <p>If the test is going to return true, then:
	 * <li>REQUIRED: call	result.setResultObjectA(<some reason, e.g. "Same details">);</li>
	 * <li>OPTIONAL: any other worker setup necessary</li></p>
	 *
	 * @param newTopic
	 * @return
	 */
	protected abstract boolean doWeCare(INode newTopic);
	
	/* (non-Javadoc)
	 * @see org.topicquests.solr.agents.merge.api.IPortfolioAgent#evaluateTopic(org.topicquests.model.api.INode)
	 */
	@Override
	public void evaluateTopic(INode newTopic) {
		agentEnvironment.logDebug("BasePortfolioAgent.evaluateTopic "+newTopic.getLocator());
		theNode = newTopic;
		startWorker();
	}
	
	@Override
	public void evaluateTopic(String newTopicLocator) {
		IResult t = solrEnvironment.getDataProvider().getNode(newTopicLocator, credentials);
		if (t.hasError())
			errorMessages.add(t.getErrorString());
		if (t.getResultObject() != null)
			evaluateTopic((INode)t.getResultObject());
		else
			done();
	}


	/**
	 * Called when the agent has completed its mission
	 */
	public void done() {
		Map<String,Object>m;
		Iterator<INode>itr = this.hits.keySet().iterator();
		Integer counts;
		Double votes;
		INode node;
		while (itr.hasNext()) {
			node = itr.next();
			m = hits.get(node);
			votes = (Double)m.get("votes");
			counts = (Integer)m.get("numHits");
			double d = 0;
			if (votes > 0 && counts > 0) 
				d = votes.doubleValue()/(double)counts.intValue();
			host.assignVote(node, myReason, new Double(d));
		}
		
		host.acceptPortfolioAgent(this);
	}

	/**
	 * <p>Return <code>true</code> if <code>theHit</code> is the same as
	 * <code>theNode</code>; no sense in testing yourself! OR if the node
	 * is already a <em>VirtualProxy</em> to which we never merge.</p> WRONG
	 * <p>Also blocks merge tuples as if they are the <em>same node</em></p>
	 * @param theHit
	 * @return
	 */
	public boolean isSameNode(INode theHit) {
		boolean result = theHit.getLocator().equals(theNode.getLocator());
	//	result |= theHit.getIsVirtualProxy();
	//	if (!result)
	//		result = isMergeTuple(theHit);
		return result;
	}
	
	/**
	 * <p>Compare node type or subOf to see if <code>theHit</code>
	 * is the same as <code>theNode</code>. There is no sense in
	 * doing any other test if the nodes are not of the same type.</p>
	 * @param theHit
	 * @return <code>true</code> if the same type
	 */
	public boolean compareNodeTypes(INode theHit) {
		String aType = theNode.getNodeType();
		String bType = theHit.getNodeType();
		agentEnvironment.logDebug("BasePortfolioAgent.compareNodeTypes-1 "+aType+" "+bType);

		if (aType == null && bType == null) {
			//must test subclasses
			List<String>aSubs = theNode.listSuperclassIds();
			List<String>bSubs = theHit.listSuperclassIds();
			agentEnvironment.logDebug("BasePortfolioAgent.compareNodeTypes-2 "+aSubs+" "+bSubs);
			if (aSubs.isEmpty() && bSubs.isEmpty())
				//the case where these are untyped nodes; let the mergers fly!
				return true;
			else  {
				Collection<String>ctr = CollectionUtils.intersection(aSubs, bSubs);
				//require that the intersection of two same-sized collections
				// be the same size: they each contain the same values
				if (!ctr.isEmpty())
					return (ctr.size() == aSubs.size());
					
			}
		} else if (aType != null && bType != null && aType.equals(bType))
			return true;
		
		return false;
	}
	
	/**
	 * <p>Deal with this <code>hitNode</code></p>
	 * <p>Note: this deals with merge comparisons where language-based objects,
	 * e.g. labels or details, are in play</p>
	 * @param hitNode
	 * @param objects
	 * @param language
	 * @param baseField
	 */
	public void addToHits(INode hitNode, 
				List<String> objects, String language, String baseField) {
		Map<String,Object>o = hits.get(hitNode);
		
		if (o == null) {
			o = new HashMap<String,Object>();
			o.put("numHits", new Integer(0));
			o.put("votes", new Double(0));
			hits.put(hitNode, o);
		}
		Integer ix = (Integer)o.get("numHits");
		int iix = ix.intValue();
		iix++;
		o.put("numHits", new Integer(iix));
			List<String>labs = null;
			if (baseField.equals(ITopicQuestsOntology.LABEL_PROPERTY))
				labs = hitNode.listLabels(language);
			else if (baseField.equals(ITopicQuestsOntology.DETAILS_PROPERTY))
				labs = hitNode.listDetails(language);
			else {
				agentEnvironment.logError("BasePortfolioAgent.addToHits bad baseField "+baseField, null);
				return;
			}
			agentEnvironment.logDebug("BasePortfolioAgent.addToHits- "+labs);
			Collection<String> inters = CollectionUtils.intersection(objects, labs);
			int count = inters.size();
			//we cannot be here if count = 0
			double vote = (double)count / (double)objects.size();
			Double d = (Double)o.get("votes");
			double dd = d.doubleValue();
			dd += vote;
			o.put("votes", new Double(dd));
			agentEnvironment.logDebug("BasePortfolioAgent.addToHits-1 "+hits);
	}
	
	String makeField(String fieldBase, String language) {
		String result = fieldBase;
		if (!language.equals("en"))
			result += language;
		return result;
	}
	/**
	 * <p>Deal with this <code>hitNode</code></p>
	 * <p>Note: this deals with merge comparisons where the test has
	 * decided what the vote will be</p>
	 * @param hitNode
	 * @param confidence
	 */
	public void addToHits(INode hitNode, double vote) {
		Map<String,Object>o = hits.get(hitNode);
		
		if (o == null) {
			o = new HashMap<String,Object>();
			o.put("numHits", new Integer(0));
			o.put("votes", new Double(0));
			hits.put(hitNode, o);
		}
		Double d = (Double)o.get("votes");
		double dd = d.doubleValue();
		dd += vote;
		o.put("votes", new Double(dd));
		Integer ix = (Integer)o.get("numHits");
		int iix = ix.intValue();
		iix++;
		o.put("numHits", new Integer(iix));
		agentEnvironment.logDebug("BasePortfolioAgent.addToHits-2 "+hits);
	}
	/**
	 * <p>Return an {@link INode} if <code>sourceNode</code> is the <em>target</em></p>
	 * <p>This relies on the notion that there can be one and only one MergeAssertion per node</p>
	 * in a MergeAssertion
	 * @param sourceNode
	 * @return
	 */
	private IResult hasVirtualProxy(INode sourceNode) {
		IResult result = new ResultPojo();
		IResult tq = tupleQuery.listSubjectNodesByObjectAndRelation(sourceNode.getLocator(), ITopicQuestsOntology.MERGE_ASSERTION_TYPE, credentials);
		if (tq.hasError())
			result.addErrorString(tq.getErrorString());
		if (tq.getResultObject() != null) {
			List<INode>l = (List<INode>)tq.getResultObject();
			if (!l.isEmpty()) {
				ITuple t = (ITuple)l.get(0);
				tq = database.getNode(t.getSubjectLocator(), credentials);
				result.setResultObject((INode)tq.getResultObject());
				if (tq.hasError())
					result.addErrorString(tq.getErrorString());
			}
		}
		return result;
	}

}
