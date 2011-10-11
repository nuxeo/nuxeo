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

package org.nuxeo.ecm.core.api.adapter;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AnnotatedDocumentAdapter implements AnnotatedDocument {

    final DocumentModel doc;
    final Map<String, Object> annotations;

    public AnnotatedDocumentAdapter(DocumentModel doc) {
        this.doc = doc;
        // initialize adapter -> in real cases you may get a proxy to a remote service
        annotations = new HashMap<String, Object>();
    }

    @Override
    public Object getAnnotation(String name) {
        return annotations.get(name);
    }

    @Override
    public void putAnnotation(String name, Object value) {
        annotations.put(name, value);
    }

}
