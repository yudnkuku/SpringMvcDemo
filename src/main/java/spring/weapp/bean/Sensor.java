package spring.weapp.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sensor {

    private String doorname;

    private String doorno;

    private String lockkey;

    private String lockid;

    private int state;
}
