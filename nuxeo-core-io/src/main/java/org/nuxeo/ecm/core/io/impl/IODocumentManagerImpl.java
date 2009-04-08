/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.IODocumentManager;
import org.nuxeo.ecm.core.io.exceptions.ExportDocumentException;
import org.nuxeo.ecm.core.io.exceptions.ImportDocumentException;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentsListReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.runtime.api.Framework;

/**
 * IODocumentManager basic implementation
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IODocumentManagerImpl implements IODocumentManager {

    private static final long serialVersionUID = -3131999198524020179L;

    private static final Log log = LogFactory.getLog(IODocumentManagerImpl.class);


    private static CoreSession getCoreSession(String repo)
            throws ClientException {
        CoreSession systemSession;
        try {
            Framework.login();
            RepositoryManager manager = Framework.getService(RepositoryManager.class);
            Repository repository = manager.getRepository(repo);
            if (repository == null) {
                log.error("repository " + repo + " not in available repos: " + manager.getRepositories());
                throw new ClientException("cannot get repository: " + repo);
            }
            systemSession = repository.open();
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException(
                    "Failed to open core session to repository " + repo, e);
        }
        return systemSession;
    }

    public DocumentTranslationMap importDocuments(InputStream in, String repo,
            DocumentRef root) throws ImportDocumentException, ClientException {
        CoreSession coreSession = getCoreSession(repo);
        final DocumentModel dst = coreSession.getDocument(root);

        DocumentReader reader = null;
        DocumentModelWriter writer = null;

        try {
            reader = new NuxeoArchiveReader(in);
            writer = new DocumentModelWriter(coreSession, dst.getPathAsString());
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            return pipe.run();
        } catch (Exception e) {
            throw new ImportDocumentException(e);
        } finally {
            // make docs available to all
            coreSession.save();

            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
     }

    public DocumentTranslationMap importDocuments(InputStream in, DocumentWriter customDocWriter)
            throws ImportDocumentException {

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
        } catch (Exception e) {
            throw new ImportDocumentException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            // writer will be closed by caller
        }
    }

    public DocumentTranslationMap exportDocuments(OutputStream out,
            String repo, Collection<DocumentRef> sources, boolean recurse,
            String format) throws ExportDocumentException, ClientException {
        CoreSession coreSession = getCoreSession(repo);

        DocumentReader reader = null;
        DocumentWriter writer = null;

        try {
            DocumentPipe pipe = new DocumentPipeImpl(10);
            // XXX check format before creating writer
            writer = new NuxeoArchiveWriter(out);
            pipe.setWriter(writer);
            if (!recurse) {
                reader = DocumentsListReader.createDocumentsListReader(
                        coreSession, sources);
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
        } catch (Exception e) {
            throw new ExportDocumentException(e);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    public DocumentTranslationMap exportDocuments(OutputStream out,
            DocumentReader customDocReader, String format)
            throws ExportDocumentException {

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
        } catch (Exception e) {
            throw new ExportDocumentException(e);
        } finally {
            // reader will be closed by caller
            if (writer != null) {
                writer.close();
            }
        }
    }

    public DocumentTranslationMap importDocuments(
            DocumentReader customDocReader, DocumentWriter customDocWriter)
            throws ImportDocumentException {

        try {
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(customDocReader);
            pipe.setWriter(customDocWriter);
            DocumentTranslationMap map = pipe.run();

            // will need to save session before notifying events, otherwise docs
            // won't be found
            // writer.close();

            return map;
        } catch (Exception e) {
            throw new ImportDocumentException(e);
        }
    }

}
