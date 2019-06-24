package spring.weapp.bean;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class InstallationDevice {

    @NonNull
    private String devid;

    @NonNull
    private String devname;

    @NonNull
    private String devtype;

    private String latitude;

    private String longitude;

    private String province;

    private String city;

    private String district;

    private String address;

    private String regionname;

    private String regionid;

    private String devstate;

    private String isfilock;
}
