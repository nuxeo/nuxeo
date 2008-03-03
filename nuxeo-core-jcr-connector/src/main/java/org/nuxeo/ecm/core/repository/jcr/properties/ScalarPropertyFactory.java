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

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.repository.jcr.JCRNodeProxy;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ScalarPropertyFactory {

    static final Map<String, ScalarPropertyFactory> factories = new HashMap<String, ScalarPropertyFactory>();

    public abstract org.nuxeo.ecm.core.model.Property create(
            JCRNodeProxy parent, Property property, Field field)
            throws DocumentException;


    /**
     * Gets the property instance.
     * <p>
     * If the parent argument is null, then the property argument must not
     * be null.
     * <p>
     * If the property argument is null, then the parent and name property
     * must not be null.
     *
     * @param parent the proxy node parent.
     *        Must not be null if property is null
     * @param property the JCR property to wrap.
     *        If this is null then parent and name must be both not null
     * @param field
     * @return the property
     * @throws DocumentException
     */
    public static org.nuxeo.ecm.core.model.Property getProperty(JCRNodeProxy parent,
            Property property, Field field) throws DocumentException {
        assert field != null;
        if (property == null) {
            assert parent != null;
            String name = field.getName().getPrefixedName();
            try {
                if (parent.isConnected()) {
                    property = parent.getNode().getProperty(name);
                } else {
                    assert name != null;
                }
            } catch (PathNotFoundException e) {
                assert name != null;
            } catch (RepositoryException e) {
                throw new DocumentException("failed to create scalar property " + name, e);
            }
        }
        Type type = field.getType();
        ScalarPropertyFactory factory;
        if (type.isListType()) { // support for scalar lists (stored as multivalue properties)
            factory = ARRAY;
        } else {
            factory = factories.get(field.getType().getName());
        }
        if (factory != null) {
            return factory.create(parent, property, field);
        }
        return new StringProperty(parent, property, field);
        //throw new DocumentException("cannot find a property factory for type "+type.getName());
    }

    // ------------- factories --------------

    public static final ScalarPropertyFactory STRING = new ScalarPropertyFactory() {
        @Override
        public org.nuxeo.ecm.core.model.Property create(JCRNodeProxy parent, Property property, Field field) {
            return new StringProperty(parent, property, field);
        }
    };

    public static final ScalarPropertyFactory LONG = new ScalarPropertyFactory() {
        @Override
        public org.nuxeo.ecm.core.model.Property create(JCRNodeProxy parent, Property property, Field field) {
            return new LongProperty(parent, property, field);
        }
    };

    public static final ScalarPropertyFactory DOUBLE = new ScalarPropertyFactory() {
        @Override
        public org.nuxeo.ecm.core.model.Property create(JCRNodeProxy parent, Property property, Field field) {
            return new DoubleProperty(parent, property, field);
        }
    };

    public static final ScalarPropertyFactory DATE = new ScalarPropertyFactory() {
        @Override
        public org.nuxeo.ecm.core.model.Property create(JCRNodeProxy parent, Property property, Field field) {
            return new DateProperty(parent, property, field);
        }
    };

    public static final ScalarPropertyFactory BOOLEAN = new ScalarPropertyFactory() {
        @Override
        public org.nuxeo.ecm.core.model.Property create(JCRNodeProxy parent, Property property, Field field) {
            return new BooleanProperty(parent, property, field);
        }
    };

    public static final ScalarPropertyFactory ARRAY = new ScalarPropertyFactory() {
        @Override
        public org.nuxeo.ecm.core.model.Property create(JCRNodeProxy parent, Property property, Field field) {
            return new ArrayProperty(parent, property, field);
        }
    };

    public static final ScalarPropertyFactory BINARY = new ScalarPropertyFactory() {
        @Override
        public org.nuxeo.ecm.core.model.Property create(JCRNodeProxy parent, Property property, Field field) {
            return new BinaryProperty(parent, property, field);
        }
    };

    static {
        factories.put(StringType.ID, STRING);
        factories.put(DoubleType.ID, DOUBLE);
        factories.put(LongType.ID, LONG);
        factories.put(DateType.ID, DATE);
        factories.put(BooleanType.ID, BOOLEAN);
        factories.put(BinaryType.ID, BINARY);
    }
}
