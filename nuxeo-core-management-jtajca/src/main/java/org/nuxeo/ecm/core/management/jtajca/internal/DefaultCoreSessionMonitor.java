package org.nuxeo.ecm.core.management.jtajca.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import javax.management.ObjectInstance;

import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreInstance.RegistrationInfo;
import org.nuxeo.ecm.core.management.jtajca.CoreSessionMonitor;
import org.nuxeo.ecm.core.management.jtajca.Defaults;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

public class DefaultCoreSessionMonitor implements CoreSessionMonitor {

    @Override
    public int getCount() {
        return CoreInstance.getInstance().getNumberOfSessions();
    }

    @Override
    public String[] getInfos() {
        return toInfos(toSortedRegistration(CoreInstance.getInstance().getRegistrationInfos()));
    }

    public RegistrationInfo[] toSortedRegistration(
            Collection<RegistrationInfo> infos) {
        RegistrationInfo[] sortedInfos = infos.toArray(new RegistrationInfo[infos.size()]);
        Arrays.sort(sortedInfos, new Comparator<RegistrationInfo>() {

            @Override
            public int compare(RegistrationInfo o1, RegistrationInfo o2) {
                return o2.session.getSessionId().compareTo(
                        o1.session.getSessionId());
            }

        });
        return sortedInfos;
    }

    public String[] toInfos(RegistrationInfo[] infos) {
        String[] values = new String[infos.length];
        for (int i = 0; i < infos.length; ++i) {
            values[i] = Defaults.instance.printStackTrace(infos[i]);
        }
        return values;
    }

    protected ObjectInstance self;

    protected void install() {
        self = DefaultMonitorComponent.bind(this);
        String mbean = self.getObjectName().toString();
        MetricsService metrics = Framework.getLocalService(MetricsService.class);
        metrics.newGauge(mbean,
                "count", AbstractSession.class, "count");
    }

    protected void uninstall() {
        DefaultMonitorComponent.unbind(self);
        self = null;
    }

}
