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
package org.topicquests.solr.agents.merge.api;

import org.topicquests.common.api.IResult;
import org.topicquests.model.api.INode;

/**
 * @author park
 * <p>Instances of this type are <em>threaded</em> and are designed to
 * return themselves to the calling host. That host can then get
 * the vote as an {@link Double} and any error messages accumulated during 
 * the exercise</p>
 */
public interface ITopicMergeTestAgent {

	/**
	 * <p>An {@link INode} <code>newTopic</code> will be compared to
	 * <code>oldTopic</code> found in the database. The agent returns
	 * a {@link Double} which gives <em>vote</em> for/against merging the two.</p>
	 * @param newTopic
	 * @param oldTopic
	 */
	void voteForMerge(INode newTopic, INode oldTopic);
	
	/**
	 * Called by the agent's host when it returns.
	 * @return
	 */
	IResult getVote();
	
	/**
	 * Return the agent's name; it is presumed passed in at creation
	 * @return
	 */
	String getName();
	
}
