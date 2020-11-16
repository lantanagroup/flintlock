package com.lantanagroup.nandina.send;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.NandinaConfig;
import com.lantanagroup.nandina.QueryReport;

public interface IReportSender {
  void send(QueryReport report, NandinaConfig config, FhirContext ctx) throws Exception;
}