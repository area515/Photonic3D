(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrintablesController", ['$scope', '$http', '$location', '$anchorScroll', 'Upload', 'cwhWebSocket', function ($scope, $http, $location, $anchorScroll, Upload, cwhWebSocket) {
		controller = this;
		
		this.urlToUpload = null;
		this.filenameToUpload = null;
		this.fileToUpload = null;
		this.currentPrintable = null;
		this.refreshPrintables = function refreshPrintables() {
			$http.get("/services/printables/list").success(
        		function (data) {
        			controller.printables = data;
        		}
	        );
		}
		this.hostSocket = cwhWebSocket.connect("services/hostNotification", $scope).onJsonContent(
			function(data) {
				if (data.notificationEvent == "FileUploadComplete") {
					controller.refreshPrintables();
				}
			}
		);
		if (this.hostSocket == null) {
			$scope.$emit("MachineResponse",  {machineResponse: {command:"Browser Too Old", message:"You will need to use a modern browser to run this application."}});
		}
		this.printPrintable = function printPrintable() {
			var printableName = encodeURIComponent(controller.currentPrintable.name);
			var printableExtension = encodeURIComponent(controller.currentPrintable.extension);
	        $http.post("/services/printables/print/" + printableName + "." + printableExtension).success(
	        		function (data) {
	        			controller.refreshPrintables();
	        			//$scope.$emit("MachineResponse", {machineResponse: data, successFunction:refreshPrintables, afterErrorFunction:null});
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})
		}
		this.deletePrintable = function deletePrintable() {
			var printableName = encodeURIComponent(controller.currentPrintable.name);
			var printableExtension = encodeURIComponent(controller.currentPrintable.extension);
	        $http.delete("/services/printables/delete/" + printableName + "." + printableExtension).success(
	        		function (data) {
	        			controller.refreshPrintables();
	        			controller.currentPrintable = null;
	        			//$scope.$emit("MachineResponse", {machineResponse: data, successFunction:null, afterErrorFunction:null});
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})
	    }
		this.changeCurrentPrintable = function changeCurrentPrintable(newPrintable) {
			controller.currentPrintable = newPrintable;
		}
		this.uploadFile = function uploadFile() {
			//TODO: we shouldn't do this here! There needs to be validations on the form!!!
			if (controller.fileToUpload == null) {
				$scope.$emit("MachineResponse", {machineResponse: {"command":"File Upload", "message":"Select a file first!"}, successFunction:null, afterErrorFunction:null});
				return;
			}
		    controller.fileToUpload.upload = Upload.http({
		      url: '/services/printables/uploadPrintableFile/' + encodeURIComponent(controller.fileToUpload.name),
		      method: 'POST',
		      headers: {
		        'Content-Type': "application/octet-stream"//"multipart/form-data"//controller.fileToUpload.type
		      },
		      data: controller.fileToUpload
		    });
	
		    controller.fileToUpload.upload.then(function (response) {
		    	//TODO: Upload complete should reload file list
		    		controller.fileToUpload.result = response.data;
		    	}, function (response) {
			    	//TODO: Upload complete should reload file list
		    		if (response.status > 0)
		    			$scope.errorMsg = response.status + ': ' + response.data;
		    	}
		    );
	
		    controller.fileToUpload.upload.progress(function (evt) {
		      controller.fileToUpload.progress = Math.min(100, parseInt(100.0 * evt.loaded / evt.total));
		    });
		}
		
		this.uploadURL = function uploadURL() {
			$http.post("services/printables/uploadviaurl/" + encodeURIComponent(controller.filenameToUpload) + "/" + encodeURIComponent(controller.urlToUpload)).then(
		    		function (data) {
		    			alert("Upload submitted...");
		    		},
		    		function (error) {
 	        			$scope.$emit("HTTPError", {status:error.status, statusText:error.data});
		    		});
		}
		
		//TODO: When we get an upload complete message, we need to refresh file list...
		this.showUpload = function showUpload() {
			//TODO: use data-toggle="modal" don't need js...
        	$('#uploadModal').modal();
		}
		this.getPrintableIconClass = function getPrintableIconClass(printable) {
			if (printable.printFileProcessor.friendlyName === 'Image') {
				return "fa-photo";
			}
			if (printable.printFileProcessor.friendlyName === 'Maze Cube') {
				return "fa-cube";
			}			
			if (printable.printFileProcessor.friendlyName === 'STL 3D Model') {
				return "fa-object-ungroup";
			}			
			if (printable.printFileProcessor.friendlyName === 'Creation Workshop Scene') {
				return "fa-diamond";
			}
			if (printable.printFileProcessor.friendlyName === 'Zip of Slice Images') {
				return "stack-overflow";
			}
			return "fa-question-circle";
		}
		
		this.refreshPrintables();
	}])

})();