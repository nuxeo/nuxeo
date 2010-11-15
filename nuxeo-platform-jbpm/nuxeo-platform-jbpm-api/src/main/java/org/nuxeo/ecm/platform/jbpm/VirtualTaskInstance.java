/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Anahide Tchertchian
 *
 */
public class VirtualTaskInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    protected List<String> actors;

    protected String directive;

    protected String comment;

    protected Date dueDate;

    protected Map<String, Serializable> parameters = new HashMap<String,  Serializable>();

    public VirtualTaskInstance() {
    }

    public VirtualTaskInstance(List<String> actors) {
        this.actors = actors;
    }

    public VirtualTaskInstance(String actor) {
        actors = Collections.singletonList(actor);
    }

    public VirtualTaskInstance(List<String> actors, String directive,
            String comment, Date dueDate) {
        this.actors = actors;
        this.directive = directive;
        this.comment = comment;
        this.dueDate = dueDate;
    }

    public VirtualTaskInstance(String actor, String directive,
            String comment, Date dueDate) {
        this.actors = Collections.singletonList(actor);
        this.directive = directive;
        this.comment = comment;
        this.dueDate = dueDate;
    }

    public List<String> getActors() {
        if (actors == null) {
            actors = new ArrayList<String>();
        }
        return actors;
    }

    public void setActors(List<String> actors) {
        this.actors = actors;
    }

    public String getDirective() {
        return directive;
    }

    public void setDirective(String directive) {
        this.directive = directive;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Map<String, Serializable> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Serializable> parameters) {
        this.parameters = parameters;
    }

}
