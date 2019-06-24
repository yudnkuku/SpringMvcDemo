package spring.weapp.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedAuthorizationInfo {

    private String strategy;

    private String devname;

    private String doorno;

    private String timeStrategy;

    private String availableTime;

    private String unavailableTime;

    private String remark;

}
