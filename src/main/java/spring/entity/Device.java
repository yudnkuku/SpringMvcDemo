package spring.entity;

public class Device {

    private int deviceId;

    private String deviceName;

    private String deviceType;

    private double latitude;

    private double longitude;

    private String province;

    private String city;

    private String district;

    private String address;

    private String regionName;

    private String regionId;

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public static class Builder {

        private Device device;

        public Builder(String deviceName, int deviceId) {
            device.deviceName = deviceName;
            device.deviceId = deviceId;
        }

        public Builder deviceType(String deviceType) {
            device.deviceType = deviceType;
            return this;
        }

        public Builder latitude(int latitude) {
            device.latitude = latitude;
            return this;
        }

        public Builder longitude(int longitude) {
            device.longitude = longitude;
            return this;
        }

        public Builder province(String province) {
            device.province = province;
            return this;
        }

        public Builder city(String city) {
            device.city = city;
            return this;
        }

        public Builder district(String district) {
            device.district = district;
            return this;
        }

        public Builder address(String address) {
            device.address = address;
            return this;
        }

        public Builder regionName(String regionName) {
            device.regionName = regionName;
            return this;
        }

        public Builder regionId(String regionId) {
            device.regionId = regionId;
            return this;
        }

        public Device build() {
            return device;
        }
    }
}
