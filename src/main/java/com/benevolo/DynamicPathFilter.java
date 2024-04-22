package com.benevolo;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class DynamicPathFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final String HEADER = "dynamic-path";
    private static final Logger LOG = Logger.getLogger(DynamicPathFilter.class.getName());

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String uri = requestContext.getHeaderString(HEADER);
        if (uri != null) {
            requestContext.setUri(URI.create(requestContext.getUri() + uri));
            requestContext.getHeaders().remove(HEADER);
        }
        LOG.info("Request URI: " + requestContext.getUri());
        LOG.info("Request Method: " + requestContext.getMethod());
        LOG.info("Request Headers: " + requestContext.getHeaders());
        LOG.info("Request Headers: " + requestContext.getHeaders());
        LOG.info("Request Body: " + requestContext.getEntity());
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        LOG.info("Response Status: " + responseContext.getStatus());
        LOG.info("Response Headers: " + responseContext.getHeaders());
        // LOG.info("Response Body: " + readEntityStream(responseContext.getEntityStream()));
    }

    private String readEntityStream(InputStream entityStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(entityStream, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();

    }
}