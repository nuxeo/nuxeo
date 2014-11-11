category = Request.getSession(true).getAttribute("nxthemes.editor.style_category")
if (category == null) {
    category = "page"
}
return category
