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
        sr.setJobStepOutcome('error')
        sr.setJobStepSummary("This procedure is not intended to be run.")
    }


// === step ends ===

}