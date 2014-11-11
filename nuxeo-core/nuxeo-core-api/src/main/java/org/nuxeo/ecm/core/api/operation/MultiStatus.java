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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.operation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MultiStatus extends Status {

    private static final long serialVersionUID = 4645429673525324229L;

    protected Status[] children;

    public MultiStatus(int severity) {
        super(severity);
    }

    public MultiStatus(int severity, String message) {
        super(severity, message);
    }

    public MultiStatus(int severity, Throwable exception) {
        super(severity, exception);
    }

    @Override
    public boolean isMultiStatus() {
        return true;
    }

    public void addStatus(Status status) {
        //TODO
    }

    public Status[] getChildren() {
        return children;
    }

}
