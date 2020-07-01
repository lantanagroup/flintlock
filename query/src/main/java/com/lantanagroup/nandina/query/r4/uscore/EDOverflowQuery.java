package com.lantanagroup.nandina.query.r4.uscore;

import com.lantanagroup.nandina.query.IQueryCountExecutor;
import com.lantanagroup.nandina.query.BaseQuery;
import org.hl7.fhir.r4.model.Resource;

import java.util.HashMap;
import java.util.Map;

public class EDOverflowQuery extends BaseQuery implements IQueryCountExecutor {
  @Override
  public Integer execute() {
    if (!this.criteria.containsKey("reportDate") && !this.criteria.containsKey("overflowLocations")) {
      return null;
    }

    Map<String, Resource> resMap = this.getData();
    return this.getCount(resMap);
  }


  /**
   * Queries for Patient resources where
   * - the Patient is in referenced in Condition.patient and Condition.code is a code from the Covid19 value set
   * - the Patient is referenced in Encounter.patient and Encounter.location matches overflowLocations
   * The result is then further filtered to just those where the Encounter.date is equal to the reportDate
   * (Encounter.date search parameter is not working properly, so this is done procedurally)
   */
  @Override
  protected Map<String, Resource> queryForData() {
    try {
      String reportDate = this.criteria.get("reportDate");
      String overflowLocations = this.criteria.get("overflowLocations");
      String url = String.format("Patient?_has:Condition:patient:code=%s&_has:Encounter:patient:location=%s:date=ge%s,le%s",
              this.getProperties().getTerminologyCovidCodes(),
              overflowLocations,
              reportDate,
              reportDate);
      Map<String, Resource> patientMap = this.search(url);
      // Encounter.date search parameter not working with current release of HAPI, so
      // weeding out encounters outside the reportDate manually
      HashMap<String, Resource> finalPatientMap = filterPatientsByEncounterDate(reportDate, patientMap);

      this.addContextData("edOverflow", this);

      return finalPatientMap;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}