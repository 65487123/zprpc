
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 *
 * @author: Lu ZePing
 * @date: 2020/9/29 14:20
 */
public class Test {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        List<Object> list = new ArrayList<>();
        list.add("213");
        list.add("213");
        list.add("213");
        long now = System.currentTimeMillis();
        for (int i=0;i<100000;i++){
            //JSON.parseObject(JSON.toJSONString(responseDTO), Object.class);
        }
        System.out.println(System.currentTimeMillis() - now);


    }
}