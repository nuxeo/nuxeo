package org.nuxeo.template.processors.xdocreport;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.nuxeo.ecm.core.api.Blob;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class ZipXmlHelper {

    protected static final int BUFFER_SIZE = 1024 * 64; // 64K

    public static final String OOO_MAIN_FILE = "content.xml";

    public static final String DOCX_MAIN_FILE = "word/document.xml";

    public static String readXMLContent(Blob blob, String filename)
            throws Exception {
        ZipInputStream zIn = new ZipInputStream(blob.getStream());
        ZipEntry zipEntry = zIn.getNextEntry();
        String xmlContent = null;
        while (zipEntry != null) {
            if (zipEntry.getName().equals(filename)) {
                StringBuilder sb = new StringBuilder();
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = zIn.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, read));
                }
                xmlContent = sb.toString();
                break;
            }
            zipEntry = zIn.getNextEntry();
        }
        zIn.close();
        return xmlContent;
    }

}
