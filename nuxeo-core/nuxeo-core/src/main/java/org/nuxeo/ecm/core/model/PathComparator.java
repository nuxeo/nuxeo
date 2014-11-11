/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.model;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class PathComparator implements Comparator<Document>, Serializable {

    private static final Log log = LogFactory.getLog(PathComparator.class);
    private static final long serialVersionUID = 3598980450344414494L;

    @Override
    public int compare(Document o1, Document o2) {
        try {
            String path1 = o1.getPath();
            return path1.compareTo(o2.getPath());
        } catch (DocumentException e) {
            // can't throw again
            log.error("Failed getting a path from a Document instance!");
            return 0;
        }
    }

}
