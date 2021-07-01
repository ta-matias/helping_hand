package helpinghand.accesscontrol;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import helpinghand.resources.BackOfficeResource;
import static helpinghand.accesscontrol.AccessControlManager.TOKEN_ID_PARAM;

@Provider
public class AccessControlFilter implements ContainerRequestFilter{
	
	
	private Logger log = Logger.getLogger(AccessControlFilter.class.getName());
	
	private static final String ACCESS_FILTER_START = "Verifying request permissions...";
	private static final String ACCESS_DENIED_ERROR = "Insuficient permissions to execute operation";
	private static final String TOKEN_INFO = "\n operationId = [%s]\n tokenId = (%d)";
	private static final String INITIALIZING_RBAC_POLICY_START  = "Creating RBACPolicy entities";
	private static final String INITIALIZING_RBAC_POLICY_ERROR  = "Failed to create RBACPolicy entities";
	private static final String INITIALIZING_RBAC_POLICY_OK = "Successfuly created RBACPolicy entities";
	
	private static final String BACK_OFFICE_RESOURCE = BackOfficeResource.PATH.substring(1); //removing the '/'
	
	public AccessControlFilter() {}
	
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		log.info(ACCESS_FILTER_START);
		
		String method = requestContext.getMethod();
		if(method.equals("OPTIONS")) return;//to allow CORS
		
		UriInfo requestUriInfo = requestContext.getUriInfo();
		List<String> tokenList = requestUriInfo.getQueryParameters().get(TOKEN_ID_PARAM);
		long tokenId = -1;
		if(tokenList != null) { 
			tokenId = Long.parseLong(tokenList.get(0));
		}
		
		String operationId = method;
		List<PathSegment> pathSegs = requestUriInfo.getPathSegments();
		String resource = pathSegs.get(0).getPath();
		operationId += "_"+resource;
		if(pathSegs.size() > 1) {
			if(resource.equals(BACK_OFFICE_RESOURCE)) {
				operationId += "_"+pathSegs.get(1).getPath();
			}else {
				for(int i = 2; i< pathSegs.size();i++) {
					operationId += "_"+pathSegs.get(i).getPath();
				}
			}
		}
		
		
		log.info(String.format(TOKEN_INFO,operationId,tokenId));
		
		//check if RBAC Policy "table" is initialized
		if(!AccessControlManager.RBACPolicyIntitalized()) {
			//if it is not initialized
			log.info(INITIALIZING_RBAC_POLICY_START);
			if(!AccessControlManager.intitializeRBACPolicy()) {
				//if it failed to initialize
				log.info(INITIALIZING_RBAC_POLICY_ERROR);
				requestContext.abortWith(Response.status(Status.INTERNAL_SERVER_ERROR).build());
				return;
			}
			log.info(INITIALIZING_RBAC_POLICY_OK);
		}
		
		
		
		
		if(!AccessControlManager.hasAccess(tokenId, operationId)) {
			log.severe(ACCESS_DENIED_ERROR);
			requestContext.abortWith(Response.status(Status.FORBIDDEN).build());
			return;
		}
		
		
		//continues as if nothing happened
	}
	
	
}
