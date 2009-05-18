package org.nuxeo.ecm.platform.importer.source;

import java.util.List;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

public interface SourceNode {

    boolean isFolderish();

    BlobHolder getBlobHolder();

    List<SourceNode> getChildren();

    String getName();

}
