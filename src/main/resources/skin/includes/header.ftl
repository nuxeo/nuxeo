<header>
  
  <h2>${docFolder.title}</h2>

  <detail>${Context.getMessage("easyshare.label.sharedBy")}
    <a title="email address" href="mailto:${docFolder.easysharefolder.contactEmail}">${docFolder.easysharefolder.contactEmail}</a>
  </detail>

  <detail>
  ${Context.getMessage("easyshare.label.date.expired")} : ${docFolder.dublincore.expired?string("dd/MM/yyyy")}
  </detail>

</header>