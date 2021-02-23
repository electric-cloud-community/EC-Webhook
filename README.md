# EC-Webhook

The plugin provides a generic way for CloudBees CD to respond to webhooks.

## Instruction
1. Install and promote the plugin
2. Create a new trigger for a procedure or pipeline
3. Select EC-Webhook as the Plugin
4. Add a service account if needed
5. Copy the URL
6. Configure the third-party tool with this URL and any desired payload

When the procedure or pipeline is triggered from the webhook, the procedure or pipeline will be run
and the following properties will be added:
* webhookData
* webhookHeaders
These are JSON strings which can be parsed using the JSON JavaScript library:
* `$[/javascript JSON.parse(myJob.webhookData)["fieldName"]]`
* `$[/javascript JSON.parse(myPipelineRuntime.webhookData)["fieldName"]]`

Where "fieldName" is the name of a key in the JSON.

