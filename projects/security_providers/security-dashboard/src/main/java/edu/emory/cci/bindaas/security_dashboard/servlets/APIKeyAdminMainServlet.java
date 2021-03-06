package edu.emory.cci.bindaas.security_dashboard.servlets;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

import com.google.gson.annotations.Expose;

import edu.emory.cci.bindaas.core.api.IManagementTasks;
import edu.emory.cci.bindaas.core.apikey.api.IAPIKeyManager;
import edu.emory.cci.bindaas.core.model.hibernate.HistoryLog.ActivityType;
import edu.emory.cci.bindaas.framework.util.GSONUtil;
import edu.emory.cci.bindaas.framework.util.IOUtils;
import edu.emory.cci.bindaas.security.api.BindaasUser;
import edu.emory.cci.bindaas.security_dashboard.RegistrableServlet;
import edu.emory.cci.bindaas.security_dashboard.api.IPolicyManager;
import edu.emory.cci.bindaas.security_dashboard.config.SecurityDashboardConfiguration;
import edu.emory.cci.bindaas.security_dashboard.model.User;
import edu.emory.cci.bindaas.security_dashboard.util.RakshakUtils;

/**
 *   /dashboard/security/apikey-admin/main
 * @author nadir
 *
 */
public class APIKeyAdminMainServlet extends RegistrableServlet{

	private static final long serialVersionUID = 1L;
	private String templateName;
	private Template template;
	private IManagementTasks managementTask;
	private Log log = LogFactory.getLog(getClass());
	private IPolicyManager policyManager;
	
	private IAPIKeyManager apiKeyManager;
	
	public IAPIKeyManager getApiKeyManager() {
		return apiKeyManager;
	}

	public void setApiKeyManager(IAPIKeyManager apiKeyManager) {
		this.apiKeyManager = apiKeyManager;
	}


	public IPolicyManager getPolicyManager() {
		return policyManager;
	}

	public void setPolicyManager(IPolicyManager policyManager) {
		this.policyManager = policyManager;
	}

	
	public void init()
	{
		template = getVelocityEngineWrapper().getVelocityTemplateByName(templateName);
	}
	
	

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try{
			String content = IOUtils.toString(req.getInputStream());
			
			Payload payload = GSONUtil.getGSONInstance().fromJson(content, Payload.class);
			
			if(payload.requestType.equals("issueKey"))
			{
				Calendar c = Calendar.getInstance();
				c.add(Calendar.YEAR, 1);
				
				apiKeyManager.generateAPIKey(new BindaasUser(payload.username), c.getTime(), "Security-Dashboard", "Key issued using Security-Dashboard", ActivityType.APPROVE, false);
			}
			else if(payload.requestType.equals("revokeKey"))
			{
				apiKeyManager.revokeAPIKey(new BindaasUser(payload.username), "Security-Dashboard", "Key revoked using Security-Dashboard", ActivityType.REVOKE);
			}
			
		}catch(Exception e)
		{
			log.error(e);
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		try{
			SecurityDashboardConfiguration config = getConfiguration();
			
			Set<User> setOfAllUsers = RakshakUtils.getAllUsers(config , apiKeyManager);
			VelocityContext context = getVelocityEngineWrapper().createVelocityContext(req);
			Map<String , Set<User>> indexedUserList = new HashMap<String, Set<User>>();
			  for(User u : setOfAllUsers)
			  {
				  String key = u.getName().substring(0, 2);
				  if(indexedUserList.containsKey(key)){
					  Set<User> setOfNames =indexedUserList.get(key);
					  setOfNames.add(u);
				  }
				  else
				  {
					  Set<User> setOfNames = new HashSet<User>();
					  setOfNames.add(u);
					  indexedUserList.put(key, setOfNames);
				  }
			  }
			  
			  String jsonized = GSONUtil.getGSONInstance().toJson(indexedUserList);
			  
			context.put("indexedUserList", jsonized);
			
			if(req.getParameter("searchBarStartValue")!=null)
			{
				context.put("searchBarStartValue", req.getParameter("searchBarStartValue"));
			}
			else
			{
				context.put("searchBarStartValue", "");
			}
			
			if(req.getParameter("showAll")!=null)
			{
				context.put("showAll", "true");
			}
			else
			{
				context.put("showAll", "false");
			}
			
			template.merge(context, resp.getWriter());
			
		}catch(Exception e)
		{
			log.error(e);
			throw new ServletException(e);
		}
	}
	
	
	
	public IManagementTasks getManagementTask() {
		return managementTask;
	}

	public void setManagementTask(IManagementTasks managementTask) {
		this.managementTask = managementTask;
	}
	
	private static class Payload {
		@Expose private String requestType;
		@Expose private String username;		
	}
	

}
