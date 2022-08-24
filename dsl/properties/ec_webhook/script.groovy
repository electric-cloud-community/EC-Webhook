import groovy.json.JsonOutput

def trigger = args.trigger
Map<String, String> headers = args.headers
String method = args.method
String body = args.body



// Receiving trigger parameters
def paramsPropertySheet = trigger.pluginParameters
Map<String, String> pluginParameters = [:]
paramsPropertySheet['properties'].each { String k, Map<String, String> v ->
    pluginParameters[k] = v['value']
}

if (pluginParameters.get("alwaysRun") == 'true'){
    return (Map<String, Object>) [
        launchWebhook  : true,
        responseMessage: "Running the trigger (Always Run is set to 'true')"
    ]
}


// Building closure
String closureText = pluginParameters.get("checkClosure")

Closure closure
try {
    closure = (Closure) Eval.me(closureText)
} catch (Throwable ex) {
    return (Map<String, Object>) [
        launchWebhook  : false,
        responseMessage: (String) "Failed to load checkClosure: ${ex.getMessage()},\n closure text: ${closureText}"
    ]
}

// Running the closure check
try {
    def checkResult = closure.call(headers, body)

    if (!checkResult){
        return (Map<String, Object>) [
            launchWebhook  : false,
            responseMessage: (String) "Webhook check has not passed, trigger will not be launched"
        ]
    }
    else if (checkResult instanceof Boolean){
        return (Map<String, Object>) [
            launchWebhook  : checkResult,
            responseMessage: (String) (checkResult == true)
                                ? "Launching the trigger"
                                : "Skipping trigger execution"
        ]
    }
    else if (checkResult instanceof Map){
        return (Map<String, Object>) checkResult
    }
    else{
        throw new RuntimeException("Check closure should return one of: Boolean, Map")
    }
} catch (RuntimeException ex){
    return (Map<String, Object>) [
        launchWebhook  : false,
        responseMessage: (String) ex.getMessage()
    ]

}
