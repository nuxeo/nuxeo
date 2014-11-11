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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client;

/**
 * @author Alexandre Russel
 *
 */
public interface AnnotationConstant {
    String DECORATE_CLASS_NAME = "decorate";

    String DECORATE_NOT_CLASS_NAME = "decorateNot";

    String DECORATE_AREA_CLASS_NAME = "decorateArea";

    String SELECTED_CLASS_NAME = "selectedAnnotation";

    String SELECTED_NOT_CLASS_NAME = "selectedAnnotationNot";

    String SELECTED_TEXT_CLASS_NAME = "selectedText";

    int POPUP_PANEL_BLINK_TIMEOUT_MILI = 1000;

    int MAX_ANNOTATION_TEXT_LENGTH = 2000;

    String IGNORED_ELEMENT = "ignoredElement";

}
