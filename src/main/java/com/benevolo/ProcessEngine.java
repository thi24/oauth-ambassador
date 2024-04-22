package com.benevolo;

import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Produces("application/json")
@Path("")
@RegisterRestClient(baseUri = "https://engine.pe.benevolo.de", configKey = "processEngineClient")
public interface ProcessEngine {

    @POST
    @Path("")
    @Consumes("application/json")
    @HeaderParam("Authorization")
    Response post(@HeaderParam("dynamic-path") String path, JsonObject body, @HeaderParam("Authorization") String auth);

    @GET
    @Path("")
    @HeaderParam("Authorization")
    Response get(@HeaderParam("dynamic-path") String path, @HeaderParam("Authorization") String auth);

}
