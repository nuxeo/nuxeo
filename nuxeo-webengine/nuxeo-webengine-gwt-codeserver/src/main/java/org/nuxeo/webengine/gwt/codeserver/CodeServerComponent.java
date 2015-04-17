package org.nuxeo.webengine.gwt.codeserver;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class CodeServerComponent extends DefaultComponent {

	final Map<String, CodeServerConfig> servers = new HashMap<>();

	@Override
	public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) { 
		if (contribution instanceof CodeServerConfig) {
			CodeServerConfig install = (CodeServerConfig) contribution;
			servers.put(install.module, install);
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
				shutdown();
			}

		});
		startup();
	}

	protected void startup()  {
		new Runner() {

			@Override
			void doRun(CodeServerConfig server) throws Exception {
				server.startup();
			}

		}.run();
	}

	protected void shutdown() {
		new Runner() {

			@Override
			void doRun(CodeServerConfig server) throws Exception {
				server.shutdown();
			}

		}.run();
	}

	abstract class Runner {

		void run() {
			NuxeoException errors = new NuxeoException("Cannot shudown gwt code servers");
			for (CodeServerConfig server : servers.values()) {
				try {
					doRun(server);
				} catch (Exception cause) {
					errors.addSuppressed(cause);
				}
			}
			if (errors.getSuppressed().length > 0) {
				throw errors;
			}
		}

		abstract void doRun(CodeServerConfig server) throws Exception;
	}

}
