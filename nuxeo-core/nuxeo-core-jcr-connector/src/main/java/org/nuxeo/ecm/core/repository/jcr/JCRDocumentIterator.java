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

package org.nuxeo.ecm.core.repository.jcr;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.DocumentIterator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JCRDocumentIterator implements DocumentIterator {

    private final JCRSession session;

    private final NodeIterator delegate;

    public JCRDocumentIterator(JCRSession session, Node node)
            throws RepositoryException {
        this(session, node, 0);
    }

    public JCRDocumentIterator(JCRSession session, Node node, int start)
            throws RepositoryException {
        this.session = session;
        delegate = ModelAdapter.getContainerNode(node).getNodes();
        // skip first nodes if required
        for (int i = 0; i < start; i++) {
            if (delegate.hasNext()) {
                delegate.next();
            } else {
                break; // the iterator will have no elements
            }
        }
    }

    public long getSize() {
        return delegate.getSize();
    }

    public boolean hasNext() {
        return delegate.hasNext();
    }

    public Document next() {
        try {
            Node node = delegate.nextNode();
            return session.newDocument(node);
        } catch (RepositoryException e) {
            return next();
        }
    }

    public void remove() {
        delegate.remove();
    }

}
