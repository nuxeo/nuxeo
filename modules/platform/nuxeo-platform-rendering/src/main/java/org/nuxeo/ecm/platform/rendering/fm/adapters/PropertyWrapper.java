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

import java.util.ArrayList;
import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;

import freemarker.ext.beans.ArrayModel;
import freemarker.template.SimpleDate;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PropertyWrapper {

    protected final DocumentObjectWrapper wrapper;

    public PropertyWrapper(DocumentObjectWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public TemplateModel wrap(Property property) throws TemplateModelException {
        try {
            if (property.isScalar()) {
                Object value = property.getValue();
                if (value == null) {
                    return TemplateModel.NOTHING;
                }
                if (property.getType() == DateType.INSTANCE) {
                    return new SimpleDate(((Calendar) value).getTime(), TemplateDateModel.DATETIME);
                }
                return wrapper.wrap(value);
            } else if (property.isList()) {
                if (property.isContainer()) {
                    return new ListPropertyTemplate(wrapper, (ListProperty) property);
                } else {
                    Object value;
                    try {
                        value = property.getValue();
                    } catch (PropertyException e) {
                        throw new IllegalArgumentException("Cannot get array from array property " + property);
                    }
                    if (value == null) {
                        return TemplateModel.NOTHING;
                    }
                    if (value instanceof ArrayList) {
                        // FIXME: some instances of ListProperty will answer "false"
                        // to isContainer()
                        return new ListPropertyTemplate(wrapper, (ListProperty) property);
                    }
                    return new ArrayModel(value, wrapper);
                }
            } else if (property.getClass() == BlobProperty.class) {
                return new BlobTemplate(wrapper, (Blob) property.getValue());
            } else {
                return new ComplexPropertyTemplate(wrapper, property);
            }
        } catch (PropertyException e) {
            throw new TemplateModelException(e);
        }
    }

}
