package com.lantanagroup.nandina.query.fhir.r4.saner;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.nandina.IConfig;
import com.lantanagroup.nandina.query.IQueryCountExecutor;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.Map;

public class MechanicalVentilatorsQuery extends AbstractSanerQuery implements IQueryCountExecutor {
    public MechanicalVentilatorsQuery(IConfig config, IGenericClient fhirClient, HashMap<String, String> criteria) {
        super(config, fhirClient, criteria);
        // TODO Auto-generated constructor stub
    }

    @Override
    public Integer execute() {
        Map<String, Resource> data = this.getData();
        return this.countForPopulation(data, "Ventilators", "numVent");
    }
}