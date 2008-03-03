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

package org.nuxeo.ecm.platform.workflow.jbpm.util;

import java.io.InputStream;
import java.util.List;

import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.xml.sax.InputSource;

/**
 * Jpdl XML reader that deals with Jpdl check up.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class NXJpdlXmlReader extends JpdlXmlReader {

    private static final long serialVersionUID = 1L;

    public NXJpdlXmlReader(InputStream inputStream) {
       super(new InputSource(inputStream));
    }

    /**
     * Returns the status.
     *
     * @return the status
     */
    public List getStatus() {
        return problems;
    }

}
