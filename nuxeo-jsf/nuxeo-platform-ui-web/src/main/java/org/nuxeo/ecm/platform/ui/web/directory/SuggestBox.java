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

package org.nuxeo.ecm.platform.ui.web.directory;

import javax.ejb.Local;
import javax.ejb.Remote;

import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.platform.ui.web.util.SuggestionActionsBean;

/**
 * @deprecated, use {@link SuggestionActionsBean} with specific components to get
 * search results from a given directory
 *
 * @author Anahide Tchertchian
 */
@Local
@Remote
@Deprecated
public interface SuggestBox {

    /**
     * return values of a directory given its name, and the input.
     */
    @WebRemote
    String getSuggestedValues(String directoryName, String input)
            throws Exception;

}
