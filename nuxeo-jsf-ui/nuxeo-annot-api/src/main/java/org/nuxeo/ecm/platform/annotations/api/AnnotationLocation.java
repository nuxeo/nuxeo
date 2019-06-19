/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.api;

import java.io.Serializable;

public class AnnotationLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String XPointer;

    public AnnotationLocation(String xPointer) {
        this.XPointer = xPointer;
    }

    public String getXPointer() {
        return XPointer;
    }

    public void setXPointer(String pointer) {
        XPointer = pointer;
    }

}
