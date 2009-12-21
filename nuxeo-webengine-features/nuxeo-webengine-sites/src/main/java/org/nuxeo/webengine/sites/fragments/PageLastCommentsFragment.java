/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     rdarlea
 */
package org.nuxeo.webengine.sites.fragments;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.models.CommentListModel;
import org.nuxeo.webengine.sites.models.CommentModel;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment related to retrieving the
 * comments that are bounded to a <b>WebPage</b>.
 *
 * @author rux
 */
public class PageLastCommentsFragment extends AbstractFragment {

    /**
     * Retrieves the comments that are bounded to a <b>WebPage</b>
     */
    @Override
    public Model getModel() throws ModelException {
        CommentListModel model = new CommentListModel();
        if (WebEngine.getActiveContext() != null) {
            WebContext ctx = WebEngine.getActiveContext();
            DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                    DocumentModel.class);

            try {
                CommentManager commentManager = SiteUtils.getCommentManager();
                // get published comments
                CommentModel commentModel;
                String creationDate;
                String author;
                String commentText;
                for (DocumentModel doc : commentManager.getComments(documentModel)) {
                    if (CommentsConstants.PUBLISHED_STATE.equals(doc.getCurrentLifeCycleState())) {
                        GregorianCalendar modificationDate = (GregorianCalendar) doc.getProperty(
                                "comment:creationDate").getValue();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                                "dd MMMM",
                                WebEngine.getActiveContext().getLocale());
                        creationDate = simpleDateFormat.format(modificationDate.getTime());
                        author = (String) doc.getProperty("comment:author").getValue();
                        commentText = (String) doc.getProperty("comment:text").getValue();
                        commentModel = new CommentModel(creationDate, author,
                                commentText, doc.getRef().toString(), false);
                        model.addItem(commentModel);
                    }
                }

                // get pending comments

                for (DocumentModel doc : commentManager.getComments(documentModel)) {
                    if (CommentsConstants.PENDING_STATE.equals(doc.getCurrentLifeCycleState())) {
                        GregorianCalendar modificationDate = (GregorianCalendar) doc.getProperty(
                                "comment:creationDate").getValue();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                                "dd MMMM",
                                WebEngine.getActiveContext().getLocale());

                        creationDate = simpleDateFormat.format(modificationDate.getTime());
                        author = (String) doc.getProperty("comment:author").getValue();
                        commentText = (String) doc.getProperty("comment:text").getValue();
                        commentModel = new CommentModel(creationDate, author,
                                commentText, doc.getRef().toString(), true);
                        model.addItem(commentModel);

                    }
                }
            } catch (Exception e) {
                throw new ModelException(e);
            }

        }
        return model;
    }

}
