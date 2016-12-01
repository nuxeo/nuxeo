package org.nuxeo.ecm.platform.importer.queue.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.platform.importer.source.Node;

/**
 * @since 8.3
 */
public class Batch<N extends Node> {

    private final int capacity;

    final List<N> nodes = new ArrayList<>();


    public Batch(int capacity) {
        this.capacity = capacity;
    }

    public int size() {
        return nodes.size();
    }

    public void add(N src) {
        nodes.add(src);
    }

    public boolean shouldBeEvicted() {
        return false;
    }

    public boolean isFull() {
        return nodes.size() >= capacity;
    }

    public List<N> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public void clear() {
        nodes.clear();
    }
}
