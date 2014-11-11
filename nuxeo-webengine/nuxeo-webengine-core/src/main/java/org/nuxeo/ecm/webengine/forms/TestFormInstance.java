/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.forms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Form instance to be used in unit tests.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestFormInstance implements FormInstance {

    protected final Map<String, String[]> params;
    protected final Map<String, Blob[]> blobs;

    public TestFormInstance(Map<String, String[]> params, Map<String, Blob[]> blobs) {
        if (params == null) {
            this.params = new HashMap<String, String[]>();
        } else {
            this.params = params;
        }
        if (blobs == null) {
            this.blobs = new HashMap<String, Blob[]>();
        } else {
            this.blobs = blobs;
        }
    }

    public TestFormInstance() {
        this(null, null);
    }

    public TestFormInstance(Map<String, String[]> params) {
        this(params, null);
    }

    public void setField(String key, String ... values) {
        params.put(key, values);
    }

    public void addField(String key, String ... values) {
        String[] ar = params.get(key);
        if (ar  == null) {
            params.put(key, values);
        } else {
            String[] tmp = new String[ar.length+values.length];
            System.arraycopy(ar, 0, tmp, 0, ar.length);
            System.arraycopy(values, 0, tmp, ar.length, values.length);
            params.put(key, tmp);
        }
    }

    public void setField(String key, Blob ... values) {
        blobs.put(key, values);
    }

    public void addField(String key, Blob ... values) {
        Blob[] ar = blobs.get(key);
        if (blobs == null) {
            blobs.put(key, values);
        } else {
            Blob[] tmp = new Blob[ar.length+values.length];
            System.arraycopy(ar, 0, tmp, 0, ar.length);
            System.arraycopy(values, 0, tmp, ar.length, values.length);
            blobs.put(key, tmp);
        }
    }

    public Collection<String> getKeys() {
        List<String> result = new ArrayList<String>();
        result.addAll(params.keySet());
        result.addAll(blobs.keySet());
        return result;
    }

    /**
     * TODO XXX implement it
     */
    public void fillDocument(DocumentModel doc) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Object[] get(String key) {
        Object[] val =  params.get(key);
        if (val == null) {
            val = blobs.get(key);
        }
        return val;
    }

    public Blob getBlob(String key) {
        Blob[] blobs = this.blobs.get(key);
        return blobs == null ? null : blobs[0];
    }

    public Blob[] getBlobs(String key) {
        return blobs.get(key);
    }

    public Map<String, Blob[]> getBlobFields() {
        return blobs;
    }

    public Map<String, String[]> getFormFields() {
        return params;
    }

    public String[] getList(String key) {
        return params.get(key);
    }

    public String getString(String key) {
        String[] values = params.get(key);
        return values == null ? null : values[0];
    }

}
