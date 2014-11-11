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
 *     anguenot
 *
 * $Id: IndexingThreadPoolDescriptor.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.internals;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Indexing thread pool descriptor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
@XObject("indexingThreadPool")
public class IndexingThreadPoolDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@maxPoolSize")
    public int maxPoolSize = 5;

    @XNode("@docBatchSize")
    public int docBatchSize = 1;


    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getDocBatchSize() {
        return docBatchSize;
    }

    public void setDocBatchSize(int docBatchSize) {
        this.docBatchSize = docBatchSize;
    }

}
