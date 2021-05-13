package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.QueryReport;
import com.lantanagroup.link.config.api.ApiConfig;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.IOException;

public interface IReportDownloader {
  void download(QueryReport report, HttpServletResponse response, FhirContext ctx, ApiConfig config) throws IOException, TransformerException;
}
