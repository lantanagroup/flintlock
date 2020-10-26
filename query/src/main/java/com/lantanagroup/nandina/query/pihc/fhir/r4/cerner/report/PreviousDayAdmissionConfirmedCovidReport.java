package com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.report;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter.CovidFilter;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter.PreviousDayAdmissionConfirmedCovidFilter;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.scoop.EncounterScoop;

import java.util.Date;
import java.util.List;

public class PreviousDayAdmissionConfirmedCovidReport extends Report {

    public PreviousDayAdmissionConfirmedCovidReport(EncounterScoop scoop, List<Filter> filters, Date reportDate, FhirContext ctx) {
        super(scoop, addFilters(filters, reportDate), ctx);
    }

    private static List<Filter> addFilters(List<Filter> filters, Date reportDate) {
        Filter covidFilter = new CovidFilter();
        filters.add(covidFilter);
        Filter admissionFilter = new PreviousDayAdmissionConfirmedCovidFilter(reportDate);
        filters.add(admissionFilter);
        return filters;
    }
}