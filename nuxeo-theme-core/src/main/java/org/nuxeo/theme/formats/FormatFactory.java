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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.uids.UidManager;

public final class FormatFactory {

    private static final Log log = LogFactory.getLog(FormatFactory.class);

    // This class is not supposed to be instantiated.
    private FormatFactory() {
    }

    public static Format create(final String typeName) {
        Format format = null;
        final FormatType formatType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, typeName);
        final UidManager uidManager = Manager.getUidManager();

        try {
            format = (Format) Class.forName(formatType.getFormatClass()).newInstance();
            format.setFormatType(formatType);
            uidManager.register(format);
        } catch (Exception e) {
            log.error("Could not create format",e);
        }
        return format;
    }

}
