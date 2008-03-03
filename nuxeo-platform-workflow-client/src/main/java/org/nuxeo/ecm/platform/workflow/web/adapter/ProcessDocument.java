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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: ProcessDocument.java 19229 2007-05-23 13:26:43Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.web.adapter;

import java.io.Serializable;

/**
 * Document process adapter registered interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface ProcessDocument extends Serializable {

    /**
     * Returns a list of <code>ProcessModel<code> instances.
     *
     * <p>
     * Note, several processes may be bound to one document which is reflected
     * by the <code>ProcessModel</code> collection.
     * </p>
     *
     * @see org.nuxeo.ecm.platform.workflow.web.adapter.ProcessModel
     *
     * @return a list of <code>ProcessModel<code> instances.
     * @throws ProcessDocumentAdapterException TODO
     */
    ProcessModel[] getProcessInfo();

}
