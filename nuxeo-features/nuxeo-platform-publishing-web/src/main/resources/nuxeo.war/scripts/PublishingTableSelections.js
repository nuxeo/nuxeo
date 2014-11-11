var proxyPublish = null;

function selectSectionForPublish(sectionRef, selected) {
  if (proxyPublish == null) {
    proxyPublish = Seam.Component.getInstance("publishActions");
  }
  proxyPublish.processRemoteSelectRowEvent(sectionRef,selected,selectDataTableRowCB);
}
