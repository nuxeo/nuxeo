/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.comment.ejb;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentService;
import org.nuxeo.ecm.platform.comment.service.CommentServiceHelper;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
@Stateless
@Remote(CommentManager.class)
@Local(CommentManagerLocal.class)
// XXX AT: this bean is never called...
public class CommentManagerBean implements CommentManager {

    @Resource
    EJBContext context;

    private CommentManager commentManager;

    @PostConstruct
    public void initialize() {
        CommentService commentService = CommentServiceHelper.getCommentService();
        commentManager = commentService.getCommentManager();
    }

    public void cleanup() {}

    public void remove() {}

    public DocumentModel createComment(DocumentModel docModel,
            String comment) throws ClientException {
        try {
            String author = context.getCallerPrincipal().getName();
            return commentManager.createComment(docModel, comment, author);
        } catch (Throwable e) {
            throw EJBExceptionHandler.wrapException(e);
        }
    }

    public DocumentModel createComment(DocumentModel docModel,
            String comment, String author) throws ClientException {
        try {
            return commentManager.createComment(docModel, comment, author);
        } catch (Throwable e) {
            throw EJBExceptionHandler.wrapException(e);
        }
    }

    private String updateAuthor(DocumentModel docModel) {
        String author = (String) docModel.getProperty("comment", "author");
        if (author == null) {
            author = context.getCallerPrincipal().getName();
            docModel.setProperty("comment", "author", author);
        }
        return author;
    }


    public DocumentModel createComment(DocumentModel docModel,
            DocumentModel comment) throws ClientException {
        try {
            updateAuthor(comment);
            return commentManager.createComment(docModel, comment);
        } catch (Throwable e) {
            throw EJBExceptionHandler.wrapException(e);
        }
    }

    public void deleteComment(DocumentModel docModel, DocumentModel comment)
            throws ClientException {
        try {
            commentManager.deleteComment(docModel, comment);
        } catch (Throwable e) {
            throw EJBExceptionHandler.wrapException(e);
        }
    }

    public List<DocumentModel> getComments(DocumentModel docModel)
            throws ClientException {
        try {
            return commentManager.getComments(docModel);
        } catch (Throwable e) {
            throw EJBExceptionHandler.wrapException(e);
        }
    }

    public DocumentModel createComment(DocumentModel docModel,
            DocumentModel parent, DocumentModel child) throws ClientException {
        try {
            updateAuthor(child);
            return commentManager.createComment(docModel, parent, child);
        } catch (Throwable e) {
            throw EJBExceptionHandler.wrapException(e);
        }
    }

    public List<DocumentModel> getComments(DocumentModel docModel,
            DocumentModel parent) throws ClientException {
        try {
            return commentManager.getComments(docModel, parent);
        } catch (Throwable e) {
            throw EJBExceptionHandler.wrapException(e);
        }
    }

}
