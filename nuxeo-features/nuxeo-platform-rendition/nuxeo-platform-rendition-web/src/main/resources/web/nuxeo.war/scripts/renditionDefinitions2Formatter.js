var nuxeo = nuxeo || {};

function formatSuggestedRenditionDefinitions(renditionDefinition) {
    var escapeHTML = nuxeo.utils.escapeHTML;
    return "<span class='s2existingRenditionDefinition'>" + escapeHTML(renditionDefinition.displayLabel) + "</span>"
}

function formatSelectedRenditionDefinitions(renditionDefinition) {
    var escapeHTML = nuxeo.utils.escapeHTML;
    return "<span class='s2existingRenditionDefinition'>" + escapeHTML(renditionDefinition.displayLabel) + "</span>"
}
