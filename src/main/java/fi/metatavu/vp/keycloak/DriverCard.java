package fi.metatavu.vp.keycloak;

/**
 * Driver card entity
 */
public class DriverCard {

    private String driverCardId;
    private String truckVin;

    /**
     * Returns driver card id
     *
     * @return driver card id
     */
    public String getDriverCardId() {
        return driverCardId;
    }

    /**
     * Sets driver card id
     *
     * @param driverCardId driver card id
     */
    public void setDriverCardId(String driverCardId) {
        this.driverCardId = driverCardId;
    }

    /**
     * Returns truck vin
     *
     * @return truck vin
     */
    public String getTruckVin() {
        return truckVin;
    }

    /**
     * Sets truck vin
     *
     * @param truckVin truck vin
     */
    public void setTruckVin(String truckVin) {
        this.truckVin = truckVin;
    }

}
