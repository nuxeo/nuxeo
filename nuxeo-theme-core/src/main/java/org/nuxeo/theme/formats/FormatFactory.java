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

package org.nuxeo.theme.formats;

import org.nuxeo.theme.Manager;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.uids.UidManager;

public final class FormatFactory {

    // This class is not supposed to be instantiated.
    private FormatFactory() {
    }

    public static Format create(final String typeName) throws ThemeException {
        Format format = null;
        final FormatType formatType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, typeName);
        if (formatType == null) {
            throw new ThemeException("Unknown format type: " + typeName);
        }

        final UidManager uidManager = Manager.getUidManager();
        try {
            format = (Format) Class.forName(formatType.getFormatClass()).newInstance();
        } catch (InstantiationException e) {
            throw new ThemeException(e);
        } catch (IllegalAccessException e) {
            throw new ThemeException(e);
        } catch (ClassNotFoundException e) {
            throw new ThemeException("Format creation failed: " + typeName, e);
        }
        format.setFormatType(formatType);
        uidManager.register(format);
        return format;
    }

}
