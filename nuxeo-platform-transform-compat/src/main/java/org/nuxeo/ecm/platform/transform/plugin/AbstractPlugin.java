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
 * $Id: AbstractPlugin.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.plugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;

/**
 * Plugin abstract class.
 * <p>
 * Default plugin behavior.
 *
 * @see org.nuxeo.ecm.platform.transform.interfaces.Plugin
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@Deprecated
public abstract class AbstractPlugin implements Plugin {

    private static final long serialVersionUID = 8591897901747795662L;

    protected String name = "";

    protected List<String> sourceMimeTypes = new ArrayList<String>();

    protected String destinationMimeType = "";

    protected Map<String, Serializable> defaultOptions = new HashMap<String, Serializable>();

    protected AbstractPlugin() {
    }

    protected AbstractPlugin(String name) {
        this.name = name;
    }

    protected AbstractPlugin(String name, List<String> sourceMimeTypes,
            String destinationMimeType) {
        this.name = name;
        this.sourceMimeTypes = sourceMimeTypes;
        this.destinationMimeType = destinationMimeType;
    }

    protected AbstractPlugin(String name, List<String> sourceMimeTypes,
            String destinationMimeType, Map<String, Serializable> defaultOptions) {
        this.name = name;
        this.sourceMimeTypes = sourceMimeTypes;
        this.destinationMimeType = destinationMimeType;
        this.defaultOptions = defaultOptions;
    }

    public Map<String, Serializable> getDefaultOptions() {
        return defaultOptions;
    }

    public String getDestinationMimeType() {
        return destinationMimeType;
    }

    public String getName() {
        return name;
    }

    public List<String> getSourceMimeTypes() {
        return sourceMimeTypes;
    }

    public void setDefaultOptions(Map<String, Serializable> defaultOptions) {
        this.defaultOptions = defaultOptions;
    }

    public void setDestinationMimeType(String destinationMimeType) {
        this.destinationMimeType = destinationMimeType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSourceMimeTypes(List<String> sourceMimeTypes) {
        this.sourceMimeTypes = sourceMimeTypes;
    }

    /**
     * Sets specific options.
     * <p>
     * Override default plugin options.
     *
     * @param options a map from from string to serializable.
     */
    public void setSpecificOptions(Map<String, Serializable> options) {
        if (options != null) {
            for (String key : options.keySet()) {
                // We override the default ones here.
                defaultOptions.put(key, options.get(key));
            }
        }
    }

    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        setSpecificOptions(options);
        return new ArrayList<TransformDocument>();
    }

    public List<TransformDocument> transform(Map<String, Serializable> options,
            Blob... blobs) throws Exception {

        TransformDocument[] trs = new TransformDocument[blobs.length];
        for (int i = 0; i < blobs.length; i++) {
            trs[i] = new TransformDocumentImpl(blobs[i]);
        }
        return transform(options, trs);
    }

    public boolean isSourceCandidate(TransformDocument doc) {
        String smtype;
        try {
            smtype = doc.getMimetype();
        } catch (Exception e) {
            smtype = null;
        }

        boolean candidate = false;
        if (smtype == null || sourceMimeTypes.contains(smtype)) {
            candidate = true;
        }

        return candidate;
    }

    public boolean isSourceCandidate(Blob blob) {
        String smtype;
        try {
            smtype = blob.getMimeType();
        } catch (Exception e) {
            smtype = null;
        }

        boolean candidate = false;
        if (smtype == null || sourceMimeTypes.contains(smtype)) {
            candidate = true;
        }

        return candidate;
    }

}
