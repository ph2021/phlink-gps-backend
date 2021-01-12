package com.xd.pre.common.utils;

import com.xd.pre.common.exception.PreBaseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {
    public static void copy(InputStream input, OutputStream os) {
        try {
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                os.write(buf, 0, bytesRead);
            }

        } catch (Exception ex) {
            throw new PreBaseException("文件复制出错" + ex);
        } finally {
            try {
                input.close();
                os.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
}
