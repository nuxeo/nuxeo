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

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Blobs extends ArrayList<Blob> implements OperationInput {

    private static final long serialVersionUID = 1L;

    public Blobs() {
    }

    public Blobs(int size) {
        super(size);
    }

    public Blobs(List<Blob> blobs) {
        super(blobs);
    }

    public String getInputType() {
        return "bloblist";
    }

    public String getInputRef() {
        return null;
    }

    public boolean isBinary() {
        return true;
    }

}
