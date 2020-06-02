package com.manywho.services.azure.services;

import com.auth0.jwt.JWT;
import com.google.common.base.Strings;
import com.manywho.sdk.entities.security.AuthenticatedWhoResult;
import com.manywho.sdk.entities.security.AuthenticationCredentials;
import com.manywho.sdk.enums.AuthenticationStatus;
import com.manywho.sdk.services.oauth.AbstractOauth2Provider;
import com.manywho.services.azure.configuration.SecurityConfiguration;
import com.manywho.services.azure.entities.AzureUser;
import com.manywho.services.azure.facades.AzureFacade;
import com.manywho.services.azure.oauth.AuthResponse;
import com.manywho.services.azure.oauth.AzureHttpClient;
import com.manywho.services.azure.oauth.AzureProvider;

import javax.inject.Inject;

public class AuthenticationService {
    private SecurityConfiguration securityConfiguration;
    private AzureHttpClient azureHttpClient;
    private AzureFacade azureFacade;

    @Inject
    public AuthenticationService( SecurityConfiguration securityConfiguration, AzureHttpClient azureHttpClient, AzureFacade azureFacade) {
        this.securityConfiguration = securityConfiguration;
        this.azureHttpClient = azureHttpClient;
        this.azureFacade = azureFacade;
    }

    public AuthenticatedWhoResult getAuthenticatedWhoResult(AbstractOauth2Provider provider, AuthenticationCredentials credentials) {
        AuthResponse authResponse = azureHttpClient.getAccessTokenByAuthCode(
                credentials.getCode(),
                AzureProvider.REDIRECT_URI,
                securityConfiguration.getOauth2ClientId(),
                securityConfiguration.getOauth2ClientSecret());

        String mailProperty = "";
        String defaultEmail = "";

        if (credentials.hasConfigurationValues()) {
            mailProperty = credentials.getConfigurationValues().getContentValue("Email Property Name");
            defaultEmail = credentials.getConfigurationValues().getContentValue("Default Email");
        }

        JWT jwt = JWT.decode(authResponse.getAccess_token());
        AzureUser azureUser = azureFacade.fetchCurrentUser(jwt.getToken(), mailProperty, defaultEmail);
        AuthenticatedWhoResult authenticatedWhoResult = new AuthenticatedWhoResult();

        if (Strings.isNullOrEmpty(azureUser.getEmail()) == true) {
            authenticatedWhoResult = AuthenticatedWhoResult.createDeniedResult();
            authenticatedWhoResult.setStatusMessage("This account doesn't have an email address - please provide an email in your account and try again.");
            return authenticatedWhoResult;
        }

        authenticatedWhoResult.setDirectoryId( provider.getClientId());
        authenticatedWhoResult.setDirectoryName( provider.getName());
        authenticatedWhoResult.setEmail(azureUser.getEmail());
        authenticatedWhoResult.setFirstName(azureUser.getGivenName());
        authenticatedWhoResult.setIdentityProvider(provider.getName());
        authenticatedWhoResult.setLastName(azureUser.getFamilyName());
        authenticatedWhoResult.setStatus(AuthenticationStatus.Authenticated);
        authenticatedWhoResult.setTenantName(provider.getClientId());
        authenticatedWhoResult.setToken( jwt.getToken());
        authenticatedWhoResult.setUserId( azureUser.getUserId());
        authenticatedWhoResult.setUsername(azureUser.getUniqueName());

        return authenticatedWhoResult;
    }


}
