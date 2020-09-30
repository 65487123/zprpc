import com.alibaba.fastjson.JSON;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.BeanSerializer;
import com.lzp.protocol.zpproto.RequestDTO;
import com.lzp.protocol.zpproto.ResponseDTO;
import com.lzp.util.RequestSearialUtil;
import com.lzp.util.ResponseSearialUtil;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.objenesis.strategy.InstantiatorStrategy;

import javax.xml.ws.Response;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
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
        RequestDTO requestDTO = new RequestDTO("!23", list);
        ResponseDTO responseDTO = new ResponseDTO(requestDTO);
        long now = System.currentTimeMillis();
        for (int i=0;i<100000;i++){
            ResponseSearialUtil.deserialize(ResponseSearialUtil.serialize(responseDTO));
            //JSON.parseObject(JSON.toJSONString(responseDTO), Object.class);
        }
        System.out.println(System.currentTimeMillis() - now);


    }
}