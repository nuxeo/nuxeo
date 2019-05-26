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
