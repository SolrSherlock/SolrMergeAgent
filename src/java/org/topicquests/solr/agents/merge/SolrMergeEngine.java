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
//import net.sf.json.JSONObject;
//import net.sf.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import org.topicquests.agent.solr.AgentEnvironment;
import org.topicquests.common.api.ITopicQuestsOntology;
import org.topicquests.model.Node;
import org.topicquests.model.api.INode;
import org.topicquests.solr.agents.merge.api.ITopicMergePortfolioListener;
import org.topicquests.util.LoggingPlatform;
import org.topicquests.util.Tracer;

/**
 * @author park
 * <p>Workflow in a Merge activity, given a document which is the equivalent
 * of a topic map node:
 * <li>Ensure we don't already have a node with the same <code>locator</code> as this one</li>
 * <li>If same locator, must test for sameness</li>
 * <li>If same locator and same content, then ignore the document</li>
 * <li>Otherwise, <em>need handlers for that case</em>, the risk being that other nodes coming in will now be ambiguous</li>
 * <li>Search for likely documents for testing: <em>query formation is important here</em></li>
 * <li>For each document (source) paired with the given document (target), run tests</li>
 * <li>Tests depend on document type</li>
 * <li>Tests result in <em>votes</em> for a merge</li>
 * <li>When a merge is to be made, we use a <code>VirtualNode</code> as a proxy for both nodes to be merged</li>
 * <li>If the target is not already associated with a <code>VirtualNode</code>, create one</li>
 * <li>Associate source and target (if necessary) with the <code>VirtualNode</code></li>
 */
public class SolrMergeEngine implements ITopicMergePortfolioListener {
	private LoggingPlatform log;
	private Tracer tracer;
	private AgentEnvironment agentEnvironment;
	private List<String> nodesInMerge;
	private JSONParser parser;

	/**
	 * 
	 */
	public SolrMergeEngine(AgentEnvironment e) {
		agentEnvironment = e;
		log  = LoggingPlatform.getInstance();
		tracer = log.getTracer("SolrMergeEngine");
		nodesInMerge = new ArrayList<String>();
		parser = new JSONParser();
	}

	public List<String> listNodesInMerge() {
		synchronized(nodesInMerge) {
			return nodesInMerge;
		}
	}
	
	public void studyDocument(String jsonString) {
		System.out.println(jsonString);
		tracer.trace(0, jsonString);
		Map<String,Object>o = new HashMap<String,Object>();
		try {
			JSONObject jo = (JSONObject)parser.parse(jsonString);
			String locator = (String)jo.get(ITopicQuestsOntology.LOCATOR_PROPERTY);
			// are we busy looking at this puppy
			synchronized(nodesInMerge) {
				if (nodesInMerge.contains(locator))
					return;
			}
			//TODO dump the JSONObject to this map
			Iterator<String>itr = jo.keySet().iterator();
			Object vo;
			String key;
			
			while (itr.hasNext()) {
				key = itr.next();
				vo = jo.get(key);
				if (vo instanceof String)
					o.put(key, vo);
				else if (vo instanceof JSONArray) {
					JSONArray ja = (JSONArray)vo;
					Iterator<String>jitr = ja.iterator();
					List<String>vl = new ArrayList<String>();
					while(jitr.hasNext())
						vl.add(jitr.next());
					o.put(key, vl);
				}
			}
		} catch (Exception e) {
			agentEnvironment.logError(e.getMessage(), e);
			e.printStackTrace();
			return; //we cannot study this document
		}
		//should not try to merge tuples
		Object isTuple = o.get(ITopicQuestsOntology.TUPLE_SUBJECT_PROPERTY);
		//should not have an isVirtual property
		Object isVirtual = o.get(ITopicQuestsOntology.IS_VIRTUAL_PROXY);
		//should not have a mergeTuple property
		Object hasVirtual = o.get(ITopicQuestsOntology.MERGE_TUPLE_PROPERTY);
		//which nodeType?
		String typ = (String)o.get(ITopicQuestsOntology.INSTANCE_OF_PROPERTY_TYPE);
		boolean ismerge = false;
		if (typ != null)
			ismerge = (typ.equals(ITopicQuestsOntology.MERGE_ASSERTION_TYPE) ||
					   typ.equals(ITopicQuestsOntology.POSSIBLE_MERGE_ASSERTIONTYPE)||
					   typ.equals(ITopicQuestsOntology.UNMERGE_ASSERTION_TYPE));
		// we do not merge virtual proxies or merge assertion tuples
		if (isVirtual == null && hasVirtual == null && !ismerge && isTuple == null) {
			INode node = new Node(o);
			TopicMergePortfolio pf = new TopicMergePortfolio(agentEnvironment, this);
			pf.studyNode(node);
			
			//poof! It's gone. That's a threaded activity
			//Trust me: it will be back!
		}
	}

	/**
	 * It's back!
	 * @param p
	 */
	@Override
	public void acceptPortfolio(TopicMergePortfolio p) {
		INode n = p.getMyNode();
		
		//what to do with that puppy?
		if (n.getProperties() != null) {
			agentEnvironment.logDebug("XYZ "+n.toXML());
			tracer.trace(0, n.toJSON());
			agentEnvironment.logDebug("ACCEPTED "+n.toJSON());
		} else
			agentEnvironment.logError("SolrMergeEngine.acceptPortfolio bad node", null);
	}
}
