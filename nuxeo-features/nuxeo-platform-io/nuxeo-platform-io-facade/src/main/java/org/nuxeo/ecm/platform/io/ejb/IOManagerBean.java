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
 * $Id: IOManagerBean.java 27118 2007-11-13 17:29:43Z dmihalache $
 */

package org.nuxeo.ecm.platform.io.ejb;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.io.api.IOManager;
import org.nuxeo.ecm.platform.io.api.IOResourceAdapter;
import org.nuxeo.ecm.platform.io.api.ejb.IOManagerLocal;
import org.nuxeo.ecm.platform.io.api.ejb.IOManagerRemote;
import org.nuxeo.runtime.api.Framework;

/**
 * IO Manager bean
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@Stateless
@Local(IOManagerLocal.class)
@Remote(IOManagerRemote.class)
public class IOManagerBean implements IOManager {

    private static final long serialVersionUID = 5868307995104155402L;

    private static final Log log = LogFactory.getLog(IOManagerBean.class);

    private IOManager service;

    @PostConstruct
    public void initialize() {
        try {
            // get Runtime service
            service = Framework.getLocalService(IOManager.class);
        } catch (Exception e) {
            log.error("Could not get IOManager service", e);
        }
    }

    public void remove() {
    }

    @Override
    public void addAdapter(String name, IOResourceAdapter adapter)
            throws ClientException {
        try {
            service.addAdapter(name, adapter);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public Collection<DocumentRef> copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, DocumentLocation targetLocation,
            Collection<String> ioAdapters) throws ClientException {
        try {
            return service.copyDocumentsAndResources(repo, sources,
                    targetLocation, ioAdapters);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public void exportDocumentsAndResources(OutputStream out, String repo,
            Collection<DocumentRef> sources, boolean recurse, String format,
            Collection<String> ioAdapters) throws ClientException {
        try {
            service.exportDocumentsAndResources(out, repo, sources, false,
                    format, ioAdapters);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public IOResourceAdapter getAdapter(String name) throws ClientException {
        try {
            return service.getAdapter(name);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    // ------------ this is not goood
    @Override
    public void importDocumentsAndResources(InputStream in, String repo,
            DocumentRef root) throws ClientException {
        try {
            service.importDocumentsAndResources(in, repo, root);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public void removeAdapter(String name) throws ClientException {
        try {
            service.removeAdapter(name);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public void copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, String serverAddress, int rmiPort,
            DocumentLocation targetLocation, Collection<String> ioAdapters)
            throws ClientException {
        try {
            service.copyDocumentsAndResources(repo, sources, serverAddress,
                    rmiPort, targetLocation, ioAdapters);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public void importExportedFile(String uri, DocumentLocation targetLocation)
            throws ClientException {
        try {
            service.importExportedFile(uri, targetLocation);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public void copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, String serverAddress,
            int jndiPort, DocumentLocation targetLocation,
            String docReaderFactoryName, Map<String, Object> rFactoryParams,
            String docWriterFactoryName, Map<String, Object> wFactoryParams,
            Collection<String> ioAdapters) throws ClientException {
        try {
            service.copyDocumentsAndResources(repo, sources, serverAddress,
                    jndiPort, targetLocation, docReaderFactoryName,
                    rFactoryParams, docWriterFactoryName, wFactoryParams,
                    ioAdapters);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public void importExportedFile(String uri, DocumentLocation targetLocation,
            String docWriterFactoryName, Map<String, Object> wFactoryParams)
            throws ClientException {
        try {
            service.importExportedFile(uri, targetLocation,
                    docWriterFactoryName, wFactoryParams);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public void copyDocumentsAndResources(String repo,
            Collection<DocumentRef> sources, IOManager remoteIOManager,
            DocumentLocation targetLocation, Collection<String> ioAdapters)
            throws ClientException {
        try {
            service.copyDocumentsAndResources(repo, sources, remoteIOManager,
                    targetLocation, ioAdapters);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public String externalizeExport(String repo,
            Collection<DocumentRef> sources, Collection<String> ioAdapters)
            throws ClientException {
        try {
            String uri = service.externalizeExport(repo, sources, ioAdapters);
            return uri;
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public String externalizeExport(String repo, String docReaderFactoryName,
            Map<String, Object> readerFactoryParams,
            Collection<String> ioAdapters) throws ClientException {
        try {
            String uri = service.externalizeExport(repo, docReaderFactoryName,
                    readerFactoryParams, ioAdapters);
            return uri;
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public String externalizeExport(String repo,
            Collection<DocumentRef> sources, String docReaderFactoryName,
            Map<String, Object> readerFactoryParams,
            Collection<String> ioAdapters) throws ClientException {
        try {
            String uri = service.externalizeExport(repo, sources,
                    docReaderFactoryName, readerFactoryParams, ioAdapters);
            return uri;
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public void disposeExport(String uri) throws ClientException {
        try {
            service.disposeExport(uri);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public void importFromStreamSource(String uri,
            DocumentLocation targetLocation, String docReaderFactoryClassName,
            Map<String, Object> rFactoryParams,
            String docWriterFactoryClassName, Map<String, Object> wFactoryParams)
            throws ClientException {
        try {
            service.importFromStreamSource(uri, targetLocation,
                    docReaderFactoryClassName, rFactoryParams,
                    docWriterFactoryClassName, wFactoryParams);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Override
    public void importFromStream(InputStream in,
            DocumentLocation targetLocation, String docReaderFactoryClassName,
            Map<String, Object> rFactoryParams,
            String docWriterFactoryClassName, Map<String, Object> wFactoryParams)
            throws ClientException {
        try {
            service.importFromStream(in, targetLocation,
                    docReaderFactoryClassName, rFactoryParams,
                    docWriterFactoryClassName, wFactoryParams);
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

}
