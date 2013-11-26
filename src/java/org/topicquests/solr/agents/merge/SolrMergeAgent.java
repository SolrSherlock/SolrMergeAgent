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

import org.semispace.Tuple;
import org.semispace.api.ISemiSpace;
import org.semispace.api.ISemiSpaceTuple;
import org.semispace.api.ITupleFields;
import org.semispace.api.ITupleTags;
import org.topicquests.agent.api.IPluggableAgent;
import org.topicquests.agent.solr.AgentEnvironment;
import org.topicquests.common.ResultPojo;
import org.topicquests.common.api.IResult;
import org.topicquests.solr.SolrEnvironment;
import org.topicquests.util.LoggingPlatform;
import org.topicquests.util.Tracer;

/**
 * @author park
 *
 */
public class SolrMergeAgent implements  IPluggableAgent {
	private SolrMergeThread mergeThread = null;
	private LoggingPlatform log;
	private Tracer tracer;
	private AgentEnvironment agentEnvironment;
	private SolrEnvironment solrEnvironment;
	private ISemiSpace blackboard;
	private Worker thread;
	/**
	 * <code>agentName</code> is used both in the internal blackboard,
	 * and the remote one
	 */
	private String agentName;

	@Override
	public IResult init(AgentEnvironment env, String agentName) {
		log  = LoggingPlatform.getLiveInstance();
		log.logDebug("SolrMergeAgent starting");
		this.agentName = agentName;
		agentEnvironment = env;
		solrEnvironment = env.getSolrEnvironment();
		//fetch the AgentFramework's blackboard
		//which is filled by the blackboard with, among other things, objects
		//sent over due to listening to SolrUpdateResponseHandler
		blackboard = agentEnvironment.getTupleSpaceEnvironment().getTupleSpace();
//		tracer = log.getTracer(agentName);
		mergeThread = new SolrMergeThread(agentEnvironment);
		IResult result = new ResultPojo();
		thread = new Worker();
		return result;
	}

	/**
	 * For testing only
	 * @return
	 */
	public SolrMergeThread getMergeThread() {
		return mergeThread;
	}
	
	class Worker extends Thread {
		private boolean isRunning = true;
		private Object synchObject = new Object();
		private ISemiSpaceTuple template;
		
		public void shutDown() {
			synchronized(synchObject) {
				isRunning = false;
				synchObject.notify();
			}
		}
		
		Worker() {
			//build a template for querying the blackboard
			//this template looks only for NewSolrDoc objects
			//which are sent over from Solr
			template = new Tuple(1, ITupleTags.NEW_SOLR_DOC);
			template.set(ITupleFields.AGENT_NAME, agentName);
			this.start();
		}
		
		public void run() {
			log.logDebug( "MockAgent.Worker started");
			ISemiSpaceTuple t=null;
			String cargo;
			while(isRunning) {
				t = blackboard.read(template, 1000); // leave up to a second
				if (t == null) {
					synchronized(synchObject) {
						try {
							synchObject.wait(1000);
						} catch (Exception e) {}
					}
				}
				if (isRunning && t != null) {
					log.logDebug("SolrMergeAgent GOT"+ t.getJSON());
					if (containsData(t)) {
						cargo = (String)t.get(ITupleFields.CARGO);
						System.out.println(cargo);
						//send it off to the merge jungle
						mergeThread.addDocument(cargo);
					}
					t = null;
				}
			}
		}
		/**
		 * {"id":1361127537959,"tag":"NewSolrDoc","cargo":"nodata"}
		 * is an example of a returned tuple with "no data"
		 * @param t
		 * @return
		 */
		boolean containsData(ISemiSpaceTuple t) {
			String cargo = (String)t.get(ITupleFields.CARGO);
			if (cargo.equals("nodata"))
				return false;
			return true;
		}

	}
	@Override
	public void shutDown() {
		mergeThread.halt();
		if (thread != null)
			thread.shutDown();
		if (tracer != null)
			tracer.shutDown();
		log.shutDown();	
	}


}
