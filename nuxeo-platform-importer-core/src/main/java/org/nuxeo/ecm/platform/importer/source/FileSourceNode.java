package org.nuxeo.ecm.platform.importer.source;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

public class FileSourceNode implements SourceNode {

    protected File file;

    public FileSourceNode(File file) {
        this.file=file;
    }

    public FileSourceNode(String path) {
        this.file=new File(path);
    }

    public BlobHolder getBlobHolder() {
        return new SimpleBlobHolder(new FileBlob(file));
    }

    public List<SourceNode> getChildren() {

        List<SourceNode> children = new ArrayList<SourceNode>();

        for (File child : file.listFiles()) {
            children.add(new FileSourceNode(child));
        }
        return children;
    }

    public boolean isFolderish() {
        return file.isDirectory();
    }

    public String getName() {
        return file.getName();
    }

}
