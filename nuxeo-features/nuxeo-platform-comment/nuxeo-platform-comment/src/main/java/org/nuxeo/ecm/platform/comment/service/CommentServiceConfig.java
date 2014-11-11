/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.comment.api.CommentConverter;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
@XObject("config")
public class CommentServiceConfig {

    // These instance variables are accessed directly from other classes in
    // other packages.

    @XNode("converterClass")
    // public Class commentConverterClass;
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
            Class commentConverterClass = Class.forName(commentConverterClassName);
            return (CommentConverter) commentConverterClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("failed to create comment converter", e);
        }
    }

}
