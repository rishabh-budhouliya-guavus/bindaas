package edu.emory.cci.bindaas.core.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.JsonObject;


import edu.emory.cci.bindaas.core.api.IManagementTasks;
import edu.emory.cci.bindaas.core.api.IPersistenceDriver;
import edu.emory.cci.bindaas.core.api.IProviderRegistry;
import edu.emory.cci.bindaas.core.api.IValidator;
import edu.emory.cci.bindaas.core.bundle.Activator;
import edu.emory.cci.bindaas.core.exception.DuplicateException;
import edu.emory.cci.bindaas.core.exception.NotFoundException;
import edu.emory.cci.bindaas.core.exception.ProviderNotFoundException;
import edu.emory.cci.bindaas.core.exception.FrameworkEntityException.Type;
import edu.emory.cci.bindaas.core.model.DeleteEndpointRequestParameter;
import edu.emory.cci.bindaas.core.model.QueryEndpointRequestParameter;
import edu.emory.cci.bindaas.core.model.SubmitEndpointRequestParameter;
import edu.emory.cci.bindaas.framework.api.IProvider;
import edu.emory.cci.bindaas.framework.api.IQueryHandler;
import edu.emory.cci.bindaas.framework.api.ISubmitHandler;
import edu.emory.cci.bindaas.framework.model.DeleteEndpoint;
import edu.emory.cci.bindaas.framework.model.Profile;
import edu.emory.cci.bindaas.framework.model.QueryEndpoint;
import edu.emory.cci.bindaas.framework.model.Stage;
import edu.emory.cci.bindaas.framework.model.SubmitEndpoint;
import edu.emory.cci.bindaas.framework.model.Workspace;
import edu.emory.cci.bindaas.framework.util.GSONUtil;

public class ManagementTasksImpl implements IManagementTasks {

	private IProviderRegistry providerRegistry;
	private IPersistenceDriver persistenceDriver;
	private IValidator validator;
	private Log log = LogFactory.getLog(getClass());
	private Map<String,Workspace> workspaces;
	
	
	public ManagementTasksImpl()
	{
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("class", getClass().getName());
		Activator.getContext().registerService(IManagementTasks.class.getName(), this, props);
	}
	public IProviderRegistry getProviderRegistry() {
		return providerRegistry;
	}

	public void setProviderRegistry(IProviderRegistry providerRegistry) {
		this.providerRegistry = providerRegistry;
	}

	public IPersistenceDriver getPersistenceDriver() {
		return persistenceDriver;
	}

	public void setPersistenceDriver(IPersistenceDriver persistenceDriver) {
		this.persistenceDriver = persistenceDriver;
	}

	public IValidator getValidator() {
		return validator;
	}

	public void setValidator(IValidator validator) {
		this.validator = validator;
	}
	
	public void init() throws Exception
	{
		loadWorkspaces();
	}

	public void loadWorkspaces() throws IOException
	{
		log.debug("Loading All workspaces");
		workspaces = new HashMap<String, Workspace>();
		List<String> loadedContent = persistenceDriver.loadAllWorkspaces();
		for(String content : loadedContent)
		{
			try {
					Workspace workspace = GSONUtil.getGSONInstance().fromJson(content, Workspace.class);
					workspaces.put(workspace.getName(), workspace);
					log.trace("Workspace Loaded\n" + workspace);
			}
			catch(Exception e)
			{
				log.error(e);
			}
			
			
		}
		
	}
	
	public void updateAllWorkspaces() throws IOException
	{
		log.debug("Updating All Workspaces");
		for(Workspace workspace : workspaces.values())
		{
			persistenceDriver.createOrUpdateWorkspace(workspace.getName(), workspace.toString());
			log.trace("Created/Updated Workspace\n" + workspace);
		}
	}
	
	public void updateOrCreateWorkspace(Workspace workspace) throws IOException
	{
		persistenceDriver.createOrUpdateWorkspace(workspace.getName(), workspace.toString());
		log.debug("Created/Updated Workspace\n" + workspace);
	}
	
