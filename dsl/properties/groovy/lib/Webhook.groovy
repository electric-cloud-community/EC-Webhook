import com.cloudbees.flowpdf.*

/**
* Webhook
*/
class Webhook extends FlowPlugin {

    @Override
    Map<String, Object> pluginInfo() {
        return [
                pluginName     : '@PLUGIN_KEY@',
                pluginVersion  : '@PLUGIN_VERSION@',
                configFields   : ['config'],
                configLocations: ['ec_plugin_cfgs'],
                defaultConfigValues: [:]
        ]
    }
// === check connection ends ===
/**
    * webhook - webhook/webhook
    * Add your code into this method and it will be called when the step runs
    * @param signature (required: false)
    
    */
    def webhook(StepParameters p, StepResult sr) {
        // Use this parameters wrapper for convenient access to your parameters
        WebhookParameters sp = WebhookParameters.initParameters(p)

        // Calling logger:
        log.info p.asMap.get('signature')
        

        // Setting job step summary to the config name
        sr.setJobStepSummary(p.getParameter('config').getValue() ?: 'null')

        sr.setReportUrl("Sample Report", 'https://cloudbees.com')
        sr.apply()
        log.info("step webhook has been finished")
    }

/**
    * setupWebhook - SetupWebhook/SetupWebhook
    * Add your code into this method and it will be called when the step runs
    * @param ec_trigger (required: true)
    
    */
    def setupWebhook(StepParameters p, StepResult sr) {
        // Use this parameters wrapper for convenient access to your parameters
        SetupWebhookParameters sp = SetupWebhookParameters.initParameters(p)

        // Calling logger:
        log.info p.asMap.get('ec_trigger')
        

        // Setting job step summary to the config name
        sr.setJobStepSummary(p.getParameter('config').getValue() ?: 'null')

        sr.setReportUrl("Sample Report", 'https://cloudbees.com')
        sr.apply()
        log.info("step SetupWebhook has been finished")
    }

// === step ends ===

}