package edu.emory.cci.bindaas.datasource.provider.genericsql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonObject;


import edu.emory.cci.bindaas.datasource.provider.genericsql.model.DataSourceConfiguration;
import edu.emory.cci.bindaas.datasource.provider.genericsql.model.OutputFormat;
import edu.emory.cci.bindaas.datasource.provider.genericsql.model.OutputFormatProps;
import edu.emory.cci.bindaas.datasource.provider.genericsql.outputformat.IFormatHandler;
import edu.emory.cci.bindaas.datasource.provider.genericsql.outputformat.OutputFormatRegistry;
import edu.emory.cci.bindaas.framework.api.IQueryHandler;
import edu.emory.cci.bindaas.framework.model.ProviderException;
import edu.emory.cci.bindaas.framework.model.QueryEndpoint;
import edu.emory.cci.bindaas.framework.model.QueryResult;
import edu.emory.cci.bindaas.framework.util.GSONUtil;

public class GenericSQLQueryHandler implements IQueryHandler {
	private AbstractSQLProvider provider;
	private OutputFormatRegistry outputFormatRegistry;
	private Log log = LogFactory.getLog(getClass());
	
	public OutputFormatRegistry getOutputFormatRegistry() {
		return outputFormatRegistry;
	}
	public void setOutputFormatRegistry(OutputFormatRegistry outputFormatRegistry) {
		this.outputFormatRegistry = outputFormatRegistry;
	}
	public AbstractSQLProvider getProvider() {
		return provider;
	}
	public void setProvider(AbstractSQLProvider provider) {
		this.provider = provider;
	}
	
	@Override
	public QueryResult query(JsonObject dataSource,
			JsonObject outputFormatProps, String queryToExecute)
			throws ProviderException {
		try {
			OutputFormatProps props = GSONUtil.getGSONInstance().fromJson(
					outputFormatProps, OutputFormatProps.class); // Get
																	// outputFormat
																	// props
			
			OutputFormat outputFormat = props.getOutputFormat();

			IFormatHandler formatHandler = outputFormatRegistry.getHandler(outputFormat);
			if (formatHandler != null) {
				Connection connection = null;
				try {
					DataSourceConfiguration configuration = GSONUtil
							.getGSONInstance().fromJson(dataSource,
									DataSourceConfiguration.class);
					connection = provider.getConnection(configuration);
					Statement statement = connection.createStatement();
					ResultSet resultSet = statement
							.executeQuery(queryToExecute);
					QueryResult queryResult = formatHandler.format(props,
							resultSet);
					return queryResult;
				} catch (Exception er) {
					log.error(er);
					throw er;
				} finally {
					if (connection != null)
						connection.close();
				}
			} else {
				throw new Exception("No Handler for OutputFormat=[" + outputFormat + "]");
			}

		} catch (Exception e) {
			log.error(e);
			throw new ProviderException(AbstractSQLProvider.class.getName() , AbstractSQLProvider.VERSION , "Query Not Executed", e);
		}
	}

	@Override
	public QueryEndpoint validateAndInitializeQueryEndpoint(
			QueryEndpoint queryEndpoint) throws ProviderException {
		try {
			OutputFormatProps props = GSONUtil.getGSONInstance().fromJson(
					queryEndpoint.getOutputFormat(), OutputFormatProps.class); // Get
																				// outputFormat
																				// props
			
			OutputFormat outputFormat = props.getOutputFormat();

			IFormatHandler formatHandler = outputFormatRegistry.getHandler(outputFormat);
			if (formatHandler != null) {
				formatHandler.validate(props);
			} else {
				throw new Exception("No Handler for OutputFormat=[" + outputFormat + "]");
			}
		} catch (Exception e) {
			log.error(e);
			throw new ProviderException(AbstractSQLProvider.class.getName() , AbstractSQLProvider.VERSION , 
					"Validation of Input Parameters failed", e);
		}

		return queryEndpoint;
	}

	
	@Override
	public JsonObject getOutputFormatSchema() {
		// TODO: later
		return new JsonObject();
	}

}