	public void removeWorkspaceFromDB(Workspace workspace) throws IOException
	{
		persistenceDriver.removeWorkspace(workspace.getName());
		log.debug("Workspace removed\n"+workspace);
	}

	@Override
	public Workspace createWorkspace(String name, JsonObject parameters,
			String createdBy) throws Exception {
		if(workspaces.containsKey(name) == false)
		{
			Workspace workspace = new Workspace();
			workspace.setName(name);
			workspace.setCreatedBy(createdBy);
			workspace.setParams(parameters);
			workspaces.put(name, workspace);
			updateOrCreateWorkspace(workspace);
			log.debug("Workspace created\n" + workspace);
			return workspace;
		}
		else
			throw new DuplicateException(name, Type.Workspace);
		
	}

	@Override
	public Profile createProfile(String name, String workspaceName,
			JsonObject parameters, String createdBy) throws Exception {
		
		// locate Workspace
		Workspace workspace = getWorkspace(workspaceName);
		if(workspace.getProfiles().containsKey(name) == false)
		{
			
			Profile profile = new Profile();
			profile.setCreatedBy(createdBy);
			profile.setName(name);
			
			// locate provider
			if(parameters.has("providerId") && parameters.has("providerVersion"))
			{
				try{
					String providerId = parameters.getAsJsonPrimitive("providerId").getAsString();
					int providerVersion = parameters.getAsJsonPrimitive("providerVersion").getAsInt();
					profile.setDataSource(parameters.getAsJsonObject("dataSource"));
					
					log.debug("Locating Provider id=[" + providerId + "] and version=[" + providerVersion + "]");
					IProvider provider = providerRegistry.lookupProvider(providerId, providerVersion);
					if(provider!=null)
					{
						log.debug("Provider found. Performing validation and initialization");
						profile = provider.validateAndInitializeProfile(profile);
						profile.setProviderId(provider.getId());
						profile.setProviderVersion(provider.getVersion());
						workspace.getProfiles().put(name, profile);
						updateOrCreateWorkspace(workspace);
						return profile;
						
					}
					else
					{
						throw new ProviderNotFoundException(providerId,providerVersion);
					}
					
				}
				catch(NullPointerException e)
				{
					throw new IllegalArgumentException("providerId and/or providerVersion don't have correct values" + parameters);
				}
				catch(NumberFormatException e)
				{
					throw new IllegalArgumentException("providerId and/or providerVersion don't have correct values" + parameters);
				}
				
				
				
			}
			else
			{
				throw new IllegalArgumentException("providerId and/or providerVersion not specified in  " + parameters);
			}
		}
		else
		{
				throw new DuplicateException(name, Type.Profile);
		}
}

