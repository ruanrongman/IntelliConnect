package top.rslly.iot.utility;

import java.io.IOException;
import java.util.List;

public class JsonCreate {
    public static StringBuffer create(List<String> key, List<String> value, List<String> type) throws IOException {
        if(key.size()==value.size()&&value.size()== type.size()){
            StringBuffer sBuffer = new StringBuffer("{");
            for (int i = 0; i < key.size(); i++) {
                sBuffer.append("\"");
                sBuffer.append(key.get(i));
                sBuffer.append("\"");
                sBuffer.append(":");
                if (type.get(i).equals("string")) sBuffer.append("\"");
                sBuffer.append(value.get(i));
                if (type.get(i).equals("string")) sBuffer.append("\"");
                if (i < key.size() - 1)
                    sBuffer.append(",");
                else break;
            }
            sBuffer.append("}");
            return sBuffer;
        }
        else throw new IOException("禁止三个输入长度不等的String数组！！！");
    }
}
