package org.apache.zeppelin.rest;

import org.apache.zeppelin.server.JsonResponse;
import org.apache.zeppelin.utils.SecurityContextHolder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by piyush.mukati on 15/09/15.
 */

@Path("/auth")
@Produces("application/json")
public class AuthRestApi {
    public AuthRestApi() {
        super();
    }

   // used to pass token to websites
    @GET
    @Path("token")
    public Response token() {
        Map<String, String> data = new HashMap<>();
        data.put("user", SecurityContextHolder.getSecurityContext().getUser());
        data.put("token", SecurityContextHolder.getSecurityContext().getToken());
        return new JsonResponse(Response.Status.OK, "", data).build();
    }
}