	@Override
	public QueryEndpoint createQueryEndpoint(String name, String workspaceName,
			String profileName, JsonObject parameters, String createdBy)
			throws Exception {
		
		Workspace  workspace = getWorkspace(workspaceName);
		Profile profile = getProfile(workspaceName, profileName);
		
		// locate provider
		if(profile.getQueryEndpoints().containsKey(name) == false)
		{
			try{
				String providerId = profile.getProviderId();
				int providerVersion = profile.getProviderVersion();
				log.debug("Locating Provider id=[" + providerId + "] and version=[" + providerVersion + "]");
				IProvider provider = providerRegistry.lookupProvider(providerId, providerVersion);
				if(provider!=null)
				{
					IQueryHandler queryHandler = provider.getQueryHandler();
					
					if(queryHandler!=null)
					{
					
						QueryEndpoint queryEndpoint = new QueryEndpoint();
						queryEndpoint.setName(name);
						queryEndpoint.setCreatedBy(createdBy);
						queryEndpoint.setStage(Stage.UNVERIFIED);
					
						queryEndpoint = constructQueryEndpointFromProps(queryEndpoint, parameters);
					
					
						log.debug("Provider found. Extracting QueryHandler");
						
						// perform framework validation
						validator.validateQueryModifierRequestChain(queryEndpoint.getQueryModifiers());
						validator.validateQueryResultModifierRequestChain(queryEndpoint.getQueryResultModifiers());
						// perform provider validation
						queryEndpoint = queryHandler.validateAndInitializeQueryEndpoint(queryEndpoint);
						
						// register queryEndpoint
						profile.getQueryEndpoints().put(name, queryEndpoint);
						
						updateOrCreateWorkspace(workspace);
						return queryEndpoint;	
					}
					else
					{
						throw new Exception("Query Functionality not available from provider [" + providerId + "] version=[" + providerVersion  +"]");
					}
					
					
				}
				else
				{
					throw new ProviderNotFoundException(providerId,providerVersion);
				}
				
			}
			catch(NullPointerException e)
			{
				throw new IllegalArgumentException("providerId and/or providerVersion don't have correct values" + parameters);
			}
			catch(NumberFormatException e)
			{
				throw new IllegalArgumentException("providerId and/or providerVersion don't have correct values" + parameters);
			}
			
			
			
		}
		else
		{
			throw new DuplicateException(name, Type.QueryEndpoint);
		}
	}

	
	private QueryEndpoint constructQueryEndpointFromProps(QueryEndpoint queryEndpoint , JsonObject parameters) throws Exception {
		
		QueryEndpointRequestParameter qerp = GSONUtil.getGSONInstance().fromJson(parameters, QueryEndpointRequestParameter.class);
		check(qerp!=null, "QueryEndpoints params cannot be null");
		check(qerp.getBindVariables()!=null, "BindVariables  cannot be null");
		check(qerp.getQueryTemplate()!=null, "QueryTemplate cannot be null");
		
		
		queryEndpoint.setBindVariables(qerp.getBindVariables());
		queryEndpoint.setMetaData(qerp.getMetaData());
		queryEndpoint.setOutputFormat(qerp.getOutputFormat());
		queryEndpoint.setQueryModifiers(qerp.getQueryModifiers());
		queryEndpoint.setQueryResultModifiers(qerp.getQueryResultModifiers());
		queryEndpoint.setTags(qerp.getTags());
		queryEndpoint.setQueryTemplate(qerp.getQueryTemplate());
		queryEndpoint.setDescription(qerp.getDescription());
		
		
		return queryEndpoint;
	}

	@Override
	public DeleteEndpoint createDeleteEndpoint(String name,
			String workspaceName, String profileName, JsonObject parameters,
			String createdBy) throws Exception {
		Workspace  workspace = getWorkspace(workspaceName);
		Profile profile = getProfile(workspaceName, profileName);
		
		// locate provider
		if(profile.getDeleteEndpoints().containsKey(name) == false)
		{
			try{
				String providerId = profile.getProviderId();
				int providerVersion = profile.getProviderVersion();
				
				log.debug("Locating Provider id=[" + providerId + "] and version=[" + providerVersion + "]");
				IProvider provider = providerRegistry.lookupProvider(providerId, providerVersion);
				if(provider!=null)
				{
					log.debug("Provider found. Extracting DeleteHandler");
					if(provider.getDeleteHandler()!=null)
					{
						DeleteEndpoint deleteEndpoint = new DeleteEndpoint();
						deleteEndpoint.setName(name);
						deleteEndpoint.setCreatedBy(createdBy);
						deleteEndpoint.setStage(Stage.UNVERIFIED);
						
						deleteEndpoint = constructDeleteEndpointFromProps(deleteEndpoint, parameters);
						
						profile.getDeleteEndpoints().put(name, deleteEndpoint);
						
						
						
						updateOrCreateWorkspace(workspace);
						return deleteEndpoint;
					}
					else
					{
						throw new Exception("Delete Functionality not available from provider [" + providerId + "] version=[" + providerVersion  +"]");
					}
					
					
					
				}
				else
				{
					throw new ProviderNotFoundException(providerId,providerVersion);
				}
				
			}
			catch(NullPointerException e)
			{
				throw new IllegalArgumentException("providerId and/or providerVersion don't have correct values" + parameters);
			}
			catch(NumberFormatException e)
			{
				throw new IllegalArgumentException("providerId and/or providerVersion don't have correct values" + parameters);
			}
			
			
			
		}
		else
		{
			throw new DuplicateException(name, Type.DeleteEndpoint);
		}

					
	}

