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
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.properties;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Property;
import org.nuxeo.ecm.core.repository.jcr.JCRNodeProxy;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class CompositePropertyFactory {

    static final Map<String, CompositePropertyFactory> FACTORIES
            = new HashMap<String, CompositePropertyFactory>();

    public abstract Property create(
            JCRNodeProxy parent, Node property, Field field)
            throws DocumentException;


    public static Property getProperty(JCRNodeProxy parent, Node property,
            Field field) throws DocumentException {
        assert field != null;
        if (property == null) {
            assert parent != null;
            String name = field.getName().getPrefixedName();
            try {
                if (parent.isConnected()) {
                    property = parent.getNode().getNode(name);
                } else {
                    assert name != null;
                }
            } catch (PathNotFoundException e) {
                assert name != null;
            } catch (RepositoryException e) {
                throw new DocumentException("failed to create complex property " + name, e);
            }
        }
        CompositePropertyFactory factory = FACTORIES.get(field.getType().getName());
        if (factory != null) {
            return factory.create(parent, property, field);
        }
        if (field.getType().isListType()) {
            return new JCRComplexListProperty(parent, property, field);
        } else {
            return new JCRComplexProperty(parent, property, field);
        }
    }

    // ------------- factories --------------

    public static final CompositePropertyFactory CONTENT = new CompositePropertyFactory() {
        @Override
        public org.nuxeo.ecm.core.model.Property create(JCRNodeProxy parent,
                Node property, Field field) {
            return new BlobProperty(parent, property, field);
        }
    };

    static {
        FACTORIES.put(TypeConstants.CONTENT, CONTENT);
    }

}
