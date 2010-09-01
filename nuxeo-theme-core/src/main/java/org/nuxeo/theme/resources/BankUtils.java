package org.nuxeo.theme.resources;

import java.io.File;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;

public class BankUtils {

    public static String getFileContent(File file) {
        String content = "";
        if (file.exists()) {
            try {
                content = FileUtils.readFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return content;
    }

    public static String getDomId(String id) {
        return id.replaceAll("[\\s\\.]+", "-");
    }
}
