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

    public StatementInfoImpl(Statement statement, NodeInfo subjectRepresentation, NodeInfo predicateRepresentation,
            NodeInfo objectRepresentation) {
        this.statement = statement;
        this.subjectRepresentation = subjectRepresentation;
        this.predicateRepresentation = predicateRepresentation;
        this.objectRepresentation = objectRepresentation;
    }

    @Override
    public boolean isIncoming() {
        return incoming;
    }

    @Override
    public boolean isOutgoing() {
        return outgoing;
    }

    @Override
    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }

    @Override
    public void setOutgoing(boolean outgoing) {
        this.outgoing = outgoing;
    }

    @Override
    public Statement getStatement() {
        return statement;
    }

    @Override
    public Subject getSubject() {
        return statement.getSubject();
    }

    @Override
    public NodeInfo getSubjectInfo() {
        if (subjectRepresentation == null) {
            return new NodeInfoImpl(getSubject());
        }
        return subjectRepresentation;
    }

    @Override
    public Resource getPredicate() {
        return statement.getPredicate();
    }

    @Override
    public NodeInfo getPredicateInfo() {
        if (predicateRepresentation == null) {
            return new NodeInfoImpl(getPredicate());
        }
        return predicateRepresentation;
    }

    @Override
    public Node getObject() {
        return statement.getObject();
    }

    @Override
    public NodeInfo getObjectInfo() {
        if (objectRepresentation == null) {
            return new NodeInfoImpl(getObject());
        }
        return objectRepresentation;
    }

    // metadata

    @Override
    public String getComment() {
        String comment = null;
        Node node = statement.getProperty(RelationConstants.COMMENT);
        if (node != null && node.isLiteral()) {
            comment = ((Literal) node).getValue();
        }
        return comment;
    }

    @Override
    public Date getCreationDate() {
        Date date = null;
        Node dateNode = statement.getProperty(RelationConstants.CREATION_DATE);
        if (dateNode != null && dateNode.isLiteral()) {
            date = RelationDate.getDate((Literal) dateNode);
        }
        return date;
    }

    @Override
    public Date getModificationDate() {
        Date date = null;
        Node dateNode = statement.getProperty(RelationConstants.MODIFICATION_DATE);
        if (dateNode != null && dateNode.isLiteral()) {
            date = RelationDate.getDate((Literal) dateNode);
        }
        return date;
    }

    @Override
    public String getAuthor() {
        String source = null;
        Node node = statement.getProperty(RelationConstants.AUTHOR);
        if (node != null && node.isLiteral()) {
            source = ((Literal) node).getValue();
        }
        return source;
    }

}
