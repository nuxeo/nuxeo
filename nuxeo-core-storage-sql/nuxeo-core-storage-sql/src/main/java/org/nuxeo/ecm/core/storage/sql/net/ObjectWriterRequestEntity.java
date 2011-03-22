/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.net;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.httpclient.methods.RequestEntity;

/**
 * Class defining a {@link RequestEntity} that writes a list of object through
 * an {@link ObjectOutputStream}.
 */
public class ObjectWriterRequestEntity implements RequestEntity {

    public final List<Object> queue;

    public ObjectWriterRequestEntity() {
        queue = new ArrayList<Object>();
    }

    public void add(String methodName, Object... objects) {
        queue.add(methodName);
        if (objects != null) {
            queue.addAll(Arrays.asList(objects));
        }
        queue.add(MapperClient.EOF);
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public void writeRequest(OutputStream out) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(out);
        for (Object object : queue) {
            oos.writeObject(object);
        }
        oos.flush();
    }

    @Override
    public long getContentLength() {
        return -1;
    }

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

}
