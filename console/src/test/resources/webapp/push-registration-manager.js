function PushRegistrationManager( webPushServerUrl ) {
    this.webPushServerUrl = webPushServerUrl;
}

PushRegistrationManager.parseLinkHeader = function( linkHeader ) {
    var links = linkHeader.split(',');
    var parsed = [];
    for (var i = 0; i < links.length; i++) {
        var weblink = links[i].replace(/<(.*)>/, '$1').trim();
        var weblinks = weblink.split(';');
        var url = weblinks[0];
        var type = weblinks[1].substring(weblinks[1].indexOf("=") + 1);
        parsed.push({"type": type, "url": url});
    }
    return parsed;
}

PushRegistrationManager.prototype.connect = function() {
    var _this = this;
    return new Promise(function( resolve, reject ) {
        try {
        var xhr = new XMLHttpRequest();
        xhr.open( 'POST', _this.webPushServerUrl + "/webpush/register", true );
        xhr.onload = function ( e ) {
            if (xhr.readyState === 4) {
                if ( xhr.status === 200 ) {
                    var linkHeader = xhr.getResponseHeader('link');
                    var webLinks = PushRegistrationManager.parseLinkHeader( linkHeader );
                    for (var i = 0; i < webLinks.length; i++) {
                        if (webLinks[i].type === '"push:monitor"') {
                            _this.monitorUrl = webLinks[i].url;
                        } else if (webLinks[i].type ==='"push:channel"') {
                            _this.channelUrl = webLinks[i].url;
                        }
                    }
                    console.log("monitorUrl:", _this.monitorUrl);
                    console.log("channelUrl:", _this.channelUrl);
                    resolve( xhr.status );
                } else {
                    reject( Error(xhr.statusText) )
                }
            }
        }
        xhr.send( null );
        } catch ( error ) {
            console.log( error);
        }
    });
}

PushRegistrationManager.prototype.register = function() {
    var _this = this;
    // this will be moved into the service worker and done once oninstall
    return new Promise(function( resolve, reject ) {
            // check and ask for permission to recieve push notifications.
            if (!("Notification" in window)) {
                // The spec says that this should be a DOMException
                //var exception = DOMException.NOT_SUPPORTED_ERR;
                //exception.name = 'PermissionDeniedError';
                reject(Error("Notifications not supported by browser."));
                return promise;
            } else if (Notification.permission !== "denied") {
                Notification.requestPermission(function( permission ) {
                    if (permission !== "granted") {
                        //var notGranted = DOMException.NOT_SUPPORTED_ERR;
                        //notGranted.name = 'PermissionDeniedError';
                        reject(Error("Notifications not granted by user"));
                        return promise;
                    }
                });
            }
            var xhr = new XMLHttpRequest();
            xhr.open( 'POST', _this.webPushServerUrl + "/" + _this.channelUrl, true );
            xhr.onload = function ( e ) {
                if (xhr.readyState === 4) {
                    if ( xhr.status === 201 ) {
                        console.log("Created channel with url:", xhr.getResponseHeader('location'));
                        resolve( {registrationId: "1234", endpoint: xhr.getResponseHeader('location') });
                    } else {
                        reject(Error(xhr.statusText))
                    }
                }
            }
            xhr.send( null );
    });
}

PushRegistrationManager.prototype.unregister = function() {
}

PushRegistrationManager.prototype.monitor = function() {
    console.log("monitor...");
    var _this = this;
    try {
        var source = new EventSource( _this.webPushServerUrl + "/" + _this.monitorUrl );
    } catch ( error ) {
        console.log(error);
    }
    source.addEventListener('message', _this.onmessage, false);
    source.addEventListener('open', _this.onopen, false);
    source.addEventListener('error', _this.onerror, false);
    /*
    return new Promise(function( resolve, reject ) {
            var xhr = new XMLHttpRequest();
            xhr.open( 'GET', _this.webPushServerUrl + "/" + _this.monitorUrl, true );
            xhr.onload = function ( e ) {
                if (xhr.readyState === 4) {
                    if ( xhr.status === 200 ) {
                        console.log("Monitoring...");
                        resolve();
                    } else {
                        reject(Error(xhr.statusText))
                    }
                }
            }
            xhr.send( null );
    });
    */
}

PushRegistrationManager.prototype.onmessage = function( event ) {
    console.log("onmessage: ", event.data);
}

PushRegistrationManager.prototype.onopen = function( e ) {
}

PushRegistrationManager.prototype.onerror = function( e ) {
    if (e.readyState == EventSource.CLOSED) {
        console.log("EventSource closed:", e);
    }
}
