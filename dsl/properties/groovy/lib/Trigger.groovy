import com.cloudbees.flowpdf.FWLog
import com.cloudbees.flowpdf.FlowAPI
import com.electriccloud.client.groovy.models.Filter
import com.electriccloud.client.groovy.models.Select
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class Trigger {

    final String triggerId
    final String triggerName
    final String triggerType
    final boolean triggerEnabled
    long quietTimeMinutes
    final String webhookUrl

    // Container
    final Map<String, String> containerOpts

    final String projectName
    final String pluginKey
    final Map<String, String> pluginParameters

    private ArrayList<TriggerErrorDetail> errors
    private TriggerMetadata state

    Trigger(Map triggerProperties) {
        this.projectName = triggerProperties['projectName']
        this.pluginKey = triggerProperties['pluginKey']

        this.triggerId = triggerProperties['triggerId']
        this.triggerName = triggerProperties['triggerName']
        this.triggerType = triggerProperties['triggerType']
        this.triggerEnabled = triggerProperties['triggerEnabled']
        this.quietTimeMinutes = Long.valueOf(triggerProperties['quietTimeMinutes'])
        this.webhookUrl = triggerProperties['webhookUrl']

        this.containerOpts = [
            pipelineName   : triggerProperties['pipelineName'],
            procedureName  : triggerProperties['procedureName'],
            releaseName    : triggerProperties['releaseName'],
            catalogName    : triggerProperties['catalogName'],
            catalogItemName: triggerProperties['catalogItemName'],
            applicationName: triggerProperties['applicationName'],
            processName    : triggerProperties['processName'],
        ] as Map<String, String>

        this.pluginParameters = triggerProperties['pluginParameters'] as Map<String, String>

        if (triggerProperties['stateValues']) {
            try {
                TriggerMetadata state = null
                if (triggerType == 'polling') {
                    state = new PollingTriggerMetadata(
                        this, triggerProperties['stateValues'] as Map<String, String>
                    )
                } else {
                    state = new TriggerMetadata(
                        this, triggerProperties['stateValues'] as Map<String, String>
                    )
                }
                this.state = state
            } catch (RuntimeException ex) {
                FWLog.logDebug(
                    "Failed to create metadata from findObjects response: " + ex.getMessage()
                )
            }
        }
    }

    private TriggerMetadata readStateFromProperty() {
        String statePropertyPath = buildTriggerMetadataPath()

        String values

        try {
            values = FlowAPI.getFlowProperty(statePropertyPath)
            FWLog.logDebug("Trigger metadata values: " + values)
        } catch (RuntimeException ex) {
            FWLog.logDebug("Failed to receive metadata: " + ex.getMessage())
            return null
        }

        FWLog.logDebug("Metadata value", values)
        if (values == null || values == '') {
            FWLog.logInfo("Metadata is empty.")
            return null
        }

        TriggerMetadata md
        try {
            if (triggerType == 'polling') {
                md = new PollingTriggerMetadata(this, values)
            } else  {
                md = new TriggerMetadata(this, values)
            }
        } catch (RuntimeException ex) {
            FWLog.logWarnDiag(
                "Can't build metadata from property value: " + ex.getMessage(),
                "Value: '${values.toString()}'"
            )
            return null
        }

        return md
    }

    TriggerMetadata getState() {
        if (state == null) {
            state = readStateFromProperty()
        }
        return state
    }

    void clearErrors() {
        ArrayList<TriggerErrorDetail> errors = this.getErrors()

        FWLog.logDebug("ERRORS TO CLEAR: " + errors.collect({ it -> it.id }).join(', '))

        errors.each {
            String id = it.getId()
            try {
                def resp = FlowAPI.getEc().deleteTriggerErrorDetail(
                    triggerErrorDetailId: id
                )
                FWLog.logDebug("Error delete response: " + resp)
            }
            catch (RuntimeException ex) {
                FWLog.logWarnDiag("Failed to remove TriggerErrorDetail '${id}': " + ex.getMessage())
            }
        }
    }

    ArrayList<TriggerErrorDetail> getErrors() {
        if (errors != null) {
            return errors
        }

        def response = null
        try {
            response = FlowAPI.getEc().getTriggerErrorDetails(
                path: buildTriggerPath()
            )
        } catch (RuntimeException ex) {
            FWLog.logErrorDiag("Failed to receive TriggerErrorDetails: " + ex.getMessage())
        }

        FWLog.logTrace("TriggerErrorDetails response: " + response.toString())

        this.errors = new ArrayList<String>()
        if (response != null) {
            response['triggerErrorDetail'].each({ it ->
                this.errors.push(new TriggerErrorDetail(
                    id: it['triggerErrorDetailId'],
                    // We will not use this fields, so why save them?
                    // errorMessage: it['errorMessage'],
                    // webhookHeaders: it['webhookHeaders'],
                    // webhookPayload: it['webhookPayload']
                ))
            })
        }

        return this.errors
    }

    private String buildTriggerPath() {
        String path = "/projects/${projectName}"

        def opts = containerOpts

        if (opts.pipelineName != null) {
            path += "/pipelines/${opts.pipelineName}"
        } else if (opts.procedureName != null) {
            path += "/procedures/${opts.procedureName}"
        } else if (opts.releaseName != null) {
            path += "/releases/${opts.releaseName}"
        } else if (opts.catalogName != null) {
            path += "/catalogs/${opts.catalogName}/catalogItems/${opts.catalogItemName}"
        } else if (opts.applicationName != null) {
//            path += "/applications/${opts.applicationName}/processes/${opts.processName}"
            path += "/applications/${opts.applicationName}"
        } else {
            throw new RuntimeException("Can't build trigger path for container opts: " + opts)
        }

        return path + "/triggers/${triggerName}"
    }

    private String buildTriggerMetadataPath() {
        String triggerPath = buildTriggerPath()
        String triggerMetadataPath = triggerPath + '/ec_trigger_state/triggerState'

        FWLog.logTrace("State path: ${triggerMetadataPath}")
        return triggerMetadataPath
    }

    void run() {
        FWLog.logDebug("Running trigger: '${triggerName}'")

        try {
            def resp = FlowAPI.getEc().runTrigger(
                path: buildTriggerPath()
//                webhookData   : '',
//                webhookHeaders: ''
            )
            FWLog.logInfoDiag("Run trigger '${triggerName}' response: ${resp.toString()}")
        } catch (RuntimeException ex) {
            FWLog.logWarnDiag("Failed to run the trigger: " + ex.getMessage())
            createError("Failed to run the trigger ${buildTriggerPath()}: " + ex.getMessage())
        }
    }

    PollingTriggerMetadata buildPollingTriggerMetadata(String timestamp, String commitSha) {
        return new PollingTriggerMetadata(this, [
            timestamp: timestamp,
            commitSha: commitSha,
            repoUrl  : pluginParameters.get("repoUrl"),
            branch   : pluginParameters.get("branch")
        ] as Map<String, String>
        )
    }

    TriggerMetadata buildTriggerMetadata(Map<String, String> values) {
        values['triggerId'] = this.triggerId
        return new TriggerMetadata(this, values)
    }


    void update(TriggerMetadata newState) {
        FWLog.logTrace("SAVING TRIGGER STATE: ${getTriggerName()}")

        String statePropertyPath = buildTriggerMetadataPath()

        // Set new state
        String newMetadata = newState.toJson()
        FlowAPI.setFlowProperty(statePropertyPath, newMetadata)

//        String newErrorsStr = JsonOutput.toJson(errors)
//        FlowAPI.setFlowProperty(statePropertyPath + '/errors', newErrorsStr)
        FWLog.logTrace("SAVED NEW TRIGGER STATE: ${newMetadata}")
    }

    static ArrayList<Trigger> getPluginTriggers(String triggerType, String triggerId = null) {
        Filter pluginFilter = new Filter('pluginKey', 'equals', '@PLUGIN_KEY@')

        ArrayList<Filter> filters = [pluginFilter]

        if (triggerType) {
            filters.push(new Filter('triggerType', 'equals', triggerType))
        }

        if (triggerId) {
            filters.push(new Filter('triggerId', 'equals', triggerId))
        }

        def triggersResponse = FlowAPI.ec.findObjects(
            objectType: 'trigger',
            filters: filters,
            selects: [new Select('ec_trigger_state/triggerState')],
            viewName: 'Details'
        )

        FWLog.logDebug("Triggers response:", JsonOutput.toJson(triggersResponse))

        ArrayList<Trigger> result = new ArrayList<>()

        for (Map<String, Object> obj : (triggersResponse['object'] as List<Map<String, Object>>)) {
            def triggerObject = obj['trigger']

            Map<String, String> plParams = [:]
            triggerObject['pluginParameters']['parameterDetail'].each {
                Map<String, String> paramPair ->
                    plParams[paramPair.parameterName] = paramPair.parameterValue ?: ''
            }

            // Too many handling for reading a property, so just try/catch
            Map<String, String> stateValues
            try {
                String metadataJson = obj?.'property'?.getAt('0')?.value
                FWLog.logDebug("findObjects metadata value: " + metadataJson)
                if (metadataJson){
                    stateValues = (new JsonSlurper()).parseText(metadataJson) as Map<String, String>
                }
            }
            catch (Throwable ex) {
                FWLog.logDebug(
                    "Can't create state from the findObjects response: ",
                    ex.getMessage()
                )
            }


            Trigger tr = new Trigger(
                projectName: triggerObject['projectName'],
                pluginKey: triggerObject['pluginKey'],

                triggerId: triggerObject['triggerId'],
                triggerName: triggerObject['triggerName'],
                triggerType: triggerObject['triggerType'],
                triggerEnabled: triggerObject['triggerEnabled'],

                pipelineName: triggerObject['pipelineName'],
                procedureName: triggerObject['procedureName'],
                releaseName: triggerObject['releaseName'],
                catalogName: triggerObject['catalogName'],
                catalogItemName: triggerObject['catalogItemName'],
                applicationName: triggerObject['applicationName'],
                processName: triggerObject['processName'],

                pluginParameters: plParams,
                stateValues: stateValues,

                quietTimeMinutes: triggerObject['quietTimeMinutes'],
                webhookUrl: triggerObject['webhookUrl']
            )

            result.push(tr)
        }

        return result
    }

    void createError(String errorMessage) {
        try {
            def errorDetail = FlowAPI.getEc().createTriggerErrorDetail(
                errorMessage: errorMessage,
                path: buildTriggerPath()
            )
            FWLog.logDebug("Created new error detail: ", errorDetail)
            getErrors().push(new TriggerErrorDetail(
                id: errorDetail['triggerErrorDetailId'],
                errorMessage: errorDetail['errorMessage']
            ))
        } catch (RuntimeException e) {
            FWLog.logWarnDiag("Failed to create TriggerErrorDetail: " + e.getMessage())
        }
    }

    boolean isSuppressedByQuietTime() {
        TriggerMetadata md = getState()

        if (!quietTimeMinutes || md == null) {
            return false
        }

        String mdTimestamp = md?.getValues()?.get('timestamp')

        if (mdTimestamp == null) {
            return false
        }

        long currentMillis = System.currentTimeMillis()
        long mdMillis = Long.valueOf(mdTimestamp)

        if (currentMillis - mdMillis < quietTimeMinutes * 60 * 1000) {
            return true
        }

        return true
    }

}

