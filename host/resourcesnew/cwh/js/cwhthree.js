angular.module("tjsModelViewer", [])
	.directive(
		"tjsModelViewer",
		[function () {
			return {
				restrict: "AE",
				scope: {
					assimpUrl: "=assimpUrl"
				},
				link: function (scope, elem, attr) {
					var camera;
					var scene;
					var renderer;
					var previous;
					var parentDiv = elem[0].parentElement;
					
					init();
					
					var loader1 = new THREE.Photonic3dTriangleLoader();
 
					scope.$watch("assimpUrl", function(newValue, oldValue) {
						if ((newValue != null && oldValue != null && newValue.id != oldValue.id) || (oldValue == null && newValue != null)) {
							if (newValue != null) {
								loadModel("/services/printJobs/geometry/" + newValue.id);
							} else {
								//TODO: Somehow we need to clear out the geometry model
								if (previous) {
									scene.remove(previous);
									previous = null;
								}
							}
						}
					});
 
					function loadModel(modelUrl) {
						loader1.load(modelUrl, function (geometry) {
							//TODO: if there is a custom scale we could adjust it here
							var material = new THREE.MeshBasicMaterial( { color: 0x00ff00 } );
							var current = new THREE.Mesh( geometry, material );
							if (previous) {
								scene.remove(previous);
							}
							
							// This works
							var test = new THREE.BoxGeometry( 2, 2, 2 );
							var material = new THREE.MeshBasicMaterial( { color: 0xff0000 } );
							var cube = new THREE.Mesh( test, material );
							scene.add( cube );

							scene.add(current);
							previous = current;
						});
					}
					
					if (scope.assimpUrl != null) {
						loadModel(scope.assimpUrl);
						animate();
					}
					
					function init() {
						var width = parentDiv.clientWidth - (parentDiv.offsetLeft * 2);
						var height = 500;
						
						scene = new THREE.Scene();
						camera = new THREE.PerspectiveCamera( 75, width / height, 0.1, 1000 );
						camera.position.z = 100;
						
						/*
						scene = new THREE.Scene();
						camera = new THREE.PerspectiveCamera(50, width / height, 1, 2000);
						camera.position.set(2, 4, 5);
						scene.fog = new THREE.FogExp2(0x000000, 0.035);
						
						// Lights
						scene.add(new THREE.AmbientLight(0xcccccc));
						var directionalLight = new THREE.DirectionalLight(0xeeeeee);
						directionalLight.position.x = Math.random() - 0.5;
						directionalLight.position.y = Math.random() - 0.5;
						directionalLight.position.z = Math.random() - 0.5;
						directionalLight.position.normalize();
						scene.add(directionalLight);*/
						
						// Renderer
						renderer = new THREE.WebGLRenderer();
						renderer.setSize(width, height);
						elem[0].appendChild(renderer.domElement);//*/
 
						// Events
						window.addEventListener('resize', onWindowResize, false);
						animate();
					}
 
					function onWindowResize(event) {
						var width = parentDiv.clientWidth - (parentDiv.offsetLeft * 2);
						var height = 500;
						renderer.setSize(width, height);
						camera.aspect = parentDiv.clientWidth / height;
						camera.updateProjectionMatrix();
					}
 
					function animate() {
						requestAnimationFrame(animate);
						render();
					}
 
					function render() {
						var timer = Date.now() * 0.0005;
						camera.position.x = Math.cos(timer) * 10;
						camera.position.y = 100;
						camera.position.z = Math.sin(timer) * 10;
						camera.lookAt(scene.position);
						renderer.render(scene, camera);
					}
				}
			}
		}
	]);