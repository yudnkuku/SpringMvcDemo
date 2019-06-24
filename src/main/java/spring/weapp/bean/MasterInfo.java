package spring.weapp.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MasterInfo {

    private String hostid;

    private String macaddr;

    private List<Sensor> sensorlist;

}
