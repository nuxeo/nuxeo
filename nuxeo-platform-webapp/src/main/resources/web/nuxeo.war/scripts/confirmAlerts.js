function getTranslatedJSLabel(labelId) {
	return document.getElementById('js-'+labelId).innerHTML;
}
function confirmDeleteDocuments() {
    return confirm(getTranslatedJSLabel('label.documents.confirmDeleteDocuments'));
}
function confirmUndeleteDocuments() {
    return confirm(getTranslatedJSLabel('label.documents.confirmUndeleteDocuments'));
}
function confirmDeleteDocumentsForever() {
    return confirm(getTranslatedJSLabel('label.documents.confirmDeleteDocumentsForever'));
}
function confirmDeleteTask() {
    return confirm(getTranslatedJSLabel('label.review.confirmDeleteTask'));
}