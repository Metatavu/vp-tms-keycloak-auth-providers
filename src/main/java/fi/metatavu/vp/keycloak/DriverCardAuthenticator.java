package fi.metatavu.vp.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Driver card authenticator
 */
public class DriverCardAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(DriverCardAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        Map<String, String> queryParams = authSession.getClientNotes();
        String loginHint = queryParams.get("login_hint");
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        String truckId = loginHint == null || !loginHint.startsWith("truck-id:") ? "" : loginHint.substring("truck-id:".length());

        if (truckId.isEmpty()) {
            logger.warn("Could not find truck id from login hint, using default truck id");
            truckId = config.getConfig().get(DriverCardAuthenticationConfig.DEFAULT_TRUCK_ID);
        }

        boolean autoLogin = "true".equals(config.getConfig().get(DriverCardAuthenticationConfig.AUTO_LOGIN));
        if (truckId.isEmpty()) {
            logger.warn("Could not find default truck id, disabling auto login");
            autoLogin = false;
        }

        boolean hideTruckIdInput = "true".equals(config.getConfig().get(DriverCardAuthenticationConfig.HIDE_TRUCK_ID_INPUT));
        if (truckId.isEmpty()) {
            logger.warn("Could not find default truck id, showing truck id input");
            hideTruckIdInput = false;
        }

        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        Response response = form
                .setAttribute("truckId", truckId)
                .setAttribute("autoLoginInterval", config.getConfig().get(DriverCardAuthenticationConfig.AUTO_LOGIN_INTERVAL))
                .setAttribute("hideTruckIdInput", hideTruckIdInput)
                .setAttribute("autoLogin", autoLogin)
                .createForm("card-login-form.ftl");

        context.challenge(response);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String truckId = formData.getFirst("truckId");
        RealmModel realm = context.getRealm();

        try {
            DriverCard driverCard = findDriverCardByTruckId(context.getAuthenticatorConfig(), truckId);
            if (driverCard != null) {
                UserModel cardUser = session
                    .users()
                    .searchForUserByUserAttributeStream(realm, "driverCardId", driverCard.getId())
                    .findFirst()
                    .orElse(null);

                if (cardUser != null) {
                    logger.info("Driver card found for truck id: " + truckId);
                    context.setUser(cardUser);
                    context.success();
                    return;
                } else {
                    logger.info("Driver card not found for truck id: " + truckId);
                }
            } else {
                logger.info("Driver card not found for truck id: " + truckId);
            }
        } catch (InterruptedException | IOException e) {
            logger.error("Failed to find driver card by truck id", e);
        }

        context.resetFlow();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {}

    @Override
    public void close() {}

    /**
     * Finds driver card by truck vin
     *
     * @param config authenticator config
     * @param truckId truck id
     * @return driver card
     * @throws IOException exception thrown when API request fails on I/O level
     * @throws InterruptedException exception thrown when API request is interrupted
     */
    private DriverCard findDriverCardByTruckId(AuthenticatorConfigModel config, String truckId) throws IOException, InterruptedException {
        String apiKey = config.getConfig().get("vehicleManagementApiKey");
        String apiUrl = config.getConfig().get("vehicleManagementApiUrl");

        URI uri = UriBuilder.fromUri(apiUrl)
            .path(String.format("v1/trucks/%s/driverCards", truckId))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .header("X-API-Key", apiKey)
            .GET()
            .build();

        HttpResponse<InputStream> response = HttpClient
            .newBuilder()
            .build()
            .send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 200) {
            ObjectMapper objectMapper = new ObjectMapper();
            DriverCard[] cards = objectMapper.readValue(response.body(), DriverCard[].class);
            if (cards.length == 1) {
                return cards[0];
            }
        }

        return null;
    }

}
