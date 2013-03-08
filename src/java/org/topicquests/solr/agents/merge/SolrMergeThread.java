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

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.SolrCore;
import org.topicquests.agent.solr.AgentEnvironment;

/**
 * @author park
 *
 */
public class SolrMergeThread extends Thread {
	private AgentEnvironment agentEnvironment;
	private SolrMergeEngine mergeEngine;
	private List<String> documents;
	private boolean isRunning = true;
	/**
	 * 
	 */
	public SolrMergeThread(AgentEnvironment e) {
		documents = new ArrayList<String>();
		agentEnvironment = e;
		mergeEngine = new SolrMergeEngine(agentEnvironment);
		isRunning = true;
		this.start();
	}
	

	/**
	 * Add a <code>jsonString</code> to process
	 * @param jsonString
	 */
	public void addDocument(String jsonString) {
		synchronized(documents) {
			documents.add(jsonString);
			documents.notify();
		}
	}
	
	public void halt() {
		synchronized(documents) {
			isRunning = false;
			documents.notify();
		}
	}
	
	
	public void run() {
		String theDoc = null;
		while(isRunning) {
			synchronized(documents) {
				theDoc = null;
				if (documents.isEmpty()) {
					try {
						documents.wait();
					} catch (Exception e) {}
				} else if (!documents.isEmpty()){
					theDoc = documents.remove(0);
				}
			}
			if (isRunning) {
				if (theDoc != null) {
					mergeEngine.studyDocument(theDoc);
					theDoc = null;
				}
					
			}
		}
	}
}
