package com.benevolo;


import io.quarkus.rest.client.reactive.ClientBasicAuth;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Consumes("application/x-www-form-urlencoded")
@Produces("application/json")
@Path("")
@ClientBasicAuth(username = "benevolo", password = "${keycloak.password}")
@RegisterRestClient(baseUri = "https://auth.benevolo.de")
public interface ValidateToken {

    @POST
    @Path("/realms/benevolo/protocol/openid-connect/token/introspect")
    Response validate(@FormParam("token") String token);

}
