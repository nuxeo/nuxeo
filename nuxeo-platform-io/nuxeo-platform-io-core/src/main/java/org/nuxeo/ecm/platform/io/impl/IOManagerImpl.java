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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: IOManagerImpl.java 27208 2007-11-14 19:59:25Z dmihalache $
 */

package org.nuxeo.ecm.platform.io.impl;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentTreeIterator;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentReaderFactory;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.DocumentWriterFactory;
import org.nuxeo.ecm.core.io.DocumentsExporter;
import org.nuxeo.ecm.core.io.DocumentsImporter;
import org.nuxeo.ecm.core.io.IODocumentManager;
import org.nuxeo.ecm.core.io.exceptions.ExportDocumentException;
import org.nuxeo.ecm.core.io.exceptions.ImportDocumentException;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;
import org.nuxeo.ecm.core.io.impl.IODocumentManagerImpl;
import org.nuxeo.ecm.platform.io.api.IOManager;
import org.nuxeo.ecm.platform.io.api.IOResourceAdapter;
import org.nuxeo.ecm.platform.io.api.IOResources;

/**
 * IOManager implementation
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IOManagerImpl implements IOManager {

    private static final long serialVersionUID = 5789086884484295921L;

    private static final Log log = LogFactory.getLog(IOManagerImpl.class);

    protected final Map<String, IOResourceAdapter> adaptersRegistry;

    public IOManagerImpl() {
        adaptersRegistry = new HashMap<String, IOResourceAdapter>();
    }

    private static void closeStream(Closeable stream) throws IOException {
        if (stream != null) {
            stream.close();
        }
    }

    @Override
    public IOResourceAdapter getAdapter(String name) throws ClientException {
        return adaptersRegistry.get(name);
    }

    @Override
    public void addAdapter(String name, IOResourceAdapter adapter)
            throws ClientException {
        if (DOCUMENTS_ADAPTER_NAME.equals(name)) {
            log.error("Cannot register adapter with name "
                    + DOCUMENTS_ADAPTER_NAME);
            return;
        }
        adaptersRegistry.put(name, adapter);
    }

    @Override
    public void removeAdapter(String name) throws ClientException {
        adaptersRegistry.remove(name);
    }

    public void exportDocumentsAndResources(OutputStream out, String repo,
            final String format, Collection<String> ioAdapters,
            final DocumentReader customDocReader)
            throws ExportDocumentException, IOException, ClientException {

        DocumentsExporter docsExporter = new DocumentsExporter() {
            @Override
            public DocumentTranslationMap exportDocs(OutputStream out)
                    throws ExportDocumentException, ClientException,
                    IOException {
                IODocumentManager docManager = new IODocumentManagerImpl();
                DocumentTranslationMap map = docManager.exportDocuments(out,
                        customDocReader, format);
                return map;
            }
        };

        exportDocumentsAndResources(out, repo, docsExporter, ioAdapters);
    }

    @Override
    public void exportDocumentsAndResources(OutputStream out,
            final String repo, final Collection<DocumentRef> sources,
            final boolean recurse, final String format,
            final Collection<String> ioAdapters) throws IOException,
            ClientException, ExportDocumentException {

        DocumentsExporter docsExporter = new DocumentsExporter() {
            @Override
            public DocumentTranslationMap exportDocs(OutputStream out)
                    throws ExportDocumentException, ClientException,
                    IOException {
                IODocumentManager docManager = new IODocumentManagerImpl();
                DocumentTranslationMap map = docManager.exportDocuments(out,
                        repo, sources, recurse, format);
                return map;
            }
        };

        exportDocumentsAndResources(out, repo, docsExporter, ioAdapters);
    }

    void exportDocumentsAndResources(OutputStream out, String repo,
            DocumentsExporter docsExporter, Collection<String> ioAdapters)
            throws IOException, ClientException, ExportDocumentException {

        List<String> doneAdapters = new ArrayList<String>();

        ZipOutputStream zip = new ZipOutputStream(out);
        zip.setMethod(ZipOutputStream.DEFLATED);
        zip.setLevel(9);

        ByteArrayOutputStream docsZip = new ByteArrayOutputStream();
        DocumentTranslationMap map = docsExporter.exportDocs(docsZip);

        ZipEntry docsEntry = new ZipEntry(DOCUMENTS_ADAPTER_NAME + ".zip");
        zip.putNextEntry(docsEntry);
        zip.write(docsZip.toByteArray());
        zip.closeEntry();
        docsZip.close();
        doneAdapters.add(DOCUMENTS_ADAPTER_NAME);

        Collection<DocumentRef> allSources = map.getDocRefMap().keySet();

        if (ioAdapters != null && !ioAdapters.isEmpty()) {
            for (String adapterName : ioAdapters) {
                String filename = adapterName + ".xml";
                IOResourceAdapter adapter = getAdapter(adapterName);
                if (adapter == null) {
                    log.warn("Adapter " + adapterName + " not found");
                    continue;
                }
                if (doneAdapters.contains(adapterName)) {
                    log.warn("Export for adapter " + adapterName
                            + " already done");
                    continue;
                }
                IOResources resources = adapter.extractResources(repo,
                        allSources);
                resources = adapter.translateResources(repo, resources, map);
                ByteArrayOutputStream adapterOut = new ByteArrayOutputStream();
                adapter.getResourcesAsXML(adapterOut, resources);
                ZipEntry adapterEntry = new ZipEntry(filename);
                zip.putNextEntry(adapterEntry);
                zip.write(adapterOut.toByteArray());
                zip.closeEntry();
                doneAdapters.add(adapterName);
                adapterOut.close();
            }
        }
        try {
            zip.close();
        } catch (ZipException e) {
            // empty zip file, do nothing
        }
    }

    @Override
    public void importDocumentsAndResources(InputStream in, final String repo,
            final DocumentRef root) throws IOException, ClientException,
            ImportDocumentException {

        DocumentsImporter docsImporter = new DocumentsImporter() {

            @Override
            public DocumentTranslationMap importDocs(InputStream sourceStream)
                    throws ImportDocumentException, ClientException,
                    IOException {
                IODocumentManager docManager = new IODocumentManagerImpl();
                return docManager.importDocuments(sourceStream, repo, root);
            }

        };

        importDocumentsAndResources(docsImporter, in, repo);
    }

    public void importDocumentsAndResources(InputStream in, final String repo,
            final DocumentRef root, final DocumentWriter customDocWriter)
            throws IOException, ClientException, ImportDocumentException {

        DocumentsImporter docsImporter = new DocumentsImporter() {

            @Override
            public DocumentTranslationMap importDocs(InputStream sourceStream)
                    throws ImportDocumentException, ClientException,
                    IOException {
                IODocumentManager docManager = new IODocumentManagerImpl();
                return docManager.importDocuments(sourceStream, customDocWriter);
            }

        };

        importDocumentsAndResources(docsImporter, in, repo);
    }

    void importDocumentsAndResources(DocumentsImporter docsImporter,
            InputStream in, String repo) throws IOException, ClientException,
            ImportDocumentException {

        ZipInputStream zip = new ZipInputStream(in);

        // first entry will be documents
        ZipEntry zentry = zip.getNextEntry();
        String docZipFilename = DOCUMENTS_ADAPTER_NAME + ".zip";
        if (zentry == null || !docZipFilename.equals(zentry.getName())) {
            zip.close();
            throw new ImportDocumentException("Invalid archive");
        }

        // fill in a new stream
        File temp = File.createTempFile("nuxeo-import-adapters-", ".zip");
        FileOutputStream outDocs = new FileOutputStream(temp);
        try {
            FileUtils.copy(zip, outDocs);
        } finally {
            outDocs.close();
        }
        zip.closeEntry();

        InputStream tempIn = new FileInputStream(temp.getPath());
        DocumentTranslationMap map = docsImporter.importDocs(tempIn);
        tempIn.close();
        temp.delete();

        while ((zentry = zip.getNextEntry()) != null) {
            String entryName = zentry.getName();
            if (entryName.endsWith(".xml")) {
                String ioAdapterName = entryName.substring(0,
                        entryName.length() - 4);
                IOResourceAdapter adapter = getAdapter(ioAdapterName);
                if (adapter == null) {
                    log.warn("Adapter "
                            + ioAdapterName
                            + " not available. Unable to import associated resources.");
                    continue;
                }
                IOResources resources = adapter.loadResourcesFromXML(zip);
                IOResources newResources = adapter.translateResources(repo,
                        resources, map);
                log.info("store resources with adapter " + ioAdapterName);
                adapter.storeResources(newResources);
            } else {
                log.warn("skip entry: " + entryName);
            }
            try {
                // we might have an undesired stream close in the client
                zip.closeEntry();
            } catch (Exception e) {
                // TODO fix this
                log.error("Please check code handling entry " + entryName, e);
            }
        }
        zip.close();
    }

    @Override
    public Collection<DocumentRef> copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, DocumentLocation targetLocation,
            Collection<String> ioAdapters) throws ClientException {
        if (sources == null || sources.isEmpty()) {
            return null;
        }

        String newRepo = targetLocation.getServerName();
        if (!repo.equals(newRepo)) {
            // TODO: maybe import and export (?), assume copy is recursive.
            throw new ClientException("Cannot copy to different server");
        }

        List<DocumentRef> roots = new ArrayList<DocumentRef>();
        try (CoreSession session = CoreInstance.openCoreSession(repo)) {
            for (DocumentRef source : sources) {
                DocumentTranslationMap map = new DocumentTranslationMapImpl(
                        repo, repo);
                DocumentModel sourceDoc = session.getDocument(source);
                DocumentModel destDoc = session.copy(source,
                        targetLocation.getDocRef(), null);
                roots.add(destDoc.getRef());
                // iterate on each tree to build translation map
                DocumentTreeIterator sourceIt = new DocumentTreeIterator(
                        session, sourceDoc);
                DocumentTreeIterator destIt = new DocumentTreeIterator(session,
                        destDoc);
                while (sourceIt.hasNext()) {
                    DocumentModel sourceItem = sourceIt.next();
                    DocumentRef sourceRef = sourceItem.getRef();
                    if (!destIt.hasNext()) {
                        map.put(sourceRef, null);
                    } else {
                        DocumentModel destItem = destIt.next();
                        DocumentRef destRef = destItem.getRef();
                        map.put(sourceRef, destRef);
                    }
                }
                Collection<DocumentRef> allSources = map.getDocRefMap().keySet();
                if (ioAdapters != null && !ioAdapters.isEmpty()) {
                    for (String adapterName : ioAdapters) {
                        IOResourceAdapter adapter = getAdapter(adapterName);
                        if (adapter == null) {
                            log.warn("Adapter " + adapterName + " not found");
                            continue;
                        }
                        IOResources resources = adapter.extractResources(repo,
                                allSources);
                        IOResources newResources = adapter.translateResources(
                                repo, resources, map);
                        adapter.storeResources(newResources);
                    }
                }
                session.save();
            }
        }
        return roots;
    }

    private static DocumentWriter createDocWriter(String docWriterFactoryName,
            Map<String, Object> factoryParams) throws ClientException {
        // create a custom writer using factory instance

        Object factoryObj;
        try {
            Class clazz = Class.forName(docWriterFactoryName);
            factoryObj = clazz.newInstance();
        } catch (Exception e) {
            throw new ClientException("cannot instantiate factory "
                    + docWriterFactoryName, e);
        }

        DocumentWriter customDocWriter;
        if (factoryObj instanceof DocumentWriterFactory) {
            customDocWriter = ((DocumentWriterFactory) factoryObj).createDocWriter(factoryParams);
        } else {
            throw new ClientException("bad class type: " + factoryObj);
        }

        if (customDocWriter == null) {
            throw new ClientException("null DocumentWriter created by "
                    + docWriterFactoryName);
        }

        return customDocWriter;
    }

    private static DocumentReader createDocReader(String docReaderFactoryName,
            Map<String, Object> factoryParams) throws ClientException {
        // create a custom reader using factory instance

        Object factoryObj;
        try {
            Class clazz = Class.forName(docReaderFactoryName);
            factoryObj = clazz.newInstance();
        } catch (Exception e) {
            throw new ClientException("cannot instantiate factory "
                    + docReaderFactoryName, e);
        }

        DocumentReader customDocReader;
        if (factoryObj instanceof DocumentReaderFactory) {
            customDocReader = ((DocumentReaderFactory) factoryObj).createDocReader(factoryParams);
        } else {
            throw new ClientException("bad class type: " + factoryObj);
        }

        if (customDocReader == null) {
            throw new ClientException("null DocumentReader created by "
                    + docReaderFactoryName);
        }

        return customDocReader;
    }

    @Override
    public void importFromStream(InputStream in,
            DocumentLocation targetLocation, String docReaderFactoryClassName,
            Map<String, Object> rFactoryParams,
            String docWriterFactoryClassName, Map<String, Object> wFactoryParams)
            throws ClientException {

        DocumentWriter customDocWriter = createDocWriter(
                docWriterFactoryClassName, wFactoryParams);
        DocumentReader customDocReader = null;

        try {
            if (rFactoryParams == null) {
                rFactoryParams = new HashMap<String, Object>();
            }
            rFactoryParams.put("source_stream", in);
            customDocReader = createDocReader(docReaderFactoryClassName,
                    rFactoryParams);

            IODocumentManager docManager = new IODocumentManagerImpl();
            DocumentTranslationMap map = docManager.importDocuments(
                    customDocReader, customDocWriter);
        } finally {
            if (customDocReader != null) {
                customDocReader.close();
            }
            customDocWriter.close();

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }

    }

}
