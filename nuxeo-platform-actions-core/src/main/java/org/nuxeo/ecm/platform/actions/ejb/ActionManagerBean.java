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
 * $Id: ActionManagerBean.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions.ejb;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ActionFilter;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 */
@Stateless
@Local(ActionManagerLocal.class)
@Remote(ActionManager.class)
public class ActionManagerBean implements ActionManagerLocal {

    private static final long serialVersionUID = 8398790411119200730L;

    private static final Log log = LogFactory.getLog(ActionManagerBean.class);

    private ActionManager actionService;

    @PostConstruct
    public void initialize() {
        try {
            actionService = Framework.getLocalService(ActionManager.class);
        } catch (Exception e) {
            log.error("Failed to lookup ActionService", e);
        }
    }

    public List<Action> getActions(String category, ActionContext context) {
        return getActions(category, context, true);
    }

    public List<Action> getActions(String category, ActionContext context,
            boolean hideUnavailableActions) {
        return actionService.getActions(category, context,
                hideUnavailableActions);
    }

    public boolean isEnabled(String actionId, ActionContext context) {
        return actionService.isEnabled(actionId, context);
    }

    public boolean isRegistered(String actionId) {
        return actionService.isRegistered(actionId);
    }

    public Action getAction(String actionId) {
        return actionService.getAction(actionId);
    }

    public ActionFilter[] getFilters(String actionId) {
        return actionService.getFilters(actionId);
    }

    @Remove
    public void remove() {
        actionService = null;
    }

    @PostActivate
    public void readState() {
        log.info("PostActivate");
        initialize();
    }

    @PrePassivate
    public void saveState() {
        log.info("PrePassivate");
        remove();
    }

    public List<Action> getAllActions(String category) {
        return actionService.getAllActions(category);
    }

}
