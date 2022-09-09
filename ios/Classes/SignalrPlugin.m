#import "SignalrPlugin.h"
#if __has_include(<signalr/signalr-Swift.h>)
#import <signalr/signalr-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "signalr-Swift.h"
#endif

@implementation SignalrPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftSignalrPlugin registerWithRegistrar:registrar];
}
@end
