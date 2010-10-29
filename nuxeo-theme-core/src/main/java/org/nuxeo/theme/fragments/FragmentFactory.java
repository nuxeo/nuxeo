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

package org.nuxeo.theme.fragments;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.ElementType;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.types.TypeRegistry;
import org.nuxeo.theme.uids.UidManager;

public final class FragmentFactory {

    private static final Log log = LogFactory.getLog(FragmentFactory.class);

    public static Fragment create(String typeName) {
        TypeRegistry typeRegistry = Manager.getTypeRegistry();
        ElementType elementType = (ElementType) typeRegistry.lookup(
                TypeFamily.ELEMENT, "fragment");
        FragmentType fragmentType = (FragmentType) typeRegistry.lookup(
                TypeFamily.FRAGMENT, typeName);

        if (fragmentType == null) {
            log.error("Fragment type not found: " + typeName);
            return null;
        }

        String className = fragmentType.getClassName();
        UidManager uidManager = Manager.getUidManager();

        Fragment fragment = null;
        try {
            fragment = (Fragment) Class.forName(className).newInstance();
            fragment.setElementType(elementType);
            fragment.setFragmentType(fragmentType);

            uidManager.register(fragment);
        } catch (Exception e) {
            log.error(e, e);
        }
        return fragment;
    }

}
