package spring.weapp.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationInfo {

    List<UnifiedAuthorizationInfo> unifiedAuthInfoList;

    List<TemporaryAuthInfo> tempAuthInfoList;

}
