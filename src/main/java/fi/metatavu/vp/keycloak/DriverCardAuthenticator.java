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
        String truckVin = loginHint == null || loginHint.startsWith("truck-vin:") ? "" : loginHint.substring("truck-vin:".length());

        if (truckVin.isEmpty()) {
            truckVin = config.getConfig().get(DriverCardAuthenticationConfig.DEFAULT_TRUCK_VIN);
        }

        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        Response response = form
                .setAttribute("truckVin", truckVin)
                .setAttribute("autoLoginInterval", config.getConfig().get(DriverCardAuthenticationConfig.AUTO_LOGIN_INTERVAL))
                .setAttribute("hideTruckVinInput", "true".equals(config.getConfig().get(DriverCardAuthenticationConfig.HIDE_TRUCK_VIN_INPUT)))
                .setAttribute("autoLogin", "true".equals(config.getConfig().get(DriverCardAuthenticationConfig.AUTO_LOGIN)))
                .createForm("card-login-form.ftl");

        context.challenge(response);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String truckVin = formData.getFirst("truckVin");
        RealmModel realm = context.getRealm();

        try {
            DriverCard driverCard = findDriverCardByTruckVin(context.getAuthenticatorConfig(), truckVin);
            if (driverCard != null) {
                UserModel cardUser = session
                    .users()
                    .searchForUserByUserAttributeStream(realm, "driverCardId", driverCard.getDriverCardId())
                    .findFirst()
                    .orElse(null);

                if (cardUser != null) {
                    logger.info("Driver card found for truck vin: " + truckVin);
                    context.setUser(cardUser);
                    context.success();
                    return;
                } else {
                    logger.info("Driver card not found for truck vin: " + truckVin);
                }
            } else {
                logger.info("Driver card not found for truck vin: " + truckVin);
            }
        } catch (InterruptedException | IOException e) {
            logger.error("Failed to find driver card by truck vin", e);
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
     * @param truckVin truck vin
     * @return driver card
     * @throws IOException exception thrown when API request fails on I/O level
     * @throws InterruptedException exception thrown when API request is interrupted
     */
    private DriverCard findDriverCardByTruckVin(AuthenticatorConfigModel config, String truckVin) throws IOException, InterruptedException {
        String apiKey = config.getConfig().get("vehicleManagementApiKey");
        String apiUrl = config.getConfig().get("vehicleManagementApiUrl");

        URI uri = UriBuilder.fromUri(apiUrl)
            .path("v1/driverCards")
            .queryParam("truckVin", truckVin)
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
