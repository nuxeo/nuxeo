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

    public static String[] listFilesSorted(File file) {
        String[] items = file.list();
        /*
         * Arrays.sort(items, new Comparator<String>() {
         * 
         * @Override public int compare(String o1, String o2) { return
         * o1.toLowerCase().compareTo(o2.toLowerCase()); } });
         */
        return items;

    }
}
