(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrintersController", ['$scope', '$http', '$location', '$anchorScroll', function ($scope, $http, $location, $anchorScroll) {
		controller = this;
		
		function refreshPrinters() {
	        $http.get('/services/printers/list').success(function(data) {
            	$scope.printers = data;
            	if (data != null && data.length > 0) {
            		controller.currentPrinter = data[0];
            	} else {
            		controller.currentPrinter = null;
            	}
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
        	$('#editModal').modal();
		}
		
		this.editCurrentPrinter = function editCurrentPrinter(editTitle) {
			controller.editTitle = editTitle;
			controller.editPrinter = JSON.parse(JSON.stringify(controller.currentPrinter));
			//TODO: use data-toggle="modal" don't need js...
        	$('#editModal').modal();
		}

		this.savePrinter = function savePrinter() {
			//These must be set before we save the printer, but this is probably going to overwrite the names on existing printers. Maybe this should only be done on new printers...
			controller.editPrinter.configuration.MachineConfigurationName = controller.editPrinter.configuration.name;
			controller.editPrinter.configuration.SlicingProfileName = controller.editPrinter.configuration.name;
			this.executeActionAndRefreshPrinters("Save Printer", "No printer selected to save.", '/services/printers/save', controller.editPrinter, true);
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
			
			//TODO: Fix for Mobile!
		    //$location.hash('printer');
		    //$anchorScroll();
		}
		
		$http.get('/services/machine/serialPorts/list').success(
				function (data) {
					controller.serialPorts = data;
				});
		
		$http.get('/services/machine/graphicsDisplays/list').success(
				function (data) {
					controller.graphicsDisplays = data;
				});
		
		refreshPrinters();
	}])

})();