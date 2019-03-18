package demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import org.junit.Assert;
import org.junit.Test;
import spring.entity.User;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SerializableTest {

    public static void main(String[] agrs) throws Exception {
        File file = new File("D:" + File.separator + "s.text");
        OutputStream os = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(new SerializableObject("str0","srt1"));
        oos.close();

        InputStream is = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(is);
        SerializableObject obj = (SerializableObject) ois.readObject();
        System.out.println(obj.getStr0());
        System.out.println(obj.getStr1());
        ois.close();
    }

    @Test
    public void testJacksonSerialize() throws IOException {
        User user = new User("deacon", "123", "yudnkuku@163.com", new Date());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT,true);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        String serializingStr = mapper.writeValueAsString(user);
        System.out.println("jackson序列化结果为：" + serializingStr);
        Object deserializingObj = mapper.readValue(serializingStr, User.class);
        System.out.println(deserializingObj.getClass().getName());
        System.out.println(deserializingObj.toString());
    }

    /**
     * 序列化枚举类型策略：name() ordinal() toString()，默认name()
     * @throws JsonProcessingException
     */
    @Test
    public void testEnumSerialize() throws JsonProcessingException {
        TestPOJO testPOJO = new TestPOJO();
        testPOJO.setName("name");
        testPOJO.setTestEnum(TestEnum.ENUM01);
        ObjectMapper mapper1 = new ObjectMapper();
        mapper1.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, false);
        mapper1.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, false);
        String jsonStr1 = mapper1.writeValueAsString(testPOJO);
        Assert.assertEquals("{\"testEnum\":\"ENUM01\",\"name\":\"name\"}", jsonStr1);

        ObjectMapper mapper2 = new ObjectMapper();
        mapper2.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        String jsonStr2 = mapper2.writeValueAsString(testPOJO);
        Assert.assertEquals("{\"testEnum\":\"enum_01\",\"name\":\"name\"}", jsonStr2);

        ObjectMapper mapper3 = new ObjectMapper();
        mapper3.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true);
        String jsonStr3 = mapper3.writeValueAsString(testPOJO);
        Assert.assertEquals("{\"testEnum\":0,\"name\":\"name\"}", jsonStr3);

    }

    @Data
    public static class TestPOJO {
        private TestEnum testEnum;

        private String name;
    }

    public static enum TestEnum {
        ENUM01("enum_01"),ENUM02("enum_02"),ENUM03("enum_03");

        private String title;

        TestEnum(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    /**
     * 序列化单元素数组或者集合，是否需要以数组形式显示，默认false，即以数组形式显示[a,b,c]，
     * 如果为true,则显示为a,b,c
     * @throws JsonProcessingException
     */
    @Test
    public void testArrayPOJO() throws JsonProcessingException {
        TestArrayPOJO pojo = new TestArrayPOJO();
        pojo.setName("deacon");
        List<String> list = new ArrayList<>();
        list.add("element1");
        pojo.setList(list);

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr1 = mapper.writeValueAsString(pojo);
        Assert.assertEquals("{\"name\":\"deacon\",\"list\":[\"element1\"]}", jsonStr1);

        mapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, true);
        String jsonStr2 = mapper.writeValueAsString(pojo);
        Assert.assertEquals("{\"name\":\"deacon\",\"list\":\"element1\"}", jsonStr2);
    }

    @Data
    public static class TestArrayPOJO {
        private String name;

        private List<String> list;
    }

}
