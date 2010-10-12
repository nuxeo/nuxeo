package org.nuxeo.theme.resources;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

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

    public static File[] listFilesSorted(File folder) {
        if (!folder.isDirectory()) {
            return null;
        }
        File files[] = folder.listFiles();
        Arrays.sort(files, new Comparator() {
            @Override
            public int compare(final Object o1, final Object o2) {
                return new Long(((File) o1).lastModified()).compareTo(new Long(
                        ((File) o2).lastModified()));
            }
        });
        return files;
    }

}
