/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An iterator over a tree of documents
 * <p>
 * The tree is traversed from top to bottom and left to right.
 * <p>
 * TODO: move this in an utility package
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentTreeIterator implements Iterator<DocumentModel> {

    private static final Log log = LogFactory.getLog(DocumentTreeIterator.class);

    /**
     * The document manager session.
     */
    protected final CoreSession session;

    /**
     * Root document.
     */
    protected final DocumentModel root;

    /**
     * The current sequence.
     */
    protected Iterator<DocumentModel> sequence;

    /**
     * The sequence queue.
     */
    protected final Queue<Iterator<DocumentModel>> queue = new LinkedList<>();

    /**
     * Creates the iterator given the tree root.
     */
    public DocumentTreeIterator(CoreSession session, DocumentModel root) {
        this(session, root, false);
    }

    public DocumentTreeIterator(CoreSession session, DocumentModel root, boolean excludeRoot) {
        this.root = root;
        this.session = session;
        if (excludeRoot) {
            sequence = session.getChildrenIterator(root.getRef(), null, null, null);
        } else {
            sequence = new OneDocSequence(root);
        }
    }

    /**
     * Gets next non empty sequence from queue.
     * <p>
     * This will remove from the queue all traversed sequences (the empty ones and the first not empty sequence found).
     *
     * @return the first non empty sequence or null if no one was found
     */
    protected Iterator<DocumentModel> getNextNonEmptySequence() {
        while (true) {
            Iterator<DocumentModel> seq = queue.poll();
            if (seq == null) {
                return null;
            } else if (seq.hasNext()) {
                return seq;
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (sequence == null || !sequence.hasNext()) {
            // no current valid sequence
            sequence = getNextNonEmptySequence();
            if (sequence == null) {
                return false;
            }
        }
        // we have a sequence to iterate over
        return true;
    }

    @Override
    public DocumentModel next() {
        // satisfy iterator contract - throw an exception if no more elements to
        // iterate
        if (!hasNext()) {
            throw new NoSuchElementException("no more documents to iterate over");
        }
        // we have a non empty sequence to iterate over
        DocumentModel doc = sequence.next();
        if (doc.isFolder()) {
            // TODO: load children after the document was traversed
            // update the sequence queue with children from this folder
            queue.add(session.getChildrenIterator(doc.getRef(), null, null, null));
        }
        return doc;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove is not yet supported");
    }

    /**
     * A sequence of a single doc.
     *
     * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
     */
    static class OneDocSequence implements Iterator<DocumentModel> {
        final DocumentModel doc;

        boolean hasNext = true;

        OneDocSequence(DocumentModel doc) {
            this.doc = doc;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public DocumentModel next() {
            if (doc == null) {
                throw new NoSuchElementException("no more documents to iterate over");
            }
            hasNext = false;
            return doc;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove is not yet supported");
        }
    }

    /**
     * Resets the iterator back to the tree root and clear any cached data.
     */
    public void reset() {
        sequence = new OneDocSequence(root);
        queue.clear();
    }

}
