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
 *     matic
 */
package org.nuxeo.ecm.core.repository.jcr.lock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author matic
 *
 */
public class RandomRepositoryOperationFactory {

    RandomRepositoryOperationFactory(Random random, Class<? extends RepositoryOperation>... types) {
        this.random = random;
        this.types.addAll(Arrays.asList(types));
    }

    protected final Random random;

    protected final List<Class<? extends RepositoryOperation>> types = new ArrayList<Class<? extends RepositoryOperation>>();

    public void addType(Class<? extends RepositoryOperation> type) {
        types.add(type);
    }

    RepositoryOperation getRandomOperation(CoreSession session, DocumentModel doc) {
        int index = Math.abs(random.nextInt()) % types.size();
        try {
            return types.get(index).getConstructor(CoreSession.class,
                    DocumentModel.class).newInstance(session, doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
