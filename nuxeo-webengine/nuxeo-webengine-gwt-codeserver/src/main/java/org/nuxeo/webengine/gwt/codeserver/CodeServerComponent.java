/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.webengine.gwt.codeserver;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoException;
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
		startup();
	}

	@Override
	public void applicationStopped(ComponentContext context, Instant deadline) {
	    shutdown();
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
