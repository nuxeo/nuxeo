/*
 * (C) Copyright 2006-2009 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.editor;

public class UndoBuffer {

    private String savedVersion;

    private String message;

    public UndoBuffer() {
        savedVersion = null;
    }

    public void save(String source, String message) {
        savedVersion = source;
        this.message = message;
    }

    public String getSavedVersion() {
        return savedVersion;
    }

    public void setSavedVersion(String savedVersion) {
        this.savedVersion = savedVersion;
    }

    public boolean canUndo() {
        return savedVersion != null;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void clearBuffer() {
        savedVersion = null;
    }
}
