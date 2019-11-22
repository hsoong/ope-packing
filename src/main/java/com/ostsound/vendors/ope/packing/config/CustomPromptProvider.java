package com.ostsound.vendors.ope.packing.config;

import com.ostsound.vendors.ope.packing.util.Utils;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomPromptProvider implements PromptProvider {

    @Override
    public AttributedString getPrompt() {
        String prompt = "unknown";

        String workDir = Utils.workDir();
        if (!StringUtils.isEmpty(workDir)) {
            List<String> coms = Arrays.stream(workDir.split(File.separator))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!coms.isEmpty()) {
                if (coms.size() >= 2) {
                    prompt = coms.get(coms.size() - 2).concat(File.separator).concat(coms.get(coms.size() - 1));
                } else {
                    prompt = coms.get(coms.size() - 1);
                }
            }
        }
        prompt = "--- " + prompt + " »";
        return new AttributedString(prompt, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
    }

}