	private DeleteEndpoint constructDeleteEndpointFromProps(DeleteEndpoint deleteEndpoint ,
			JsonObject parameters) throws Exception {
		
		DeleteEndpointRequestParameter derp = GSONUtil.getGSONInstance().fromJson(parameters, DeleteEndpointRequestParameter.class);
		check(derp!=null, "DeleteEndpoint params cannot be null");
		check(derp.getBindVariables()!=null, "BindVariables  cannot be null");
		check(derp.getQueryTemplate()!=null, "QueryTemplate cannot be null");
		
		deleteEndpoint.setBindVariables(derp.getBindVariables());
		deleteEndpoint.setQueryTemplate(derp.getQueryTemplate());
		deleteEndpoint.setTags(derp.getTags());
		deleteEndpoint.setDescription(derp.getDescription());
		return deleteEndpoint;
	}

	@Override
	public SubmitEndpoint createSubmitEndpoint(String name,
			String workspaceName, String profileName, JsonObject parameters,
			String createdBy) throws Exception {
		
		Workspace  workspace = getWorkspace(workspaceName);
		Profile profile = getProfile(workspaceName, profileName);
		
		// locate provider
		if(profile.getSubmitEndpoints().containsKey(name) == false)
		{
			try{
				String providerId = profile.getProviderId();
				int providerVersion = profile.getProviderVersion();
				
				log.debug("Locating Provider id=[" + providerId + "] and version=[" + providerVersion + "]");
				IProvider provider = providerRegistry.lookupProvider(providerId, providerVersion);
				if(provider!=null)
				{
					ISubmitHandler submitHandler = provider.getSubmitHandler();
					
					if(submitHandler!=null)
					{
						SubmitEndpoint submitEndpoint = new SubmitEndpoint();
						submitEndpoint.setName(name);
						submitEndpoint.setCreatedBy(createdBy);
						submitEndpoint = constructSubmitEndpointFromProps(submitEndpoint, parameters);
						
						// perform framework validation
						
						validator.validateSubmitPayloadModifierRequestChain(submitEndpoint.getSubmitPayloadModifiers());

						// perform provider validation
						submitEndpoint = submitHandler.validateAndInitializeSubmitEndpoint(submitEndpoint);
						profile.getSubmitEndpoints().put(name, submitEndpoint);
						
						
						updateOrCreateWorkspace(workspace);
						return submitEndpoint;
					}
					else
					{
						throw new Exception("Submit Functionality not available from provider [" + providerId + "] version=[" + providerVersion  +"]");
					}
					
					
					
				}
				else
				{
					throw new ProviderNotFoundException(providerId,providerVersion);
				}
				
			}
			catch(NullPointerException e)
			{
				throw new IllegalArgumentException("providerId and/or providerVersion don't have correct values" + parameters);
			}
			catch(NumberFormatException e)
			{
				throw new IllegalArgumentException("providerId and/or providerVersion don't have correct values" + parameters);
			}
			
			
			
		}
		else
		{
			throw new DuplicateException(name, Type.SubmitEndpoint);
		}
					
	}

	private SubmitEndpoint constructSubmitEndpointFromProps(SubmitEndpoint submitEndpoint,
			JsonObject parameters) throws Exception {

		SubmitEndpointRequestParameter serp = GSONUtil.getGSONInstance().fromJson(parameters, SubmitEndpointRequestParameter.class);
		check(serp!=null, "SubmitEndpoint params cannot be null");
		
		submitEndpoint.setProperties(serp.getProperties());
		submitEndpoint.setSubmitPayloadModifiers(serp.getSubmitPayloadModifiers());
		return submitEndpoint;
	}
	

	@Override
	public void deleteWorkspace(String workspaceName) throws Exception {
		// locate Workspace
		Workspace workspace = workspaces.remove(workspaceName);
		if(workspace!=null)
		{
			this.removeWorkspaceFromDB(workspace);
		}
		
		
	}

