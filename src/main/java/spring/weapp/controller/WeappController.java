package spring.weapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import spring.weapp.bean.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/weapp")
public class WeappController {

    @PostMapping("/qrcode")
    @ResponseBody
    public ResponseBean scanQrCode(@RequestBody Map<String, String> map) {
        ResponseBean responseBean = new ResponseBean();
        String path = map.get("qrcode");
        if (null == path || "" == path) {
            responseBean.setData(null);
            responseBean.setCode(Code.FAIL);
            responseBean.setMsg("error");
            return responseBean;
        }
        Sensor sensor = new Sensor("door1", "1", "key",
                "id1", 0);
        List<Sensor> sensorList = new ArrayList<>();
        sensorList.add(sensor);
        MasterInfo masterInfo = new MasterInfo("hostid", "34:03:DE:35:5F:1D",
                sensorList);
        List<MasterInfo> masterInfoList = new ArrayList<>();
        masterInfoList.add(masterInfo);
        Device device = new Device("deviceid", "devicename", "0", "fiberhome",
                masterInfoList);
        responseBean.setData(device);
        responseBean.setCode(Code.SUCCESS);
        responseBean.setMsg("success");
        return responseBean;
    }

    @PostMapping("/remoteOpenLock")
    @ResponseBody
    public ResponseBean remoteOpenLock(@RequestBody Map<String, String> map)
            throws InterruptedException {
        ResponseBean responseBean = new ResponseBean();
        String path = map.get("qrcode");
        if (null == path || "" == path) {
            responseBean.setData(null);
            responseBean.setCode(Code.FAIL);
            responseBean.setMsg("error");
            return responseBean;
        }
        Thread.sleep(1000);
        responseBean.setData("远程开锁请求响应");
        responseBean.setCode(Code.SUCCESS);
        responseBean.setMsg("success");
        return responseBean;
    }

    @GetMapping("/getAuthInfo")
    @ResponseBody
    public ResponseBean getAuthInfo() {
        ResponseBean responseBean = new ResponseBean(Code.SUCCESS, "success", null);
        List<UnifiedAuthorizationInfo> uList = new ArrayList<>();
        uList.add(new UnifiedAuthorizationInfo("s1", "dev1",
                "1", "timeStragety",
                "20190403", "20190503", "测试"));
        List<TemporaryAuthInfo> tList = new ArrayList<>();
        tList.add(new TemporaryAuthInfo("dev1", 1, "临时授权",
                "20190403", "20190403", "20190503", "测试"));
        responseBean.setData(new AuthorizationInfo(uList, tList));
        return responseBean;
    }

    @PostMapping("/requestTempAuth")
    @ResponseBody
    public ResponseBean requestTempAuth(@RequestBody Map<String, String> map)
            throws InterruptedException {
        ResponseBean responseBean = new ResponseBean(Code.SUCCESS, "success", null);
        Thread.sleep(1000);
        System.out.println("二维码：" + map.get("qrcode") + "\n授权原因：" + map.get("reason") +
                    "\n授权开始时间：" + map.get("authorizebegintime") + "\n授权结束时间：" +
                    map.get("authorizeendtime"));
        return responseBean;
    }

    @PostMapping("/getDeviceInfo")
    @ResponseBody
    public ResponseBean getDeviceInfo(@RequestBody Map<String, String> map) throws InterruptedException {
        ResponseBean responseBean = new ResponseBean(Code.SUCCESS, "success", null);
        Thread.sleep(1000);
        List<InstallationDevice> list = new ArrayList<>();
        list.add(new InstallationDevice("13172737", "testdevice", "1",
                "30.92", "50.91", "湖北", "武汉", "洪山", "关东创业街",
                "洪山区", "42", "0", "1"));
        responseBean.setData(list);
        return responseBean;
    }

    @PostMapping("/getDevInfoAround")
    @ResponseBody
    public ResponseBean getDevInfoAround(@RequestBody Map<String, String> map) throws InterruptedException {
        ResponseBean responseBean = new ResponseBean(Code.SUCCESS, "success", null);
        Thread.sleep(1000);
        List<InstallationDevice> list = new ArrayList<>();
        InstallationDevice dev1 = new InstallationDevice("1", "device1", "1");
        dev1.setLatitude("30.60276");
        dev1.setLongitude("114.30525");
        dev1.setAddress("湖北省武汉市洪山区创业街烽火通信");
        dev1.setRegionname("洪山区");
        list.add(dev1);
        InstallationDevice dev2 = new InstallationDevice("2", "device2", "0");
        dev2.setLatitude("31.60276");
        dev2.setLongitude("120.30525");
        dev2.setAddress("湖北省武汉市洪山区高新四路烽火通信");
        dev2.setRegionname("洪山区");
        list.add(dev2);
        responseBean.setData(list);
        return responseBean;
    }
}
