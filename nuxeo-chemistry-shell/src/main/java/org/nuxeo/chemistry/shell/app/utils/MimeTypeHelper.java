package org.nuxeo.chemistry.shell.app.utils;

public class MimeTypeHelper {

    private MimeTypeHelper() {
    }

    public static String getMimeType(String fileName) {

        if (fileName == null) {
            return "application/octet-stream";
        } else if (fileName.endsWith(".doc")) {
            return "application/msword";
        } else if (fileName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (fileName.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".xml")) {
            return "text/xml";
        } else if (fileName.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".odt")) {
            return "application/vnd.oasis.opendocument.text";
        } else if (fileName.endsWith(".zip")) {
            return "application/zip";
        } else {
            return "application/octet-stream";
        }
    }
}
