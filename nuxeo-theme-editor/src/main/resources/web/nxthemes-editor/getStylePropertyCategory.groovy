category = Request.getSession(true).getAttribute("nxthemes.editor.style_property_category")
if (!category) {
    category = '*'
}
return category
