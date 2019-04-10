package org.nuxeo.apidoc.introspection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.nuxeo.apidoc.documentation.DefaultDocumentationType;
import org.nuxeo.apidoc.documentation.ResourceDocumentationItem;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;

public class EmbeddedDocExtractor {

    public static final String DOC_PREFIX = "doc/";

    public static final String PARENT_DOC_PREFIX = "doc-parent/";

    public static void extractEmbeddedDoc(ZipFile jarFile, BundleInfoImpl bi)
            throws IOException {

        Enumeration<? extends ZipEntry> entries = jarFile.entries();

        Map<String, ResourceDocumentationItem> localDocs = new HashMap<String, ResourceDocumentationItem>();
        Map<String, ResourceDocumentationItem> parentDocs = new HashMap<String, ResourceDocumentationItem>();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.getName().startsWith(PARENT_DOC_PREFIX)
                    && !entry.isDirectory()) {
                InputStream is = jarFile.getInputStream(entry);
                String content = FileUtils.read(is);
                is.close();
                String name = new Path(entry.getName()).lastSegment();
                if (name.length() >= 6
                        && name.substring(0, 6).equalsIgnoreCase("readme")) {

                    ResourceDocumentationItem docItem = new ResourceDocumentationItem(
                            name, content, bi,
                            DefaultDocumentationType.DESCRIPTION.toString());

                    parentDocs.put(
                            DefaultDocumentationType.DESCRIPTION.toString(),
                            docItem);
                } else {
                    ResourceDocumentationItem docItem = new ResourceDocumentationItem(
                            name, content, bi,
                            DefaultDocumentationType.HOW_TO.toString());
                    parentDocs.put(DefaultDocumentationType.HOW_TO.toString(),
                            docItem);
                }
            }
            if (entry.getName().startsWith(DOC_PREFIX) && !entry.isDirectory()) {
                InputStream is = jarFile.getInputStream(entry);
                String content = FileUtils.read(is);
                is.close();
                String name = new Path(entry.getName()).lastSegment();
                if (name.length() >= 6
                        && name.substring(0, 6).equalsIgnoreCase("readme")) {

                    ResourceDocumentationItem docItem = new ResourceDocumentationItem(
                            name, content, bi,
                            DefaultDocumentationType.DESCRIPTION.toString());
                    localDocs.put(
                            DefaultDocumentationType.DESCRIPTION.toString(),
                            docItem);
                } else {
                    ResourceDocumentationItem docItem = new ResourceDocumentationItem(
                            name, content, bi,
                            DefaultDocumentationType.HOW_TO.toString());
                    localDocs.put(DefaultDocumentationType.HOW_TO.toString(),
                            docItem);
                }
            }
        }
        bi.setLiveDoc(localDocs);
        bi.setParentLiveDoc(parentDocs);
    }
}
