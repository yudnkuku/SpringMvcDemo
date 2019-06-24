package spring.weapp.bean;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class Device {

    @NonNull
    private String devid;

    @NonNull
    private String devname;

    private String devtype;

    private String address;

    private List<MasterInfo> masterinfolist;

}
