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
 * $Id: TransformDocumentImpl.java 21688 2007-06-30 21:55:55Z sfermigier $
 */
package org.nuxeo.ecm.platform.transform.document;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.mimetype.MimetypeDetectionException;
import org.nuxeo.ecm.platform.mimetype.MimetypeNotFoundException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.transform.exceptions.MimetypeRegistryNotFoundException;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.runtime.api.Framework;

/**
 * Transform document implementation.
 *
 * @see TransformDocument
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TransformDocumentImpl implements TransformDocument {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(TransformDocumentImpl.class);

    private static transient MimetypeRegistry mimeService;

    protected String mimetype;

    protected final Blob blob;

    protected final Map<String, Serializable> properties = new HashMap<String, Serializable>();

    public TransformDocumentImpl() {
        blob = new StringBlob("");
    }

    public TransformDocumentImpl(Blob blob) {
        this.blob = blob;
    }

    public TransformDocumentImpl(Blob blob, String mimetype) {
        this.blob = blob;
        this.mimetype = mimetype;
    }

    public String getMimetype() throws MimetypeRegistryNotFoundException,

    MimetypeNotFoundException, MimetypeDetectionException {
        if (mimetype == null) {
            // Lazily detect mimetype.
            setMimetype(null);
        }
        return mimetype;
    }

    public void setMimetype(String mimetype)
            throws MimetypeRegistryNotFoundException,
            MimetypeNotFoundException, MimetypeDetectionException {
        if (mimetype != null) {
            this.mimetype = mimetype;
        } else {
            this.mimetype = blob.getMimeType();
            if (this.mimetype == null) {
                MimetypeRegistry reg = getMimetypeRegistry();
                if (reg == null) {
                    throw new MimetypeRegistryNotFoundException();
                }
                this.mimetype = reg.getMimetypeFromBlob(blob);
            }
        }
    }

    private static MimetypeRegistry getMimetypeRegistry() {
        if (mimeService == null) {
            try {
                mimeService = Framework.getService(MimetypeRegistry.class);
            } catch (Exception e) {
                log.error("Unable to get Mimetype Service : " + e.getMessage());
            }
        }
        return mimeService;
    }

    public Serializable getPropertyValue(String name) {
        return properties.get(name);
    }

    public void setPropertyValue(String name, Serializable value) {
        properties.put(name, value);
    }

    public Map<String, Serializable> getProperties() {
        return properties;
    }

    public Blob getBlob() {
        return blob;
    }

}
