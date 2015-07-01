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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LeafFaceletHandler.java 30553 2008-02-24 15:51:31Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.facelets;

/**
 * Leaf Facelet Handler
 * <p>
 * Facelet handler that does nothing.
 * <p>
 * Used when there is no next handler to apply, as next handler can never be null.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated since 7.4: use {@link org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler} instead.
 */
public class LeafFaceletHandler extends org.nuxeo.ecm.platform.ui.web.tag.handler.LeafFaceletHandler {

}
