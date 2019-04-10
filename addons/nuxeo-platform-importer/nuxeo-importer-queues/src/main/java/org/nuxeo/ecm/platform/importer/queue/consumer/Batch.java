package org.nuxeo.ecm.platform.importer.queue.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.platform.importer.source.SourceNode;

/**
 * @since 8.3
 */
public class Batch {

    private final int size;

    List<SourceNode> nodes = new ArrayList<>();


    public Batch(int size) {
        this.size = size;

    }

    public void add(SourceNode src) {
        nodes.add(src);
    }

    public boolean shouldBeEvicted() {
        return false;
    }

    public boolean isFull() {
        return nodes.size() >= size;
    }

    public List<SourceNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public void clear() {
        nodes.clear();
    }
}
