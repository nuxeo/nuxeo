/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor object for registering {@link org.nuxeo.ecm.quota.QuotaStatsUpdater}s.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
@XObject("quotaStatsUpdater")
public class QuotaStatsUpdaterDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("@class")
    protected Class<? extends QuotaStatsUpdater> quotaStatsUpdaterClass;

    @XNode("@label")
    protected String label;

    @XNode("@descriptionLabel")
    protected String descriptionLabel;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Class<? extends QuotaStatsUpdater> getQuotaStatsUpdaterClass() {
        return quotaStatsUpdaterClass;
    }

    public void setQuotaStatsUpdaterClass(Class<? extends QuotaStatsUpdater> quotaStatsUpdaterClass) {
        this.quotaStatsUpdaterClass = quotaStatsUpdaterClass;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescriptionLabel() {
        return descriptionLabel;
    }

    public void setDescriptionLabel(String descriptionLabel) {
        this.descriptionLabel = descriptionLabel;
    }

    @Override
    public QuotaStatsUpdaterDescriptor clone() {
        QuotaStatsUpdaterDescriptor clone = new QuotaStatsUpdaterDescriptor();
        clone.setName(getName());
        clone.setEnabled(isEnabled());
        clone.setQuotaStatsUpdaterClass(getQuotaStatsUpdaterClass());
        clone.setLabel(getLabel());
        clone.setDescriptionLabel(getDescriptionLabel());
        return clone;
    }

}
