package com.benevolo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.undertow.Undertow;
import io.vertx.ext.web.Router;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.logging.Logger;

public class StaticResources {
    private static final Logger LOG = Logger.getLogger(StaticResources.class.getName());
    @RestClient
    ValidateToken validateToken;

    @RestClient
    ProcessEngine processEngine;

    @ConfigProperty(name = "PROCESS_ENGINE_TOKEN")
    String processEngineToken;

    @PostConstruct
    void initialize() {
        RestClientBuilder builder = RestClientBuilder.newBuilder();
        builder.baseUri(URI.create("https://engine.pe.benevolo.de"));
        builder.register(DynamicPathFilter.class);
        processEngine = builder.build(ProcessEngine.class);
    }

    void installRoute(@Observes StartupEvent startupEvent, Router router) {
        try {
            final Undertow server1 = Undertow.builder()
                    .addHttpListener(8081, "0.0.0.0")
                    .setHandler(exchange -> exchange.dispatch(() -> {
                        if(!exchange.getRequestPath().endsWith("/api/v1/messages/Bezahlinformation/trigger")) {
                            String token = exchange.getRequestHeader("Authorization");
                            // Check if a Keycloak token is present and if it is valid
                            Response responseToken = null;
                            try {
                                responseToken = validateToken.validate(token.split(" ")[1]);
                            } catch (WebApplicationException e) {
                                exchange.setStatusCode(e.getResponse().getStatus());
                                exchange.setResponseHeader("Content-Type", "application/json");
                                try {
                                    exchange.getOutputStream().write(e.getResponse().getEntity().toString().getBytes());
                                } catch (IOException ioException) {
                                    throw new RuntimeException(ioException);
                                }
                                exchange.endExchange();
                            }
                            // Handling of Token response (Username, Email, etc.)
                            String responseBody = responseToken.readEntity(String.class);
                            LOG.info("Token Response: " + responseBody);
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode rootNode = null;
                            try {
                                rootNode = mapper.readTree(responseBody);
                            } catch (JsonProcessingException e) {
                                // Return 401 Unauthorized
                                exchange.setStatusCode(401);
                                exchange.close();
                                throw new WebApplicationException("Error parsing Token JSON", Response.Status.UNAUTHORIZED);
                            }
                            boolean isActive = rootNode.get("active").asBoolean();
                            if (!isActive) {
                                LOG.info("Token is not valid");
                                // Return 401 Unauthorized
                                exchange.setStatusCode(401);
                                exchange.close();
                            } else {
                                LOG.info("Token is valid");
                            }
                        }

                        // Gather Request Method and Path
                        String requestPath = exchange.getRequestURI();
                        LOG.info("Request Path: " + requestPath);
                        String requestMethod = exchange.getRequestMethod();
                        LOG.info("Request Method: " + requestMethod);

                        // Proxy the request to the Process Engine
                        String authorizationHeader = "Bearer " + processEngineToken;
                        if (requestMethod.equals("GET")) {
                            // Make a GET request to the Process Engine
                            Response processEngineResponse = processEngine.get(requestPath.substring(1), authorizationHeader);
                            LOG.info("Process Engine Response: " + processEngineResponse.getStatus());
                            String processResponseBody = processEngineResponse.readEntity(String.class);
                            LOG.info("Process Engine Response Body: " + processResponseBody);
                            // Forward the response to the client
                            exchange.setStatusCode(processEngineResponse.getStatus());
                            exchange.setResponseHeader("Content-Type", "application/json");
                            try {
                                exchange.getOutputStream().write(processResponseBody.getBytes());
                            } catch (IOException e) {
                                throw new WebApplicationException("Error writing response to client", Response.Status.INTERNAL_SERVER_ERROR);
                            }
                            exchange.endExchange();
                        } else if (requestMethod.equals("POST")) {
                            // Read the request body
                            InputStream requestBodyStream = exchange.getInputStream();
                            java.util.Scanner s = new java.util.Scanner(requestBodyStream).useDelimiter("\\A");
                            String requestBody = s.hasNext() ? s.next() : "";

                            // Parse the request body into a JsonObject
                            JsonReader jsonReader = Json.createReader(new StringReader(requestBody));
                            JsonObject requestBodyJson = jsonReader.readObject();
                            jsonReader.close();
                            // Make a POST request to the Process Engine
                            try {
                                Response processEngineResponse = processEngine.post(requestPath.substring(1), requestBodyJson, authorizationHeader);
                                String processResponseBody = processEngineResponse.readEntity(String.class);
                                exchange.setStatusCode(processEngineResponse.getStatus());
                                exchange.setResponseHeader("Content-Type", "application/json");
                                exchange.getOutputStream().write(processResponseBody.getBytes());
                                LOG.info("Process Engine Response: " + processEngineResponse.getStatus());
                                LOG.info("Process Engine Response Body: " + processResponseBody);
                            } catch (Exception e) {
                                LOG.info("Error in Process Engine Request: " + e.getMessage());
                            }
                            exchange.endExchange();
                        } else {
                            // Return 405 Method Not Allowed
                            exchange.setStatusCode(405);
                            exchange.close();
                        }
                    }))
                    .build();
            server1.start();

        } catch (Exception e) {
            LOG.info("Error in Undertow Server: " + e.getMessage());
        }
    }
}