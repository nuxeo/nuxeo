/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.theme.formats;

import org.nuxeo.theme.properties.PropertySheet;
import org.nuxeo.theme.relations.Predicate;
import org.nuxeo.theme.relations.Relate;
import org.nuxeo.theme.uids.Identifiable;

public interface Format extends Relate, Identifiable, PropertySheet {

    FormatType getFormatType();

    void setFormatType(FormatType formatType);

    Predicate getPredicate();

    void setDescription(String description);

    String getDescription();

    void clonePropertiesOf(Format source);

    boolean isNamed();

    boolean isCustomized();

    boolean isRemote();

    void setRemote(boolean remote);

    void setCustomized(boolean customized);

}
