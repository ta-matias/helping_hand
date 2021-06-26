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

@Provider
public class AccessControlFilter implements ContainerRequestFilter{
	
	
	private Logger log = Logger.getLogger(AccessControlFilter.class.getName());
	
	private static final String ACCESS_FILTER_START = "Verifying request permissions...";
	
	private static final String ACCESS_DENIED_ERROR = "Insuficient permissions to execute operation";
	
	public AccessControlFilter() {}
	
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		log.info(ACCESS_FILTER_START);
		
		String method = requestContext.getMethod();
		if(method.equals("OPTIONS")) return;//to allow CORS
		
		UriInfo requestUriInfo = requestContext.getUriInfo();
		List<String> tokenList = requestUriInfo.getQueryParameters().get("tokenId");
		String tokenId = null;
		if(tokenList != null) { 
			tokenId = tokenList.get(0);
		}
		
		String operationId = method;
		List<PathSegment> pathSegs = requestUriInfo.getPathSegments();
		for(PathSegment seg:pathSegs) {
			operationId += "_"+seg.getPath();
		}
		log.info(String.format("\n operationId = [%s]\n tokenId = [%s]",
				operationId,
				tokenId));
		
		//check if RBAC Policy "table" is initialized
		if(!AccessControlManager.RBACPolicyIntitalized()) {
			//if it is not initialized
			log.info("Initializing RBAC Policy");
			if(!AccessControlManager.intitializeRBACPolicy()) {
				//if it failed to initialize
				log.info("Error initializing RBAC Policy");
				requestContext.abortWith(Response.status(Status.INTERNAL_SERVER_ERROR).entity("RBAC Policy could not be initialized.").build());
				return;
			}
			log.info("RBAC Policy initialized successfully");
		}
		
		
		
		
		if(!AccessControlManager.hasAccess(tokenId, operationId)) {
			log.severe(ACCESS_DENIED_ERROR);
			requestContext.abortWith(Response.status(Status.FORBIDDEN).build());
			return;
		}
		
		
		//continues as if nothing happened
	}
	
	
}
