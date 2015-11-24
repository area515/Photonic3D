   
    $( document ).ready(function() {

      $("#step-one").load("modals/step-one-modal.html");
      $("#step-two").load("modals/step-two-modal.html");
      $("#step-three").load("modals/step-three-modal.html");
      $("#step-four").load("modals/step-four-modal.html");
      $("#step-five").load("modals/step-five-modal.html"); 
//    });

    alert("Welcome to the print wizard demo\n\nEnjoy!!");
    
    $("#step-one-next").click(function() {
    $("#step-one-modal").modal('hide');
    $("#step-two-modal").modal('show');
	});

	$("#step-two-next").click(function() {
    $("#step-two-modal").modal('hide');
    $("#step-three-modal").modal('show');
	});
	$("#step-three-next").click(function() {
    $("#step-three-modal").modal('hide');
    $("#step-four-modal").modal('show');
	});
	$("#step-four-next").click(function() {
    $("#step-four-modal").modal('hide');
    $("#step-five-modal").modal('show');
	});

  $("#step-five-next").click(function() {
    alert("Thanks!!\n\nYou have reached the end of the print wizard demo");
  });



});