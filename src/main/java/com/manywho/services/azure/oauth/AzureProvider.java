package com.manywho.services.azure.oauth;

import com.manywho.sdk.services.oauth.AbstractOauth2Provider;
import com.manywho.services.azure.configuration.SecurityConfiguration;
import org.scribe.model.OAuthConfig;

import javax.inject.Inject;

public class AzureProvider extends AbstractOauth2Provider {
    private final SecurityConfiguration configuration;
    public static final String REDIRECT_URI = "https://flow.manywho.com/api/run/1/oauth2";
    public static final String GRAPH_RESOURCE = "00000003-0000-0000-c000-000000000000";
    public static final String AUTHORITY_URI_V1 = "https://login.microsoftonline.com/common";
    public static final String AUTHORITY_URI_V2 = "https://login.microsoftonline.com";

    @Inject
    public AzureProvider(SecurityConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getName() {
        return "azure-manywho";
    }

    @Override
    public String getClientId() {
        return configuration.getOauth2ClientId();
    }

    @Override
    public String getClientSecret() {
        return configuration.getOauth2ClientSecret();
    }

    @Override
    public String getRedirectUri() {
        return REDIRECT_URI;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return null;
    }

    @Override
    public String getAuthorizationUrl(OAuthConfig config) {

        return String.format("%s/oauth2/v2.0/authorize?client_id=%s&scope=%s&response_type=%s",
                AUTHORITY_URI_V1, config.getApiKey(), "User.Read" , "code");
    }
}
