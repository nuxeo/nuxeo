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
 * $Id: ActionService.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ActionService extends DefaultComponent implements ActionManager {

    public static final ComponentName ID = new ComponentName("org.nuxeo.ecm.platform.actions.ActionService");

    private static final long serialVersionUID = -5256555810901945824L;

    private static final Log log = LogFactory.getLog(ActionService.class);

    private ActionContributionHandler actions;

    private FilterContributionHandler filters;

    protected final MetricRegistry metrics = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    private static final String LOG_MIN_DURATION_KEY = "nuxeo.actions.debug.log_min_duration_ms";

    private static final long LOG_MIN_DURATION_NS = Long.parseLong(
            Framework.getService(ConfigurationService.class).getProperty(LOG_MIN_DURATION_KEY, "-1")) * 1000000;

    private Timer actionsTimer;

    private Timer actionTimer;

    private Timer filtersTimer;

    private Timer filterTimer;

    @Override
    public void activate(ComponentContext context) {
        filters = new FilterContributionHandler();
        actions = new ActionContributionHandler(filters);
        actionsTimer = metrics.timer(MetricRegistry.name("nuxeo", "ActionService", "ations"));
        actionTimer = metrics.timer(MetricRegistry.name("nuxeo", "ActionService", "action"));
        filtersTimer = metrics.timer(MetricRegistry.name("nuxeo", "ActionService", "filters"));
        filterTimer = metrics.timer(MetricRegistry.name("nuxeo", "ActionService", "filter"));
    }

    @Override
    public void deactivate(ComponentContext context) {
        actions = null;
        filters = null;
        actionsTimer = null;
        actionTimer = null;
        filtersTimer = null;
        filterTimer = null;
    }

    /**
     * Return the action registry
     *
     * @deprecated since 5.5: use interface methods on ActionManager instead of public methods on ActionService.
     */
    @Deprecated
    public final ActionRegistry getActionRegistry() {
        return actions.getRegistry();
    }

    /**
     * Return the action filter registry
     *
     * @deprecated since 5.5: use interface methods on ActionManager instead of public methods on ActionService.
     */
    @Deprecated
    public final ActionFilterRegistry getFilterRegistry() {
        return filters.getRegistry();
    }

    private void applyFilters(ActionContext context, List<Action> actions) {
        Iterator<Action> it = actions.iterator();
        while (it.hasNext()) {
            Action action = it.next();
            action.setFiltered(true);
            if (!checkFilters(context, action)) {
                it.remove();
            }
        }
    }

    @Override
    public boolean checkFilters(Action action, ActionContext context) {
        return checkFilters(context, action);
    }

    private boolean checkFilters(ActionContext context, Action action) {
        if (action == null) {
            return false;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Checking access for action '%s'...", action.getId()));
        }

        boolean granted = checkFilters(action, action.getFilterIds(), context);
        if (granted) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Granting access for action '%s'", action.getId()));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Denying access for action '%s'", action.getId()));
            }
        }
        return granted;
    }

    @Override
    public List<Action> getActions(String category, ActionContext context) {
        return getActions(category, context, true);
    }

    @Override
    public List<Action> getAllActions(String category) {
        return getActionRegistry().getActions(category);
    }

    @Override
    public List<Action> getActions(String category, ActionContext context, boolean hideUnavailableActions) {
        final Timer.Context timerContext = actionsTimer.time();
        try {
            List<Action> actions = getActionRegistry().getActions(category);
            if (hideUnavailableActions) {
                applyFilters(context, actions);
                return actions;
            } else {
                List<Action> allActions = new ArrayList<Action>();
                allActions.addAll(actions);
                applyFilters(context, actions);

                for (Action a : allActions) {
                    a.setAvailable(actions.contains(a));
                }
                return allActions;
            }
        } finally {
            long duration = timerContext.stop();
            if ((LOG_MIN_DURATION_NS >= 0) && (duration > LOG_MIN_DURATION_NS)) {
                log.info(String.format("Resolving actions for category '%s' took: %.2f ms", category,
                        duration / 1000000.0));
            }
        }
    }

    @Override
    public Action getAction(String actionId, ActionContext context, boolean hideUnavailableAction) {
        final Timer.Context timerContext = actionTimer.time();
        try {
            Action action = getActionRegistry().getAction(actionId);
            if (action != null) {
                if (hideUnavailableAction) {
                    if (!checkFilters(context, action)) {
                        return null;
                    }
                } else {
                    if (!checkFilters(context, action)) {
                        action.setAvailable(false);
                    }
                }
            }

            action.setFiltered(true);
            return action;
        } finally {
            long duration = timerContext.stop();
            if ((LOG_MIN_DURATION_NS >= 0) && (duration > LOG_MIN_DURATION_NS)) {
                log.info(String.format("Resolving action with id '%s' took: %.2f ms", actionId, duration / 1000000.0));
            }
        }
    }

    @Override
    public Action getAction(String actionId) {
        return getActionRegistry().getAction(actionId);
    }

    @Override
    public boolean isRegistered(String actionId) {
        return getActionRegistry().getAction(actionId) != null;
    }

    @Override
    public boolean isEnabled(String actionId, ActionContext context) {
        Action action = getActionRegistry().getAction(actionId);
        if (action != null) {
            return isEnabled(action, context);
        }
        return false;
    }

    public boolean isEnabled(Action action, ActionContext context) {
        ActionFilterRegistry filterReg = getFilterRegistry();
        for (String filterId : action.getFilterIds()) {
            ActionFilter filter = filterReg.getFilter(filterId);
            if (filter != null && !filter.accept(action, context)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ActionFilter[] getFilters(String actionId) {
        Action action = getActionRegistry().getAction(actionId);
        if (action == null) {
            return null;
        }
        ActionFilterRegistry filterReg = getFilterRegistry();
        List<String> filterIds = action.getFilterIds();
        if (filterIds != null && !filterIds.isEmpty()) {
            ActionFilter[] filters = new ActionFilter[filterIds.size()];
            for (int i = 0; i < filters.length; i++) {
                String filterId = filterIds.get(i);
                filters[i] = filterReg.getFilter(filterId);
            }
            return filters;
        }
        return null;
    }

    @Override
    public boolean checkFilter(String filterId, ActionContext context) {
        final Timer.Context timerContext = filterTimer.time();
        try {
            ActionFilterRegistry filterReg = getFilterRegistry();
            ActionFilter filter = filterReg.getFilter(filterId);
            if (filter == null) {
                return false;
            }
            return filter.accept(null, context);
        } finally {
            long duration = timerContext.stop();
            if ((LOG_MIN_DURATION_NS >= 0) && (duration > LOG_MIN_DURATION_NS)) {
                log.info(String.format("Resolving filter with id '%s' took: %.2f ms", filterId, duration / 1000000.0));
            }
        }
    }

    @Override
    public boolean checkFilters(List<String> filterIds, ActionContext context) {
        return checkFilters(null, filterIds, context);
    }

    protected boolean checkFilters(Action action, List<String> filterIds, ActionContext context) {
        final Timer.Context timerContext = filtersTimer.time();
        try {
            ActionFilterRegistry filterReg = getFilterRegistry();
            for (String filterId : filterIds) {
                ActionFilter filter = filterReg.getFilter(filterId);
                if (filter == null) {
                    continue;
                }
                if (!filter.accept(action, context)) {
                    // denying filter found => ignore following filters
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Filter '%s' denied access", filterId));
                    }
                    return false;
                }
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Filter '%s' granted access", filterId));
                }
            }
            return true;
        } finally {
            long duration = timerContext.stop();
            if ((LOG_MIN_DURATION_NS >= 0) && (duration > LOG_MIN_DURATION_NS)) {
                log.info(String.format("Resolving filters %s took: %.2f ms", filterIds, duration / 1000000.0));
            }
        }
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("actions".equals(extensionPoint)) {
            actions.addContribution((Action) contribution);
        } else if ("filters".equals(extensionPoint)) {
            if (contribution.getClass() == FilterFactory.class) {
                registerFilterFactory((FilterFactory) contribution);
            } else {
                filters.addContribution((DefaultActionFilter) contribution);
            }
        } else if ("typeCompatibility".equals(extensionPoint)) {
            actions.getRegistry().getTypeCategoryRelations().add((TypeCompatibility) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if ("actions".equals(extensionPoint)) {
            actions.removeContribution((Action) contribution);
        } else if ("filters".equals(extensionPoint)) {
            if (contribution.getClass() == FilterFactory.class) {
                unregisterFilterFactory((FilterFactory) contribution);
            } else {
                filters.removeContribution((DefaultActionFilter) contribution);
            }
        }
    }

    /**
     * @deprecated seems not used in Nuxeo - should be removed - and anyway the merge is not done
     * @param ff
     */
    @Deprecated
    protected void registerFilterFactory(FilterFactory ff) {
        getFilterRegistry().removeFilter(ff.id);
        try {
            ActionFilter filter = (ActionFilter) Thread.currentThread()
                                                       .getContextClassLoader()
                                                       .loadClass(ff.className)
                                                       .newInstance();
            filter.setId(ff.id);
            getFilterRegistry().addFilter(filter);
        } catch (ReflectiveOperationException e) {
            log.error("Failed to create action filter", e);
        }
    }

    /**
     * @deprecated seems not used in Nuxeo - should be removed - and anyway the merge is not done
     * @param ff
     */
    @Deprecated
    public void unregisterFilterFactory(FilterFactory ff) {
        getFilterRegistry().removeFilter(ff.id);
    }

    @Override
    public void remove() {
    }

}
