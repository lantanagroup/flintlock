package com.lantanagroup.nandina.query;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.Config;
import com.lantanagroup.nandina.Helper;
import com.lantanagroup.nandina.IConfig;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EDOverflowAndVentilatedQuery extends AbstractQuery implements IQueryCountExecutor {
	
    public EDOverflowAndVentilatedQuery(IConfig config, IGenericClient fhirClient) {
		super(config, fhirClient);
		// TODO Auto-generated constructor stub
	}

    @Override
    public Integer execute(String reportDate, String overflowLocations) {
        if (overflowLocations != null && !overflowLocations.isEmpty()) {
            if (Helper.isNullOrEmpty(config.getTerminologyCovidCodes())) {
                this.logger.error("Covid codes have not been specified in configuration. Cannot execute query.");
                return null;
            }

            if (Helper.isNullOrEmpty(config.getTerminologyDeviceTypeCodes())) {
                this.logger.error("Device-type codes have not been specified in configuration. Cannot execute query.");
                return null;
            }

            try {
                String url = String.format("Patient?_summary=true&_has:Condition:patient:code=%s&_has:Encounter:patient:location=%s&_has:Device:patient:type=%s",
                        Config.getInstance().getTerminologyCovidCodes(),
                        overflowLocations,
                        Config.getInstance().getTerminologyDeviceTypeCodes());
                Bundle edOverflowAndVentilated = fhirClient.search()
                        .byUrl(url)
                        .returnBundle(Bundle.class)
                        .execute();
                return edOverflowAndVentilated.getTotal();
            } catch (Exception ex) {
                this.logger.error("Could not retrieve ED/overflow count: " + ex.getMessage(), ex);
            }
        }

        return null;
    }
    
}
