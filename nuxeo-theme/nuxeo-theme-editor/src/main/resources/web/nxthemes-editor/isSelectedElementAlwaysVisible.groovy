
import org.nuxeo.theme.Manager

element = Context.runScript("getSelectedElement.groovy")
return Manager.getPerspectiveManager().isAlwaysVisible(element)
