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

package org.nuxeo.ecm.platform.el;

import java.io.Serializable;

import javax.el.BeanELResolver;
import javax.el.ELContext;
import javax.el.PropertyNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;

/**
 * Resolves expressions for the {@link DocumentModel} framework.
 * <p>
 * To specify a property on a document mode, the following syntax is available:
 * <code>myDocumentModel.dublincore.title</code> where 'dublincore' is the
 * schema name and 'title' is the field name. It can be used to get or set the
 * document title: {@code <h:outputText value="#{currentDocument.dublincore.title}" />}
 * or {@code <h:inputText value="#{currentDocument.dublincore.title}" />}.
 * <p>
 * Simple document properties are get/set directly: for instance, the above
 * expression will return a String value on get, and set this String on the
 * document for set. Complex properties (maps and lists) are get/set through
 * the {@link Property} object controlling their value: on get, sub properties
 * will be resolved at the next iteration, and on set, they will be set on the
 * property instance so the document model is aware of the change.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
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
                        + property);
                value = getDocumentPropertyValue(docProperty);
            } catch (ClientException pe) {
                // avoid errors, return null
                log.warn(pe.getMessage());
            }
            context.setPropertyResolved(true);
        } else if (base instanceof Property) {
            try {
                Property docProperty = (Property) base;
                Property subProperty = getDocumentProperty(docProperty,
                        property);
                value = getDocumentPropertyValue(subProperty);
            } catch (PropertyException pe) {
                // avoid errors, return null
                log.warn(pe.getMessage());
            }
            context.setPropertyResolved(true);
        }

        return value;
    }

    private static Property getDocumentProperty(Property docProperty,
            Object propertyValue) throws PropertyException {
        Property subProperty = null;
        if ((docProperty instanceof ArrayProperty || docProperty instanceof ListProperty)
                && propertyValue instanceof Long) {
            subProperty = docProperty.get(((Long) propertyValue).intValue());
        } else if ((docProperty instanceof ArrayProperty || docProperty instanceof ListProperty)
                && propertyValue instanceof Integer) {
            Integer idx = (Integer) propertyValue;
            if (idx < docProperty.size()) {
                subProperty = docProperty.get((Integer) propertyValue);
            }
        } else if (docProperty instanceof ComplexProperty
                && propertyValue instanceof String) {
            subProperty = docProperty.get((String) propertyValue);
        }
        if (subProperty == null) {
            throw new PropertyException(String.format(
                    "Could not resolve subproperty '%s' under '%s'",
                    propertyValue, docProperty.getPath()));
        }
        return subProperty;
    }

    private static Object getDocumentPropertyValue(Property docProperty)
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
            try {
                ctx.doc.setPropertyValue(ctx.schema + ":" + (String) property,
                        (Serializable) value);
            } catch (ClientException e) {
                // avoid errors here too
                log.warn(e.getMessage());
            }
            context.setPropertyResolved(true);
        } else if (base instanceof Property) {
            try {
                Property docProperty = (Property) base;
                Property subProperty = getDocumentProperty(docProperty,
                        property);
                value = FieldAdapterManager.getValueForStorage(value);
                subProperty.setValue(value);
            } catch (PropertyException pe) {
                // avoid errors here too
                log.warn(pe.getMessage());
            }
            context.setPropertyResolved(true);
        }
    }

}
