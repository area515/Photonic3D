(function() {
	var cwhApp = angular.module('cwhApp');

	cwhApp.controller("UploadFileController", ['$rootScope', '$scope', '$http', '$uibModalInstance', 'Upload', 'title', 'supportedFileTypes', 'getRestfulFileUploadURL', 'getRestfulURLUploadURL', function ($rootScope, $scope, $http, $uibModalInstance, Upload, title, supportedFileTypes, getRestfulFileUploadURL, getRestfulURLUploadURL) {
		$scope.title = title;
		$scope.urlToUpload = null;
		$scope.filenameToUpload = null;
		$scope.fileToUpload = null;
		$scope.submitionType = null;
		$scope.supportedFileTypes = supportedFileTypes;
		$scope.getRestfulURLUploadURL = getRestfulURLUploadURL;
		
		$scope.formSubmit = function formSubmit() {
			if ($scope.submitionType == 'uploadURL') {
				$scope.uploadURL();
			} else if ($scope.submitionType == 'uploadFile') {
				$scope.uploadFile();
			}
		}
		$scope.uploadURLClicked = function uploadURLClicked() {
			$scope.submitionType = 'uploadURL';
		}
		$scope.uploadFileClicked = function uploadFileClicked() {
			$scope.submitionType = 'uploadFile';
		}

		$scope.uploadURL = function uploadURL() {
			$uibModalInstance.close();
			
			$http.post(getRestfulURLUploadURL($scope.filenameToUpload, $scope.urlToUpload)).then(
		    		function (data) {
						$rootScope.$emit("MachineResponse", {machineResponse: data.data, successFunction:null, afterErrorFunction:null});
		    		},
		    		function (error) {
 	        			$rootScope.$emit("HTTPError", {status:error.status, statusText:error.data});
		    		});
		}
		
		$scope.uploadFile = function uploadFile() {			
			//TODO: we shouldn't do this here! There needs to be validations on the form!!!
			if ($scope.fileToUpload == null) {
				$rootScope.$emit("MachineResponse", {machineResponse: {"command":"File Upload", "message":"Select a file first!"}, successFunction:null, afterErrorFunction:null});
				return;
			}
		    $scope.fileToUpload.upload = Upload.http({
		      url: getRestfulFileUploadURL($scope.fileToUpload.name),
		      method: 'POST',
		      headers: {
		        'Content-Type': "application/octet-stream"//"multipart/form-data"//$scope.fileToUpload.type
		      },
		      data: $scope.fileToUpload
		    });
	
		    $scope.fileToUpload.upload.then(function (response) {
		    		/*if (!response.data.response) {
						$rootScope.$emit("MachineResponse", {machineResponse: {"command":"File Upload", "message":response.data}, successFunction:null, afterErrorFunction:null});
		    		} else {
			    		$scope.fileToUpload.result = response.data;
		    		}*/
		    	}, function (response) {
			    	//TODO: Upload complete should reload file list
		    		if (response.status != 200) {
						$rootScope.$emit("MachineResponse", {machineResponse: {"command":"File Upload", "message":response.data}, successFunction:null, afterErrorFunction:null});
		    			//$scope.errorMsg = response.status + ': ' + response.data;
		    		}
		    	}
		    );
		    $scope.fileToUpload.upload.progress(function (evt) {
		    	$scope.fileToUpload.progress = Math.min(100, parseInt(100.0 * evt.loaded / evt.total));   
		      	var percent = $scope.fileToUpload.progress;
		      	if (percent <= 100) {
		      		$('.progress-bar').animate({
		      			width: percent + "%"
		      		}, 1)
			      	// $(".progress-bar").css('width', percent);
			      	$(".progress-bar").html(percent + "%");
			      	// if (percent == 100) {
			     		// $uibModalInstance.close();
			      	// }
		      	}
		    });
		}

		
		if ($scope.supportedFileTypes == null) {
			$http.get("/services/machine/supportedFileTypes").success(
	        		function (data) {
	        			$scope.supportedFileTypes = data.map(function (element) {
	        				return "." + element;
	        			}).join();
	        		}
		        );
		}
		
		$scope.save = function () {
			$uibModalInstance.close();
		};
		
		$scope.cancel = function () {
			$uibModalInstance.dismiss('cancel');
		};
	}])
})();