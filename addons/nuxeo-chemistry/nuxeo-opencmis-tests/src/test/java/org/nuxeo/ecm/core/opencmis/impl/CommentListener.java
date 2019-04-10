/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

/**
 * Logs what comments we get.
 */
public class CommentListener implements EventListener {

    public static List<String> comments = new ArrayList<>();

    @Override
    public void handleEvent(Event event) {
        DocumentEventContext ctx = (DocumentEventContext) event.getContext();
        String comment = (String) ctx.getProperty("comment");
        String checkInComment = (String) ctx.getProperty("checkInComment");
        comments.add(event.getName() + ":comment=" + comment + ",checkInComment=" + checkInComment);
    }

    public static void clearComments() {
        comments.clear();
    }

    public static List<String> getComments() {
        return comments;
    }

}
