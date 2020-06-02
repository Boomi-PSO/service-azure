package com.manywho.services.azure.facades;

import com.manywho.sdk.entities.run.elements.type.Object;
import com.manywho.services.azure.entities.AzureUser;
import com.manywho.services.azure.services.ObjectMapperService;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.apache.olingo.client.api.communication.request.retrieve.v4.RetrieveRequestFactory;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.v4.ODataClient;
import org.apache.olingo.client.api.uri.v4.URIBuilder;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.domain.v4.ODataEntity;
import org.apache.olingo.commons.api.domain.v4.ODataEntitySet;

import javax.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AzureFacade {
    private final static String GRAPH_ENDPOINT = "https://graph.microsoft.com/v1.0";
    private ObjectMapperService objectMapperService;
    private final ODataClient client;
    private final RetrieveRequestFactory retrieveRequestFactory;

    @Inject
    public AzureFacade(ObjectMapperService objectMapperService) {
        this.objectMapperService = objectMapperService;
        client = ODataClientFactory.getV4();
        retrieveRequestFactory = client.getRetrieveRequestFactory();
    }

    public List<Object> fetchGroups(String token, String searchTerm) {
        String filter = AzureFilterBuilder.buildFilterExpression(searchTerm);
        ODataRetrieveResponse<ODataEntitySet> sitesEntitySetResponse = getEntitiesSetResponse(token, "groups", filter);

        return responseGroups(sitesEntitySetResponse.getBody().getEntities());
    }
    public List<Object> fetchMemberOfGroups(String token) {
        ODataRetrieveResponse<ODataEntitySet> sitesEntitySetResponse = getEntitiesSetResponse(token, "me/memberOf", "");

        return responseGroups(sitesEntitySetResponse.getBody().getEntities());
    }

    public List<Object> fetchUsers(String token, String searchTerm) {
        String filter = AzureFilterBuilder.buildFilterExpression(searchTerm);
        ODataRetrieveResponse<ODataEntitySet> sitesEntitySetResponse = getEntitiesSetResponse(token, "users", filter);

        return responseUsers(sitesEntitySetResponse.getBody().getEntities());
    }

    public String fetchCurrentUserId(String token) {
        ODataEntity sitesEntitySetResponse = getEntitySetResponse(token, "me").getBody();

        return sitesEntitySetResponse.getProperty("id").getValue().toString();
    }

    public AzureUser fetchCurrentUser(String token, String mailProperty, String defaultEmail) {
        ODataEntity sitesEntitySetResponse = getEntitySetResponse(token, "me").getBody();

        String email = "";
        
        if (!mailProperty.isEmpty()) {
            email = sitesEntitySetResponse.getProperty(mailProperty).getValue().toString();
        }
        else {
            email = sitesEntitySetResponse.getProperty("mail").getValue().toString();
        }

        if (email.isEmpty()) {
            email = defaultEmail;
        }

        return new AzureUser(email,
                sitesEntitySetResponse.getProperty("givenName").getValue().toString(),
                sitesEntitySetResponse.getProperty("surname").getValue().toString(),
                sitesEntitySetResponse.getProperty("id").getValue().toString(),
                sitesEntitySetResponse.getProperty("userPrincipalName").getValue().toString()
        );
    }

    private List<Object> responseUsers(List<ODataEntity> entities) {
        List<Object> groupsArray = new ArrayList<>();

        for (ODataEntity siteEntity : entities) {
            groupsArray.add(this.objectMapperService.buildUserObject(siteEntity));
        }

        return groupsArray;
    }

    private ODataRetrieveResponse<ODataEntitySet> getEntitiesSetResponse(String token, String urlEntity, String filter) {
        URI entitySetURI = buildUri(urlEntity, filter);
        ODataEntitySetRequest<ODataEntitySet> entitySetRequest = retrieveRequestFactory.getEntitySetRequest(entitySetURI);
        entitySetRequest.addCustomHeader("Authorization", String.format("Bearer %s", token));

        return entitySetRequest.execute();
    }

    private ODataRetrieveResponse<ODataEntity> getEntitySetResponse(String token, String entryPoint) {
        URI entityUri = client.newURIBuilder(GRAPH_ENDPOINT).appendEntitySetSegment(entryPoint).build();
        ODataEntityRequest<ODataEntity> entitySetRequest = retrieveRequestFactory.getEntityRequest(entityUri);
        entitySetRequest.addCustomHeader("Authorization", String.format("Bearer %s", token));
        return entitySetRequest.execute();
    }

    private List<Object> responseGroups(List<ODataEntity> groups) {
        List<Object> groupsArray = new ArrayList<>();

        for (ODataEntity siteEntity : groups) {
            groupsArray.add(this.objectMapperService.buildGroupObject(siteEntity));
        }

        return groupsArray;
    }

    private URI buildUri(String entryPoint, String filter){

        URIBuilder builder = client.newURIBuilder(GRAPH_ENDPOINT);

        builder.appendEntitySetSegment(entryPoint);

        if(filter != null && !filter.isEmpty()){
            builder.addQueryOption("filter", filter, false);
        }

        builder.addQueryOption("top", "999", false);

        return builder.build();
    }
}
