package fi.metatavu.vp.keycloak.pincode;

import fi.metatavu.vp.keycloak.tms.TmsApiClient;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.ClientAppsApi;
import org.openapitools.client.model.VerifyClientAppRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Pin code authenticator
 */
public class PinCodeAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(PinCodeAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        Map<String, String> queryParams = authSession.getClientNotes();
        String loginHint = queryParams.get("login_hint");
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        String deviceId = loginHint == null || !loginHint.startsWith("device-id:") ? "" : loginHint.substring("device-id:".length());

        if (deviceId.isEmpty()) {
            String defaultDeviceId = config.getConfig().get(PinCodeAuthenticationConfig.DEFAULT_DEVICE_ID);
            if (defaultDeviceId == null) {
                logger.error("Could not find default device id");
                context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            } else {
                logger.warn("Could not find device id from login hint, using default device id");
                deviceId = defaultDeviceId;
            }
        }

        boolean hideDeviceIdInput = "true".equals(config.getConfig().get(PinCodeAuthenticationConfig.HIDE_DEVICE_ID_INPUT));

        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        Response response = form
                .setAttribute("deviceId", deviceId)
                .setAttribute("hideDeviceIdInput", hideDeviceIdInput)
                .createForm("pin-code-login-form.ftl");

        context.challenge(response);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        KeycloakSession session = context.getSession();
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String deviceId = formData.getFirst("deviceId");
        String pinCode = formData.getFirst("pinCode");
        RealmModel realm = context.getRealm();

        List<UserModel> pinCodeUsers = session
            .users()
            .searchForUserByUserAttributeStream(realm, "pinCode", pinCode)
            .toList();

        if (pinCodeUsers.size() > 1) {
            logger.warn(String.format("Multiple users found with pin code %s", pinCode));
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
            return;
        }

        UserModel pinCodeUser = pinCodeUsers.isEmpty() ? null : pinCodeUsers.get(0);

        if (pinCodeUser == null) {
            logger.warn(String.format("Pin code %s is not valid", pinCode));
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
        }

        try {
            if (!isVerifiedClientApp(deviceId, context.getAuthenticatorConfig())) {
                logger.warn(String.format("Device %s is not verified", deviceId));
                context.failure(AuthenticationFlowError.ACCESS_DENIED);
            } else {
                context.setUser(pinCodeUser);
                context.success();
            }
        } catch (MalformedURLException e) {
            logger.error("Failed to verify client app", e);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
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
     * Verifies that the client app is verified
     *
     * @param deviceId device id
     * @param config authenticator config
     * @return true if the client app is verified
     * @throws MalformedURLException if the user management API URL is invalid
     */
    private boolean isVerifiedClientApp(String deviceId, AuthenticatorConfigModel config) throws MalformedURLException {
        String apiKey = config.getConfig().get(PinCodeAuthenticationConfig.USER_MANAGEMENT_API_KEY);
        String apiUrl = config.getConfig().get(PinCodeAuthenticationConfig.USER_MANAGEMENT_API_URL);

        TmsApiClient tmsApiClient = new TmsApiClient(apiKey, URI.create(apiUrl).toURL());
        ClientAppsApi clientAppsApi = tmsApiClient.getClientAppsApi();

        try {
            return Boolean.TRUE.equals(clientAppsApi.verifyClientApp(new VerifyClientAppRequest().deviceId(deviceId)));
        } catch (ApiException e) {
            return false;
        }
    }

}
