/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.comment.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.ecm.platform.comment.api.CommentConverter;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
@XObject("config")
@XRegistry(compatWarnOnMerge = true)
public class CommentServiceConfig {

    // These instance variables are accessed directly from other classes in
    // other packages.

    @XNode("converterClass")
    public String commentConverterClassName;

    @XNode("graphName")
    public String graphName;

    @XNode("commentNamespace")
    public String commentNamespace;

    @XNode("predicateNamespace")
    public String predicateNamespace;

    @XNode("documentNamespace")
    public String documentNamespace;

    public CommentConverter getCommentConverter() {
        try {
            Class<?> commentConverterClass = Class.forName(commentConverterClassName);
            return (CommentConverter) commentConverterClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("failed to create comment converter", e);
        }
    }

}
