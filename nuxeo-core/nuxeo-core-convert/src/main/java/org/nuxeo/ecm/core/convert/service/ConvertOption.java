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
 */
package org.nuxeo.ecm.core.convert.service;

/**
 * Helper class to manage mime-types chains.
 *
 * @author tiry
 */
public class ConvertOption {

    protected final String mimeType;
    protected final String converter;

    public ConvertOption(String converter, String mimeType) {
        this.mimeType = mimeType;
        this.converter = converter;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getConverterName() {
        return converter;
    }

}
