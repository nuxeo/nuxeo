/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Julien Anguenot
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

/**
 * Exception thrown when an attempt is made to follow an incorrect lifecycle transition or to create a document with an
 * incorrect initial lifecycle state
 *
 * @see CoreSession#followTransition
 */
// inherits from a deprecated base LifeCycleException so that old code catching the old one still works
@SuppressWarnings("deprecation")
public class LifeCycleException extends org.nuxeo.ecm.core.lifecycle.LifeCycleException {

    private static final long serialVersionUID = 1L;

    public LifeCycleException() {
        super();
    }

    public LifeCycleException(String message) {
        super(message);
    }

    public LifeCycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public LifeCycleException(Throwable cause) {
        super(cause);
    }

}
