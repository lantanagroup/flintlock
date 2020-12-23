package com.lantanagroup.nandina.query.filter;

import com.lantanagroup.nandina.query.PatientData;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.Patient;

import java.util.Calendar;
import java.util.Date;

public final class DeathFilter extends Filter {

  Date reportDate;

  public DeathFilter(Date reportDate) {
    super();
    this.reportDate = reportDate;
  }

  @Override
  public boolean runFilter(PatientData pd) {
    boolean dead = false;

    Patient p = pd.getPatient();
    logger.debug("Checking if " + p.getId() + " died");
    if (p.hasDeceasedDateTimeType()) {
      Calendar deathDate = p.getDeceasedDateTimeType().toCalendar();
      if (DateUtils.isSameDay(deathDate.getTime(), reportDate)) {
        dead = true;
      }
    }
    return dead;
  }
}
