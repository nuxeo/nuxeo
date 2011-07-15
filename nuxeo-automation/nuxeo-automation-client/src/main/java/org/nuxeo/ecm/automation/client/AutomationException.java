/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AutomationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AutomationException() {
    }

    public AutomationException(String message) {
        super(message);
    }

    public AutomationException(Throwable cause) {
        super("", cause);
    }

    public AutomationException(String message, Throwable cause) {
        super(message, cause);
    }

}
