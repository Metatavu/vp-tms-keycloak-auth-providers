package fi.metatavu.vp.keycloak;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * Driver card authenticator factory
 */
public class DriverCardAuthenticatorFactory implements AuthenticatorFactory {
    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES;

    @Override
    public String getDisplayType() {
        return "Driver Card Authenticator";
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
        return "Driver Card Authenticator";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
            .property()
            .name(DriverCardAuthenticationConfig.VEHICLE_MANAGEMENT_API_KEY)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Vehicle Management API Key")
            .helpText("API key for vehicle management")
            .add()
            .property()
            .name(DriverCardAuthenticationConfig.VEHICLE_MANAGEMENT_API_URL)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Vehicle Management API URL")
            .helpText("URL for vehicle management API")
            .add()
            .property()
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .name(DriverCardAuthenticationConfig.HIDE_TRUCK_VIN_INPUT)
            .label("Hide Truck VIN Input")
            .helpText("Hide truck VIN input in login form.")
            .defaultValue("true")
            .add()
            .property()
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .name(DriverCardAuthenticationConfig.AUTO_LOGIN)
            .label("Auto Login")
            .helpText("Automatically login user if vin number is entered.")
            .defaultValue("true")
            .add()
            .property()
            .type(ProviderConfigProperty.STRING_TYPE)
            .name(DriverCardAuthenticationConfig.AUTO_LOGIN_INTERVAL)
            .label("Auto Login Interval")
            .helpText("Auto login interval in milliseconds.")
            .defaultValue("1000")
            .add()
            .property()
            .type(ProviderConfigProperty.STRING_TYPE)
            .name(DriverCardAuthenticationConfig.DEFAULT_TRUCK_VIN)
            .label("Default Truck VIN")
            .helpText("Default truck VIN to use for auto login. This is for testing purposes only.")
            .add()
            .build();
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession) {
        return new DriverCardAuthenticator();
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
        return "driver-card-authenticator";
    }

    static {
        REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED};
    }
}
