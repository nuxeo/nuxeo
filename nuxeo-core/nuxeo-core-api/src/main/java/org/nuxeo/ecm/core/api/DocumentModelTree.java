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

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.List;

/**
 * A serializable list of documents that holds information about the
 * structure of the sections tree.
 *
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
public interface DocumentModelTree extends List<DocumentModelTreeNode>, Serializable {

}
