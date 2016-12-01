package org.nuxeo.ecm.platform.importer.queue.tests;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public class BuggySourceNode implements SourceNode {

    private int index;
    private boolean txBuggy;
    private boolean exceptionBuggy;

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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(index);
        out.writeBoolean(txBuggy);
        out.writeBoolean(exceptionBuggy);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        index = in.readInt();
        txBuggy = in.readBoolean();
        exceptionBuggy = in.readBoolean();
    }

}