class PollingTriggerMetadata extends TriggerMetadata {

    PollingTriggerMetadata(Trigger tr, String metadataJson) throws RuntimeException {
        super(tr, metadataJson)
    }

    PollingTriggerMetadata(Trigger tr, Map<String, String> values) throws RuntimeException {
        super(tr, values)
    }


    @Override
    void validateStoredMetadata(Trigger tr, Map<String, String> values) throws RuntimeException {

        // Checking metadata is valid and full
        if (values['timestamp'] == null || values['commitSha'] == null) {
            throw new RuntimeException(
                "Metadata does not contain fields 'timestamp' and 'commitSha'."
                    + " Ignoring saved state."
            )
        }

        // Checking plugin parameters has not changed
        FWLog.logTrace("before repoUrl and branch check")

        if (values['repoUrl'] != tr.getPluginParameters()['repoUrl']
            || values['branch'] != tr.getPluginParameters()['branch']
        ) {
            throw new RuntimeException(
                "Values for parameters 'repoUrl' and 'branch' has been changed."
                    + " Ignoring saved state."
            )
        }
    }

    @Override
    String toJson() {
        def metadata = [
            timestamp: values['timestamp'],
            commitSha: values['commitSha'],
            repoUrl  : values['repoUrl'],
            branch   : values['branch']
        ]

        return JsonOutput.toJson(metadata)
    }

