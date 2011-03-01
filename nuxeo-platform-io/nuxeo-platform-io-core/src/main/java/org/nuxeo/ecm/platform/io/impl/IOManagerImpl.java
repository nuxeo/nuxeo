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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentTreeIterator;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServiceManager;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamManager;
import org.nuxeo.runtime.services.streaming.StreamSource;

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

    private static CoreSession getCoreSession(String repo)
            throws ClientException {
        CoreSession systemSession;
        try {
            Framework.login();
            RepositoryManager manager = Framework.getService(RepositoryManager.class);
            systemSession = manager.getRepository(repo).open();
        } catch (Exception e) {
            throw new ClientException(
                    "Failed to open core session to repository " + repo, e);
        }
        return systemSession;
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
        CoreSession coreSession = getCoreSession(repo);
        for (DocumentRef source : sources) {
            DocumentTranslationMap map = new DocumentTranslationMapImpl(repo,
                    repo);
            DocumentModel sourceDoc = coreSession.getDocument(source);
            DocumentModel destDoc = coreSession.copy(source,
                    targetLocation.getDocRef(), null);
            roots.add(destDoc.getRef());
            // iterate on each tree to build translation map
            DocumentTreeIterator sourceIt = new DocumentTreeIterator(
                    coreSession, sourceDoc);
            DocumentTreeIterator destIt = new DocumentTreeIterator(coreSession,
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
                    IOResources newResources = adapter.translateResources(repo,
                            resources, map);
                    adapter.storeResources(newResources);
                }
            }
        }
        coreSession.save();
        return roots;
    }

    @Override
    public void copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, String serverAddress,
            int jndiPort, DocumentLocation targetLocation,
            Collection<String> ioAdapters) throws ClientException {

        String docReaderFactoryName = null;
        String docWriterFactoryName = null;
        Map<String, Object> readerFactoryParams = null;
        Map<String, Object> writerFactoryParams = null;

        copyDocumentsAndResources(repo, sources, serverAddress, jndiPort,
                targetLocation, docReaderFactoryName, readerFactoryParams,
                docWriterFactoryName, writerFactoryParams, ioAdapters);
    }

    @Override
    public void copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, IOManager remoteIOManager,
            DocumentLocation targetLocation, Collection<String> ioAdapters)
            throws ClientException {

        String docReaderFactoryName = null;
        String docWriterFactoryName = null;
        Map<String, Object> readerFactoryParams = null;
        Map<String, Object> writerFactoryParams = null;

        copyDocumentsAndResources(repo, sources, remoteIOManager,
                targetLocation, docReaderFactoryName, readerFactoryParams,
                docWriterFactoryName, writerFactoryParams, ioAdapters);
    }

    @Override
    public void importExportedFile(String uri, DocumentLocation targetLocation)
            throws ClientException {
        File tempFile = getLocalFile(uri);

        InputStream in = null;
        try {
            in = new FileInputStream(tempFile);
            importDocumentsAndResources(in, targetLocation.getServerName(),
                    targetLocation.getDocRef());
        } catch (IOException e) {
            throw new ClientException(e);
        } finally {
            tempFile.delete();
            try {
                closeStream(in);
            } catch (IOException e) {
                throw new ClientException(e);
            }
        }
    }

    @Override
    public void copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, String serverAddress,
            int jndiPort, DocumentLocation targetLocation,
            String docReaderFactoryName,
            Map<String, Object> readerFactoryParams,
            String docWriterFactoryName,
            Map<String, Object> writerFactoryParams,
            Collection<String> ioAdapters) throws ClientException {

        // service uri will be the form of
        // jboss://localhost:1099/nuxeo/IOManagerBean/remote
        String serviceUri = "jboss://" + serverAddress + ":" + jndiPort
                + "/nuxeo/IOManagerBean/remote";
        log.info("Connect to IOManager at: " + serviceUri);
        IOManager remoteIOManager;
        try {
            remoteIOManager = (IOManager) ServiceManager.getInstance().getService(
                    serviceUri);
        } catch (Exception e) {
            throw new ClientException(e);
        }

        copyDocumentsAndResources(repo, sources, remoteIOManager,
                targetLocation, docReaderFactoryName, readerFactoryParams,
                docWriterFactoryName, writerFactoryParams, ioAdapters);
    }

    public void copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, IOManager remoteIOManager,
            DocumentLocation targetLocation, String docReaderFactoryName,
            Map<String, Object> readerFactoryParams,
            String docWriterFactoryName,
            Map<String, Object> writerFactoryParams,
            Collection<String> ioAdapters) throws ClientException {

        String uri = null;
        try {
            uri = externalizeExport(repo, sources, docReaderFactoryName,
                    readerFactoryParams, ioAdapters);

            // call remote server to do the upload
            if (docWriterFactoryName == null) {
                // default import
                remoteIOManager.importExportedFile(uri, targetLocation);
            } else {
                remoteIOManager.importExportedFile(uri, targetLocation,
                        docWriterFactoryName, writerFactoryParams);
            }

            // ?? localStreamManager.stop();
        } finally {
            if (uri != null) {
                disposeExport(uri);
            }
        }
    }

    @Override
    public String externalizeExport(String repo,
            Collection<DocumentRef> sources, Collection<String> ioAdapters)
            throws ClientException {

        return externalizeExport(repo, sources, null, null, ioAdapters);
    }

    @Override
    public String externalizeExport(String repo, String docReaderFactoryName,
            Map<String, Object> readerFactoryParams,
            Collection<String> ioAdapters) throws ClientException {

        return externalizeExport(repo, null, docReaderFactoryName,
                readerFactoryParams, ioAdapters);
    }

    @Override
    public String externalizeExport(String repo,
            Collection<DocumentRef> sources, String docReaderFactoryName,
            Map<String, Object> readerFactoryParams,
            Collection<String> ioAdapters) throws ClientException {
        File tempFile = null;
        FileOutputStream fos = null;
        StreamSource src = null;
        try {
            // copy via a temp file...
            tempFile = File.createTempFile("export-import", ".zip");

            fos = new FileOutputStream(tempFile);
            // TODO specify format
            String format = null;
            if (docReaderFactoryName == null) {
                // default export
                exportDocumentsAndResources(fos, repo, sources, true, format,
                        ioAdapters);
            } else {
                // create a custom reader using factory instance
                Class clazz = Class.forName(docReaderFactoryName);
                Object factoryObj = clazz.newInstance();
                if (factoryObj instanceof DocumentReaderFactory) {
                    DocumentReader customDocReader = ((DocumentReaderFactory) factoryObj).createDocReader(readerFactoryParams);
                    exportDocumentsAndResources(fos, repo, format, ioAdapters,
                            customDocReader);
                } else {
                    throw new ClientException("bad class type: " + factoryObj);
                }
            }

            StreamManager localStreamManager = Framework.getLocalService(StreamManager.class);

            if (localStreamManager == null) {
                throw new ClientException(
                        "StreamManager service not available locally");
            }

            src = new FileSource(tempFile);
            double start = System.currentTimeMillis();
            String uri = null;
            try {
                uri = localStreamManager.addStream(src);
                double end = System.currentTimeMillis();
                log.info(">>> upload took " + ((end - start) / 1000) + " sec.");
            } catch (IOException e) {
                throw new ClientException(e);
            }
            return uri;

        } catch (Exception e) {
            throw new ClientException(e);
        } finally {
            try {
                closeStream(fos);
            } catch (IOException e) {
                throw new ClientException(e);
            }
            if (src != null) {
                src.destroy();
            }
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    @Override
    public void disposeExport(String uri) throws ClientException {
        StreamManager localStreamManager = Framework.getLocalService(StreamManager.class);

        if (localStreamManager == null) {
            throw new ClientException(
                    "StreamManager service not available locally");
        }

        localStreamManager.removeStream(uri);
    }

    @Override
    public void importExportedFile(String uri, DocumentLocation targetLocation,
            String docWriterFactoryName, Map<String, Object> factoryParams)
            throws ClientException {

        DocumentWriter customDocWriter = createDocWriter(docWriterFactoryName,
                factoryParams);

        File tempFile = getLocalFile(uri);
        InputStream in = null;
        try {
            in = new FileInputStream(tempFile);
            importDocumentsAndResources(in, targetLocation.getServerName(),
                    targetLocation.getDocRef(), customDocWriter);
        } catch (IOException e) {
            throw new ClientException(e);
        } finally {
            customDocWriter.close();
            try {
                closeStream(in);
            } catch (IOException e) {
                throw new ClientException(e);
            }
            tempFile.delete();
        }
    }

    private static File getLocalFile(String uri) throws ClientException {
        StreamManager streamManager = Framework.getLocalService(StreamManager.class);

        double start = System.currentTimeMillis();
        File tempFile;
        try {
            tempFile = File.createTempFile("export-import", ".zip");
            log.info("get stream source at uri: " + uri);
            StreamSource source = streamManager.getStream(uri);
            if (source == null) {
                throw new ClientException("cannot get StreamSource at uri: "
                        + uri);
            }
            source.copyTo(tempFile);
        } catch (IOException e) {
            throw new ClientException(e);
        }
        double end = System.currentTimeMillis();
        log.info(">>> download took " + ((end - start) / 1000) + " sec.");
        return tempFile;
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

    @Override
    public void importFromStreamSource(String uri,
            DocumentLocation targetLocation, String docReaderFactoryClassName,
            Map<String, Object> rFactoryParams,
            String docWriterFactoryClassName, Map<String, Object> wFactoryParams)
            throws ClientException {

        log.info("import source uri: " + uri);

        File tempFile = getLocalFile(uri);

        InputStream in = null;
        try {
            in = new FileInputStream(tempFile);
            importFromStream(in, targetLocation, docReaderFactoryClassName,
                    rFactoryParams, docWriterFactoryClassName, wFactoryParams);
        } catch (IOException e) {
            String msg = "Cannot import from uri: " + uri;
            throw new ClientException(msg, e);
        } finally {
            tempFile.delete();
            disposeExport(uri);
        }
    }

}
