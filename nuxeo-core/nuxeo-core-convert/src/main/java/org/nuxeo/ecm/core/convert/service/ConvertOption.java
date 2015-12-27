/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
