package com.lantanagroup.nandina.query.fhir.r4.cerner;


import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class PatientData {

  protected static final Logger logger = LoggerFactory.getLogger(PatientData.class);
  protected static FhirContext ctx;
  protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  protected Date dateCollected;
  protected Patient patient;
  protected Encounter primaryEncounter;
  protected Bundle encounters;
  protected Bundle conditions;
  protected Bundle meds;
  protected Bundle labResults;
  protected Bundle allergies;
  protected Bundle procedures;
  protected CodeableConcept primaryDx = null;

  public PatientData(EncounterScoop scoop, Patient pat, FhirContext fhirContext) {
    patient = pat;
    ctx = fhirContext;
    dateCollected = new Date();
    Map<Patient, Encounter> patEncMap = scoop.getPatientEncounterMap();
    primaryEncounter = patEncMap.get(patient);
    encounters = scoop.rawSearch("Encounter?patient=Patient/" + pat.getIdElement().getIdPart());
    conditions = scoop.rawSearch("Condition?patient=Patient/" + pat.getIdElement().getIdPart());
    meds = scoop.rawSearch("MedicationRequest?patient=Patient/" + pat.getIdElement().getIdPart());
    labResults = scoop.rawSearch("Observation?patient=Patient/" + pat.getIdElement().getIdPart() + "&category=http://terminology.hl7.org/CodeSystem/observation-category|laboratory");
    allergies = scoop.rawSearch("AllergyIntolerance?patient=Patient/" + pat.getIdElement().getIdPart());
    procedures = scoop.rawSearch("Procedure?patient=Patient/" + pat.getIdElement().getIdPart());
  }


  public Bundle getBundle() {
    Bundle b = new Bundle();
    b.setType(BundleType.COLLECTION);
    b.addEntry().setResource(patient);
    b.addEntry().setResource(encounters);
    b.addEntry().setResource(conditions);
    b.addEntry().setResource(meds);
    b.addEntry().setResource(labResults);
    b.addEntry().setResource(allergies);
    return b;
  }
  
  public String getBundleXml() {
	  return ctx.newXmlParser().encodeResourceToString(getBundle());
  }


  public CodeableConcept getPrimaryDx() {
    return primaryDx;
  }


  /**
   * The setPrimaryDx() method is the only setter here,
   * since the primary diagnosis is not known until filters are run.
   * If not for that, this class could be a read-only final class.
   *
   * @param primaryDx
   */
  public void setPrimaryDx(CodeableConcept primaryDx) {
    this.primaryDx = primaryDx;
  }


  public Date getDateCollected() {
    return dateCollected;
  }


  public Patient getPatient() {
    return patient;
  }


  public Encounter getPrimaryEncounter() {
    return primaryEncounter;
  }


  public Bundle getEncounters() {
    return encounters;
  }


  public Bundle getConditions() {
    return conditions;
  }


  public Bundle getMeds() {
    return meds;
  }


  public Bundle getLabResults() {
    return labResults;
  }


  public Bundle getAllergies() {
    return allergies;
  }


  public Bundle getProcedures() {
    return procedures;
  }

}