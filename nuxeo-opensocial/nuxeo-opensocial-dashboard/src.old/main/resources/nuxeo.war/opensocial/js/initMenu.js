$(document).ready( function() {

   activeMenu();
   // toggle des icones des onglets permettant d'afficher les menus lorsqu'on clique dessus
  $('.showPreferences').click( function(){
    return false;
  });

});

//permet de charger les menus pour chaque onglet
function activeMenu(){
   var options = {minWidth: 120, copyClassAttr: true, showDelay: 2000, hideDelay: 2000, offsetTop: 5};

   $(".showPreferences").each(function (){
     var imgId = '#' + $(this).attr('id');
     var ulId = '#' + 'ul-' + $(this).attr('id');
     $(imgId).menu(options, ulId);
   });
};