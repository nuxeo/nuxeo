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

package org.nuxeo.ecm.core.query.sql.model;

import java.io.Serializable;

/**
 * The base of all query nodes.
 * <p>
 * An AST method accepts a visitor and that's all.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ASTNode extends Serializable {

    /**
     * Accept the given visitor.
     * @param visitor the AST node visitor
     */
    void accept(IVisitor visitor);

}
