(function() {
	var cwhApp = angular.module('cwhApp');
	cwhApp.controller("UsersController", ['$scope', '$http', '$location', '$anchorScroll', '$uibModal', 'photonicUtils', function ($scope, $http, $location, $anchorScroll, $uibModal, photonicUtils) {
		$scope.guid = function guid() {
			  function s4() {
			    return Math.floor((1 + Math.random()) * 0x10000)
			      .toString(16)
			      .substring(1);
			  }
			  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
			    s4() + '-' + s4() + s4() + s4();
			}
		
		$scope.refreshUsers = function refreshUsers() {
	        $http.get('/services/users/list').success(function(data) {
	        	$scope.users = data;
	        });
	    }

		$scope.refreshMessages = function refreshMessages() {
			$http.get('/services/messages/list').success(
				function (data) {
					$scope.messages = data;
				});
		}
		
		$scope.openSaveUserDialog = function openSaveUserDialog(editTitle, isNewUser) {
			var editUserModal = $uibModal.open({
		        animation: true,
		        templateUrl: 'editUser.html',
		        controller: 'EditUserController',
		        size: "lg",
		        resolve: {
		        	title: function () {return editTitle;},
		        	editUser: function () {return $scope.editUser;}
		        }
			});
		    editUserModal.result.then(function (savedUser) {$scope.saveUser(savedUser, isNewUser)});
		}		
		
		$scope.openSaveMessageDialog = function openSaveMessageDialog(editTitle, isNewUser) {
			var editUserModal = $uibModal.open({
		        animation: true,
		        templateUrl: 'editMessage.html',
		        controller: 'EditMessageController',
		        size: "lg",
		        resolve: {
		        	title: function () {return editTitle;},
		        	editMessage: function () {return $scope.editMessage;},
		        	users: function () {return $scope.users;}
		        }
			});
		    editUserModal.result.then(function (savedUser) {$scope.saveMessage(savedUser, isNewUser)});
		}
		
		$scope.changeCurrentUser = function changeCurrentUser(newUser) {
			$scope.currentUser = newUser;
		}
		
		$scope.changeCurrentMessage = function changeCurrentMessage(newMessage) {
			$scope.currentMessage = newMessage;
		}
		
		$scope.deleteCurrentUser = function deleteCurrentUser() {
			$http.delete("/services/users/" + $scope.currentUser.userId).success(function (data) {
		        $scope.currentUser = null;
				$scope.refreshUsers();
			}).error(function (data, status, headers, config, statusText) {
     			$scope.$emit("HTTPError", {status:status, statusText:data});
    		});
		}		
		
		$scope.deleteCurrentMessage = function deleteCurrentMessage() {
			$http.delete("/services/messages/" + $scope.currentMessage.id).success(function (data) {
		        $scope.currentMessage = null;
				$scope.refreshMessages();
			}).error(function (data, status, headers, config, statusText) {
     			$scope.$emit("HTTPError", {status:status, statusText:data});
    		});
		}
		
		$scope.editCurrentUser = function editCurrentUser(editTitle) {
			$scope.editUser = JSON.parse(JSON.stringify($scope.currentUser));
			$scope.openSaveUserDialog(editTitle, false);
		}
		
		$scope.saveUser = function saveUser(user, isNewUser) {
			$http.post("/services/users", user).success(function (data) {
				$scope.refreshUsers();
				user.credential = null;
				$scope.currentUser = user;
		        $scope.editUser = null;
			}).error(function (data, status, headers, config, statusText) {
     			$scope.$emit("HTTPError", {status:status, statusText:data});
    		});
		}
		
		$scope.saveMessage = function saveMessage(message, isNewUser) {
			$http.put("/services/messages", message).success(function (data) {
				$scope.refreshMessages();
				$scope.currentMessage = data;
		        $scope.editMessage = null;
			}).error(function (data, status, headers, config, statusText) {
     			$scope.$emit("HTTPError", {status:status, statusText:data});
    		});
		}

		$scope.createNewUser = function createNewUser(editTitle) {
			if ($scope.currentUser == null) {
				$scope.editUser = {
						name : "New User",
						remote : false,
						credential : null,
						roles : ["login"],
						email : null,
				};
			} else {
				$scope.editUser = JSON.parse(JSON.stringify($scope.currentUser));
				$scope.editUser.name = $scope.editUser.name + " (Copy)";
				$scope.editUser.userId = null;
				$scope.editUser.credential = null;
				$scope.editUser.remote = false;
			}
			
			$scope.openSaveUserDialog(editTitle, true);
		}

		$scope.createNewMessage = function createNewMessage(editTitle) {
			$scope.editMessage = {
					fromUser: $scope.myUser,
				    message: "test message"	
				};
			
			if ($scope.currentUser != null) {
				$scope.editMessage.toUser = $scope.currentUser;
			} 
			
			if ($scope.currentMessage != null) {
				$scope.editMessage.toUser = $scope.currentMessage.fromUser;
			}
			
			if (editTitle == null) {
				editTitle = "Message from " + $scope.myUser.name;
			}
			
			$scope.openSaveMessageDialog(editTitle, true);
		}

		//TODO: We need to add in a watch for messages with a web socket
		$scope.refreshUsers();
		$scope.refreshMessages();
		
        $http.get('/services/users/whoAmI').success(function(data) {
        	$scope.myUser = data;
        });
	}])
})();
