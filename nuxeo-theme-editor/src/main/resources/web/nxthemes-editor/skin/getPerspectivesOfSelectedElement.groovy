
import org.nuxeo.theme.Manager
import org.nuxeo.theme.perspectives.PerspectiveType

element = Context.runScript("getSelectedElement.groovy")

perspectives = []
for (PerspectiveType perspectiveType : Manager.getPerspectiveManager().getPerspectivesFor(element)) {
    perspectives.add(perspectiveType.name)
}
return perspectives
