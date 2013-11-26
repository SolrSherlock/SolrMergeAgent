/**
 * 
 */
package org.topicquests.solr.agents.merge.api;

/**
 * @author park
 * <p>A merge within a structured tree does must deal with these issues:
 * <li>When two siblings of a parent are merged, only that parent sees
 *  the new VirtualNode</li>
 * <li>Merges of a the siblings outside the tree go to a VirtualNode
 *  which exists outside the tree</li>
 * </p>
 */
public interface IStructuredTreeMergeAgent {

}
