//package demo;
//
//import java.text.DecimalFormat;
//import java.util.Date;
//
//public class ConvertTool {
//
//
//        /**
//         * byte数组复制，将src里面的数据复制到tar
//         * @param src
//         * @param srcStart
//         * @param tar
//         * @param tarStart
//         * @param length
//         */
//        public static void byteCopy(byte[] src, Integer srcStart,
//                                    byte[] tar, Integer tarStart, Integer length) {
//            assert(length != null);
//            if (tarStart >= tar.length ||
//                    (srcStart+length) > src.length ||
//                    (tarStart+length) > tar.length) {
//                assert(false);
//                return;
//            }
//            if (srcStart == null) {
//                srcStart = 0;
//            }
//
//            int index = 0;
//            while (index < length) {
//                tar[tarStart + index] = src[srcStart + index];
//                index ++;
//            }
//        }
//
//        /**
//         * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToInt（）配套使用
//         * @param value	要转换的int值
//         * @return byte数组
//         */
//        public static byte[] intToByte4(int value) {
//            byte[] targets = new byte[4];
//            targets[0] = (byte) ((value >> 0) & 0xff);// 最低位
//            targets[1] = (byte) ((value >> 8) & 0xff);// 次低位
//            targets[2] = (byte) ((value >> 16) & 0xff);// 次高位
//            targets[3] = (byte) (value >> 24);// 最高位,无符号右移。
//            return targets;
//        }
//        public static byte[] intToByte2(int value) {
//            byte[] targets = new byte[2];
//            targets[0] = (byte) ((value >> 0) & 0xff);// 低位
//            targets[1] = (byte) ((value >> 8) & 0xff);// 高位
//            return targets;
//        }
//        public static byte[] intToByte1(int value) {
//            byte[] targets = new byte[1];
//            targets[0] = (byte) ((value >> 0) & 0xff);// 低位
//            return targets;
//        }
//
//        /**
//         *
//         * @Title: swapEndian
//         * @Description: 字节充切换
//         * @param: @param src
//         * @param: @param byteLen
//         * @param: @return
//         * @return: byte[]
//         * @author:luowei
//         * @date:2018年6月20日 上午10:58:53
//         */
//        public static byte[] swapEndian(byte[] src, int byteLen) {
//            if(byteLen > 0) {
//                byte[] targets = new byte[byteLen];
//                for(int i=0; i<byteLen; i++) {
//                    targets[byteLen-i-1] = src[i];
//                }
//                return targets;
//            }
//            else return src;
//        }
//
//        /**
//         * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
//         * @param src
//         * @param offset
//         * @return
//         */
//        public static int bytes4ToInt(byte[] src, int offset) {
//            int value;
//            value = (int) ((src[offset] & 0xFF<<0)
//                    | ((src[offset+1] & 0xFF)<<8)
//                    | ((src[offset+2] & 0xFF)<<16)
//                    | ((src[offset+3] & 0xFF)<<24));
//            return value;
//        }
//        public static int bytes2ToInt(byte[] src, int offset) {
//            int value;
//            value = (int) ((src[offset] & 0xFF<<0)
//                    | ((src[offset+1] & 0xFF)<<8));
//            return value;
//        }
//        public static int bytes1ToInt(byte src) {
//            int value;
//            value = (int) ((src & 0xFF<<0));
//            return value;
//        }
//
//        /**
//         * str的长度如果不足len，则前缀补0
//         * @param str
//         * @param len
//         * @return
//         */
//        public static String plusPreZero(String str, int len) {
//            StringBuffer sb = new StringBuffer();
//            for (int i = 0; i < len - str.length(); i ++) {
//                sb.append("0");
//            }
//            sb.append(str);
//
//            return sb.toString();
//        }
//
//        /**
//         * @功能: 10进制串转为BCD码 ，再转为16进制，再转为byte[]。如果16进制字符串长度为奇数，则末尾补0
//         * @参数: 10进制串
//         * @结果: BCD码
//         */
//        public static byte[] str2Bcd(String asc) {
//            StringBuffer hexSB = new StringBuffer(asc);
//            if (asc.length() % 2 == 1) {
//                hexSB.append("0");
//            }
//            byte[] result = new byte[hexSB.length() / 2];
//
//            String hexStr = hexSB.toString();
//            int index = 0;
//            while (index + 2 <= hexStr.length()) {
//                String hex = hexStr.substring(index, index + 2);
//                byte[] srcBytes = HexString.HexString2Bytes(hex);
//                result[index/2] = srcBytes[0];
//                index += 2;
//            }
//
//            return result;
//        }
//
//        /**
//         * @功能: byte[]转16进制，再转BCD码，再转10进制
//         * @参数: BCD码
//         * @结果: 10进制串
//         */
//        public static String bcd2Str(byte[] bytes) {
//            String str = HexString.printHexString(bytes);
//            return str;
//        }
//
//        /**
//         * @Description: byte字节转8位bit
//         * @param byteData
//         * @return
//         * @return: String
//         * @author: wy
//         * @date: 2017年5月16日
//         */
//        public static String byte2Bit(byte byteData){
//            StringBuffer sb = new StringBuffer();
//            sb.append((byteData>>7)&0x1)
//                    .append((byteData>>6)&0x1)
//                    .append((byteData>>5)&0x1)
//                    .append((byteData>>4)&0x1)
//                    .append((byteData>>3)&0x1)
//                    .append((byteData>>2)&0x1)
//                    .append((byteData>>1)&0x1)
//                    .append((byteData>>0)&0x1);
//            return sb.toString();
//        }
//
//        /**
//         * @Description: bit转byte
//         * @param bit
//         * @return
//         * @return: byte
//         * @author: wy
//         * @date: 2017年5月16日
//         */
//        public static byte bitToByte(String bit) {
//            int re, len;
//            if (null == bit) {
//                return 0;
//            }
//            len = bit.length();
//            if (len != 4 && len != 8) {
//                return 0;
//            }
//            if (len == 8) {// 8 bit处理
//                if (bit.charAt(0) == '0') {// 正数
//                    re = Integer.parseInt(bit, 2);
//                } else {// 负数
//                    re = Integer.parseInt(bit, 2) - 256;
//                }
//            } else {//4 bit处理
//                re = Integer.parseInt(bit, 2);
//            }
//            return (byte) re;
//        }
//
//        /**
//         * @Description: 加速度传感器三轴坐标算倾角
//         * @param x
//         * @param y
//         * @param z
//         * @return
//         * @return: float
//         * @author: wy
//         * @date: 2017年5月16日
//         */
//        public static float angle(int x, int y, int z) {
//            return 90;
//        }
//
//        /**
//         * @Description: 温度的计算
//         * @param Byte[]
//         * @return
//         * @return: float
//         * @author: huating
//         * @date: 2017年6月6日
//         */
//        public static float temp(byte[] bytes) {
//            String bits = "";
//            int temp = 0;
//            for(int i = bytes.length-1 ;i >=0 ; i--){
//                bits = bits + byte2Bit(bytes[i]);
//            }
//            if(bits.length()!=16){
//                return 0xFFFF;
//            }
//            if(bits.charAt(0)=='1'){
//                temp = 32768 - Integer.parseInt(bits,2);
//            }else{
//                temp = bytes2ToInt(bytes, 0);
//            }
//            DecimalFormat df = new DecimalFormat("0.00");//格式化小数
//            String num = df.format((float)temp/256);
//            return Float.parseFloat(num);
//        }
//        public static byte[] temp2byte(String temp){
//            int temperature = (int)(Float.parseFloat(temp)*256);
//            if(temperature < 0){
//                temperature = 32768 + temperature*(-1);
//            }
//            byte[] temps = intToByte2(temperature);
//            return temps;
//        }
//
//        /**
//         * @Description: 时间信息的转化
//         * @param Byte[]
//         * @return
//         * @return: String
//         * @author: huating
//         * @date: 2017年6月7日
//         */
//        public static String translateDate(byte[] bytes) {
//            int year = bytes1ToInt(bytes[0])+2000;
//            int month = bytes1ToInt(bytes[1]);
//            int day = bytes1ToInt(bytes[2]);
//            int hour = bytes1ToInt(bytes[3]);
//            int min = bytes1ToInt(bytes[4]);
//            int sec = bytes1ToInt(bytes[5]);
//            String date = year+"-"+month+"-"+day+" "+hour+":"+min+":"+sec;
//            date = DateUtils.dataformat(date, DateUtils.DATE_PATTERN_DB);
//            return date;
//        }
//        /**
//         * @Description: 设备编码的转化
//         * @param Byte[]
//         * @return
//         * @return: char
//         * @author: huating
//         * @date: 2017年6月9日
//         */
//        public static char byteToChar(byte[] b) {
//            char c = (char) (((b[0] & 0xFF) << 8) | (b[1] & 0xFF));
//            return c;
//        }
//
//        /**
//         * @Description: 设备编码的转化
//         * @param Byte[]
//         * @return
//         * @return: String
//         * @author: huating
//         * @date: 2017年6月9日
//         */
//        public static String translateCode(byte[] bytes) {
//            StringBuffer str = new StringBuffer();
//            for(int i = 0 ; i < bytes.length ;){
//    		/*char code = (char) bytes[i];
//    		str.append(code);*/
//                char c = (char) (((bytes[i] & 0xFF) << 8) | (bytes[i+1] & 0xFF));
//                str.append(c);
//                i=i+2;
//            }
//            return str.toString();
//        }
//
//        /**
//         * @Description: 时间信息的转化byte[]
//         * @param String
//         * @return
//         * @return: byte[]
//         * @author: huating
//         * @date: 2017年6月9日
//         */
//        public static byte[] Date2byte(String date) {
//            byte[] time = new byte[Constant.LEN_TIME];
//            //date = date.substring(2);
//            String year = date.substring(2,4);
//            String mon = date.substring(4,6);
//            String day = date.substring(6,8);
//            String hour = date.substring(8,10);
//            String min = date.substring(10,12);
//            String sec = date.substring(12,14);
//            byteCopy(intToByte1(Integer.parseInt(year)), 0, time, 0, 1);
//            byteCopy(intToByte1(Integer.parseInt(mon)), 0, time, 1, 1);
//            byteCopy(intToByte1(Integer.parseInt(day)), 0, time, 2, 1);
//            byteCopy(intToByte1(Integer.parseInt(hour)), 0, time, 3, 1);
//            byteCopy(intToByte1(Integer.parseInt(min)), 0, time, 4, 1);
//            byteCopy(intToByte1(Integer.parseInt(sec)), 0, time, 5, 1);
//            return time;
//        }
//        //授权时间转换，YYMMDDHH，年减2000， YY[0-99], MM[1-12] , DD[1-31] , HH[0-23]  4字节
//        public static byte[] AppDate2byte(String date) {
//
//            byte[] time = new byte[AppConstant.LEN_GRANT_TIME];
//            //date = date.substring(2);2017-12-23 12:10:20
//            String year = date.substring(2,4);
//            String mon = date.substring(5,7);
//            String day = date.substring(8,10);
//            String hour = date.substring(11,13);
//            byteCopy(intToByte1(Integer.parseInt(year)), 0, time, 0, 1);
//            byteCopy(intToByte1(Integer.parseInt(mon)), 0, time, 1, 1);
//            byteCopy(intToByte1(Integer.parseInt(day)), 0, time, 2, 1);
//            byteCopy(intToByte1(Integer.parseInt(hour)), 0, time, 3, 1);
//            return time;
//        }
//
//        /**
//         *
//         * @Description: 日期转4字节的时间戳
//         * @param date
//         * @return
//         * @return: byte[]
//         * @author: huating
//         * @date: 2018年2月6日
//         */
//        public static byte[] dateToMill(Date date){
//            long mill = DateUtils.getMillis(date);
//            String mills = mill+"";
//            mills = mills.substring(0,mills.length()-3);
//            return intToByte4(Integer.parseInt(mills));
//        }
//
//        /**
//         *
//         * @Description: 4字节的时间戳转日期
//         * @param mills
//         * @return
//         * @return: Date
//         * @author: huating
//         * @date: 2018年2月6日
//         */
//        public static Date millToDate(byte[] mills){
//            int mill = bytes4ToInt(mills, 0);
//            String time = StringUtil.fillZeroTail(mill+"", 13);
//            Date date = DateUtils.getDate(Long.parseLong(time));
//            return date;
//        }
//
//        /**
//         * @Description: ip地址的转化
//         * @param String
//         * @return
//         * @return: byte[]
//         * @author: huating
//         * @date: 2017年6月9日
//         */
//        public static byte[] Ip2byte(String ip) {
//            byte[] ipInfo = new byte[Constant.LEN_MONITOR_SERVER_IP];
//            String[] ips = ip.split("\\.");
//            for(int i = 0; i < ips.length ;i++ ){
//                byte[] ip2 = intToByte1(Integer.parseInt(ips[i]));
//                byteCopy(ip2, 0, ipInfo, i, ip2.length);
//            }
//            return ipInfo;
//        }
//
//        public static String byte2Ip(byte[] bytes) {
//            StringBuffer str = new StringBuffer();
//            for(int i=0 ; i < bytes.length ; i++){
//                int ip = bytes1ToInt(bytes[i]);
//                str.append(ip);
//                if(i < bytes.length-1){
//                    str.append(".");
//                }
//            }
//            return str.toString();
//        }
//
//        /**
//         * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在后，高位在前)的顺序。
//         * @param value	要转换的int值
//         * @return byte数组
//         */
//        public static byte[] int2Byte4(int value) {
//            byte[] targets = new byte[4];
//            targets[3] = (byte) ((value >> 0) & 0xff);// 最低位
//            targets[2] = (byte) ((value >> 8) & 0xff);// 次低位
//            targets[1] = (byte) ((value >> 16) & 0xff);// 次高位
//            targets[0] = (byte) (value >> 24);// 最高位,无符号右移。
//            return targets;
//        }
//        public static byte[] int2Byte2(int value) {
//            byte[] targets = new byte[2];
//            targets[1] = (byte) ((value >> 0) & 0xff);// 低位
//            targets[0] = (byte) ((value >> 8) & 0xff);// 高位
//            return targets;
//        }
//
//        /**
//         * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序，和int2Bytes（）配套使用
//         * @param src
//         * @param offset
//         * @return
//         */
//        public static int bytes42Int(byte[] src, int offset) {
//            int value;
//            value = (int) ((src[offset+3] & 0xFF<<0)
//                    | ((src[offset+2] & 0xFF)<<8)
//                    | ((src[offset+1] & 0xFF)<<16)
//                    | ((src[offset] & 0xFF)<<24));
//            return value;
//        }
//        public static int bytes22Int(byte[] src, int offset) {
//            int value;
//            value = (int) ((src[offset+1] & 0xFF<<0)
//                    | ((src[offset] & 0xFF)<<8));
//            return value;
//        }
//
//        /**
//         *
//         * @Description: 转换controllerId
//         * ID编号规则：A-BBBB-CCCC-DDDD-EEEEEEE
//         * A: 设备类型3Bit表示：1->控制器，2->电子钥匙，3->锁芯，4->APP，5->调测工具
//         * BBBB：项目流水号13bit表示，从0开始填，最大值为8191(0x1FFF)
//         * CCCC：生产日期16bit表示，格式为年周，如2017年50周，则数据填1750
//         * DDDD：区域码12bit表示：区域码分为二段，对应市6bit[0,63]、区6bit[0,63]
//         * EEEEEEE：生产流水号20bit表示，从0开始，最大值为1048575(0xFFFFF)
//         * @param String
//         * @return: byte
//         * @author: huating
//         * @date: 2018年2月5日
//         */
//        public static byte[] controllerIdToByte(String controllerId){
//            byte[] id = new byte[8];
//            String A = controllerId.substring(0, 1);
//            String B = controllerId.substring(1, 5);
//            String C = controllerId.substring(5, 9);
//            String D1 = controllerId.substring(9, 11);
//            String D2 = controllerId.substring(11, 13);
//            String E = controllerId.substring(13);
//            byte byteA = (byte)Integer.parseInt(A);
//            String bitA = byte2Bit(byteA).substring(5);
//            byte[] byteB = int2Byte2(Integer.parseInt(B));
//            String bitB1 = byte2Bit(byteB[1]).substring(3);
//            id[0] = bitToByte(bitB1 + bitA);
//            id[1] = bitToByte(byte2Bit(byteB[0]).substring(3) + byte2Bit(byteB[1]).substring(0,3));
//            byte[] byteC = int2Byte2(Integer.parseInt(C));
//            id[2] = byteC[1];
//            id[3] = byteC[0];
//            byte byteD1 = (byte)Integer.parseInt(D1);
//            byte byteD2 = (byte)Integer.parseInt(D2);
//            String bitD1 = byte2Bit(byteD1).substring(2);
//            String bitD2 = byte2Bit(byteD2).substring(2);
//            id[4] = bitToByte(bitD2.substring(4, 6) + bitD1);
//            byte[] byteE = int2Byte4(Integer.parseInt(E));
//            String bitE = byte2Bit(byteE[1]).substring(4) + byte2Bit(byteE[2]) + byte2Bit(byteE[3]);
//            id[5] = bitToByte(bitE.substring(16) + bitD2.substring(0, 4));
//            id[6] = bitToByte(bitE.substring(8,16));
//            id[7] = bitToByte(bitE.substring(0,8));
//            return id;
//        }
//
//
//        /**
//         *
//         * @Description: 转换controllerId
//         * ID编号规则：A-BBBB-CCCC-DDDD-EEEEEEE
//         * A: 设备类型3Bit表示：1->控制器，2->电子钥匙，3->锁芯，4->APP，5->调测工具
//         * BBBB：项目流水号13bit表示，从0开始填，最大值为8191(0x1FFF)
//         * CCCC：生产日期16bit表示，格式为年周，如2017年50周，则数据填1750
//         * DDDD：区域码12bit表示：区域码分为二段，对应市6bit[0,63]、区6bit[0,63]
//         * EEEEEEE：生产流水号20bit表示，从0开始，最大值为1048575(0xFFFFF)
//         * @param byte[]
//         * @return: String
//         * @author: huating
//         * @date: 2018年2月5日
//         */
//        public static String byteToControllerId(byte[] controllerId){
//            String id = "";
//            String bit1 = byte2Bit(controllerId[0]);
//            String bit2 = byte2Bit(controllerId[1]);
//            String bit3 = byte2Bit(controllerId[2]);
//            String bit4 = byte2Bit(controllerId[3]);
//            String bit5 = byte2Bit(controllerId[4]);
//            String bit6 = byte2Bit(controllerId[5]);
//            String bit7 = byte2Bit(controllerId[6]);
//            String bit8 = byte2Bit(controllerId[7]);
//            Integer A = Integer.parseInt("0"+bit1.substring(5, 8), 2);
//            Integer B = Integer.parseInt(bit2 + bit1.substring(0, 5), 2);
//            Integer C = Integer.parseInt(bit4 + bit3, 2);
//            Integer D1 = Integer.parseInt("00"+bit5.substring(2, 8), 2);
//            Integer D2 = Integer.parseInt("00" + bit6.substring(4, 8) + bit5.substring(0, 2), 2);
//            Integer E = Integer.parseInt(bit8 + bit7 + bit6.substring(0,4), 2);
//            id = id + plusPreZero(A.toString(), 1);
//            id = id + plusPreZero(B.toString(), 4);
//            id = id + plusPreZero(C.toString(), 4);
//            id = id + plusPreZero(D1.toString(), 2);
//            id = id + plusPreZero(D2.toString(), 2);
//            id = id + plusPreZero(E.toString(), 7);
//            return id;
//        }
//
//        /**
//         *
//         * @Description: 根据设备id算出哈希值，用作授权删除操作
//         * @param bytes
//         * @return
//         * @return: byte[]
//         * @author: huating
//         * @date: 2018年3月29日
//         */
//        public static byte[] authCalcHashValue(byte[] bytes) {
//            int hash = 0;
//            if(bytes == null || bytes.length == 0) {
//                return intToByte4(hash);
//            }
//            for(int i = 0; i < bytes.length; i++){
//                hash = bytes1ToInt(bytes[i]) + hash*31;
//            }
//            return intToByte4(hash);
//        }
//
//        public static void main(String args[]){
//            byte[] b1 = controllerIdToByte("30888181263630000000");
//            byte[] b2 = controllerIdToByte("30888181263630000010");
//            byte[] bytes1 = new byte[16];
//            System.out.println(byteToControllerId(controllerIdToByte("30888181263630000010")));
//            System.out.println(HexString.printHexString(controllerIdToByte("70002183601010000001")));
//            System.out.println(byteToControllerId(HexString.HexString2Bytes("17002C0741100000")));
//            //byte[] bytes = authCalcHashValue();
//            //System.out.println(HexString.printHexString(bytes));
//        }
//
//}
