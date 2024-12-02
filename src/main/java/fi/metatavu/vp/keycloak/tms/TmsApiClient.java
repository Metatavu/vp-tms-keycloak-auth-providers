package fi.metatavu.vp.keycloak.tms;

import org.openapitools.client.ApiClient;
import org.openapitools.client.api.ClientAppsApi;
import org.openapitools.client.api.TrucksApi;

import java.net.URL;

/**
 * TMS API client
 */
public class TmsApiClient {

    private final String apiKey;
    private final URL apiUrl;

    /**
     * Constructor
     *
     * @param apiKey API key
     * @param apiUrl API URL
     */
    public TmsApiClient(String apiKey, URL apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    /**
     * Returns Trucks API client
     *
     * @return Trucks API client
     */
    public TrucksApi getTrucksApi() {
        return new TrucksApi(getApiClient());
    }

    /**
     * Returns ClientApps API client
     *
     * @return ClientApps API client
     */
    public ClientAppsApi getClientAppsApi() {
        return new ClientAppsApi(getApiClient());
    }

    /**
     * Returns API client authenticated with API key
     *
     * @return API client authenticated with API key
     */
    private ApiClient getApiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setHost(apiUrl.getHost());
        apiClient.setPort(apiUrl.getPort());
        apiClient.setBasePath(apiUrl.getPath());
        apiClient.setScheme(apiUrl.getProtocol());
        apiClient.setRequestInterceptor(request -> request.header("X-Keycloak-API-Key", apiKey));
        return apiClient;
    }

}
