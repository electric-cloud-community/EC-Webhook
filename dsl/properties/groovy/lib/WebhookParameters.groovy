
// DO NOT EDIT THIS BLOCK BELOW=== Parameters starts ===
// PLEASE DO NOT EDIT THIS FILE

import com.cloudbees.flowpdf.StepParameters

class WebhookParameters {
    /**
    * Label: Trigger Id, type: entry
    */
    String ec_trigger

    static WebhookParameters initParameters(StepParameters sp) {
        WebhookParameters parameters = new WebhookParameters()

        def ec_trigger = sp.getRequiredParameter('ec_trigger').value
        parameters.ec_trigger = ec_trigger

        return parameters
    }
}
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== Parameters ends, checksum: a9715d529b3a28a0c9b4827d891fe616 ===