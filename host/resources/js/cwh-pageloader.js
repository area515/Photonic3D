   
$( document ).ready(function() {

  $("#tabstatus").load("pages/status.html");
  $("#tabjobs").load("pages/jobs.html");
  $("#tabmotors").load("pages/motors.html");
  $("#tabdlp").load("pages/dlp.html");
  $("#tabselectprinter").load("pages/selectprinter.html");
  $("#tabnewprinter").load("pages/newprinter.html");

  //alert("Welcome to the print wizard demo\n\nEnjoy!!");

  $(document).on('click','.navbar-collapse.in',function(e) {
    if( $(e.target).is('a') ) {
        $(this).collapse('hide');
    }
  });

//   $(".maincontent").on('click',function(e) {
//    e.preventDefault(); // stops link form loading
//    $('.maincontent').hide(); // hides all content divs
//    $( $(this).attr('href') ).show(); //get the href and use it find which div to show
// });

});