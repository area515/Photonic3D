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
		
		this.editCurrentPrinter = function editCurrentPrinter(editTitle) {
			controller.editTitle = editTitle;
			controller.editPrinter = JSON.parse(JSON.stringify(controller.currentPrinter));
			//TODO: use data-toggle="modal" don't need js...
        	$('#editModal').modal();
		}
		
		this.executeActionAndRefreshPrinters = function executeActionAndRefreshPrinters(command, message, service, postData) {
			if (controller.currentPrinter == null) {
    			$scope.$emit("MachineResponse", {machineResponse: {command:command, message:message, successFunction:null, afterErrorFunction:null}});
		        return;
			}

			var printerName = encodeURIComponent(controller.currentPrinter.configuration.name);
			if (postData == null) {
		        $http.get(service + printerName).success(
		        		function (data) {
		        			$scope.$emit("MachineResponse", {machineResponse: data, successFunction:refreshPrinters, afterErrorFunction:null});
		        		}).error(
	    				function (data, status, headers, config, statusText) {
	 	        			$scope.$emit("HTTPError", {status:status, statusText:statusText});
		        		})
		    } else {
		        $http.post(service, postData).success(
		        		function (data) {
		        			$scope.$emit("MachineResponse", {machineResponse: data, successFunction:refreshPrinters, afterErrorFunction:null});
		        		}).error(
	    				function (data, status, headers, config, statusText) {
	 	        			$scope.$emit("HTTPError", {status:status, statusText:statusText});
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
	 	        			$scope.$emit("HTTPError", {status:status, statusText:statusText});
		        		})
		        return;
			}
			
			controller.editPrinter = JSON.parse(JSON.stringify(controller.currentPrinter));
			controller.editPrinter.configuration.name = controller.editPrinter.configuration.name + " (Copy)";
        	$('#editModal').modal();
		}
		
		this.savePrinter = function savePrinter() {
			this.executeActionAndRefreshPrinters("Save Printer", "No printer selected to save.", '/services/printers/save', controller.editPrinter);
	        controller.editPrinter = null;
		}
		
		this.startCurrentPrinter = function startCurrentPrinter() {
			this.executeActionAndRefreshPrinters("Start Printer", "No printer selected to start.", '/services/printers/start/', null);
		}
		
		this.stopCurrentPrinter = function stopCurrentPrinter() {
			this.executeActionAndRefreshPrinters("Stop Printer", "No printer selected to Stop.", '/services/printers/stop/', null);
		}
		
		this.deleteCurrentPrinter = function deleteCurrentPrinter() {
			this.executeActionAndRefreshPrinters("Delete Printer", "No printer selected to Delete.", '/services/printers/delete/', null);
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