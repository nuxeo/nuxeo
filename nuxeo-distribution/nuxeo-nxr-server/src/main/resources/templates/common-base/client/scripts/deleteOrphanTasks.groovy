import org.apache.logging.log4j.LogManager
import org.nuxeo.ecm.platform.task.Task
import org.nuxeo.ecm.core.api.IdRef
import org.nuxeo.ecm.core.api.ScrollResult
import org.nuxeo.runtime.transaction.TransactionHelper

def log = LogManager.getLogger("deleteOrphanTasks.groovy")
def nxql = "SELECT * FROM Document WHERE ecm:mixinType = 'Task'"
def dryRun = true
def session = Context.getCoreSession()
def ScrollResult<String> scrollResult = session.scroll(nxql, 500, 60)
def removedCount = 0
def taskCount = 0
def cache = [:]

log.info("Start deleting orphan tasks...")
while (scrollResult.hasResults()) {
    def results = scrollResult.getResults()
    taskCount += results.size()
    for (id in results) {
        def task = session.getDocument(new IdRef(id)).getAdapter(Task.class)
        def workflowRef = new IdRef(task.getProcessId())
        if (!cache.computeIfAbsent(workflowRef, session.&exists)) {
            if (dryRun) {
                log.info("Would remove orphan task: {}", id)
            } else {
                log.info("Removing orphan task: {}", id)
                session.removeDocument(new IdRef(id))
                removedCount += 1
            }
        }
    }
    TransactionHelper.commitOrRollbackTransaction()
    TransactionHelper.startTransaction()
    scrollResult = session.scroll(scrollResult.getScrollId())
}

log.info("Finished deleting orphan tasks: removed {} out of {} tasks", removedCount, taskCount)
