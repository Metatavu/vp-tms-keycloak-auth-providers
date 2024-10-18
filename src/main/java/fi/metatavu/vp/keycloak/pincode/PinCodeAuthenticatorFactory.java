package fi.metatavu.vp.keycloak.pincode;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * PIN Code authenticator factory
 */
public class PinCodeAuthenticatorFactory implements AuthenticatorFactory {
    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES;

    @Override
    public String getDisplayType() {
        return "PIN Code Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return "vp";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "PIN Code Authenticator";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
            .property()
            .name(PinCodeAuthenticationConfig.USER_MANAGEMENT_API_KEY)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("User Management API Key")
            .helpText("API key for user management service")
            .add()

            .property()
            .name(PinCodeAuthenticationConfig.USER_MANAGEMENT_API_URL)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("User Management API URL")
            .helpText("URL for user management API service")
            .add()

            .property()
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .name(PinCodeAuthenticationConfig.HIDE_DEVICE_ID_INPUT)
            .label("Hide Device Id Input")
            .helpText("Hide device id input in login form. Device id should only be visible for testing purposes.")
            .defaultValue("true")
            .add()

            .property()
            .type(ProviderConfigProperty.STRING_TYPE)
            .name(PinCodeAuthenticationConfig.DEFAULT_DEVICE_ID)
            .label("Default Device ID")
            .helpText("Default device id. This is for testing purposes only.")
            .add()
            .build();
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
        return new PinCodeAuthenticator();
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "pin-code-authenticator";
    }

    static {
        REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED};
    }
}
