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
 *     Nuxeo - initial API and implementation
 *
 * $Id: IODocumentManagerImpl.java 29979 2008-02-07 16:00:26Z dmihalache $
 */

package org.nuxeo.ecm.core.io.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.IODocumentManager;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentsListReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;

/**
 * IODocumentManager basic implementation.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IODocumentManagerImpl implements IODocumentManager {

    private static final long serialVersionUID = -3131999198524020179L;

    @Override
    public DocumentTranslationMap importDocuments(InputStream in, String repo, DocumentRef root) {
        DocumentReader reader = null;
        DocumentModelWriter writer = null;
        try (CloseableCoreSession coreSession = CoreInstance.openCoreSessionSystem(repo)) {
            final DocumentModel dst = coreSession.getDocument(root);
            reader = new NuxeoArchiveReader(in);
            writer = new DocumentModelWriter(coreSession, dst.getPathAsString());
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            DocumentTranslationMap map = pipe.run();
            coreSession.save();
            return map;
        } catch (IOException e) {
            throw new NuxeoException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public DocumentTranslationMap importDocuments(InputStream in, DocumentWriter customDocWriter) {

        DocumentReader reader = null;

        try {
            reader = new NuxeoArchiveReader(in);
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(customDocWriter);
            DocumentTranslationMap map = pipe.run();

            // will need to save session before notifying events, otherwise docs won't be found
            customDocWriter.close();

            return map;
        } catch (IOException e) {
            throw new NuxeoException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            // writer will be closed by caller
        }
    }

    @Override
    public DocumentTranslationMap exportDocuments(OutputStream out, String repo, Collection<DocumentRef> sources,
            boolean recurse, String format) {
        DocumentReader reader = null;
        DocumentWriter writer = null;
        try (CloseableCoreSession coreSession = CoreInstance.openCoreSessionSystem(repo)) {
            DocumentPipe pipe = new DocumentPipeImpl(10);
            // XXX check format before creating writer
            writer = new NuxeoArchiveWriter(out);
            pipe.setWriter(writer);
            if (!recurse) {
                reader = DocumentsListReader.createDocumentsListReader(coreSession, sources);
                pipe.setReader(reader);
                return pipe.run();
            } else {
                List<DocumentTranslationMap> maps = new ArrayList<DocumentTranslationMap>();
                for (DocumentRef rootSource : sources) {
                    // create a tree reader for each doc
                    reader = new DocumentTreeReader(coreSession, rootSource);
                    pipe.setReader(reader);
                    DocumentTranslationMap map = pipe.run();
                    if (map != null) {
                        maps.add(map);
                    }
                }
                return DocumentTranslationMapImpl.merge(maps);
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public DocumentTranslationMap exportDocuments(OutputStream out, DocumentReader customDocReader, String format) {

        DocumentWriter writer = null;

        try {
            DocumentPipe pipe = new DocumentPipeImpl(10);
            // XXX check format before creating writer
            writer = new NuxeoArchiveWriter(out);
            pipe.setWriter(writer);
            pipe.setReader(customDocReader);

            List<DocumentTranslationMap> maps = new ArrayList<DocumentTranslationMap>();
            DocumentTranslationMap map = pipe.run();
            if (map != null) {
                maps.add(map);
            }

            return DocumentTranslationMapImpl.merge(maps);
        } catch (IOException e) {
            throw new NuxeoException(e);
        } finally {
            // reader will be closed by caller
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public DocumentTranslationMap importDocuments(DocumentReader customDocReader, DocumentWriter customDocWriter) {

        try {
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(customDocReader);
            pipe.setWriter(customDocWriter);
            DocumentTranslationMap map = pipe.run();

            // will need to save session before notifying events, otherwise docs
            // won't be found
            // writer.close();

            return map;
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

}
