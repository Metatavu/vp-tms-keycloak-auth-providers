package fi.metatavu.vp.keycloak.drivercard;

import fi.metatavu.vp.keycloak.tms.TmsApiClient;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.TrucksApi;
import org.openapitools.client.model.TruckDriverCard;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        if (truckId == null || truckId.isEmpty()) {
            logger.warn("Could not find default truck id, disabling auto login");
            autoLogin = false;
        }

        boolean hideTruckIdInput = "true".equals(config.getConfig().get(DriverCardAuthenticationConfig.HIDE_TRUCK_ID_INPUT));
        if (truckId == null || truckId.isEmpty()) {
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
            TruckDriverCard driverCard = findDriverCardByTruckId(context.getAuthenticatorConfig(), truckId);
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
        } catch (MalformedURLException e) {
            logger.error("Failed to fetch driver card for truck id: " + truckId, e);
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
     */
    private TruckDriverCard findDriverCardByTruckId(AuthenticatorConfigModel config, String truckId) throws MalformedURLException {
        String apiKey = config.getConfig().get(DriverCardAuthenticationConfig.VEHICLE_MANAGEMENT_API_KEY);
        String apiUrl = config.getConfig().get(DriverCardAuthenticationConfig.VEHICLE_MANAGEMENT_API_URL);

        TmsApiClient tmsApiClient = new TmsApiClient(apiKey, URI.create(apiUrl).toURL());
        TrucksApi trucksApi = tmsApiClient.getTrucksApi();

        try {
            List<TruckDriverCard> truckDriverCard = trucksApi.listTruckDriverCards(UUID.fromString(truckId));
            if (truckDriverCard.size() == 1) {
                return truckDriverCard.get(0);
            }
        } catch (ApiException e) {
            logger.error("Failed to fetch driver card for truck id: " + truckId + ", using apiUrl: " + apiUrl, e);
            return null;
        }

        return null;
    }

}
