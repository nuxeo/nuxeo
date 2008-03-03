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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr;

import java.io.Serializable;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.AbstractProperty;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentPartReader {

    // Utility class.
    private DocumentPartReader() {
    }

    public static void readDocumentPart(JCRDocument doc, DocumentPart dp) throws Exception {
        // use the key to transmit the repository ID and user session ID - this is needed by lazy blobs
        // to be referenced later from the client -> TODO this should be refactored : lazy blob
        // replaced by streaming blobs which are also lazy
        dp.setData(
                doc.getRepository().getName() + ':' + doc.getSession().getUserSessionId());

        // proxy document is forwarding props to refered doc
        Node parent = doc.isProxy() ? ((JCRDocumentProxy)doc).getTargetNode()
                : doc.getNode();
        for (Property prop : dp) {
            readProperty(parent, prop);
        }

        // reset the key
        dp.setData(null);
    }

    public static void readProperty(Node parent, Property prop) throws Exception {
        try {
            if (!prop.isContainer()) {
                readScalarProperty(parent, prop);
            } else if (prop.isList()) {
                Node node = parent.getNode(getNodeName(prop));
                readListProperty(node, prop);
            } else { // complex
                Node node = parent.getNode(getNodeName(prop));
                readComplexProperty(node, prop);
            }
        } catch (PathNotFoundException e) {
            // do nothing
        }
    }

    public static void readComplexProperty(Node node, Property property) throws Exception {
        // TODO better to iterate over nodes than properties
        for (Property prop : property) {
            readProperty(node, prop);
        }
    }

    public static void readListProperty(Node node, Property property) throws Exception {
        NodeIterator it =node.getNodes();
        while (it.hasNext()) {
            Node childNode = it.nextNode();
            Property prop = property.add();
            prop.setData(childNode.getName()); // store real name using key attr of the property
            readProperty(node, prop);
        }
        ((AbstractProperty) property).removePhantomFlag();
        // TODO: make this a method in api -> empty or non empty lists are not phantoms
    }

    public static void readScalarProperty(Node parent, Property property) throws Exception {
        if (property.isScalar()) { // a scalar property
            readPrimitiveProperty(parent, property);
        } else if (property.isList()) { // an array property -> TODO this is for compatibility
            readArrayProperty(parent, property);
        } else { // a structured property like blobs
            Node node = parent.getNode(property.getName());
            readStructuredProperty(node, property);
        }
    }

    public static void readPrimitiveProperty(Node parent, Property property) throws Exception {
        javax.jcr.Property p = parent.getProperty(property.getName());
        switch (p.getType()) {
        case PropertyType.STRING:
            property.init(p.getString());
            break;
        case PropertyType.LONG:
            property.init(p.getLong());
            break;
        case PropertyType.DOUBLE:
            property.init(p.getDouble());
            break;
        case PropertyType.DATE:
            property.init(p.getDate());
            break;
        case PropertyType.BOOLEAN:
            property.init(p.getBoolean());
            break;
        case PropertyType.BINARY:
            property.init((Serializable)p.getStream());
            //TODO XXX FIXME: how to handle streams?
            break;
        default:
            property.init(p.getString());
            break;
        }
    }

    public static void readArrayProperty(Node parent, Property property) throws Exception {
        javax.jcr.Property p = parent.getProperty(property.getName());
        Object array = jcrValuesToArray(p.getValues(), p.getType());
        property.init((Serializable)array);
    }

    public static void readStructuredProperty(Node node, Property property) throws Exception {
        if (!StructuredPropertyManager.read(node, property)) {
            // no accessor defined for this structured property.
            // treat the property as a complex one
            readComplexProperty(node, property);
        }
    }

    // FIXME: this code is duplicated from ArrayProperty. Refactor?
    public static Object jcrValuesToArray(Value[] values, int type)
            throws DocumentException {
        try {
            if (type == PropertyType.STRING) {
                String[] ar = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getString();
                }
                return ar;
            } else if (type == PropertyType.LONG) {
                long[] ar = new long[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getLong();
                }
                return ar;
            } else if (type == PropertyType.DOUBLE) {
                double[] ar = new double[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getDouble();
                }
                return ar;
            } else if (type == PropertyType.DATE) {
                long[] ar = new long[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getLong();
                }
                return ar;
            } else if (type == PropertyType.BOOLEAN) {
                boolean[] ar = new boolean[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getBoolean();
                }
                return ar;
            } else if (type == PropertyType.PATH) {
                String[] ar = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getString();
                }
                return ar;
            } else if (type == PropertyType.REFERENCE) {
                String[] ar = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    ar[i] = values[i].getString();
                }
                return ar;
            }
            return null;
        } catch (RepositoryException e) {
            throw new DocumentException(
                    "failed to get array value from JCR multi value", e);
        }
    }

    public static String getNodeName(Property property) {
        String name = (String)property.getData();
        return name == null ? property.getName() : name;
    }

}
