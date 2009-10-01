package org.nuxeo.opensocial.maven;

public class FakeRun implements Runnable {

	public static boolean hasRun;

	public void run() {
		FakeRun.hasRun = true;
	}

}