    @Override
    int compareTo(Object other) {
        if (!other instanceof TriggerMetadata) {
            throw new InvalidObjectException(
                "TriggerMetadata can only be compared" +
                    " to another TriggerMetadata instance")
        }

        String sha = values['commitSha']
        String otherSha = (other as TriggerMetadata).getValues()['commitSha']

        FWLog.logDebug("Comparing $sha and $otherSha")

        return (sha == otherSha) ? 0 : 1
    }
}

class TriggerMetadata implements Comparable {

    Trigger trigger
    Map<String, String> values

    TriggerMetadata(Trigger tr, String metadataJson) throws RuntimeException {
        // TODO: add invalid JSON exceptions
        Map<String, String> values = (Map<String, String>) (new JsonSlurper()).parseText(metadataJson)

        validateStoredMetadata(tr, values)
        this.trigger = tr
        this.values = values
    }

    TriggerMetadata(Trigger tr, Map<String, String> values) {
        validateStoredMetadata(tr, values)
        this.trigger = tr
        this.values = values
    }

    void validateStoredMetadata(Trigger tr, Map<String, String> values) {
        if (values['triggerId'] && tr.triggerId
            && values['triggerId'] != tr.triggerId) {
            throw new RuntimeException("Wrong trigger id in stored metadata.")
        }
    }

    String toJson() {
        return JsonOutput.toJson(values)
    }

    @Override
    int compareTo(Object o) {
        return 0
    }
}


class TriggerErrorDetail {
    String id
    String errorMessage

    def webHookData
    def webHookHeaders
}

