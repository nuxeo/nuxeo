package org.nuxeo.ecm.plateform.jbpm.core.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.graph.exe.Comment;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskComment;

public class JBPMTaskWrapper implements Task {

    private static final long serialVersionUID = 1L;

    private TaskInstance ti;

    private String directive;

    private String targetDocId;

    private String initiator;

    private Boolean validated;

    public JBPMTaskWrapper(TaskInstance ti) {
        this.ti = ti;
        targetDocId = (String) ti.getVariable(JbpmService.VariableName.documentId.name());
        directive = (String) ti.getVariable(JbpmService.TaskVariableName.directive.name());
        validated = (Boolean) ti.getVariable(JbpmService.TaskVariableName.validated.name());
        initiator = (String) ti.getVariable(JbpmService.VariableName.initiator.name());
    }

    @Override
    public DocumentModel getDocument() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        return String.valueOf(ti.getId());
    }

    @Override
    public String getTargetDocumentId() {
        return targetDocId;
    }

    @Override
    public List<String> getActors() throws ClientException {
        Set<PooledActor> pooledActors = ti.getPooledActors();
        List<String> actors = new ArrayList<String>(pooledActors.size());
        for (PooledActor pooledActor : pooledActors) {
            String actor = pooledActor.getActorId();
            if (actor.contains(":")) {
                actors.add(actor.split(":")[1]);
            } else {
                actors.add(actor);
            }
        }
        return actors;
    }

    @Override
    public String getInitiator() throws ClientException {
        return initiator;
    }

    @Override
    public String getName() throws ClientException {
        return ti.getName();
    }

    @Override
    public String getDescription() throws ClientException {
        return ti.getDescription();
    }

    @Override
    public String getDirective() throws ClientException {
        return directive;

    }

    @Override
    public List<TaskComment> getComments() throws ClientException {
        List<Comment> jbpmComments = ti.getComments();
        List<TaskComment> comments = new ArrayList<TaskComment>(
                jbpmComments.size());
        for (Comment taskComment : jbpmComments) {
            comments.add(new TaskComment(taskComment.getActorId(),
                    taskComment.getMessage(), taskComment.getTime()));
        }
        return comments;
    }

    @Override
    public String getVariable(String key) throws ClientException {
        return (String) ti.getVariable(key);
    }

    @Override
    public Date getDueDate() throws ClientException {
        return ti.getDueDate();
    }

    @Override
    public Date getCreated() throws ClientException {
        return ti.getCreate();
    }

    @Override
    public Boolean isCancelled() throws ClientException {
        return ti.isCancelled();
    }

    @Override
    public Boolean isOpened() throws ClientException {
        return ti.isOpen();
    }

    @Override
    public Boolean hasEnded() throws ClientException {
        return ti.hasEnded();
    }

    @Override
    public Boolean isAccepted() throws ClientException {
        return validated;
    }

    @Override
    public Map<String, String> getVariables() throws ClientException {
        return ti.getVariables();
    }

    @Override
    public void setActors(List<String> actors) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInitiator(String initiator) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTargetDocumentId(String targetDocumentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setName(String name) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDescription(String description) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDirective(String directive) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVariable(String key, String value) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDueDate(Date dueDate) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCreated(Date created) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAccepted(Boolean accepted) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVariables(Map<String, String> variables)
            throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addComment(String author, String text) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancel(CoreSession coreSession) throws ClientException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void end(CoreSession coreSession) throws ClientException {
        throw new UnsupportedOperationException();
    }

}
