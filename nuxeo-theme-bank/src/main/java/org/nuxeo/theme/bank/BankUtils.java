package org.nuxeo.theme.bank;

import java.io.File;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;

public class BankUtils {

    public static String getFileContent(File file) {
        String content = "";
        try {
            content = FileUtils.readFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}
