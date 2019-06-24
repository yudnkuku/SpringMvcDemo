package spring.weapp.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryAuthInfo {

    private String devname;

    private int doorno;

    private String authReason;

    private String requestTime;

    private String availableTime;

    private String unavailableTime;

    private String remark;

}
