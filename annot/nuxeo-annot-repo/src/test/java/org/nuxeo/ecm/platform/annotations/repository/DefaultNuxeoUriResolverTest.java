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

package org.nuxeo.ecm.platform.annotations.repository;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;

/**
 * @author Alexandre Russel
 *
 */
public class DefaultNuxeoUriResolverTest extends AbstractRepositoryTestCase {

    private DefaultNuxeoUriResolver resolver;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setUpRepository();
    }

    @Test
    public void testGetDocumentRef() throws AnnotationException {
           assertNotNull(uri);
           resolver = new DefaultNuxeoUriResolver();
           DocumentRef ref = resolver.getDocumentRef(uri);
           assertNotNull(ref);
    }

}
