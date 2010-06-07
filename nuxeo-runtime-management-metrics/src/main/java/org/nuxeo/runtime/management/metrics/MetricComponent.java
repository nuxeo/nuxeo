package org.nuxeo.runtime.management.metrics;

import org.javasimon.SimonManager;
import org.nuxeo.runtime.model.ComponentContext;
import org.osgi.framework.BundleContext;


public class MetricComponent extends org.nuxeo.runtime.model.DefaultComponent {


    protected MetricSerializer serializer = new MetricSerializer();

    protected MetricEnabler enabler = new MetricEnabler();


    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (MetricSerializerMXBean.class.isAssignableFrom(adapter)) {
            return adapter.cast(serializer);
        }
        return super.getAdapter(adapter);
    }

    protected MetricRegisterer register = new MetricRegisterer();

    protected MetricRegisteringCallback  registeringCB = new  MetricRegisteringCallback();

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        doStart();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        super.deactivate(context);
    }


    public void start(BundleContext context) {
        doStart();
    }

    public void stop(BundleContext context) {
        doStop();
    }

    protected void doStart() {
        enabler.setSerializer(serializer);
        SimonManager.enable();
        SimonManager.callback().addCallback(registeringCB);
        register.registerMXBean(enabler);
        register.registerMXBean(serializer);
    }

    protected void doStop() {
        SimonManager.disable();
        SimonManager.callback().removeCallback(registeringCB);
        register.unregisterMXBean(enabler);
        register.unregisterMXBean(serializer);
    }
}
