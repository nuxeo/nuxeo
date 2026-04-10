/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.audit.impl;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2025.19
 * @deprecated since 2025.19, this generator is used to disable the Hibernate sequencer and leverage one from Audit
 *             service
 */
@SuppressWarnings("removal")
@Deprecated(since = "2025.19", forRemoval = true)
public class LogEntrySequenceGenerator extends SequenceStyleGenerator {

    public static final String USE_NUXEO_SEQUENCER_PROPERTY = "nuxeo.audit.backend.default.sql.use_nuxeo_sequencer";

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        if (Framework.isBooleanPropertyTrue(USE_NUXEO_SEQUENCER_PROPERTY)) {
            return ((LogEntryImpl) object).getOriginalId();
        } else {
            return super.generate(session, object);
        }
    }
}
