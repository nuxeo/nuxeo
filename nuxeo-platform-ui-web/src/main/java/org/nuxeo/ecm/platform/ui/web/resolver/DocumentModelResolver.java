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
 * $Id: DocumentModelResolver.java 23589 2007-08-08 16:50:40Z fguillaume $
 */

package org.nuxeo.ecm.platform.ui.web.resolver;

import javax.el.BeanELResolver;
import javax.el.ELContext;
import javax.el.PropertyNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;

/**
 * Resolves our custom expressions based on our custom {@link DocumentModel}.
 * <p>
 * In order to specify a given property of the {@link DocumentModel} the
 * following syntax is available: <code>myDocumentModel.dublincore.title</code>
 * where common is the schema name and title is the schema property. Using this
 * you can access the document title for example.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class DocumentModelResolver extends BeanELResolver {

    private static final Log log = LogFactory.getLog(DocumentModelResolver.class);

    // XXX AT: see if getFeatureDescriptor needs to be overloaded to return
    // datamodels descriptors.

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        Class<?> type = null;
        if (base instanceof DocumentModel) {
            try {
                type = super.getType(context, base, property);
            } catch (PropertyNotFoundException e) {
                type = DocumentPropertyContext.class;
                context.setPropertyResolved(true);
            }
        } else if (base instanceof DocumentPropertyContext
                || base instanceof Property) {
            type = Object.class;
            context.setPropertyResolved(true);
        }
        return type;
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        Object value = null;
        if (base instanceof DocumentModel) {
            try {
                // try document getters first to resolve doc.id for instance
                value = super.getValue(context, base, property);
            } catch (PropertyNotFoundException e) {
                value = new DocumentPropertyContext((DocumentModel) base,
                        (String) property);
                context.setPropertyResolved(true);
            }
        } else if (base instanceof DocumentPropertyContext) {
            try {
                DocumentPropertyContext ctx = (DocumentPropertyContext) base;
                Property docProperty = ctx.doc.getProperty(ctx.schema + ":"
                        + (String) property);
                value = getDocumentPropertyValue(docProperty);
                context.setPropertyResolved(true);
            } catch (PropertyException pe) {
            }
        } else if (base instanceof Property) {
            try {
                Property docProperty = (Property) base;
                Property subProperty = getDocumentProperty(docProperty,
                        property);
                value = getDocumentPropertyValue(subProperty);
                context.setPropertyResolved(true);
            } catch (PropertyException pe) {
            }
        }

        // XXX end by bean resolver to overcome a mysterious StackOverflow error
        // at first login, and maybe resolve Property getters.
        if (!context.isPropertyResolved()) {
            try {
                value = super.getValue(context, base, property);
            } catch (Exception e) {
                context.setPropertyResolved(false);
            }
        }

        return value;
    }

    private Property getDocumentProperty(Property docProperty,
            Object propertyValue) throws PropertyException {
        Property subProperty = null;
        if ((docProperty instanceof ArrayProperty || docProperty instanceof ListProperty)
                && propertyValue instanceof Long) {
            subProperty = docProperty.get(((Long) propertyValue).intValue());
        } else if (docProperty instanceof ComplexProperty
                && propertyValue instanceof String) {
            subProperty = docProperty.get((String) propertyValue);
        }
        if (subProperty == null) {
            throw new PropertyException("Property not found");
        }
        return subProperty;
    }

    private Object getDocumentPropertyValue(Property docProperty)
            throws PropertyException {
        if (docProperty == null) {
            throw new PropertyException("Null property");
        }
        Object value = docProperty;
        if (!docProperty.isContainer()) {
            // return the value
            value = docProperty.getValue();
            value = FieldAdapterManager.getValueForDisplay(value);
        }
        return value;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        boolean readOnly = false;
        try {
            readOnly = super.isReadOnly(context, base, property);
        } catch (PropertyNotFoundException e) {
            if (base instanceof DocumentModel
                    || base instanceof DocumentPropertyContext) {
                readOnly = false;
                context.setPropertyResolved(true);
            } else if (base instanceof Property) {
                readOnly = ((Property) base).isReadOnly();
                context.setPropertyResolved(true);
            }
        }
        return readOnly;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property,
            Object value) {
        if (base instanceof DocumentModel) {
            try {
                super.setValue(context, base, property, value);
            } catch (PropertyNotFoundException e) {
                // nothing else to set on doc model
            }
        } else if (base instanceof DocumentPropertyContext) {
            DocumentPropertyContext ctx = (DocumentPropertyContext) base;
            value = FieldAdapterManager.getValueForStorage(value);
            ctx.doc.setProperty(ctx.schema, (String) property, value);
            context.setPropertyResolved(true);
        } else if (base instanceof Property) {
            try {
                Property docProperty = (Property) base;
                Property subProperty = getDocumentProperty(docProperty,
                        property);
                value = FieldAdapterManager.getValueForStorage(value);
                subProperty.setValue(value);
                context.setPropertyResolved(true);
            } catch (PropertyException pe) {
            }
        }
    }

}
