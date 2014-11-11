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
 * $Id: StatementInfoImpl.java 22098 2007-07-06 12:39:33Z gracinet $
 */

package org.nuxeo.ecm.platform.relations.web;

import java.util.Date;

import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;

/**
 * Statement representation for easier display.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class StatementInfoImpl implements StatementInfo {

    private static final long serialVersionUID = 8474035719439311510L;

    private final Statement statement;

    private NodeInfo subjectRepresentation;

    private NodeInfo predicateRepresentation;

    private NodeInfo objectRepresentation;

    private boolean incoming = false;

    private boolean outgoing = false;

    public StatementInfoImpl() {
        this(null);
    }

    public StatementInfoImpl(Statement statement) {
        this.statement = statement;
    }

    public StatementInfoImpl(Statement statement,
            NodeInfo subjectRepresentation, NodeInfo predicateRepresentation,
            NodeInfo objectRepresentation) {
        this.statement = statement;
        this.subjectRepresentation = subjectRepresentation;
        this.predicateRepresentation = predicateRepresentation;
        this.objectRepresentation = objectRepresentation;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }

    public void setOutgoing(boolean outgoing) {
        this.outgoing = outgoing;
    }

    public Statement getStatement() {
        return statement;
    }

    public Subject getSubject() {
        return statement.getSubject();
    }

    public NodeInfo getSubjectInfo() {
        if (subjectRepresentation == null) {
            return new NodeInfoImpl(getSubject());
        }
        return subjectRepresentation;
    }

    public Resource getPredicate() {
        return statement.getPredicate();
    }

    public NodeInfo getPredicateInfo() {
        if (predicateRepresentation == null) {
            return new NodeInfoImpl(getPredicate());
        }
        return predicateRepresentation;
    }

    public Node getObject() {
        return statement.getObject();
    }

    public NodeInfo getObjectInfo() {
        if (objectRepresentation == null) {
            return new NodeInfoImpl(getObject());
        }
        return objectRepresentation;
    }

    // metadata

    public String getComment() {
        String comment = null;
        Node node = statement.getProperty(RelationConstants.COMMENT);
        if (node != null && node.isLiteral()) {
            comment = ((Literal) node).getValue();
        }
        return comment;
    }

    public Date getCreationDate() {
        Date date = null;
        Node dateNode = statement.getProperty(RelationConstants.CREATION_DATE);
        if (dateNode != null && dateNode.isLiteral()) {
            date = RelationDate.getDate((Literal) dateNode);
        }
        return date;
    }

    public Date getModificationDate() {
        Date date = null;
        Node dateNode = statement.getProperty(RelationConstants.MODIFICATION_DATE);
        if (dateNode != null && dateNode.isLiteral()) {
            date = RelationDate.getDate((Literal) dateNode);
        }
        return date;
    }

    public String getAuthor() {
        String source = null;
        Node node = statement.getProperty(RelationConstants.AUTHOR);
        if (node != null && node.isLiteral()) {
            source = ((Literal) node).getValue();
        }
        return source;
    }

}
