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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;

import freemarker.template.AdapterTemplateModel;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BlobTemplate extends PropertyWrapper implements TemplateHashModelEx, AdapterTemplateModel {

    protected static final String[] keys = { "filename", "data", "length", "mimeType", "encoding", "digest" };

    protected final Blob blob;

    public BlobTemplate(DocumentObjectWrapper wrapper, Blob blob) {
        super(wrapper);
        this.blob = blob;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdaptedObject(Class hint) {
        return blob;
    }

    @Override
    public TemplateCollectionModel keys() throws TemplateModelException {
        return (TemplateCollectionModel) wrapper.wrap(keys);
    }

    @Override
    public int size() throws TemplateModelException {
        return keys.length;
    }

    @Override
    public TemplateCollectionModel values() throws TemplateModelException {
        try {
            List<Object> list = new ArrayList<>(keys.length);
            if (blob != null) {
                list.add(blob.getFilename());
                list.add(blob.getString());
                list.add(blob.getLength());
                list.add(blob.getMimeType());
                list.add(blob.getEncoding());
                list.add(blob.getDigest());
            } else {
                list.add("");
                list.add("");
                list.add("");
                list.add("");
                list.add("");
                list.add("");
            }
            return (TemplateCollectionModel) wrapper.wrap(list);
        } catch (IOException e) {
            throw new TemplateModelException("Failed to adapt complex property values", e);
        }
    }

    @Override
    public TemplateModel get(String name) throws TemplateModelException {
        try {
            if (blob != null) {
                if (keys[0].equals(name)) {
                    return new SimpleScalar(blob.getFilename());
                } else if (keys[1].equals(name)) {
                    return new SimpleScalar(blob.getString());
                } else if (keys[2].equals(name)) {
                    return new SimpleNumber(blob.getLength());
                } else if (keys[3].equals(name)) {
                    return new SimpleScalar(blob.getMimeType());
                } else if (keys[4].equals(name)) {
                    return new SimpleScalar(blob.getEncoding());
                } else if (keys[5].equals(name)) {
                    return new SimpleScalar(blob.getDigest());
                }
            }
            return NOTHING;
        } catch (IOException e) {
            throw new TemplateModelException(e);
        }
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

}
