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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.List;

/**
 * A serializable list of document references. Use this instead of <code>List&lt;DocumentRef&gt;</code> when a list of
 * document references should be returned.
 * <p>
 * This object is type safe and can help services which need to dynamically discover which type of object is returned.
 * (see operation framework for this)
 * <p>
 * This class is the equivalent of {@link DocumentModelList} but for document references.
 *
 * @author Bogdan Stefanescu
 */
public interface DocumentRefList extends List<DocumentRef>, Serializable {

    /**
     * Returns the total size of the bigger list this is a part of.
     *
     * @return the total size
     */
    long totalSize();

}
