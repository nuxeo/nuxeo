package org.nuxeo.ecm.platform.importer.queue.tests;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class BuggySourceNode implements SourceNode, Serializable {

    private final int index;
    private final boolean txBuggy;
    private final boolean exceptionBuggy;

    public BuggySourceNode(int index, boolean txBuggy, boolean exceptionBuggy) {
        this.index = index;
        this.txBuggy = txBuggy;
        this.exceptionBuggy = exceptionBuggy;
    }

    @Override
    public BlobHolder getBlobHolder() throws IOException {
        return null;
    }

    @Override
    public List<SourceNode> getChildren() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return String.format("node-%08d", index);
    }

    @Override
    public String getSourcePath() {
        return "";
    }

    @Override
    public boolean isFolderish() {
        return false;
    }

    public int getIndex() {
        return index;
    }

    public boolean isTransactionBuggy() {
        return txBuggy;
    }

    public boolean isExceptionBuggy() {
        return exceptionBuggy;
    }

}
