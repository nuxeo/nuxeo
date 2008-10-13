
import org.nuxeo.theme.elements.Element
import org.nuxeo.theme.fragments.Fragment
import org.nuxeo.theme.fragments.FragmentType
import org.nuxeo.theme.views.ViewType
import org.nuxeo.theme.themes.ThemeManager

selectedElement = Context.runScript("getSelectedElement.groovy")
templateEngine = Context.runScript("getTemplateEngine.groovy")

viewNames = []

if (selectedElement == null) {
    return viewNames
}

if (!selectedElement.getElementType().getTypeName().equals("fragment")) {
    return viewNames
}


fragmentType = ((Fragment) selectedElement).getFragmentType()
for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
    viewName = viewType.getViewName()
    viewTemplateEngine = viewType.getTemplateEngine()
    if (!"*".equals(viewName) && templateEngine.equals(viewTemplateEngine)) {
        viewNames.add(viewName)
    }
}

return viewNames
