
Element selectedElement = Context.runScript("getSelectedElement.groovy")
templateEngine = Context.runScript("getTemplateEngine.groovy")

viewNames = []

if (selectedElement == null) {
    return viewNames
}

if (!selectedElement.getElementType().getTypeName().equals("fragment")) {
    return viewNames
}


FragmentType fragmentType = ((Fragment) selectedElement).getFragmentType()
for (ViewType viewType : ThemeManager.getViewTypesForFragmentType(fragmentType)) {
    String viewName = viewType.getViewName()
    String viewTemplateEngine = viewType.getTemplateEngine()
    if (!"*".equals(viewName) && templateEngine.equals(viewTemplateEngine)) {
        viewNames.add(viewName)
    }
}
return viewNames
