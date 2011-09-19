/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     mguillaume
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.api;

/**
 * @author <a href="mailto:mg@nuxeo.com">Mathieu Guillaume</a>
 *
 */
public class RenderingOutputClosedException extends RenderingException {

    private static final long serialVersionUID = -550344945294689984L;

    public RenderingOutputClosedException() {
    }

    public RenderingOutputClosedException(String message) {
        super(message);
    }

    public RenderingOutputClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RenderingOutputClosedException(Throwable cause) {
        super(cause);
    }

}
