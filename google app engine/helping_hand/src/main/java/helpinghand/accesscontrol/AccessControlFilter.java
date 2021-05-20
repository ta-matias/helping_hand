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
	
	public AccessControlFilter() {}
	
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		
		boolean allAccess = false; //true if request was done without a token
		boolean hasClient = false; //true if the request sent along the id of the client and needs it to be the owner of the token
		
		String method = requestContext.getMethod();
		UriInfo requestUriInfo = requestContext.getUriInfo();
		List<String> tokenList = requestUriInfo.getQueryParameters().get("tokenId");
		String tokenId = "";
		if(tokenList == null) { //no tokens were provided
			allAccess = true;
		}else {
			tokenId = tokenList.get(0);//there should only be one "token" query parameter, others will be ignored
		}
		
		String clientId = "";
		String operationId = method;
		List<PathSegment> pathSegs = requestUriInfo.getPathSegments();
		switch(pathSegs.size()) {
			case 2:
				operationId += "_"+pathSegs.get(0).getPath();
				clientId = pathSegs.get(1).getPath();
				if(!clientId.equals(""))hasClient = true; //in case clientId is not really passed as parameter (ex: createUser, createInst)
				break;
			case 3:
				operationId += "_"+pathSegs.get(0).getPath()+"_"+pathSegs.get(2).getPath();
				clientId = pathSegs.get(1).getPath();
				hasClient = true;
				break;
			default:
				//not implemented
				break;
		}
		log.info(String.format("allAccess = [%s]\n hasClient = [%s]\n operationId = [%s]\n clientId = [%s]\n tokenId = [%s]",
				allAccess,
				hasClient,
				operationId,
				clientId,
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
		
		
		
		//if the was no token provided
		if(allAccess)log.info("Using allHasAccess()");
		if(allAccess && !AccessControlManager.allHasAccess(operationId)) {
			requestContext.abortWith(Response.status(Status.FORBIDDEN).entity("Access denied in filter.").build());
			return;
		}
		
		//if the token and clientId were provided
		if(!allAccess && hasClient)log.info("Using clientHasAccess()");
		if(!allAccess && hasClient && !AccessControlManager.clientHasAccess(clientId, tokenId, operationId)) {
			requestContext.abortWith(Response.status(Status.FORBIDDEN).entity("Access denied in filter.").build());
			return;
		}
		
		// if only the token was provided, not used in ALPHA
		if(!allAccess && !hasClient)log.info("Using tokenHasAccess()");
		if(!allAccess && !hasClient && !AccessControlManager.tokenHasAccess(tokenId, operationId)) {
			requestContext.abortWith(Response.status(Status.FORBIDDEN).entity("Access Denied in filter.").build());
			return;
		}
		
		//continues as if nothing happened
	}
	
	
}
