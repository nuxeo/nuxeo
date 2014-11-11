/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
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
                    return new SimpleDate(((Calendar) value).getTime(),
                            TemplateDateModel.DATETIME);
                }
                return wrapper.wrap(value);
            } else if (property.isList()) {
                if (property.isContainer()) {
                    return new ListPropertyTemplate(wrapper,
                            (ListProperty) property);
                } else {
                    Object value;
                    try {
                        value = property.getValue();
                    } catch (PropertyException e) {
                        throw new IllegalArgumentException(
                                "Cannot get array from array property "
                                        + property);
                    }
                    if (value == null) {
                        return TemplateModel.NOTHING;
                    }
                    if (value instanceof ArrayList) {
                        // FIXME: some instances of ListProperty will answer "false"
                        // to isContainer()
                        return new ListPropertyTemplate(wrapper,
                                (ListProperty) property);
                    }
                    return new ArrayModel(value, wrapper);
                }
            } else if (property.getClass() == BlobProperty.class) {
                return new BlobTemplate(wrapper, (Blob) property.getValue());
            } else {
                return new ComplexPropertyTemplate(wrapper, property);
            }
        } catch (Exception e) {
            throw new TemplateModelException(e);
        }
    }

}
