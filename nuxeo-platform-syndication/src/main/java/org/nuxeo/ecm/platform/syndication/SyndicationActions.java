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
 * $Id: NXTransformExtensionPointHandler.java 18651 2007-05-13 20:28:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.syndication;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Remove;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.util.ECInvalidParameterException;

import com.sun.syndication.io.FeedException;

/**
 *
 * @author bchaffangeon
 *
 */
@Local
public interface SyndicationActions {

    /**
     * This method is called when a syndication URL is called.
     * It initializes variables thanks to GET paramaters
     * and launches syndication for a document
     * syndicateDocument()
     *
     * @throws ClientException
     * @throws FeedException
     * @throws IOException
     * @throws ParseException
     */
    void getSyndicationDocument() throws ClientException, IOException,
            FeedException, ParseException;

    /**
     * This method is called when a syndication url is called.
     * It initializes variables thanks to GET paramaters
     * and launches syndication for searchs results.
     *
     * @throws ClientException
     * @throws IOException
     * @throws FeedException
     * @throws ParseException
     * @throws ECInvalidParameterException
     */
    void getSyndicationSearch() throws ClientException, IOException,
            FeedException, ParseException, ECInvalidParameterException;

    /**
     * Not used for now.
     * Will be used with actions-contrib
     * @return
     */
    List<Action> getActionsForSyndication();


    /**
     * @return the complete URL for a Document syndication without the feed format
     */
    String getFullSyndicationDocumentUrl();

    /**
     * @return the complete URL for a Document syndication in RSS
     */
    String getFullSyndicationDocumentUrlInRss();

    /**
     * @return the complete URL for a Document syndication in ATOM
     */
    String getFullSyndicationDocumentUrlInAtom();

    /**
     * @return the complete URL for Search results syndication without the format
     */
    String getFullSyndicationSearchUrl();

    /**
     * @return the complete URL for Search results syndication in RSS
     */
    String getFullSyndicationSearchUrlInRss();

    /**
     * @return the complete URL for Search results syndication in ATOM
     */
    String getFullSyndicationSearchUrlInAtom();


    @Remove
    void destroy();

}
