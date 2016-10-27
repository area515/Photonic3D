(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("PrintJobsController", ['$scope', '$http', '$location', '$anchorScroll', 'Upload', 'cwhWebSocket', 'photonicUtils', function ($scope, $http, $location, $anchorScroll, Upload, cwhWebSocket, photonicUtils) {
		controller = this;
		
		this.urlToUpload = null;
		this.filenameToUpload = null;
		this.fileToUpload = null;
		this.currentPrintJob = null;
		this.printJobs = null;
		this.currentSliceImage = null;
		this.currentBuildPhoto = {width: 500, height: 500, url: null};
		this.currentBuildVideo = {width: 100, height: 100, url: null};
		this.currentBuildLiveStream = {url: null, clientId: Math.random()}

		function refreshSelectedPrintJob(printJobList) {
        	var foundPrintJob = false;
        	
        	for (printJob of printJobList) {
        		if (controller.currentPrintJob != null && printJob.id === controller.currentPrintJob.id) {
        			controller.currentPrintJob = printJob;
        			foundPrintJob = true;
        		}
        	}
        	
        	if (!foundPrintJob) {
        		controller.currentPrintJob = null;
        	}
        }
		this.refreshPrintJobs = function refreshPrintJobs() {
			$http.get("/services/printJobs/list").success(function (data) {
				controller.printJobs = data;
				refreshSelectedPrintJob(data);
			});
		}
		this.hostSocket = cwhWebSocket.connect("services/hostNotification", $scope).onJsonContent(
			function(data) {
				if (data.notificationEvent == "PrintJobChanged") {
					controller.refreshPrintJobs();
					if (controller.currentPrintJob == null) {
						controller.currentSliceImage = "/services/printJobs/currentSliceImage/unknown";
					} else {
						controller.currentSliceImage = "/services/printJobs/currentSliceImage/" + encodeURIComponent(controller.currentPrintJob.id) + "?_=" + controller.currentPrintJob.currentSlice;
					}
				}
			}
		);
		if (this.hostSocket == null) {
			$scope.$emit("MachineResponse",  {machineResponse: {command:"Browser Too Old", message:"You will need to use a modern browser to run this application."}});
		}
		this.deletePrintJob = function deletePrintJob() {
			var printJobId = encodeURIComponent(controller.currentPrintJob.id);
	        $http.delete("/services/printJobs/delete/" + printJobId).success(
	        		function (data) {
	        			if (data.response) {
		        			controller.refreshPrintJobs();
	        			} else {
		        			$scope.$emit("MachineResponse", {machineResponse: data, successFunction:null, afterErrorFunction:null});
	        			}
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})
	    }
		this.stopPrintJob = function stopPrintJob() {
			$('#stop-btn').attr('class', 'fa fa-refresh fa-spin');
			var printJobId = encodeURIComponent(controller.currentPrintJob.id);
	        $http.post("/services/printJobs/stopJob/" + printJobId).success(function (data) {
	        			if (data.response) {
		        			controller.refreshPrintJobs();
	        			} else {
		        			$scope.$emit("MachineResponse", {machineResponse: data, successFunction:null, afterErrorFunction:null});
	        			}
	        			$('#stop-btn').attr('class', 'fa fa-stop');
	        		}).error(function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
 	        			$('#stop-btn').attr('class', 'fa fa-stop');
	        		})
	    }
		this.changeCurrentPrintJob = function changeCurrentPrintJob(newPrintJob) {
			controller.currentPrintJob = newPrintJob;
		}
		this.takeBuildPhoto = function takeBuildPhoto() {
			if (controller.currentPrintJob.printInProgress) {
				controller.currentBuildPhoto.url = "/services/media/takesnapshot/" + controller.currentPrintJob.printer.configuration.name + "/x/" + controller.currentBuildPhoto.width + "/y/" + controller.currentBuildPhoto.height + "?_=" + Math.random();
			} else {
				//TODO: Show an error if they don't have a printer selected
				//$scope.$emit("MachineResponse", {machineResponse: {status:"Failure", statusText:"Failure"}, successFunction:null, afterErrorFunction:null});
			}
		}
		this.startLiveStream = function startLiveStream() {
			if (controller.currentPrintJob.printInProgress) {
				controller.currentBuildLiveStream.url = "/services/media/startlivemjpegstream/" + controller.currentPrintJob.printer.configuration.name + "/clientid/" + controller.currentBuildLiveStream.clientId + "/x/" + controller.currentBuildVideo.width + "/y/" + controller.currentBuildVideo.height + "?_=" + Math.random();
			} else {
				//TODO: Show an error if they don't have a printer selected
				//$scope.$emit("MachineResponse", {machineResponse: {status:"Failure", statusText:"Failure"}, successFunction:null, afterErrorFunction:null});
			}
		}
		
		this.togglePausePrintJob = function togglePausePrintJob() {
			var printJobId = encodeURIComponent(controller.currentPrintJob.id);
	        $http.get("/services/printJobs/togglePause/" + printJobId).success(
	        		function (data) {
	        			if (data.response) {
		        			controller.refreshPrintJobs();
	        			} else {
		        			$scope.$emit("MachineResponse", {machineResponse: data, successFunction:null, afterErrorFunction:null});
	        			}
	        		}).error(
    				function (data, status, headers, config, statusText) {
 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
	        		})
		}
		
		this.videoRecord = function videoRecord(action, width, height) {
			//This is designed to stop the client from streaming
			if (action == 'stop' && controller.currentBuildLiveStream.url != null) {
				controller.currentBuildLiveStream.url = null;
				//TODO: no need to do this right?    controller.currentBuildLiveStream.clientId = Math.random();
		        $http.get("/services/media/stoplivemjpegstream/" + controller.currentPrintJob.printer.configuration.name + "/clientid/" + controller.currentBuildLiveStream.clientId + "?_=" + Math.random()).success(
		        		function (data) {
		        			if (data.response) {
			        			controller.refreshPrintJobs();
		        			} else {
			        			$scope.$emit("MachineResponse", {machineResponse: data, successFunction:null, afterErrorFunction:null});
		        			}
		        		}).error(
	    				function (data, status, headers, config, statusText) {
	 	        			$scope.$emit("HTTPError", {status:status, statusText:data});
		        		})
				
				return;
			}
			
			var parameters = encodeURIComponent(controller.currentPrintJob.printer.configuration.name);
			if (width != null) {
				parameters += "/x/" + width;
			}
			if (height != null) {
				parameters += "/y/" + height;
			}
	        $http.get("/services/media/" + action + "recordvideo/" + parameters).success(function (data) {
        		$scope.$emit("MachineResponse", {machineResponse: data, successFunction:null, afterErrorFunction:null});
        		if (action == 'stop') {
        			controller.currentBuildVideo.url = "/video/" + parameters + Math.random() + '.mp4'
					$("video").load();
        		}
    		}).error(function (data, status, headers, config, statusText) {
    			$scope.$emit("HTTPError", {status:status, statusText:data});
    		})
		}
		
		this.getPrintJobIconClass = function getPrintJobIconClass(printable) {
			return photonicUtils.getPrintFileProcessorIconClass(printable);
		}
		
		this.refreshPrintJobs();
	}])

})();
