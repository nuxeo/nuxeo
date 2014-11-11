package org.nuxeo.theme.resources;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.FileUtils;

public class BankUtils {

    final static Pattern filenamePattern = Pattern.compile("^\\p{IsAlnum}+[a-z0-9_\\-\\. ]*\\p{IsAlnum}+$|^\\p{IsAlnum}$");

    public static String getFileContent(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }
        return FileUtils.readFile(file);
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

    public static boolean checkFilePath(String path) {
        for (String f : path.split("/")) {
            if ("".equals(f)) {
                continue;
            }
            Matcher m = filenamePattern.matcher(f);
            if (!m.find()) {
                return false;
            }
        }
        return true;
    }
}
