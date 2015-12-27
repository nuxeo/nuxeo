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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;

import freemarker.ext.beans.ArrayModel;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentObjectWrapper extends DefaultObjectWrapper {

    protected final FreemarkerEngine engine;

    public DocumentObjectWrapper(FreemarkerEngine engine) {
        this.engine = engine;
    }

    @Override
    public final TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj == null) {
            return null;
        }
        if (obj instanceof DocumentModel) {
            return new DocumentTemplate(this, (DocumentModel) obj);
        } else if (obj instanceof SchemaTemplate.DocumentSchema) {
            return new SchemaTemplate(this, (SchemaTemplate.DocumentSchema) obj);
        } else if (obj instanceof Property) {
            Property p = (Property) obj;
            if (p.isScalar()) {
                return new PropertyWrapper(this).wrap(p);
            } else if (p.isList()) {
                if (obj instanceof ListProperty) {
                    return new ListPropertyTemplate(this, (ListProperty) obj);
                } else if (obj instanceof ArrayProperty) {
                    Object value;
                    try {
                        value = ((ArrayProperty) obj).getValue();
                    } catch (PropertyException e) {
                        throw new IllegalArgumentException("Cannot get array from array property " + obj);
                    }
                    if (value == null) {
                        return TemplateModel.NOTHING;
                    }
                    return new ArrayModel(value, this);
                }
            } else if (p.getClass() == BlobProperty.class) {
                try {
                    Blob blob = (Blob) p.getValue();
                    if (blob == null) {
                        return TemplateModel.NOTHING;
                    } else {
                        return new BlobTemplate(this, blob);
                    }
                } catch (PropertyException e) {
                    throw new TemplateModelException(e);
                }
            } else {
                return new ComplexPropertyTemplate(this, (Property) obj);
            }
        }
        return super.wrap(obj);
    }

}
