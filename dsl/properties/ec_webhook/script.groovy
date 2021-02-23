import groovy.json.JsonOutput

def trigger = args.trigger
Map<String, String> headers = args.headers
String method = args.method
String body = args.body

Map<String, Object> response = [
        launchWebhook  : true,
		webhookData : body,
		webhookHeader : JsonOutput.toJson(headers),
		webhookMethod: method
] as Map<String, Object>

return response