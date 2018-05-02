package pers.philips.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Constants {
    static String path;
    static final Properties confPro = new Properties();
    static{
        try {
            path = File.separator + new File(".").getCanonicalPath()+File.separator;
            confPro.load(new FileInputStream(path+"conf"+File.separator+"conf.properties"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private Constants(){
        throw new IllegalAccessError("Utility class");
    }

    //配置文件所在目录
    //private static final String CONF_DIR = "conf"+File.separator;
    private static final String INPUT_DIR = "inputFiles"+File.separator;
    public static final String SERVER_FILE = path + INPUT_DIR + File.separator + "server.txt";
    public static final String MODEL_FILE = path + INPUT_DIR + File.separator + "model.txt";
    public static final String ATTACH_FILE = path + INPUT_DIR + File.separator + "attach.txt";
    public static final String DETACH_FILE = path + INPUT_DIR + File.separator + "detach.txt";
    public static String getProperty(String str) {
        return confPro.getProperty(str);
    }

}
