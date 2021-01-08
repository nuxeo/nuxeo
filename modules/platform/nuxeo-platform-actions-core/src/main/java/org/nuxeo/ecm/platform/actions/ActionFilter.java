/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.actions;

/**
 * Interface for filter.
 */
public interface ActionFilter {

    String getId();

    /**
     * @see #accept(ActionContext)
     * @deprecated since 11.5: use {@link #accept(ActionContext)} instead.
     */
    @Deprecated(since = "11.5")
    default boolean accept(Action action, ActionContext context) {
        return accept(context);
    }

    /**
     * Checks whether this filter is valid in the given context.
     * <p>
     * The filter is considered valid if no denying rule is found and at least one granting rule is found. If no rule is
     * found at all, it is valid.
     * <p>
     * In other words: OR between granting rules, AND between denying rules, denial is favored (also if exceptions
     * occur), AND inside of rules, OR inside or rule items (type, facet,...).
     *
     * @param context mandatory context holding variables to check against.
     * @return true if filters configuration for given context. Returns false if an error occurs during one of the
     *         conditions evaluation.
     * @since 11.5
     */
    boolean accept(ActionContext context);

}
