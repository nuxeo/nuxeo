/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.spaces.webobject;

import javax.servlet.ServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.spaces.api.Space;
import org.nuxeo.ecm.spaces.api.Univers;
import org.nuxeo.ecm.spaces.core.impl.docwrapper.DocSpaceImpl;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * Space ( Nuxeo-spaces-api concept ) web engine object
 **/
@WebObject(type = "Space")
@Produces("text/html; charset=UTF-8")
public class SpaceWebObject extends DefaultObject {

    /**
     * Logger log4j
     */
    private static final Log LOGGER = LogFactory.getLog(SpaceWebObject.class);

    /**
     * Current space
     */
    private Space space = null;

    public Space getSpace() {
        return space;
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    @POST
    public Object doUpdate() {
        FormData form = getContext().getForm();
        String title = form.getString("dc:title");
        String description = form.getString("dc:description");
        String theme = form.getString("space:theme");

        try {
            if (title != null)
                this.space.setTitle(title);
            if (description != null)
                this.space.setDescription(description);
            if (theme != null)
                this.space.setTheme(theme);
            injectTheme();
            this.space.save();
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }

        return getView("index");

    }

    public DocumentModel getDocument() {
        if (DocSpaceImpl.class.isAssignableFrom(getSpace().getClass())) {
            return ((DocSpaceImpl) getSpace()).getDocument();
        } else {
            return null;
        }
    }

    public Univers getUnivers() {
        return (Univers) getContext().getRequest().getAttribute(
                "currentUnivers");
    }

    /**
     * Computes space data objects
     */
    @Override
    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        try {
            this.space = (Space) args[0];

            if (this.space == null)
                throw new Exception("Space argument can't be null");

            injectTheme();

        } catch (Exception e) {
            throw WebException.wrap(e);
        }
        LOGGER.debug("Space has been set");
    }

    private void injectTheme() throws ClientException {
        if (space.getTheme() != null) {
            getContext().getRequest().setAttribute("org.nuxeo.theme.theme",
                    space.getTheme() + "/default");
            LOGGER.debug("setting theme from space in context request wall again "
                    + space.getTheme());
        } else {
            LOGGER.debug("no theme found from space ");
        }
    }

    public String getBaseUrl() {
        ServletRequest request = getContext().getRequest();
        return VirtualHostHelper.getBaseURL(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.nuxeo.ecm.webengine.model.impl.AbstractResource#getAdapter(java.lang
     * .Class)
     */
    @Override
    public <A> A getAdapter(Class<A> adapter) {
        // TODO open NXP before commit
        if (adapter == DocumentModel.class) {
            return adapter.cast(this.getDocument());
        } else {
            return super.getAdapter(adapter);
        }
    }

}
