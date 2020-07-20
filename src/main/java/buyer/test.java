package buyer;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.*;

public class test {
    public static void main(String[] args) {
        Map<String,String> map = new HashMap<>();
        map.put("a","1");
        map.put("bbb","aaaa");
        String string = JSON.toJSONString(map);
        System.out.println(string);
        JSONObject stringToMap =  JSONObject.parseObject(string);
        stringToMap.put("a","2");
        System.out.println(stringToMap.toJSONString());
    }
}
