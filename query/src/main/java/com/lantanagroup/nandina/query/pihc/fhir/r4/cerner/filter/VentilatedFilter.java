package com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter;

import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.PatientData;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Procedure;

import java.util.Set;

public final class VentilatedFilter extends Filter {

  @Override
  public boolean runFilter(PatientData pd) {
    boolean b = false;
    if (hasVentilationProcedure(pd, terminology.getValueSetAsSetString("intubation-procedures"))) {
      b = true;
    }
    ;
    return b;
  }

  private boolean hasVentilationProcedure(PatientData pd, Set<String> valueSetAsSetString) {
    boolean b = false;
    for (IBaseResource res : bundleToSet(pd.getProcedures())) {
      Procedure p = (Procedure) res;
      CodeableConcept cc = p.getCode();
      b = codeInSet(cc, valueSetAsSetString);
      if (b) {
        break;
      }
    }
    return b;
  }

}
