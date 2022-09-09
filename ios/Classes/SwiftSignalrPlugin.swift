import Flutter
import UIKit

public class SwiftSignalrPlugin: NSObject, FlutterPlugin {
    static var channel: FlutterMethodChannel?
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "com.zenoti.mpos/signalr", binaryMessenger: registrar.messenger())
        let instance = SwiftSignalrPlugin()
        SwiftSignalrPlugin.channel = channel
        SignalRConnectionManager.shared().channel = channel
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case InvokeMethod.createAndConnect:
            SignalRConnectionManager.shared().channel = SwiftSignalrPlugin.channel
            if let args = call.arguments as? [String: Any] {
                SignalRConnectionManager.shared().signalRToken = args[HubArgument.hubToken] as? String
                SignalRConnectionManager.shared().signalRURL = args[HubArgument.signalRURL] as? String
                SignalRConnectionManager.shared().hubName = args[HubArgument.hubName] as? String
                if let observers = args[HubArgument.hubObservers] as? [String] {
                    SignalRConnectionManager.shared().hubObservers = observers
                }
                SignalRConnectionManager.shared().createSignalRConnection(result: result)
            }
            
        case InvokeMethod.connect:
            SignalRConnectionManager.shared().start(result: result)
            
        case InvokeMethod.disconnect:
            SignalRConnectionManager.shared().disconnect(result: result)
            
        case InvokeMethod.dispose:
            SignalRConnectionManager.shared().disconnect(result: result)
            SignalRConnectionManager.reset()

        case InvokeMethod.subscribeToHub:
            if let args = call.arguments as? [String: Any],
               let method = args[HubArgument.methodName] as? String,
               !method.isEmpty {
                SignalRConnectionManager.shared().subscribe(method: method, result: result)
            }
            
        case InvokeMethod.invokeHubMethod:
            if let args = call.arguments as? [String: Any],
               let method = args[HubArgument.methodName] as? String,
               !method.isEmpty {
                let invokeArgs = args[HubArgument.args]
                SignalRConnectionManager.shared().invoke(method: method, arguments: invokeArgs, result: result)
            }
        default:
            result(FlutterMethodNotImplemented)
        }
    }
}

class HubArgument {
    static let hubToken: String = "hubToken";
    static let signalRURL: String = "signalRURL";
    static let hubName: String = "hubName";
    static let hubObservers: String = "hubObservers";
    
    static let methodName: String = "methodName";
    static let args: String = "args";
    
    static let status: String = "status";
    static let message: String = "message";
}

class InvokeMethod {
    static let createAndConnect: String = "createAndConnect";
    static let connect: String = "connect";
    static let disconnect: String = "disconnect";
    static let subscribeToHub: String = "subscribeToHub";
    static let invokeHubMethod: String = "invokeHubMethod";
    static let dispose: String = "dispose";
}

class CallbackMethod {
    static let connectionStatus: String = "connectionStatus";
    static let onEvent: String = "onEvent";
}
