/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentPipeImpl.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentTransformer;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentPipeImpl implements DocumentPipe {

    private final List<DocumentTransformer> transformers;

    private final int pageSize;

    private DocumentReader reader;

    private DocumentWriter writer;


    public DocumentPipeImpl(int pageSize) {
        this.pageSize = pageSize;
        transformers = new ArrayList<DocumentTransformer>();
    }

    public DocumentPipeImpl() {
        this(0);
    }


    @Override
    public void addTransformer(DocumentTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    public DocumentReader getReader() {
        return reader;
    }

    @Override
    public List<DocumentTransformer> getTransformers() {
        return transformers;
    }

    @Override
    public DocumentWriter getWriter() {
        return writer;
    }

    @Override
    public void removeTransformer(DocumentTransformer transformer) {
        transformers.remove(transformer);
    }

    @Override
    public void setReader(DocumentReader reader) {
        this.reader = reader;
    }

    @Override
    public void setWriter(DocumentWriter writer) {
        this.writer = writer;
    }

    @Override
    public DocumentTranslationMap run() throws Exception {
        if (reader == null) {
            throw new IllegalArgumentException("Pipe reader cannot be null");
        }
        if (writer == null) {
            throw new IllegalArgumentException("Pipe writer cannot be null");
        }

        List<DocumentTranslationMap> maps = new ArrayList<DocumentTranslationMap>();
        readAndWriteDocs(maps);
        return DocumentTranslationMapImpl.merge(maps);
    }

    protected void handleBatchEnd() {
        // NOP
    }

    protected void readAndWriteDocs(List<DocumentTranslationMap> maps)
            throws IOException {
        if (pageSize == 0) {
            // handle single doc case
            ExportedDocument doc;
            while ((doc = reader.read()) != null) {
                applyTransforms(doc);
                DocumentTranslationMap map = writer.write(doc);
                maps.add(map);
                handleBatchEnd();
            }

        } else {
            // handle multiple doc case
            ExportedDocument[] docs;
            while ((docs = reader.read(pageSize)) != null) {
                if (docs.length != 0) {
                    applyTransforms(docs);
                    DocumentTranslationMap map = writer.write(docs);
                    if (map != null) {
                        maps.add(map);
                    }
                    handleBatchEnd();
                }
            }
        }
    }

    public void applyTransforms(ExportedDocument doc) throws IOException {
        for (DocumentTransformer tr : transformers) {
            tr.transform(doc);
        }
    }

    public void applyTransforms(ExportedDocument[] docs) throws IOException {
        for (DocumentTransformer tr : transformers) {
            for (ExportedDocument doc : docs) {
                tr.transform(doc);
            }
        }
    }

}
