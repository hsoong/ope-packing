package com.ostsound.vendors.ope.packing.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ostsound.vendors.ope.packing.config.Settings;
import com.ostsound.vendors.ope.packing.util.Utils;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@ShellCommandGroup(value = "Commands for packing Operate platform.")
@ShellComponent
public final class PackingCommands {

    final String JAVA = "web/src/main/java";
    final String WEBAPP = "web/src/main/webapp";
    final String RESOURCES = "web/src/main/resources";
    final String WEBINFCLASS = "WEB-INF/classes";

    boolean debug = false;

    @ShellMethod(value = "启用/禁用Debug输出模式. 用法 - debug [--on]", key = "debug")
    public String debug(boolean on) {
        this.debug = on;
        return "Debug Mode: " + on;
    }

    @ShellMethod(value = "显示配置信息. 用法 - config show.", key = "config show")
    public String configShow() {
        return Settings.instance().description();
    }

    @ShellMethod(value = "设置copySourcePrefix属性. 用法 - config set source [value]", key = "config set source")
    public String configSourcePrefix(String value) {
        Settings settings = Settings.instance();
        String oldValue = settings.getCopySourcePrefix();
        settings.setCopySourcePrefix(value);
        return changes("copySourcePrefix", oldValue, value);
    }

    @ShellMethod(value = "设置copyTargetPrefix属性. 用法 - config set target [value]", key = "config set target")
    public String configTargetPrefix(String value) {
        Settings settings = Settings.instance();
        String oldValue = settings.getCopyTargetPrefix();
        settings.setCopyTargetPrefix(value);
        return changes("copyTargetPrefix", oldValue, value);
    }

