/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: ActionFilter.java 20637 2007-06-17 12:37:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ActionFilter {

    String getId();

    void setId(String id);

    /**
     * Checks whether this action is valid in the given context.
     * <p>
     * The action is considered valid if no denying rule is found and at least one granting rule is found. If no rule is
     * found at all, it is valid.
     * <p>
     * In other words: OR between granting rules, AND between denying rules, denial is favored (also if exceptions
     * occur), AND inside of rules, OR inside or rule items (type, facet,...).
     *
     * @param action the optional action to check against, should be able to be null if filters evaluation only depends
     *            on given context.
     * @param context mandatory context holding variables to check against.
     * @return true if filters configuration for given action and context. Returns false if an error occurs during one
     *          of the conditions evaluation.
     */
    boolean accept(Action action, ActionContext context);

    /**
     * Returns a clone, useful for hot reload.
     *
     * @since 5.6
     */
    ActionFilter clone();

}
