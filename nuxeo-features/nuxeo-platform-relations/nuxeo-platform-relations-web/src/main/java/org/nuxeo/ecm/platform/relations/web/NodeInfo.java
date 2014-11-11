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
 * $Id: NodeInfo.java 21142 2007-06-22 16:50:45Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.web;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.relations.api.Node;

/**
 * Node representation for easier display.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public interface NodeInfo extends Node {

    boolean isLink();

    boolean isText();

    boolean isDocument();

    boolean isDocumentVisible();

    DocumentModel getDocumentModel();

    String getHref();

    String getAction();

    String getTitle();

    String getIcon();

}
