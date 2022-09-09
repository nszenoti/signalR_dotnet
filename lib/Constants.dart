class CallbackMethod {
  static const String connectionStatus = 'connectionStatus';
  static const String onEvent = 'onEvent';
}

class InvokeMethod {
  static const String createAndConnect = 'createAndConnect';
  static const String connect = 'connect';
  static const String disconnect = 'disconnect';
  static const String subscribeToHub = 'subscribeToHub';
  static const String invokeHubMethod = 'invokeHubMethod';
  static const String dispose = 'dispose';

  static const String onEvent = 'onEvent';
}

class HubArgument {
  static const String hubToken = 'hubToken';
  static const String signalRURL = 'signalRURL';
  static const String hubName = 'hubName';
  static const String hubObservers = 'hubObservers';

  static const String methodName = 'methodName';
  static const String args = 'args';
}
