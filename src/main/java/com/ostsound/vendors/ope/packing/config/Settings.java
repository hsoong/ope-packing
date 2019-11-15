package com.ostsound.vendors.ope.packing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ostsound.vendors.ope.packing.util.Utils;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class Settings {

    private static Logger logger = LoggerFactory.getLogger(Settings.class);
    public static String CONFIG = "config.json";

    private String copySourcePrefix = "";
    private String filename = "changes.txt";
    private String copyTargetPrefix = "";

    private volatile static Settings _instance;

    public static Settings instance() {
        if (_instance == null) {
            synchronized (Settings.class) {
                _instance = new Settings();
            }

            // try load config from file at work directory
            tryLoadConfig();
        }
        return _instance;
    }

    public String description() {
        Settings settings = Settings.instance();
        StringBuilder builder = new StringBuilder();
        output(builder, "workDir - 工作区", Utils.workDir());
        output(builder, "filename - 变更记录文件名", settings.filename);
        output(builder, "copySourcePrefix - 脚本copy命令源路径前缀", settings.copySourcePrefix);
        output(builder, "copyTargetPrefix - 脚本copy命令目标路径前缀", settings.copyTargetPrefix);
        return builder.toString();
    }

    public void output(StringBuilder builder, String key, String value) {
        builder.append(key);
        builder.append(":");
        builder.append("\t\t");
        if (StringUtils.isEmpty(value)) {
            builder.append("未设置！");
        } else {
            builder.append(value);
        }
        builder.append("\n");
    }

    public String validate() {
        List<String> errors = new ArrayList<>();
        if (StringUtils.isEmpty(filename)) {
            errors.add("filename is unset");
        }
        if (StringUtils.isEmpty(copySourcePrefix)) {
            errors.add("copySourcePrefix is unset");
        }
        if (StringUtils.isEmpty(copyTargetPrefix)) {
            errors.add("copyTargetPrefix is unset");
        }
        return StringUtils.collectionToCommaDelimitedString(errors);
    }

    private static void tryLoadConfig() {
        Path path = Paths.get(Utils.workDir(), CONFIG);
        File file = path.toFile();
        if (file.exists()) {
            try {
//                List<String> lines = Files.readAllLines(path, Charset.forName("utf8"));
//                String string = StringUtils.collectionToDelimitedString(lines, "");
                ObjectMapper mapper = new ObjectMapper();
                Settings settings = mapper.readValue(file, Settings.class);
                if (settings != null) {
                    BeanUtils.copyProperties(settings, Settings.instance());
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

}
