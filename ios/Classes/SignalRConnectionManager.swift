
import Foundation

class SignalRConnectionManager: NSObject {
    // MARK: - Init Methods -
    
    private static var privateShared: SignalRConnectionManager?
    
    final class func shared() -> SignalRConnectionManager {
        guard let _shared = privateShared else {
            let newInstance = SignalRConnectionManager()
            privateShared = newInstance
            return newInstance
        }
        return _shared
    }
    
    class func reset() {
        privateShared?.mirroringHub?.removeHandlers()
        privateShared?.mirroringHub = nil
        privateShared = nil
        print("reset \(String(describing: self))")
    }
    
    override private init() {
        super.init()
        print("init \(String(describing: self))")
    }
    
    deinit {
        print("deinit \(String(describing: self))")
    }
    
    // MARK: - Instance variables -
    
    var mirroringHub: Hub?
    var mirrorModeConnection: SignalR?
    var hubObservers: [String] = []
    // var name: String!
    var channel: FlutterMethodChannel?
    
    var signalRToken: String?
    var signalRURL: String?
    var hubName: String?
    
    func createSignalRConnection(result: @escaping FlutterResult) {
        guard let accessToken = signalRToken,
              !accessToken.isEmpty,
              let baseUrl = signalRURL,
              !baseUrl.isEmpty,
              let hubNameStr = hubName,
              !hubNameStr.isEmpty
        else {
            result(false)
            return
        }
        
        mirrorModeConnection = SignalR(baseUrl)
        mirroringHub = Hub(hubNameStr)
        
        guard let mirrorConnection = mirrorModeConnection,
              let _mirroringHub = mirroringHub
        else {
            result(false)
            return
        }
        
        // Set basic details
        mirrorConnection.queryString = ["hubToken": "\(accessToken)"]
        mirrorConnection.signalRVersion = .v2_2_0
        mirrorConnection.transport = .webSockets
        
        // Observe for various notifications
        let addedHubObservers = self.addHubObservers()
        mirrorConnection.addHub(_mirroringHub)
        
        // Look for various connection states
        mirrorConnection.received = { [weak self] (obj: Any?) in
            print("mirrorConnection received object : \(obj ?? "NA")")
            let data: [String: Any] = [HubArgument.status: "received", HubArgument.args: obj ?? [:]]
            self?.channel?.invokeMethod(CallbackMethod.connectionStatus, arguments: data)
        }
        
        mirrorConnection.starting = { [weak self] in
            print("mirrorConnection starting")
            let data: [String: Any] = [HubArgument.status: "starting"]
            self?.channel?.invokeMethod(CallbackMethod.connectionStatus, arguments: data)
        }
        mirrorConnection.reconnecting = { [weak self] in
            print("mirrorConnection reconnecting")
            let data: [String: Any] = [HubArgument.status: "reconnecting"]
            self?.channel?.invokeMethod(CallbackMethod.connectionStatus, arguments: data)
        }
        mirrorConnection.reconnected = { [weak self] in
            print("mirrorConnection reconnected")
            let data: [String: Any] = [HubArgument.status: "reconnected"]
            self?.channel?.invokeMethod(CallbackMethod.connectionStatus, arguments: data)
        }
        mirrorConnection.connectionSlow = { [weak self] in
            print("mirrorConnection connectionSlow")
            let data: [String: Any] = [HubArgument.status: "connectionSlow"]
            self?.channel?.invokeMethod(CallbackMethod.connectionStatus, arguments: data)
        }
        mirrorConnection.connectionFailed = { [weak self] in
            print("mirrorConnection connectionFailed")
            let data: [String: Any] = [HubArgument.status: "connectionFailed"]
            self?.channel?.invokeMethod(CallbackMethod.connectionStatus, arguments: data)
        }
        
        mirrorConnection.connected = { [weak self] in
            print("connected: \(mirrorConnection.connectionID ?? "NA")")
            let data: [String: Any] = [HubArgument.status: "connected",HubArgument.args: ["connectionID":mirrorConnection.connectionID]]
            self?.channel?.invokeMethod(CallbackMethod.connectionStatus, arguments: data)
        }
        
        mirrorConnection.disconnected = { [weak self] in
            print("mirrorConnection disconnected")
            let data: [String: Any] = [HubArgument.status: "disconnected"]
            self?.channel?.invokeMethod(CallbackMethod.connectionStatus, arguments: data)
        }
        
        mirrorConnection.error = { [weak self] (error: [String: Any]?) in
            print("Mirroring - Connection Error Info.. \(error ?? [:])")
            let data: [String: Any] = [HubArgument.status: "error", HubArgument.args: error ?? [:]]
            self?.channel?.invokeMethod(CallbackMethod.connectionStatus, arguments: data)
        }
        
        // Start the connection
        mirrorConnection.start()
        result(addedHubObservers)
    }

