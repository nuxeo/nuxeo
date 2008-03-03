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
 * $Id: StatementInfo.java 21142 2007-06-22 16:50:45Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.web;

import java.io.Serializable;
import java.util.Date;

import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;

/**
 * Statement representation for easier display.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public interface StatementInfo extends Serializable {

    boolean isIncoming();

    boolean isOutgoing();

    void setIncoming(boolean incoming);

    void setOutgoing(boolean outgoing);

    Statement getStatement();

    Subject getSubject();

    NodeInfo getSubjectInfo();

    Resource getPredicate();

    NodeInfo getPredicateInfo();

    Node getObject();

    NodeInfo getObjectInfo();

    // metadata

    Date getCreationDate();

    Date getModificationDate();

    String getComment();

    String getAuthor();
}
