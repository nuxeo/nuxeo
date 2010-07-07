/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.api;

/**
 * Class holding implementation for deprecated methods where signature does not
 * conflict with the new {@link PageProvider} API.
 *
 * @author Anahide Tchertchian
 */
public abstract class AbstractLegacyDocumentPageProvider<T> extends
        AbstractPageProvider<DocumentModel> {

    /**
     * @deprecated use {@link PageProvider#lastPage()} instead
     */
    @Deprecated
    public void last() {
        lastPage();
    }

    /**
     * @deprecated use {@link PageProvider#nextPage()} instead
     */
    @Deprecated
    public void next() {
        nextPage();
    }

    /**
     * @deprecated use {@link PageProvider#previousPage()} instead
     */
    @Deprecated
    public void previous() {
        previousPage();
    }

    /**
     * @deprecated use {@link PageProvider#firstPage()} instead
     */
    @Deprecated
    public void rewind() {
        firstPage();
    }
}
