(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrintersController", ['$scope', '$http', '$location', '$anchorScroll', '$uibModal', 'photonicUtils', function ($scope, $http, $location, $anchorScroll, $uibModal, photonicUtils) {
		controller = this;
		var PRINTERS_DIRECTORY = "printers";
		var BRANCH = "master";
		var REPO = $scope.repo;
		
		this.loadingFontsMessage = "--- Loading fonts from server ---"
		this.loadingProfilesMessage = "--- Loading slicing profiles from server ---"
		this.loadingMachineConfigMessage = "--- Loading machine configurations from server ---"
		this.autodirect = $location.search().autodirect;
		
		//TODO: Instead of having this method we should understand how the selected printer gets out of sync and fix that
		function refreshSelectedPrinterAndAutodirectIfNecessary(printerList) {
        	var foundPrinter = false;
        	if (printerList.length == 1 && printerList[0].started && controller.autodirect != 'disabled') {
        		controller.currentPrinter = printerList[0];
        		controller.gotoPrinterControls();
        		foundPrinter = true;
        	} else {
        		var printersStarted = 0;
        		var currPrinter = null;
	        	for (printer of printerList) {
	        		if (printersStarted > 1) {
	        			break;
	        		}
	        		if (printer.started) {
	        			printersStarted += 1;
	        			currPrinter = printer;
	        		}
	        		
	        		if (controller.currentPrinter != null && printer.configuration.name === controller.currentPrinter.configuration.name) {
	        			controller.currentPrinter = printer;
	        			foundPrinter = true;
	        		}
	        	}
	        	if (printersStarted == 1 && controller.autodirect != 'disabled') {
	        		controller.currentPrinter = currPrinter;
	        		controller.gotoPrinterControls();
	        		foundPrinter = true;
	        	}
        	}
        	if (!foundPrinter) {
        		controller.currentPrinter = null;
        	}
        }
		
		function refreshPrinters() {
	        $http.get('/services/printers/list').success(function(data) {
	        	$scope.printers = data;
	        	refreshSelectedPrinterAndAutodirectIfNecessary(data);
	        });
	    }
		
		function executeActionAndRefreshPrinters(command, message, service, targetPrinter, postTargetPrinter, shouldRefreshPrinterList) {
			if (targetPrinter == null) {
    			$scope.$emit("MachineResponse", {machineResponse: {command:command, message:message, successFunction:null, afterErrorFunction:null}});
		        return;
			}
			var printerName = encodeURIComponent(targetPrinter.configuration.name);
			if (postTargetPrinter) {
			   $http.post(service, targetPrinter).then(
	       			function(response) {
	       				if (shouldRefreshPrinterList) {
	       					refreshPrinters();
	       					refreshSlicingProfiles();
	       					refreshMachineConfigurations();
	       				}
	       			}, 
	       			function(response) {
 	        			$scope.$emit("HTTPError", {status:response.status, statusText:response.data});
	       		}).then(function() {
	       			    $('#start-btn').attr('class', 'fa fa-play');
	        			$('#stop-btn').attr('class', 'fa fa-stop');
	       		});
		    } else {
		       $http.get(service + printerName).then(
		       		function(response) {
		        		$scope.$emit("MachineResponse", {machineResponse: response.data, successFunction:shouldRefreshPrinterList?refreshPrinters:null, afterErrorFunction:null});
		       		}, 
		       		function(response) {
	 	        		$scope.$emit("HTTPError", {status:response.status, statusText:response.data});
		       		}).then(function() {
		       			    $('#start-btn').attr('class', 'fa fa-play');
		        			$('#stop-btn').attr('class', 'fa fa-stop');
		       		});
			}
		}
		
		$scope.editCurrentPrinter = function editCurrentPrinter(editTitle) {
			controller.editTitle = editTitle;
			controller.editPrinter = JSON.parse(JSON.stringify(controller.currentPrinter));
			openSavePrinterDialog(editTitle, false);
		}

		$scope.savePrinter = function savePrinter(printer, isNewPrinter) {
			if (isNewPrinter) {//Rename the profiles to what the user entered if this is a new printer
				controller.editPrinter.configuration.MachineConfigurationName = controller.editPrinter.configuration.name;
				controller.editPrinter.configuration.SlicingProfileName = controller.editPrinter.configuration.name;
			}
			executeActionAndRefreshPrinters("Save Printer", "No printer selected to save.", '/services/printers/save', printer, true, isNewPrinter);
	        controller.editPrinter = null;
	        controller.openType = null;
	        photonicUtils.clearPreviewExternalState();
		}
		
		function openSavePrinterDialog(editTitle, isNewPrinter) {
			var editPrinterModal = $uibModal.open({
		        animation: true,
		        templateUrl: 'editPrinter.html',
		        controller: 'EditPrinterController',
		        size: "lg",
		        resolve: {
		        	title: function () {return editTitle;},
		        	editPrinter: function () {return controller.editPrinter;}
		        }
			});
		    editPrinterModal.result.then(function (savedPrinter) {$scope.savePrinter(savedPrinter, isNewPrinter)});
		}
		
		//TODO: When we get an upload complete message, we need to refresh file list...
		$scope.showFontUpload = function showFontUpload() {
			var fileChosenModal = $uibModal.open({
		        animation: true,
		        templateUrl: 'upload.html',
		        controller: 'UploadFileController',
		        size: "lg",
		        resolve: {
		        	title: function () {return "Upload True Type Font";},
		        	supportedFileTypes: function () {return ".ttf";},
		        	getRestfulFileUploadURL: function () {return function (filename) {return '/services/machine/uploadFont';}},
		        	getRestfulURLUploadURL: function () {return null;}
		        }
			});
			
			//fileChosenModal.result.then(function (savedPrinter) {$scope.savePrinter(savedPrinter, newPrinter)});
		}
		
		$scope.installCommunityPrinter = function installCommunityPrinter(printer) {
	        $http.get(printer.url).success(
	        		function (data) {
	        			controller.editPrinter = JSON.parse(window.atob(data.content));
	        			$scope.savePrinter(controller.editPrinter, false);
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})
	        return;
	    }
		
		this.createNewPrinter = function createNewPrinter(editTitle) {
			if (controller.currentPrinter == null) {
		        $http.post('/services/printers/createTemplatePrinter').success(
		        		function (data) {
		        			controller.editPrinter = data;
		        			openSavePrinterDialog(editTitle, true);
		        		}).error(
	    				function (data, status, headers, config, statusText) {
	 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
		        		})
		        return;
			}
			
			controller.editPrinter = JSON.parse(JSON.stringify(controller.currentPrinter));
			controller.editPrinter.configuration.name = controller.editPrinter.configuration.name + " (Copy)";
			//These must be set before we save a printer, otherwise the xml files aren't saved properly
			controller.editPrinter.configuration.MachineConfigurationName = controller.editPrinter.configuration.name;
			controller.editPrinter.configuration.SlicingProfileName = controller.editPrinter.configuration.name;
			openSavePrinterDialog(editTitle, true);
		}

		this.startCurrentPrinter = function startCurrentPrinter() {
			$('#start-btn').attr('class', 'fa fa-refresh fa-spin');
			executeActionAndRefreshPrinters("Start Printer", "No printer selected to start.", '/services/printers/start/', controller.currentPrinter, false, true);
		}
		
		this.stopCurrentPrinter = function stopCurrentPrinter() {
			$('#stop-btn').attr('class', 'fa fa-refresh fa-spin');			
			executeActionAndRefreshPrinters("Stop Printer", "No printer selected to Stop.", '/services/printers/stop/', controller.currentPrinter, false, true);
		}
		
		this.deleteCurrentPrinter = function deleteCurrentPrinter() {
			executeActionAndRefreshPrinters("Delete Printer", "No printer selected to Delete.", '/services/printers/delete/', controller.currentPrinter, false, true);
	        controller.currentPrinter = null;
		}
		
		this.changeCurrentPrinter = function changeCurrentPrinter(newPrinter) {
			controller.currentPrinter = newPrinter;
		}
		
        this.gotoPrinterControls = function gotoPrinterControls() {
        	$location.path('/printerControlsPage').search({printerName: controller.currentPrinter.configuration.name})
        };
		
		this.testTemplate = function testTemplate(scriptName, script) {
			var printerNameEn = encodeURIComponent(controller.currentPrinter.configuration.name);
			var scriptNameEn = encodeURIComponent(scriptName);
			
			$http.post('/services/printers/testTemplate/' + printerNameEn + "/" + scriptNameEn, script).success(function (data) {
				if (data.error) {
	     			$scope.$emit("MachineResponse", {machineResponse: {command:scriptName, message:data.errorDescription}, successFunction:null, afterErrorFunction:null});
				} else {
	     			$scope.$emit("MachineResponse", {machineResponse: {command:scriptName, message:"Successful execution. Template returned:" + data.result, response:true}, successFunction:null, afterErrorFunction:null});
				}
			}).error(function (data, status, headers, config, statusText) {
     			$scope.$emit("HTTPError", {status:status, statusText:data});
    		})
		}
		
		this.testRemainingPrintMaterial = function testRemainingPrintMaterial(printer) {
			var printerNameEn = encodeURIComponent(printer.configuration.name);
			
			$http.get('/services/printers/remainingPrintMaterial/' + printerNameEn).success(function (data) {
				//if (data.error) {
	     			$scope.$emit("MachineResponse", {machineResponse: data, successFunction:null, afterErrorFunction:null});
				/*} else {
	     			$scope.$emit("MachineResponse", {machineResponse: {command:scriptName, message:"Successful execution. Template returned:" + data.result, response:true}, successFunction:null, afterErrorFunction:null});
				}*/
			}).error(function (data, status, headers, config, statusText) {
     			$scope.$emit("HTTPError", {status:status, statusText:data});
    		})
		}
		
		$http.get('/services/machine/supportedFontNames').success(
				function (data) {
					controller.fontNames = data;
					controller.loadingFontsMessage = "Select a font...";
				});
		
		function refreshSlicingProfiles() {
			$http.get('/services/machine/slicingProfiles/list').success(
					function (data) {
						controller.slicingProfiles = data;
						controller.loadingProfilesMessage = "Select a slicing profile...";
					});
		}
		
		function refreshMachineConfigurations() {
			$http.get('/services/machine/machineConfigurations/list').success(
					function (data) {
						controller.machineConfigurations = data;
						controller.loadingMachineConfigMessage = "Select a machine configuration...";
					});
		}
		
		$http.get("https://api.github.com/repos/" + $scope.repo + "/contents/host/" + PRINTERS_DIRECTORY + "?ref=" + BRANCH).success(
			function (data) {
				$scope.communityPrinters = data;
			}
		);
		
		this.testScript = function testScript(scriptName, returnType, script) {
			photonicUtils.testScript(controller, scriptName, returnType, script);
		};
		
		controller.inkDetectors = [
		                           {name:"Visual Ink Detector", className:"org.area515.resinprinter.inkdetection.visual.VisualPrintMaterialDetector"},
		                           {name:"Digital GPIO Ink Detector", className:"org.area515.resinprinter.inkdetection.gpio.GpioDigitalPinInkDetector"}
		                          ];
		refreshSlicingProfiles();
		refreshMachineConfigurations();
		refreshPrinters();
	}])

})();