    @ShellMethod(value = "将当前配置信息持久化到工作区文件", key = "config store")
    public String configStore() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String content = mapper.writeValueAsString(Settings.instance());
            String filepath = Utils.writeFile(content, Settings.CONFIG);
            return "当前设置信息已保存到" + filepath;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return e.getMessage();
        }
    }

    @ShellMethod(value = "生成文件变更脚本", key = "package")
    public String aliasPackage() {
        Settings settings = Settings.instance();
        String errors = settings.validate();
        if (!StringUtils.isEmpty(errors)) {
            return errors;
        }

        Path changesFilePath = Paths.get(Utils.workDir(), settings.getFilename());
        if (!Files.exists(changesFilePath)) {
            return "error: 变更记录文件" + changesFilePath.toString() + "不存在";
        }

        try {
            List<String> lines = Files.readAllLines(changesFilePath);
            boolean valid = true;
            StringBuilder commands = new StringBuilder();

            for (String line : lines) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                }

                String[] flags= line.split("\\s");
                if (flags.length >= 1) {
                    String command = "";
                    String mode = flags[0].toUpperCase();
                    if (Arrays.asList("A", "M", "D").contains(mode) && flags.length == 2) {
                        String filepath = flags[1];
                        String filename = Utils.nameOnly(filepath);

                        if ("".equals(filename)) {
                            echo("[warning] file ignore " + filepath);
                            echo("\n");
                        } else {
                            command = process(Mode.valueOf(mode), filepath);
                        }
                    } else if (mode.startsWith("R") && flags.length == 3) {
                        command = process(flags[1], flags[2]);
                    } else {
                        valid = false;
                    }

                    if (!StringUtils.isEmpty(command)) {
                        commands.append(command);
                        commands.append("\n");
                    }
                } else {
                    valid = false;
                    break;
                }
            }
            if (!valid) {
                return "error: 变更记录文件格式有误";
            } else {
                DateFormatter formatter = new DateFormatter("yyyyMMddHHmmss");
                String timestamp = formatter.print(new Date(), Locale.getDefault());
                Path root = Paths.get(Utils.workDir());
                Path directory = root.resolve(timestamp);

                if (!Files.exists(directory)) {
                    Files.createDirectory(directory);
                }

                Files.copy(root.resolve(settings.getFilename()), directory.resolve(settings.getFilename()));
                Files.write(directory.resolve(String.format("commands-%s.sh", timestamp)), commands.toString().getBytes(Charset.forName("utf8")));
                return "Jobs done. Shell script is placed at " + directory.toString();
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return e.getMessage();
        }
    }

    private void echo(String string) {
        if (debug) {
            System.out.println(string);
        }
    }

    private String changes(String key, String oldValue, String newValue) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isEmpty(oldValue)) {
            builder.append(key + " 新值: " + newValue);
        } else {
            builder.append(key + " 旧值: " + oldValue + ", 新值: " + newValue);
        }
        return builder.toString();
    }

    private String process(String originalPath, String renamedPath) {
        String originalWildFilepath = resolveWildFilepath(originalPath);
        if (StringUtils.isEmpty(originalWildFilepath)) {
            echo("[Warning] 路径解析有误 - " + originalPath);
            return "";
        }

        String renamedWildFilepath = resolveWildFilepath(renamedPath);
        if (StringUtils.isEmpty(renamedWildFilepath)) {
            echo("[Warning] 路径解析有误 - " + renamedPath);
            return "";
        }

        StringBuilder commands = new StringBuilder();
        Settings settings = Settings.instance();
        String component = WEBINFCLASS;
        if (originalPath.contains(WEBAPP)) {
            component = "";
        }
        Path delWildFilepath = Paths.get(settings.getCopyTargetPrefix(), component, originalWildFilepath);
        Path copySourceWildFilepath = Paths.get(settings.getCopySourcePrefix(), component, renamedWildFilepath);
        Path copyTargetWildFilepath = Paths.get(settings.getCopyTargetPrefix(), component, renamedWildFilepath);
        Path copyTargetWildFileParentPath = copyTargetWildFilepath.getParent();

        commands.append(String.format("rm %s", delWildFilepath.toString()));
        commands.append("\n");
        commands.append(String.format("cp %s %s", copySourceWildFilepath.toString(), copyTargetWildFileParentPath.toString()));
        commands.append("\n");

        echo(commands.toString());

        return commands.toString();
    }

    private String process(Mode mode, String filepath) {
        String wildFilepath = resolveWildFilepath(filepath);
        if (StringUtils.isEmpty(wildFilepath)) {
            return "";
        }

        String component = WEBINFCLASS;
        if (filepath.contains(WEBAPP)) {
            component = "";
        }
        Settings settings = Settings.instance();

        Path copySourceWildFilepath = Paths.get(settings.getCopySourcePrefix(), component, wildFilepath);
        Path copyTargetWildFilepath = Paths.get(settings.getCopyTargetPrefix(), component, wildFilepath);
        Path copyTargetWildFileParentPath = copyTargetWildFilepath.getParent();

        StringBuilder commands = new StringBuilder();
        switch (mode) {
            case A:
                commands.append(String.format("mkdir -p %s", copyTargetWildFileParentPath.toString()));
                commands.append("\n");
                commands.append(String.format("cp %s %s", copySourceWildFilepath.toString(), copyTargetWildFileParentPath.toString()));
                commands.append("\n");
                break;
            case D:
                commands.append(String.format("rm %s", copyTargetWildFilepath.toString()));
                commands.append("\n");
                break;
            case M:
                commands.append(String.format("rm %s", copyTargetWildFilepath.toString()));
                commands.append("\n");
                commands.append(String.format("cp %s %s", copySourceWildFilepath.toString(), copyTargetWildFileParentPath.toString()));
                commands.append("\n");
                break;
            default:
                break;
        }

        echo(commands.toString());

        return commands.toString();
    }

    private String resolveWildFilepath(String filepath) {
        int idx = -1;
        if (filepath.contains(JAVA)) {
            idx = filepath.indexOf(JAVA) + JAVA.length();
        } else if (filepath.contains(WEBAPP)) {
            idx = filepath.indexOf(WEBAPP) + WEBAPP.length();
        } else if (filepath.contains(RESOURCES)) {
            idx = filepath.indexOf(RESOURCES) + RESOURCES.length();
        }
        if (idx >= 0) {
            String relative = filepath.substring(idx);
            String filename = Utils.nameOnly(relative);
            String wildFilename = filename + "*";
            String wildFilepath = Paths.get(relative).getParent().resolve(wildFilename).toString();
            return wildFilepath;
        } else {
            return "";
        }
    }

    enum Mode {
        A, M, D, R
    }


}