	@Override
	public void deleteProfile(String workspaceName, String profileName)
			throws Exception {
		
		Workspace  workspace = getWorkspace(workspaceName);
		workspace.getProfiles().remove(profileName);
		updateOrCreateWorkspace(workspace);	
	}
	

	@Override
	public QueryEndpoint deleteQueryEndpoint(String workspaceName,
			String profileName, String queryEndpointName) throws Exception {
		Workspace  workspace = getWorkspace(workspaceName);
		Profile profile = getProfile(workspaceName, profileName);
		
		QueryEndpoint queryEndpoint = profile.getQueryEndpoints().remove(queryEndpointName);
		if(queryEndpoint!=null)
		{
			updateOrCreateWorkspace(workspace);
			return queryEndpoint;
		}
			
		else
			throw new NotFoundException(queryEndpointName, Type.QueryEndpoint);
	}

	
	@Override
	public DeleteEndpoint deleteDeleteEndpoint(String workspaceName,
			String profileName, String deleteEndpointName) throws Exception {
		
		Workspace  workspace = getWorkspace(workspaceName);
		Profile profile = getProfile(workspaceName, profileName);
		DeleteEndpoint deleteEndpoint = profile.getDeleteEndpoints().remove(deleteEndpointName);
		if(deleteEndpoint!=null)
		{
			updateOrCreateWorkspace(workspace);
			return deleteEndpoint;
		}
			
		else
			throw new NotFoundException(deleteEndpointName, Type.DeleteEndpoint);
	}
	

	@Override
	public SubmitEndpoint deleteSubmitEndpoint(String workspaceName,
			String profileName, String submitEndpointName) throws Exception {
		
		Workspace  workspace = getWorkspace(workspaceName);
		Profile profile = getProfile(workspaceName, profileName);
		
		SubmitEndpoint submitEndpoint = profile.getSubmitEndpoints().remove(submitEndpointName);
		if(submitEndpoint!=null)
		{
			updateOrCreateWorkspace(workspace);
			return submitEndpoint;
		}
			
		else
			throw new NotFoundException(submitEndpointName, Type.SubmitEndpoint);
	}

	@Override
	public Profile updateProfile(String profileName, String workspaceName,
			JsonObject parameters, String updatedBy) throws Exception {
		deleteProfile(workspaceName, profileName);
		Profile profile = this.createProfile(profileName, workspaceName, parameters, updatedBy);
		return profile;
	}

	@Override
	public QueryEndpoint updateQueryEndpoint(String queryEndpointName, String workspaceName,
			String profileName, JsonObject parameters, String updatedBy)
			throws Exception {
		deleteQueryEndpoint(workspaceName, profileName, queryEndpointName);
		QueryEndpoint queryEndpoint = this.createQueryEndpoint(queryEndpointName, workspaceName, profileName, parameters, updatedBy);
		return queryEndpoint;
	}

	@Override
	public DeleteEndpoint updateDeleteEndpoint(String deleteEndpointName,
			String workspaceName, String profileName, JsonObject parameters,
			String updatedBy) throws Exception {
		deleteDeleteEndpoint(workspaceName, profileName, deleteEndpointName);
		DeleteEndpoint deleteEndpoint = this.createDeleteEndpoint(deleteEndpointName, workspaceName, profileName, parameters, updatedBy);
		return deleteEndpoint;
	}

	@Override
	public SubmitEndpoint updateSubmitEndpoint(String submitEndpointName,
			String workspaceName, String profileName, JsonObject parameters,
			String updatedBy) throws Exception {
		deleteSubmitEndpoint(workspaceName, profileName, submitEndpointName);
		SubmitEndpoint submitEndpoint = this.createSubmitEndpoint(submitEndpointName, workspaceName, profileName, parameters, updatedBy);
		return submitEndpoint;
	}

