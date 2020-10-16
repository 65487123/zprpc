import com.lzp.dtos.ResponseDTO;
import com.lzp.util.ResponseSearialUtil;

/**
 * Description:
 *
 * @author: Zeping Lu
 * @date: 2020/10/15 13:18
 */
public class Test {
    static class ThreadWithTime {
        /**
         * Description:long 基本类型存过期时间， 占8个字节
         */
        private long deadLine;
        /**
         * Description:线程对象的引用， 指针压缩后占4个字节
         * 属性总共12字节，加上指针压缩后的对象头12字节， 刚好24字节，不需要对其填充。
         */
        private Thread blockedThread;

        public ThreadWithTime(long deadLine, Thread blockedThread) {
            this.deadLine = deadLine;
            this.blockedThread = blockedThread;
        }
    }
    public static void main(String[] args) {
        ResponseDTO responseDTO =new ResponseDTO();
        long now = System.currentTimeMillis();

        for (int i = 0 ;i<10000000;i++){
            ResponseSearialUtil.deserialize(ResponseSearialUtil.serialize(responseDTO));
        }
        System.out.println(System.currentTimeMillis()-now);
    }
}
