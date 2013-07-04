package org.nuxeo.ecm.core.test;

import java.util.concurrent.TimeUnit;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
public class MassiveFolderCreationTest {

	protected String repositoryName;

	protected final String parentPath = "/default-domain/workspaces/test";

	protected Stopwatch watchBatch;

	protected Stopwatch watchDoc;

	protected int batchSize;

	protected class BatchWork extends AbstractWork {

		protected final String title;

		protected final int offset;

		BatchWork(int offset) {
			this.offset = offset;
			this.title = String.format("massive-batch-%03d", offset);
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public void work() throws Exception {
			initSession(repositoryName);
			Split splitBatch = watchBatch.start();
			for (int index = 0; index < batchSize; ++index) {
				Split splitDoc = watchDoc.start();
				DocumentModel doc = session.createDocumentModel(parentPath,
						name(offset, index), "File");
				doc = session.createDocument(doc);
				splitDoc.stop();
			}
			splitBatch.stop();
			printWatchSummary();
		}

	}

	protected String name(int offset, int index) {
		return String.format("file-%03d-%03d", offset, index);
	}

	protected @Inject
	CoreSession repo;

	public @Before
	void setRepositoryName() {
		repositoryName = repo.getRepositoryName();
	}

	public @Before
	void setupStopwatchs() {
		SimonManager.enable();
		watchBatch = SimonManager.getStopwatch("massive.batch");
		watchDoc = SimonManager.getStopwatch("massive.doc");
		
	}

	protected @Inject
	WorkManager manager;

	public @Test
	void createThousands() throws InterruptedException {
		batchSize = 1000;

		for (int offset = 0; offset < 200; ++offset) {
			manager.schedule(new BatchWork(offset));
		}

		manager.awaitCompletion(2, TimeUnit.MINUTES);
	}
	
	public void printWatchSummary() {
		System.out.println(watchBatch.toString());
		System.out.println(watchDoc.toString());
	}
}
