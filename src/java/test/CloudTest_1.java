/**
 * 
 */
package test;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;

/**
 * @author park
 *
 */
public class CloudTest_1 {
//No live SolrServers available to handle this request:
//	[http://127.0.1.1:8983/solr/collection1, http://127.0.1.1:7574/solr/collection1, http://127.0.1.1:7590/solr/collection1]
		
	/**
	 * 
	 */
	public CloudTest_1() {
		final String zkurl = "10.1.10.178:2181";
		final String solrurla = "10.1.10.178:8983";
		final String solrurlb = "10.1.10.178:7574";
		final String solrurlc = "10.1.10.178:7590";
     	boolean shouldCommit = true;
    	try {
    		LBHttpSolrServer sv = new LBHttpSolrServer(solrurla,solrurlb,solrurlc);
    		sv.getHttpClient().getParams().setParameter("update.chain",  "merge");
	        CloudSolrServer server = new CloudSolrServer(zkurl,sv);
	    	server.setDefaultCollection("collection1");
	    //	server.setZkClientTimeout(50000);
	    	int i=0;
	    	while (i < 3) {
	    		SolrInputDocument doc = new SolrInputDocument();
	    	    doc.addField( "locator", "mynewdoc"+i);
	    	    doc.addField( "label", "document " + i);
	    	    doc.addField( "details", "This is document " + i);
	    	    server.add(doc);
	    	    System.out.println("Added document " + i);
	    	    if (shouldCommit) {
	    	    	server.commit();
	    	    	System.out.println("Committed change.");
	    	    }
	       		SolrQuery query = new SolrQuery();
	    		query.setQuery("*:*");
	    		QueryResponse response = server.query(query);
	    		long num = response.getResults().getNumFound();
	    	    System.out.println("Found " + num + " documents in the index");
	    	    Thread.currentThread().sleep(1000);
	    	    i++;
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
	}

}
