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
 */

package org.nuxeo.webengine.blogs.fragments;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.blogs.models.BlogSiteArchiveListModel;
import org.nuxeo.webengine.blogs.models.BlogSiteArchiveMonthModel;
import org.nuxeo.webengine.blogs.models.BlogSiteArchiveYearModel;
import org.nuxeo.webengine.blogs.utils.BlogQueriesCollection;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment related to retrieving the
 * archive of a certain <b>BlogSite</b>.
 *
 * @author rux
 */
public class BlogSiteArchiveFragment extends AbstractFragment {

    public static final String path = "search";

    @Override
    public Model getModel() throws ModelException {
        BlogSiteArchiveListModel model = new BlogSiteArchiveListModel();
        if (WebEngine.getActiveContext() != null) {
            WebContext ctx = WebEngine.getActiveContext();
            CoreSession session = ctx.getCoreSession();
            DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                    DocumentModel.class);

            SimpleDateFormat simpleMonthFormat = new SimpleDateFormat(
                    "MMMM MM yyyy", WebEngine.getActiveContext().getLocale());

            try {
                DocumentModel blogSite = SiteUtils.getFirstWebSiteParent(
                        session, documentModel);
                DocumentModelList blogPosts = BlogQueriesCollection.getAllBlogPosts(
                        session, blogSite.getPathAsString());
                for (DocumentModel blogPost : blogPosts) {

                    Calendar creationDate = SiteUtils.getGregorianCalendar(blogPost,
                            "dc:created");
                    if (creationDate == null) {
                        // no creation date nothing to do
                        continue;
                    }
                    String[] dateDetails = simpleMonthFormat.format(
                            creationDate.getTime()).split(" ");
                    BlogSiteArchiveYearModel archiveYearModel = getYearModel(model, dateDetails[2]);
                    if (archiveYearModel == null) {
                        archiveYearModel = new BlogSiteArchiveYearModel(
                                dateDetails[2], path, 0);

                        model.addItem(archiveYearModel);
                    }
                    archiveYearModel.increaseCount();
                    BlogSiteArchiveMonthModel archiveMonthModel = getMonthModel(archiveYearModel,
                            dateDetails[1]);
                    if (archiveMonthModel == null) {
                        archiveMonthModel = new BlogSiteArchiveMonthModel(
                                dateDetails[0], dateDetails[1], path, 0);
                        archiveYearModel.addItem(archiveMonthModel);
                    }
                    archiveMonthModel.increaseCount();
                }

            } catch (Exception e) {
                throw new ModelException(e);
            }

        }
        return model;
    }

    /**
     * Returns the model corresponding to the year received as parameter.
     *
     * @param model the model list in which the year model will be searched
     * @param year the name of the year
     * @return the model corresponding to the year received as parameter.
     */
    private static BlogSiteArchiveYearModel getYearModel(Model model, String year) {
        BlogSiteArchiveYearModel yearModel = null;
        for (Model item : model.getItems()) {
            if (item instanceof BlogSiteArchiveYearModel) {
                String itemYear = ((BlogSiteArchiveYearModel) item).getYearLong();
                if (year.equals(itemYear)) {
                    yearModel = (BlogSiteArchiveYearModel) item;
                    break;
                }
            }
        }
        return yearModel;
    }

    /**
     * Returns the model corresponding to the month received as parameter.
     *
     * @param model the model list in which the month model will be searched
     * @param month the name of the month
     * @return the model corresponding to the month received as parameter
     */
    private static BlogSiteArchiveMonthModel getMonthModel(Model model, String month) {
        BlogSiteArchiveMonthModel monthModel = null;
        for (Model item : model.getItems()) {
            if (item instanceof BlogSiteArchiveMonthModel) {
                String itemMonth = ((BlogSiteArchiveMonthModel) item).getMonthShort();
                if (month.equals(itemMonth)) {
                    monthModel = (BlogSiteArchiveMonthModel) item;
                    break;
                }
            }
        }
        return monthModel;
    }

}
