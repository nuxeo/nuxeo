/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.elements;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.uids.UidManager;

public final class ElementFactory {

    private static final Log log = LogFactory.getLog(ElementFactory.class);

    private ElementFactory() {
        // This class is not supposed to be instantiated.
    }

    public static Element create(final String typeName) {
        final ElementType elementType = (ElementType) Manager.getTypeRegistry().lookup(
                TypeFamily.ELEMENT, typeName);
        final String className = elementType.getClassName();
        final UidManager uidManager = Manager.getUidManager();

        if (className == null) {
            // throw an exception
            return null;
        }

        Element element = null;
        try {
            element = (Element) Class.forName(className).newInstance();
            element.setElementType(elementType);
            uidManager.register(element);
        } catch (Exception e) {
            log.error("Could not create element", e);
        }
        return element;
    }

}
