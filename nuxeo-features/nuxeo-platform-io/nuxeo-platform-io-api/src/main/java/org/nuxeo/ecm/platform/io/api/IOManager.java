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
 * $Id: IOManager.java 30145 2008-02-13 16:56:56Z dmihalache $
 */

package org.nuxeo.ecm.platform.io.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.exceptions.ExportDocumentException;
import org.nuxeo.ecm.core.io.exceptions.ImportDocumentException;

/**
 * Service handling complex import/export of documents and associated resources.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface IOManager extends Serializable {

    String DOCUMENTS_ADAPTER_NAME = "documents";

    /**
     * Returns the adapter with given name.
     */
    IOResourceAdapter getAdapter(String name) throws ClientException;

    /**
     * Adds an adapter with given name and definition.
     */
    void addAdapter(String name, IOResourceAdapter adapter)
            throws ClientException;

    /**
     * Removes adapter with given name.
     */
    void removeAdapter(String name) throws ClientException;

    /**
     * Import document and resources described by given input stream at given
     * document location.
     *
     * @param in stream representing the documents and resources to import. Can
     *            be a zip file of a group of export files. The service is
     *            responsible for unzipping and redirecting import to specific
     *            import services.
     * @param repo the repository name.
     * @param root Optional location of document that must be taken as root of
     *            the import (can be null).
     */
    void importDocumentsAndResources(InputStream in, String repo,
            DocumentRef root) throws IOException, ClientException,
            ImportDocumentException;

    /**
     * Export documents and resources.
     *
     * @param out stream that can be turned into a zip holding a group of file for
     *            each additional resources types.
     * @param repo TODO
     * @param sources locations of documents to export.
     * @param recurse recurse into sources children
     * @param format export format. XXX see what format is actually accepted.
     * @param ioAdapters list of adapters to use for additional resources.
     */
    void exportDocumentsAndResources(OutputStream out, String repo,
            Collection<DocumentRef> sources, boolean recurse, String format,
            Collection<String> ioAdapters) throws IOException, ClientException,
            ExportDocumentException;

    /**
     * Copy documents and resources to another location (on a same machine).
     *
     * @param repo the initial repository name.
     * @param sources locations of documents to export.
     * @param targetLocation location of the document where copies must be
     *            placed.
     * @param ioAdapters list of adapters to use for additional resources.
     * @return the list of copied documents references.
     */
    Collection<DocumentRef> copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, DocumentLocation targetLocation,
            Collection<String> ioAdapters) throws ClientException;

    /**
     * Remote copy. Copy documents and resources to another location.
     */
    void copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, String serverAddress,
            int jndiPort, DocumentLocation targetLocation,
            Collection<String> ioAdapters) throws ClientException;

    /**
     * Remote copy. Copy documents and resources to another location.
     */
    void copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, IOManager remoteIOManager,
            DocumentLocation targetLocation, Collection<String> ioAdapters)
            throws ClientException;

    /**
     * Copies documents and resources associated with the given sources from the
     * local repository to a remote repository instance (available on a machine
     * with the given serverAddress).
     * <p>
     * Also if there are custom DocumentReader and DocumentWriter classes that should
     * be used when externalizing documents and/or when re-creating them at the
     * remote location, they could be created through specific factory classes
     * (specified as class names). These factory classes are instantiated and
     * provided with the given factoryParams in order to build custom DocumentReader
     * or DocumentWriter objects.
     *
     * @param repo
     * @param sources
     * @param serverAddress
     * @param jndiPort
     * @param targetLocation
     * @param docReaderFactoryClassName classname for a DocumentReaderFactory custom implementation
     * @param rFactoryParams params to be given to custom DocumentReaderFactory to create DocumentReader
     * @param docWriterFactoryClassName classname for a DocumentWriterFactory custom implementation
     * @param wFactoryParams params to be given to custom DocumentWriterFactory to create DocumentWriter
     * @param ioAdapters
     */
    void copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, String serverAddress,
            int jndiPort, DocumentLocation targetLocation,
            String docReaderFactoryClassName, Map<String, Object> rFactoryParams,
            String docWriterFactoryClassName, Map<String, Object> wFactoryParams,
            Collection<String> ioAdapters) throws ClientException;

    /**
     * Exports the documents and resources specified by sources and creates an
     * external reachable URI (through stream service) from where a client could
     * download the export.
     */
    String externalizeExport(String repo, Collection<DocumentRef> sources,
            Collection<String> ioAdapters) throws ClientException;

    /**
     * Exports the documents and resources using custom DocumentReader that will
     * be created by specified readerFactoryName. Creates an external reachable
     * URI (through stream service) from where a client could download the
     * export.
     */
    String externalizeExport(String repo, String docReaderFactoryName,
            Map<String, Object> readerFactoryParams,
            Collection<String> ioAdapters) throws ClientException;

    /**
     * Exports documents as an streamable archive - using all options
     */
    String externalizeExport(String repo,
            Collection<DocumentRef> sources, String docReaderFactoryName,
            Map<String, Object> readerFactoryParams,
            Collection<String> ioAdapters) throws ClientException;

    /**
     * Should be called by client code after using the export created by
     * calling the {@link #externalizeExport}.
     */
    void disposeExport(String uri) throws ClientException;

    /**
     * Will be called after a successful export to import data from the file.
     */
    void importExportedFile(String uri, DocumentLocation targetLocation)
            throws ClientException;

    /**
     * Will be called after a successful export to import data from the file.
     */
    void importExportedFile(String uri, DocumentLocation targetLocation,
            String docWriterFactoryClassName, Map<String, Object> wFactoryParams)
            throws ClientException;

    void importFromStreamSource(String uri, DocumentLocation targetLocation,
            String docReaderFactoryClassName,
            Map<String, Object> rFactoryParams,
            String docWriterFactoryClassName, Map<String, Object> wFactoryParams)
            throws ClientException;

    void importFromStream(InputStream in,
            DocumentLocation targetLocation, String docReaderFactoryClassName,
            Map<String, Object> rFactoryParams,
            String docWriterFactoryClassName, Map<String, Object> wFactoryParams)
            throws ClientException;

}
