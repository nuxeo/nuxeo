package org.nuxeo.ecm.platform.importer.source;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolderWithProperties;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.importer.properties.MetadataCollector;

public class FileWithMetadataSourceNode extends FileSourceNode {

    protected static MetadataCollector collector = new MetadataCollector();

    public static String METADATA_FILENAME = "metadata.properties";

    private static final Log log = LogFactory.getLog(FileWithMetadataSourceNode.class);

    public FileWithMetadataSourceNode(File file) {
        super(file);
    }

    @Override
    public BlobHolder getBlobHolder() {
        BlobHolder bh = new SimpleBlobHolderWithProperties(new FileBlob(file),collector.getProperties(file.getPath()));
        return bh;
    }

    @Override
    public List<SourceNode> getChildren() {

        List<SourceNode> children = new ArrayList<SourceNode>();

        for (File child : file.listFiles()) {
            if (METADATA_FILENAME.equals(child.getName())) {
                try {
                    collector.addPropertyFile(child);
                } catch (Exception e) {
                    log.error("Error during properties parsing", e);
                }
                break;
            }
        }

        for (File child : file.listFiles()) {
            if (!METADATA_FILENAME.equals(child.getName())) {
                children.add(new FileWithMetadataSourceNode(child));
            }
        }
        return children;
    }


}
