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
package org.nuxeo.ecm.automation.client.model;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocRef implements OperationInput {

    private static final long serialVersionUID = 1L;

    protected final String ref;

    public static DocRef newRef(String ref) {
        if (ref.startsWith("/")) {
            return new PathRef(ref);
        } else {
            return new IdRef(ref);
        }
    }

    public DocRef(String ref) {
        this.ref = ref;
    }

    public String getInputType() {
        return "document";
    }

    public String getInputRef() {
        return "doc:" + ref;
    }

    public boolean isBinary() {
        return false;
    }

    @Override
    public String toString() {
        return ref;
    }

}
