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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
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
 *
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
                        throw new IllegalArgumentException(
                                "Cannot get array from array property " + obj);
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
