(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrintersController", ['$scope', '$http', '$location', '$anchorScroll', function ($scope, $http, $location, $anchorScroll) {
		controller = this;
		
		function refreshSelectedPrinter(printerList) {
        	var foundPrinter = false;
        	
        	for (printer of printerList) {
        		if (controller.currentPrinter != null && printer.name === controller.currentPrinter.name) {
        			controller.currentPrinter = printer;
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
	        	refreshSelectedPrinter(data);
	        });
	    }
		
		this.executeActionAndRefreshPrinters = function executeActionAndRefreshPrinters(command, message, service, targetPrinter, postTargetPrinter) {
			if (targetPrinter == null) {
    			$scope.$emit("MachineResponse", {machineResponse: {command:command, message:message, successFunction:null, afterErrorFunction:null}});
		        return;
			}

			var printerName = encodeURIComponent(targetPrinter.configuration.name);
			if (postTargetPrinter) {
		        $http.post(service, targetPrinter).success(
		        		function (data) {
		        			$scope.$emit("MachineResponse", {machineResponse: data, successFunction:refreshPrinters, afterErrorFunction:null});
		        		}).error(
	    				function (data, status, headers, config, statusText) {
	 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
		        		})
		    } else {
		        $http.get(service + printerName).success(
		        		function (data) {
		        			$scope.$emit("MachineResponse", {machineResponse: data, successFunction:refreshPrinters, afterErrorFunction:null});
		        		}).error(
	    				function (data, status, headers, config, statusText) {
	 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
		        		})
			}
		}
		
		this.createNewPrinter = function createNewPrinter(editTitle) {
			controller.editTitle = editTitle;
			if (controller.currentPrinter == null) {
		        $http.post('/services/printers/createTemplatePrinter').success(
		        		function (data) {
		        			controller.editPrinter = data;
		                	$('#editModal').modal();
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
        	$('#editModal').modal();
		}
		
		this.editCurrentPrinter = function editCurrentPrinter(editTitle) {
			controller.editTitle = editTitle;
			controller.editPrinter = JSON.parse(JSON.stringify(controller.currentPrinter));
			//TODO: use data-toggle="modal" don't need js...
        	$('#editModal').modal();
		}

		this.savePrinter = function savePrinter(printer) {
			this.executeActionAndRefreshPrinters("Save Printer", "No printer selected to save.", '/services/printers/save', printer, true);
	        controller.editPrinter = null;
		}
		
		this.startCurrentPrinter = function startCurrentPrinter() {
			this.executeActionAndRefreshPrinters("Start Printer", "No printer selected to start.", '/services/printers/start/', controller.currentPrinter, false);
		}
		
		this.stopCurrentPrinter = function stopCurrentPrinter() {
			this.executeActionAndRefreshPrinters("Stop Printer", "No printer selected to Stop.", '/services/printers/stop/', controller.currentPrinter, false);
		}
		
		this.deleteCurrentPrinter = function deleteCurrentPrinter() {
			this.executeActionAndRefreshPrinters("Delete Printer", "No printer selected to Delete.", '/services/printers/delete/', controller.currentPrinter, false);
	        controller.currentPrinter = null;
		}
		
		this.changeCurrentPrinter = function changeCurrentPrinter(newPrinter) {
			controller.currentPrinter = newPrinter;
		}
		
        this.gotoPrinterControls = function gotoPrinterControls() {
        	$location.path('/printerControlsPage').search({printerName: controller.currentPrinter.configuration.name})
        };
        
		this.testScript = function testScript(scriptName, returnType, script) {
			var printerNameEn = encodeURIComponent(controller.currentPrinter.configuration.name);
			var scriptNameEn = encodeURIComponent(scriptName);
			var returnTypeEn = encodeURIComponent(returnType);
			
			$http.post('/services/printers/testScript/' + printerNameEn + "/" + scriptNameEn + "/" + returnTypeEn, script).success(function (data) {
				controller.graph = data.result;
				if (data.error) {
	     			$scope.$emit("MachineResponse", {machineResponse: {command:scriptName, message:data.errorDescription}, successFunction:null, afterErrorFunction:null});
	     		} else if (returnType.indexOf("[") > -1){
					$('#graphScript').modal();
				} else {
	     			$scope.$emit("MachineResponse", {machineResponse: {command:scriptName, message:"Successful execution. Script returned:" + JSON.stringify(data.result), response:true}, successFunction:null, afterErrorFunction:null});
				}
			}).error(function (data, status, headers, config, statusText) {
     			$scope.$emit("HTTPError", {status:status, statusText:data});
    		})
		}
		
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
		
		$http.get('/services/machine/serialPorts/list').success(
				function (data) {
					controller.serialPorts = data;
				});
		
		$http.get('/services/machine/graphicsDisplays/list').success(
				function (data) {
					controller.graphicsDisplays = data;
				});
		
		//TODO: All of these things should come from the MachineService
		controller.comPortSpeeds = [1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200];
		controller.parities = ["Even", "Mark", "None", "Odd", "Space"];
		controller.stopBits = ["None", "One", "1.5", "Two"];
		controller.dataBits = [5, 6, 7, 8, 9];
		controller.inkDetectors = [{name:"Visual Ink Detector", className:"org.area515.resinprinter.inkdetection.visual.VisualPrintMaterialDetector"}];
		refreshPrinters();
	}])

})();