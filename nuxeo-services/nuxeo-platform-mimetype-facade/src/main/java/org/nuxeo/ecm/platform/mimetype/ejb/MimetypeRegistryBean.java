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
 * $Id: Registry.java 2531 2006-09-04 23:01:57Z janguenot $
 */
package org.nuxeo.ecm.platform.mimetype.ejb;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.ejb.interfaces.local.MimetypeRegistryLocal;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.ExtensionDescriptor;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.runtime.api.Framework;

/**
 * MimetypeEntry registry bean.
 * <p>
 * EJB Facade on the mimetype registry service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Stateless
@Local(MimetypeRegistryLocal.class)
@Remote(MimetypeRegistry.class)
public class MimetypeRegistryBean implements MimetypeRegistryLocal {

    private MimetypeRegistry service;

    private MimetypeRegistry getService() {
        if (service == null) {
            service = Framework.getLocalService(MimetypeRegistry.class);
        }
        return service;
    }

    public List<String> getExtensionsFromMimetypeName(String mimetypeName) {
        return getService().getExtensionsFromMimetypeName(mimetypeName);
    }

    // FIXME: you can't use File in an EJB context.
    public String getMimetypeFromFile(File file)
            throws MimetypeNotFoundException, MimetypeDetectionException {
        return getService().getMimetypeFromFile(file);
    }

    @Deprecated
    // use getMimetypeFromBlob instead (and better with StreamingBlob)
    public String getMimetypeFromStream(InputStream stream)
            throws MimetypeNotFoundException, MimetypeDetectionException {
        return getService().getMimetypeFromStream(stream);
    }

    @Deprecated
    // use getMimetypeFromBlobWithDefault instead (and better with
    // StreamingBlob)
    public String getMimetypeFromStreamWithDefault(InputStream is,
            String defaultMimetype) throws MimetypeDetectionException {
        return getService().getMimetypeFromStreamWithDefault(is,
                defaultMimetype);
    }

    public String getMimetypeFromBlob(Blob blob)
            throws MimetypeNotFoundException, MimetypeDetectionException {
        return getService().getMimetypeFromBlob(blob);
    }

    public String getMimetypeFromBlobWithDefault(Blob blob,
            String defaultMimetype) throws MimetypeDetectionException {
        return getService().getMimetypeFromBlobWithDefault(blob,
                defaultMimetype);
    }

    public String getMimetypeFromFilenameAndBlobWithDefault(String filename,
            Blob blob, String defaultMimetype)
            throws MimetypeDetectionException {
        return getService().getMimetypeFromFilenameAndBlobWithDefault(filename,
                blob, defaultMimetype);
    }

    // to be removed !!!!
    @Deprecated
    public MimetypeEntry getMimetypeEntryByName(String name) {
        return getService().getMimetypeEntryByName(name);
    }

    public MimetypeEntry getMimetypeEntryByMimeType(String mimetype) {
        return getService().getMimetypeEntryByMimeType(mimetype);
    }

    // make it easier to test the bean API
    public void registerMimetype(MimetypeEntry mimetype) {
        ((MimetypeRegistryService) getService()).registerMimetype(mimetype);
    }

    public void unregisterMimetype(String mimetype) {
        ((MimetypeRegistryService) getService()).unregisterMimetype(mimetype);
    }

    public void registerFileExtension(ExtensionDescriptor extension) {
        ((MimetypeRegistryService) getService()).registerFileExtension(extension);
    }

    public void unregisterFileExtension(ExtensionDescriptor extension) {
        ((MimetypeRegistryService) getService()).unregisterFileExtension(extension);
    }

    public Blob updateMimetype(Blob blob, String filename)
            throws MimetypeDetectionException {
        return getService().updateMimetype(blob, filename);
    }

    public Blob updateMimetype(Blob blob) throws MimetypeDetectionException {
        return getService().updateMimetype(blob);
    }

}
