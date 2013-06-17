SolrMergeAgent
==============

A topic map merge engine in the SolrAgentFramework

Status: *pre-alpha*<br/>

Latest edit: 20130617
## Background ##
SolrMergeAgent is a complex system. Its task is easily describe as:

- Given a new Solr document (topic in the topic map), try to answer this question: *Have I seen this before?*

Answering that question becomes the task of a variety of tiny *single-purpose* agents. Each gets a *vote* on whether to merge or not.

Presently, there are three merge agents installed and configured in the config-properties.xml file:
- DetailsMergeAgent
- LabelMergeAgent
- WebResourcesMergeAgent

Many more to follow: all merge agents are installed by way of adding the classpath to a named agent in the config-properties.xml file.

This Merge platform is built into a copy of the SolrAgentFramework; it listens to the SolrAgentCoordinator (blackboard) for new documents received by Solr.

## Changes ##
Updated to use simple json library

## ToDo ##
mavenize

## License ##
Apache 2
