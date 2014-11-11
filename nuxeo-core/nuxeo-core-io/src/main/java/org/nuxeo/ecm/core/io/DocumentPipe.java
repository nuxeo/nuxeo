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
 * $Id: DocumentPipe.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io;

import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface DocumentPipe {

    void setReader(DocumentReader reader);

    DocumentReader getReader();

    void setWriter(DocumentWriter writer);

    DocumentWriter getWriter();

    void addTransformer(DocumentTransformer transformer);

    void removeTransformer(DocumentTransformer transformer);

    List<DocumentTransformer> getTransformers();

    DocumentTranslationMap run() throws Exception;

}
