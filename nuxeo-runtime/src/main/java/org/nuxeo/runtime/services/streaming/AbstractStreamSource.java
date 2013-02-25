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

package org.nuxeo.runtime.services.streaming;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.nuxeo.common.utils.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractStreamSource implements StreamSource {

    protected String charsetName = "UTF-8";

    @Override
    public long getLength() throws IOException {
        return -1L;
    }

    @Override
    public boolean canReopen() {
        return false;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return FileUtils.readBytes(getStream());
    }

    @Override
    public String getString() throws IOException {
        return new String(getBytes(), charsetName);
    }

    @Override
    public void copyTo(File file) throws IOException {
        copyTo(new FileOutputStream(file));
    }

    @Override
    public void copyTo(OutputStream out) throws IOException {
        FileUtils.copy(getStream(), out);
    }

    @Override
    public void destroy() {
        // do nothing
    }

    /**
     * Change the charset used for serializing the String source as a byte
     * stream.
     *
     * @since 5.7
     */
    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }

    /**
     * Get the charset used for serializing the String source as a byte stream.
     *
     * @since 5.7
     */
    public String getCharsetName() {
        return charsetName;
    }

}
