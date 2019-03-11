package spring.web;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import spring.entity.Device;
import spring.entity.ResponseBean;

import java.util.Map;

@Controller
@RequestMapping("/device")
public class DeviceController {

    @RequestMapping(value = "/queryDevice", method = RequestMethod.POST)
    @ResponseBody
    public ResponseBean getDeviceListById(@RequestBody Map<String, Object> req) {
        ResponseBean resp = new ResponseBean();
        String deviceName = (String) req.get("deviceName");
        int deviceId = (Integer) req.get("deviceId");
        Device.Builder deviceBuilder = new Device.Builder(deviceName, deviceId);
        Device device = deviceBuilder.province("hubei").city("wuhan")
                .regionName("hongshan").build();
        resp.setData(device);
        return resp;
    }


}
