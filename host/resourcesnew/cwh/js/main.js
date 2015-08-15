	    (function() {
    	var cwhApp = angular.module('cwhApp', []);
    	
    	cwhApp.controller("getPrintables", function ($scope, $http) {
	        $http.get('/services/files/list').
            success(function(data) {
            	$scope.printables = data;
            });
    	})
    	
    	cwhApp.controller("getSettings", function ($scope, $http) {
	        $http.get('/services/settings/visibleCards').
            success(function(data) {
            	$scope.visibleCards = data;
            });
    	})
    	
    	cwhApp.controller("getPrinters", function ($scope, $http) {
	        $http.get('/services/machine/printers').
            success(function(data) {
            	$scope.printers = data;
            });
    	})
    	
	    //highlight first list group option (if non active yet)
	    if ( $('.list-group a.active').length === 0 ) {
	      $('.list-group a').first().addClass('active');
	    }
	
	    bootcards.init( {
	        offCanvasHideOnMainClick : true,
	        offCanvasBackdrop : true,
	        enableTabletPortraitMode : true,
	        disableRubberBanding : true,
	        disableBreakoutSelector : 'a.no-break-out'
	      });
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    //LEGACY CODE
	    //===================================
		window.onerror = function(msg, url, line, col, error) {
			showError(error, "Client Error");

		   	return true; //true will suppress error alerting to the browser
		};
		
		var uploadCompleteHandler = function(data){
			refreshListbox('/services/files/list', "filesselect")
			//TODO: Can't refresh status because you might not have a file selected: refreshStatus();
		}
		
		var errorHandler = function(xhr, ajaxOptions, thrownError) {
			var message;
			if (xhr.status == "401") {
				message = "You logged in wrong.";
			} else if (xhr.status == "501") {
				message = xhr.responseText;
			} else {
				message = "Problem communicating with host printer. (http:" + xhr.status + ")";
			}
			
			showError(message, "Host Error");
			
			$("body").css("cursor", "default");
		}
		
		function showError(body, title) {
			$('#errorModalBody').text(body);
			$('#errorModalTitle').text(title);
			$("#statustext").attr("placeholder", "Error");
			$('#errorModal').modal();
		}
		
		function progressHandlingFunction(e){
		    if(e.lengthComputable){
		        $('#uploadprogress').attr({value:e.loaded,max:e.total});
		    }
		}
		
		function refreshStatus() {
			var jobName = getSelectedOptionFromListBoxOrThrowError('filesselect', "Please select a job first");
			
			// Update DLP exposure time.
			$.ajax({
				type: 'GET', 
				url: '/services/machine/exposuretime/' + encodeURIComponent(jobName),
				dataType: "json",
	    		error: errorHandler,
				success: function(data){
					if (applyErrorIfNecessary(data, false, false)) {
						$("#exposureTimeText").val(data.message);
					}
				}
			});
			
			// Update DLP lift speed
			$.ajax({
				type: 'GET', 
				url: '/services/machine/zliftspeed/' + encodeURIComponent(jobName),
				dataType: "json",
	    		error: errorHandler,
				success: function(data){
					if (applyErrorIfNecessary(data, false, false)) {
						$("#liftSpeedText").val(data.message);
					}
				}
			});
			
			// Update DLP lift distance
			$.ajax({
				type: 'GET', 
				url: '/services/machine/zliftdistance/' + encodeURIComponent(jobName),
				dataType: "json",
	    		error: errorHandler,
				success: function(data){
					if (applyErrorIfNecessary(data, false, false)) {
						$("#liftDistanceText").val(data.message);
					}
				}
			});
			
			//TODO: Use promises instead of this crazy if train!!!
			// Update status
			$.ajax({
				type: 'GET',
				url: '/services/machine/currentslice/' + encodeURIComponent(jobName),
				dataType: "json",
	    		error: errorHandler,
				success: function(currentSliceData){
					if (applyErrorIfNecessary(currentSliceData, false, false)) {
						var currentslice = currentSliceData.message;
		
						if (currentslice === "-1") {
							$("#progresstext").text("0/0");
							$("#totalcost").text("$0.00");
							$("#sliceprogress").attr({value:0,max:1});
						} else {
							$.ajax({
								type: 'GET',
								url: '/services/machine/totalslices/' + encodeURIComponent(jobName),
								dataType: "json",
				        		error: errorHandler,
								success: function(totalSliceData){
									var totalslices = totalSliceData.message;
									
									$.ajax({
										type: 'GET', 
										url: '/services/machine/averageslicetime/' + encodeURIComponent(jobName),
										dataType: "json",
							    		error: errorHandler,
										success: function(data){
											createJobWebSocket(
										  			 jobName,
										  			 function (evt) {
												        var printJob = JSON.parse(evt.data)
														if (printJob.jobStatus != 'Printing') {
															updateStatusArea(0, "0", 0, 0, printJob.errorDescription);
															$("#currentSliceImage").attr("src", "/services/machine/currentsliceImage/noJobSelected");
														} else {
															updateStatusArea(printJob.totalCost, printJob.averageSliceTime, printJob.currentSlice, printJob.totalSlices);
															$("#currentSliceImage").attr("src", "/services/machine/currentsliceImage/" + encodeURIComponent(jobName) + "?" + new Date().getTime());
														}
												     });
										  
											updateStatusArea(0, data.message, currentslice, totalslices);

										} //end of seccess for averageslicetime
									});//end of ajax averageslicetime
								} //end of success for totalSliceData
							});//end of ajax totalSliceData
						} // end of else
					} // end of applyErrorIfNecessary if
				} // end of success for currentSlice
			}); // end of ajax currentSlice
		}//end of refressStatus
		
		function createJobWebSocket(jobName, onMessageFunction) {
		  	if ("WebSocket" in window) {
			     var ws = new WebSocket(createWebSocketURL("printjobnotification/" + encodeURIComponent(jobName)));
			     ws.onopen = function() {
			     };
			     ws.onmessage = onMessageFunction;
			     ws.onclose = function() {
			     };
			     
			     return ws;
			}
		  	
		  	return null;
		}
		
		function updateStatusArea(totalCost, averageSliceTime, currentslice, totalslices, errorDescription) {
			if (averageSliceTime === "0") {
				$("#statustext").attr("placeholder", "Time Left:Unknown");
			} else {
				//We add 1 to the currentslice because currentslice is zero based
				var timeLeft = (parseInt(totalslices) - (parseInt(currentslice) + 1)) * parseInt(averageSliceTime);
				var hoursLeft = Math.floor(timeLeft / (60 * 60 * 1000));
				timeLeft -= hoursLeft * 60 * 60 * 1000;
				var minutesLeft = Math.floor(timeLeft / (60 * 1000));
				timeLeft -= minutesLeft * 60 * 1000;
				var secondsLeft = Math.floor(timeLeft / 1000);
				var displayString = hoursLeft > 0? hoursLeft + " hours": "";
				displayString += minutesLeft > 0 || hoursLeft > 0? " " + minutesLeft + " minutes": "";
				displayString += secondsLeft > 0 || minutesLeft > 0 || hoursLeft > 0? " " + secondsLeft + " seconds": "";
				
				$("#statustext").attr("placeholder", "Approximate time Left: " + displayString);
			}
			$("#progresstext").text((parseInt(currentslice) + 1) + "/" + totalslices);
			if (totalCost > 0) {
				$("#totalcost").text("$" + totalCost);
			} else {
				$("#totalcost").text("");
			}
			$("#sliceprogress").attr({value:parseInt(currentslice) + 1, max:parseInt(totalslices)});
			if (errorDescription != null) {
				$("#statustext").attr("placeholder", errorDescription);
			}
		}
		
		function stopVideoRecord() {
			$.ajax({
				type: 'GET',
				url: "/services/media/stopvideorecord/camera1",
				dataType: "json",
	    		error: errorHandler,
				success: function(data){
					if (applyErrorIfNecessary(data, true, false)) {
						$("video > source").attr('src', 'video/camera' + Math.random() + '.mp4');
						$("video").load();
					}
				}
			});
		}
		
		function startVideoRecord() {
			$.ajax({
				type: 'GET',
				url: "/services/media/startrecordvideo/camera1",
				dataType: "json",
	    		error: errorHandler,
				success: function(data){
					if (applyErrorIfNecessary(data, true, false)) {
						//TODO: Disable the record button
					}
				}
			});
		}
		
		function createWebSocketURL(relativeURL) {
			  var loc = window.location;
			  var schema;
			  if (loc.protocol === "https:") {
				  schema = "wss:";
			  } else {
				  schema = "ws:";
			  }
			  
			  return schema + "//" + loc.host + "/services" + loc.pathname + relativeURL;
		}
		
		function refreshListbox(serviceUrl, listbox) {
			$.ajax({
				type: 'GET',
				url: serviceUrl,
				dataType: "json",
	    		error: errorHandler,
				success: function(data){
					retainSelectionAndPopulate(listbox, data)
				}
			});
		}
		
		function retainSelectionAndPopulate(selectListbox, data) {
			var retainSelectedOptions = [];
			$("#" + selectListbox + " option").each(function() {
			    $(this).remove();
			    if (this.selected) {
			    	retainSelectedOptions.push(this.text);
			    }
			});
			
			$.each(data, function( index, name ) {
				$("#" + selectListbox).append(
					$("<option/>", {
						value: name,
						text: name, 
						selected:$.inArray(name, retainSelectedOptions)?null:true}
					)
				);
			});
		}
		
		function createPrinterAndUpdatePrinters() {
			var printerName = $('#newPrinterNameText').val(); 
			var display = getSelectedOptionFromListBoxOrThrowError('displaySelect', "Please select a display first");
			var commPort = getSelectedOptionFromListBoxOrThrowError('portSelect', "Please select a serial communications port first");
			
			var requestValue = '/machine/createprinter/' + encodeURIComponent(printerName) + "/" + encodeURIComponent(display) + "/" + encodeURIComponent(commPort);

			requestAndUpdatePrinters(requestValue);
		}
		
		function deletePrinterAndUpdatePrinters() {
			var printerName = getSelectedOptionFromListBoxOrThrowError('printerSelect', "Please select a printer first");
			
			var requestValue = '/machine/deleteprinter/' + encodeURIComponent(printerName);

			requestAndUpdatePrinters(requestValue);
		}
		
		function startPrinterAndUpdatePrinters() {
			var printerName = getSelectedOptionFromListBoxOrThrowError('printerSelect', "Please select a printer first");
			
			var requestValue = '/machine/startprinter/' + encodeURIComponent(printerName);

			requestAndUpdatePrinters(requestValue);
		}
		
		function stopPrinterAndUpdatePrinters() {
			var printerName = getSelectedOptionFromListBoxOrThrowError('printerSelect', "Please select a printer first");
			
			var requestValue = '/machine/stopprinter/' + encodeURIComponent(printerName);

			requestAndUpdatePrinters(requestValue);
		}
		
		function deletefile() {
			var jobName = getSelectedOptionFromListBoxOrThrowError('filesselect', "Please select a job first");
			
			var requestValue = '/files/delete/' + encodeURIComponent(jobName);

			requestAndUpdateFiles(requestValue);
		}
		
		function startjob(){
			var jobName = getSelectedOptionFromListBoxOrThrowError('filesselect', "Please select a job first");
			var printerName = getSelectedOptionFromListBoxOrThrowError('printerSelect', "Please select a printer first");
			
			var requestValue = '/machine/startjob/' + encodeURIComponent(jobName) + "/" + encodeURIComponent(printerName);

			request(requestValue, true);
		}
		
		function stopjob(){
			var jobName = getSelectedOptionFromListBoxOrThrowError('filesselect', "Please select a job first");
			var requestValue = '/machine/stopjob/' + encodeURIComponent(jobName);

			request(requestValue, true);
		}

		function togglePause() {
			var jobName = getSelectedOptionFromListBoxOrThrowError('filesselect', "Please select a job first");
			var requestValue = '/machine/togglepause/' + encodeURIComponent(jobName);

			request(requestValue, true);
		}
		
		function setZLiftSpeed(incrementTimeLiftSpeed) {
			var jobName = getSelectedOptionFromListBoxOrThrowError('filesselect', "Please select a job first");
			var liftSpeed = parseFloat($('#liftSpeedText').val());
			if (incrementTimeLiftSpeed != null) {
				liftSpeed += parseFloat(incrementTimeLiftSpeed);
				$("#liftSpeedText").val(liftSpeed + "");
			}
			
			var requestValue = '/machine/zliftspeed/' + encodeURIComponent(jobName) + '/' + liftSpeed;

			request(requestValue, true);
		}
		
		function setZLiftDistance(incrementTimeLiftDistance) {
			var jobName = getSelectedOptionFromListBoxOrThrowError('filesselect', "Please select a job first");
			var liftDistance = parseFloat($('#liftDistanceText').val());
			if (incrementTimeLiftDistance != null) {
				liftDistance += parseFloat(incrementTimeLiftDistance);
				$("#liftDistanceText").val(liftDistance + "");
			}
			
			var requestValue = '/machine/zliftdistance/' + encodeURIComponent(jobName) + '/' + liftDistance;

			request(requestValue, true);
		}
		
		function sendGCode() {
			var gcode = $('#sendGcodeText').val();

			requestForPrinter('gcode', encodeURIComponent(gcode));
		}
		
		function sendExposureTime(incrementTime) {
			var jobName = getSelectedOptionFromListBoxOrThrowError('filesselect', "Please select a job first");
			var exposureTime = parseInt($('#exposureTimeText').val());
			if (isNaN(exposureTime)) {
				exposureTime = parseInt(incrementTime);
			} else {
				exposureTime += parseInt(incrementTime);
			}
			
			$("#exposureTimeText").val(exposureTime);
			
			var requestValue = '/machine/exposuretime/' + encodeURIComponent(jobName) + "/" + encodeURIComponent(exposureTime);

			request(requestValue, false);
		}
		
		function showblankscreen() {
			var printerName = getSelectedOptionFromListBoxOrThrowError('printerSelect', "Please select a printer first");
			
			var requestValue = '/machine/showblankscreen/' + encodeURIComponent(printerName);

			request(requestValue, false);
		}
		
		function showCalibrationScreen(incrementPixels) {
			var printerName = getSelectedOptionFromListBoxOrThrowError('printerSelect', "Please select a printer first");
			var pixels = parseInt($('#calibrationPixelsText').val());
			if (isNaN(pixels)) {
				pixels = parseInt(incrementPixels);
			} else {
				pixels += parseInt(incrementPixels);
			}
			
			$("#calibrationPixelsText").val(pixels);
			
			var requestValue = '/machine/showcalibrationscreen/' + encodeURIComponent(printerName) + "/" + encodeURIComponent(pixels);

			request(requestValue, false);
		}
		
		/*This is no longer used...
		function sendMuve1GCode(incrementTimeLiftDistance, incrementTimeLiftSpeed, incrementLiftPause) {
			var printerName = getSelectedOptionFromListBoxOrThrowError('printerSelect', "Please select a printer first");
			var liftSpeed = parseFloat($('#liftSpeedText').val());
			var liftDistance = parseFloat($('#liftDistanceText').val());
			//var liftPause = parseInt($('#liftPauseText').val());
			
			var gCode = "M650";
			
			if (incrementTimeLiftDistance != null) {
				liftDistance += parseFloat(incrementTimeLiftDistance);
				$("#liftDistanceText").val(liftDistance + "");
				gCode += " D" + liftDistance;
			}
			if (incrementTimeLiftSpeed != null) {
				liftSpeed += parseFloat(incrementTimeLiftSpeed);
				$("#liftSpeedText").val(liftSpeed + "");
				gCode += " S" + liftSpeed;
			}
			if (incrementLiftPause != null) {
				liftPause += incrementLiftPause;
				gCode += " P" + parseInt(liftPause);
			}
			
			var requestValue = '/machine/gcode/' + encodeURIComponent(printerName) + "/" + gCode;

			request(requestValue, false);
		}*/
		
		function requestAndUpdatePrinters(requestValue) {
			$("body").css("cursor", "wait");
			$.ajax({
				type: 'GET',
				url: '/services' + requestValue,
				dataType: "json",
	    		error: errorHandler,
				success: function(data){
					if (applyErrorIfNecessary(data, true, false)) {
						refreshListbox('/services/machine/printers', "printerSelect");
					}
				}
			});
		}	
		
		function requestAndUpdateFiles(requestValue) {
			$("body").css("cursor", "wait");
			$.ajax({
				type: 'GET',
				url: '/services' + requestValue,
				dataType: "json",
	    		error: errorHandler,
				success: function(data){
					if (applyErrorIfNecessary(data, true, false)) {
						refreshListbox('/services/files/list', "filesselect")
					}
				}
			});
		}
		
		function request(requestValue, updateStatus) {
			$("body").css("cursor", "wait");
			$.ajax({
				type: 'GET',
				url: '/services' + requestValue,
				dataType: "json",
	    		error: errorHandler,
				success: function(data){
					if (applyErrorIfNecessary(data, true, false) && updateStatus) {
						refreshStatus();
					}
				}
			});
		}
		
		function requestForPrinter(command, parameters){
			 var printerName = getSelectedOptionFromListBoxOrThrowError('printerSelect', "Please select a printer first");
			
			$("body").css("cursor", "wait");
			$.ajax({
				type: 'GET',
				url: '/services/machine/' + command + "/" + encodeURIComponent(printerName) + (parameters != null?'/' + parameters: ""),
				dataType: "json",
	    		error: errorHandler,
				success: function(data){
					applyErrorIfNecessary(data, true, true);
				}
			});
		}
		
		function applyErrorIfNecessary(data, showMessageOnSuccess, applyTimestamp) {
			$("body").css("cursor", "default");

			if (!data.response) {
				showError(data.message, "Error On " + data.command);
			} else if (showMessageOnSuccess) {
				if (applyTimestamp) {
					var d = new Date();
					$("#statustext").attr("placeholder", d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds() + " " + d.getMilliseconds() + " " + data.message);
				} else {
					$("#statustext").attr("placeholder", data.message);
				}
			}
			
			return data.response;
		}
		
		function getSelectedOptionFromListBoxOrThrowError(listboxId, errorMessage) {
			var listbox = $('#' + listboxId + ' option:selected').val();
			
			if (typeof listbox != 'undefined') {
				return listbox;
			}
			
			throw errorMessage;
		}
	    
	    
	    
	    
	    
    })();