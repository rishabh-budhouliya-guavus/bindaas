package edu.emory.cci.bindaas.core.api;

import java.io.InputStream;
import java.util.Map;

import edu.emory.cci.bindaas.core.exception.ExecutionTaskException;
import edu.emory.cci.bindaas.framework.model.DeleteEndpoint;
import edu.emory.cci.bindaas.framework.model.Profile;
import edu.emory.cci.bindaas.framework.model.QueryEndpoint;
import edu.emory.cci.bindaas.framework.model.QueryResult;
import edu.emory.cci.bindaas.framework.model.SubmitEndpoint;

public interface IExecutionTasks {

 // execute a QueryEndpoint
	
	public QueryResult executeQueryEndpoint(String user , Map<String,String> runtimeParameters , Profile profile , QueryEndpoint queryEndpoint) throws ExecutionTaskException;

// execute Delete Endpoint
	
	public QueryResult executeDeleteEndpoint(String user ,Map<String,String> runtimeParameters , Profile profile ,DeleteEndpoint deleteEndpoint) throws ExecutionTaskException;
	
// execute Submit Endpoint
	
	public QueryResult executeSubmitEndpoint(String user ,InputStream is , Profile profile ,SubmitEndpoint submitEndpoint) throws ExecutionTaskException;
	public QueryResult executeSubmitEndpoint(String user ,String data , Profile profile ,SubmitEndpoint submitEndpoint) throws ExecutionTaskException;
	
}