	@Override
	public Workspace getWorkspace(String workspaceName) throws Exception {
		Workspace workspace = workspaces.get(workspaceName);
		if(workspace!=null)
		{
			return workspace;
		}
		else
			throw new NotFoundException(workspaceName, Type.Workspace);
		
	}

	@Override
	public Profile getProfile(String workspaceName, String profileName)
			throws Exception {
		Workspace workspace = workspaces.get(workspaceName);
		if(workspace!=null)
		{
			Profile profile = workspace.getProfiles().get(profileName);
			if(profile!=null)
			{
				return profile;
			}
			else
			{
				throw new NotFoundException(profileName, Type.Profile);
			}
		}
		else
		{
			throw new NotFoundException(workspaceName, Type.Workspace);
		}
	}

	@Override
	public QueryEndpoint getQueryEndpoint(String workspaceName,
			String profileName, String queryEndpointName) throws Exception {
		Workspace workspace = workspaces.get(workspaceName);
		if(workspace!=null)
		{
			Profile profile = workspace.getProfiles().get(profileName);
			if(profile!=null)
			{
				QueryEndpoint queryEndpoint = profile.getQueryEndpoints().get(queryEndpointName);
				if(queryEndpoint!=null)
				{
					
					return queryEndpoint;
				}
					
				else
					throw new NotFoundException(queryEndpointName, Type.QueryEndpoint);
					
			}
			else
			{
				throw new NotFoundException(profileName, Type.Profile);
			}
			
		}
		else
		{
			throw new NotFoundException(workspaceName, Type.Workspace);
		}
	}

	@Override
	public DeleteEndpoint getDeleteEndpoint(String workspaceName,
			String profileName, String deleteEndpointName) throws Exception {
		Workspace workspace = workspaces.get(workspaceName);
		if(workspace!=null)
		{
			Profile profile = workspace.getProfiles().get(profileName);
			if(profile!=null)
			{
				DeleteEndpoint deleteEndpoint = profile.getDeleteEndpoints().get(deleteEndpointName);
				if(deleteEndpoint!=null)
				{
					
					return deleteEndpoint;
				}
					
				else
					throw new NotFoundException(deleteEndpointName, Type.DeleteEndpoint);
					
			}
			else
			{
				throw new NotFoundException(profileName, Type.Profile);
			}
			
		}
		else
		{
			throw new NotFoundException(workspaceName, Type.Workspace);
		}
	}

	@Override
	public SubmitEndpoint getSubmitEndpoint(String workspaceName,
			String profileName, String submitEndpointName) throws Exception {
		Workspace workspace = workspaces.get(workspaceName);
		if(workspace!=null)
		{
			Profile profile = workspace.getProfiles().get(profileName);
			if(profile!=null)
			{
				SubmitEndpoint submitEndpoint = profile.getSubmitEndpoints().get(submitEndpointName);
				if(submitEndpoint!=null)
				{
					
					return submitEndpoint;
				}
					
				else
					throw new NotFoundException(submitEndpointName, Type.SubmitEndpoint);
					
			}
			else
			{
				throw new NotFoundException(profileName, Type.Profile);
			}
			
		}
		else
		{
			throw new NotFoundException(workspaceName, Type.Workspace);
		}
	}

	@Override
	public void publishQueryEndpoint(String workspaceName, String profileName,
			String queryEndpointName) throws Exception {
		
		QueryEndpoint queryEndpoint = getQueryEndpoint(workspaceName, profileName, queryEndpointName);
		queryEndpoint.setStage(Stage.VERIFIED);
				
		
	}

	@Override
	public void publishDeleteEndpoint(String workspaceName, String profileName,
			String deleteEndpointName) throws Exception {
	
		DeleteEndpoint deleteEndpoint = getDeleteEndpoint(workspaceName, profileName, deleteEndpointName);
		deleteEndpoint.setStage(Stage.VERIFIED);
	}

	@Override
	public Collection<Workspace> listWorkspaces() throws Exception {
		return workspaces.values();
	}
	
	private static void check(boolean condition , String errorMessage) throws Exception
	{
		if(!condition) throw new Exception(errorMessage);
	}
	

}