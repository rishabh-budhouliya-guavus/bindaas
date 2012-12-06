package edu.emory.cci.bindaas.datasource.provider.mongodb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

import edu.emory.cci.bindaas.datasource.provider.mongodb.model.DataSourceConfiguration;
import edu.emory.cci.bindaas.datasource.provider.mongodb.model.OutputFormat;
import edu.emory.cci.bindaas.datasource.provider.mongodb.model.OutputFormatProps;
import edu.emory.cci.bindaas.datasource.provider.mongodb.operation.FindOperationHandler.FindOperationDescriptor;
import edu.emory.cci.bindaas.datasource.provider.mongodb.operation.IOperationHandler;
import edu.emory.cci.bindaas.datasource.provider.mongodb.operation.MongoDBOperation;
import edu.emory.cci.bindaas.datasource.provider.mongodb.operation.MongoDBOperationDescriptor;
import edu.emory.cci.bindaas.datasource.provider.mongodb.outputformat.IFormatHandler;
import edu.emory.cci.bindaas.datasource.provider.mongodb.outputformat.OutputFormatRegistry;
import edu.emory.cci.bindaas.framework.api.IQueryHandler;
import edu.emory.cci.bindaas.framework.model.ProviderException;
import edu.emory.cci.bindaas.framework.model.QueryEndpoint;
import edu.emory.cci.bindaas.framework.model.QueryResult;
import edu.emory.cci.bindaas.framework.util.GSONUtil;

public class MongoDBQueryHandler implements IQueryHandler {

	private Log log = LogFactory.getLog(getClass());
	private OutputFormatRegistry registry;
	private JsonParser parser = new JsonParser();
	public OutputFormatRegistry getRegistry() {
		return registry;
	}

	public void setRegistry(OutputFormatRegistry registry) {
		this.registry = registry;
	}

	@Override
	public QueryResult query(JsonObject dataSource,
			JsonObject outputFormatProps, String queryToExecute)
			throws ProviderException {
		
		try{
			if(outputFormatProps!=null)
			{
				OutputFormatProps props = GSONUtil.getGSONInstance().fromJson(outputFormatProps, OutputFormatProps.class);
				if(props!=null)
				{
					MongoDBOperationDescriptor operationDescriptor = null;
					try{
						 operationDescriptor = GSONUtil.getGSONInstance().fromJson(queryToExecute,MongoDBOperationDescriptor.class);
						 if(operationDescriptor == null || operationDescriptor.get_operation() == null || operationDescriptor.get_operation_args()==null)
						 {
							 throw new Exception("Not a valid query object"); // the query is not annotated properly
						 }
					}
					catch(Exception e)
					{
						log.warn(e);
						// default to find query
						operationDescriptor = new MongoDBOperationDescriptor();
						operationDescriptor.set_operation(MongoDBOperation.find);
						FindOperationDescriptor findArguments = new FindOperationDescriptor();
						findArguments.setQuery(parser.parse(queryToExecute).getAsJsonObject());
						operationDescriptor.set_operation_args(GSONUtil.getGSONInstance().toJsonTree(findArguments).getAsJsonObject());
					}
					
					// get DB collection
					DataSourceConfiguration configuration = GSONUtil.getGSONInstance().fromJson(dataSource, DataSourceConfiguration.class);
					Mongo mongo = null;
					try {
						mongo = new Mongo(configuration.getHost(),configuration.getPort());
						DB db = mongo.getDB(configuration.getDb());
						DBCollection collection = db.getCollection(configuration.getCollection());

						// use operationDescriptor to route to correct handler
						
						IOperationHandler operationHandler = operationDescriptor.get_operation().getHandler();
						QueryResult result = operationHandler.handleOperation(collection, props , operationDescriptor.get_operation_args(), registry);
						return result;
						
					} catch (Exception e) {
						log.error(e);
						throw e;
					}
					finally{
						if(mongo!=null)
						{
							mongo.close();
						}
					}
					
					
				}
				else
				{
					throw new Exception("outputFormat could not be parsed");
				}
			}
			else
			{
				throw new Exception("outputFormat not specified");
			}
			
	}
	catch(Exception e)
	{
		log.error(e);
		throw new ProviderException(MongoDBProvider.class.getName() , MongoDBProvider.VERSION ,e);
	}

}

	@Override
	public QueryEndpoint validateAndInitializeQueryEndpoint(
			QueryEndpoint queryEndpoint) throws ProviderException {
		try{
				if(queryEndpoint.getOutputFormat()!=null)
				{
					OutputFormatProps props = GSONUtil.getGSONInstance().fromJson(queryEndpoint.getOutputFormat(), OutputFormatProps.class);
					if(props!=null)
					{
						OutputFormat of = props.getOutputFormat();
						IFormatHandler formatHandler = registry.getHandler(of);
						if(formatHandler!=null)
						{
							formatHandler.validate(props);
							return queryEndpoint;
						}
						else
						{
							throw new Exception("No handler found for outputType=[" + of + "]");
						}
						
					}
					else
					{
						throw new Exception("outputFormat could not be parsed");
					}
				}
				else
				{
					throw new Exception("outputFormat not specified");
				}
				
		}
		catch(Exception e)
		{
			log.error(e);
			throw new ProviderException(MongoDBProvider.class.getName() , MongoDBProvider.VERSION ,e);
		}
	}

	@Override
	public JsonObject getOutputFormatSchema() {
		// TODO later
		return new JsonObject();
	}

}