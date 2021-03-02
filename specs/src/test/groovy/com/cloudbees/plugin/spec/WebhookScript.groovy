package com.cloudbees.plugin.spec

import com.electriccloud.spec.PluginSpockTestSupport

class WebhookScript extends PluginSpockTestSupport {

    static String procedureName = 'webhook'
    static String scriptPropertyPath = '/myProject/ec_webhook/default/script'

    static String webhookScriptContent = null
    static String triggerName = 'testTrigger'

    def setupSpec() {
        // Get the script
        def webhookScriptProperty = dsl("""
            getProperty(
              propertyName: '${scriptPropertyPath}',
              projectName: '/plugins/EC-Webhook/project'
            )
""")
        webhookScriptContent = webhookScriptProperty['property']['value']
        assert webhookScriptContent
    }

    def "Simple"(){
        when:
        String closure = """
            { h, b -> return true }
        """
        and:
        def result = processWebhook([checkClosure: closure])

        then:
        assert result['launchWebhook'] == true
    }

    def "Simple negative"(){
        when:
        String closure = """
            { h, b -> return false }
        """
        and:
        def result = processWebhook([checkClosure: closure])

        then:
        assert result['launchWebhook'] == false
    }

    def "Header check"(){
        when:
        String closure = """
            { h, b -> return h.get('X-Custom-Header') == 'kokoko' }
        """
        and:
        def result = processWebhook(
            [checkClosure: closure],
            ['X-Custom-Header':"kokoko"]
        )

        then:
        assert result['launchWebhook'] == true
    }

    def "Negative header check"(){
        when:
        String closure = """
            { h, b -> return h.get('X-Custom-Header') == 'kikiki' }
        """
        and:
        def result = processWebhook(
            [checkClosure: closure],
            ['X-Custom-Header':"kokoko"]
        )

        then:
        assert result['launchWebhook'] == false
    }

    def "Complex object return"(){
        when:
        String closure = """
            { h, b -> return [responseMessage: "Here comes the rooster", launchWebhook: true] }
        """
        and:
        def result = processWebhook([checkClosure: closure])

        then:
        assert result['responseMessage'] == "Here comes the rooster"
        assert result['launchWebhook'] == true
    }

    private def processWebhook(Map<String, String> triggerParameters, Map<String, String> headers = [:], String payload = "") {
        def trigger = buildTriggerDummyForParameters(triggerParameters)

        def scriptParameters = [
            trigger: trigger,
            headers: headers,
            method : 'POST',
            body   : payload,
        ]

        def result = Eval.me('args', scriptParameters, webhookScriptContent)

        return result
    }

    private Object buildTriggerDummyForParameters(Map<String, String> parameters) {
        def pluginParameters = [
            checkClosure    : [value: parameters.checkClosure],
        ]

        return [
            name            : triggerName,
            pluginParameters: [
                properties: pluginParameters
            ]
        ]
    }
}
