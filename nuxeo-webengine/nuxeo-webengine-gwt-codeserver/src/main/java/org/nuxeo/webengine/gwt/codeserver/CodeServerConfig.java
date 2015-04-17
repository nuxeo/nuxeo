package org.nuxeo.webengine.gwt.codeserver;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("codeserver")
public class CodeServerConfig {

	@XNode("@module")
	String module;

	@XNode("classpath")
	CodeServerClasspath classpath;

	@XNodeList(value = "option", type = ArrayList.class, componentType = CodeServerOption.class)
	List<CodeServerOption> options = new ArrayList<>();

	CodeServerLauncher launcher;

	private CodeServerLoader loader;

	String[] toArgs() {
		List<String> args = new ArrayList<>();
		for (CodeServerOption each : options) {
			each.toArgs(args);
		}
		args.add(module);
		return args.toArray(new String[args.size()]);
	}

	void startup() throws Exception {
		loader = new CodeServerLoader(classpath.entries);
		launcher = loader.load();
		launcher.startup(toArgs());
	}

	void shutdown() throws Exception {
		try {
			if (launcher != null) {
				launcher.shutdown();
			}
		} finally {
			launcher = null;
			try {
				if (loader != null) {
					loader.close();
				}
			} finally {
				loader = null;
			}
		}
	}
}
