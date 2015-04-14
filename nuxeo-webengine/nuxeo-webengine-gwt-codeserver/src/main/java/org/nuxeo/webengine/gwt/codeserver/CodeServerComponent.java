package org.nuxeo.webengine.gwt.codeserver;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class CodeServerComponent extends DefaultComponent {

    final List<CodeServerOption> options = new ArrayList<>();

    final CodeServerLoader loader = new CodeServerLoader();

    CodeServerLauncher launcher;

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }


    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) { // TODO
        // method
        if (contribution instanceof CodeServerOption) {
            CodeServerOption option = (CodeServerOption) contribution;
            options.add(option);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (event.id != RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP) {
                    return;
                }
                Framework.removeListener(this);
                if (launcher == null) {
                    return;
                }
                try {
                    launcher.shutdown();
                } catch (Exception cause) {
                    LogFactory.getLog(CodeServerComponent.class).error("Cannot shutdown gwt code server", cause);
                } finally {
                    launcher = null;
                }
            }
        });
        try {
            launcher = loader.load();
            launcher.startup(toArgs());
        } catch (Exception cause) {
            LogFactory.getLog(CodeServerComponent.class).error("Cannot launch gwt code server", cause);
        }
    }

    String[] toArgs() {
        List<String> args = new ArrayList<>();
        for (CodeServerOption each : options) {
            each.toArgs(args);
        }
        args.add("com.nuxeo.studio.StudioApp");
        return args.toArray(new String[args.size()]);
    }

}