    func addHubObservers() -> Bool {
        
        if let tempHub = self.mirroringHub {
            
            for method in self.hubObservers {
                tempHub.on(method) { (args) in
                    print("Received: \(method)\n Data:\(String(describing: args))")
                    var data: [String: Any] = [HubArgument.methodName: method]
                    if let arguments = args?.first {
                        data[HubArgument.args] = arguments
                    }
                    self.channel?.invokeMethod(CallbackMethod.onEvent, arguments: data)
                    
                }
            }
            return true
        } else {
            return false
        }
    }
    
    func subscribe(method: String, result: @escaping FlutterResult) {
        if let tempMirroringHub = mirroringHub {
            tempMirroringHub.on(method) { args in
                print("Received: \(method)")
                if let arguments = args {
                    let data: [String: Any] = [HubArgument.methodName: method, HubArgument.args: arguments]
                    self.channel?.invokeMethod(CallbackMethod.onEvent, arguments: data)
                }
            }
            result(true)
        } else {
            result(false)
        }
    }
    
    func invoke(method: String, arguments: Any? = nil, result: @escaping FlutterResult) {
        if let tempMirroringHub = mirroringHub {
            do {
                var args: [Any] = []
                if method == "JoinOrganization" {
                    args = arguments as? [Any] ?? []
                } else if let tempArguments = arguments {
                    args = [tempArguments]
                }
                try tempMirroringHub.invoke(method, arguments: args, callback: { success, error in
                    if let e = error {
                        print("Error: \(e)")
                        var msg = "An unknown error has occurred."
                        if let tempErr = e as? [String: Any],
                           let tempMsg = tempErr["message"] as? String {
                            msg = tempMsg
                        }
                        let data: [String: Any] = [HubArgument.status: "false", HubArgument.message: msg]
                        result(data)
                    } else {
                        print("Success!")
                        let data: [String: Any] = [HubArgument.status: "true"]
                        result(data)
                    }
                })
                
            } catch {
                print("Error: \(error)")
                let data: [String: Any] = [HubArgument.status: "false", HubArgument.message: error.localizedDescription]
                result(data)
            }
        } else {
            let data: [String: Any] = [HubArgument.status: "false", HubArgument.message: "No hub available to invoke"]
            result(data)
        }
    }
    
    func start(result: @escaping FlutterResult) {
        if let mirrorConnection = mirrorModeConnection {
            if mirrorConnection.state == .disconnected {
                mirrorConnection.start()
            }
        } else {
            let error = FlutterError(code: "1999", message: "No connection available to start", details: nil)
            result(error)
        }
    }
    
    // func restartConnection(result: @escaping FlutterResult) {
    //     if let mirrorConnection = mirrorModeConnection?.stop() {
    //         mirroringHub?.removeHandlers()
    //         mirroringHub = nil
    //         mirrorModeConnection = nil
    //         startConnection(result)
    //     } else {
    //         let error = FlutterError(code: "1999", message: "No connection available to start", details: nil)
    //         result(error)
    //     }
    // }
    
    /// Method to stop connection from SignalR
    func disconnect(result: @escaping FlutterResult) {
        if let mirrorConnection = mirrorModeConnection {
            mirrorConnection.stop()
        } else {
            let error = FlutterError(code: "1999", message: "No connection available to disconnect", details: nil)
            result(error)
        }
    }
}
