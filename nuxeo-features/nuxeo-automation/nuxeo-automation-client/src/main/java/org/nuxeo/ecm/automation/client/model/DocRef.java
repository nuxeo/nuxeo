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
