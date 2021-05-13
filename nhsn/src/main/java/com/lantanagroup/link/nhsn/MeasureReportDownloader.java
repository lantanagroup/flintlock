package com.lantanagroup.link.nhsn;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.link.IReportDownloader;
import com.lantanagroup.link.QueryReport;
import com.lantanagroup.link.FhirHelper;
import com.lantanagroup.link.config.api.ApiConfig;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MeasureReportDownloader implements IReportDownloader {
  protected static final Logger logger = LoggerFactory.getLogger(MeasureReportDownloader.class);

  @Override
  public void download(QueryReport report, HttpServletResponse response, FhirContext ctx, ApiConfig config) throws IOException, TransformerException {
    String measureReportJson = (String) report.getAnswer("measureReport");
    MeasureReport measureReport = (MeasureReport) ctx.newJsonParser().parseResource(measureReportJson);

    logger.info("Building Bundle for MeasureReport...");

    IGenericClient fhirServerStore = ctx.newRestfulGenericClient(config.getFhirServerStore());
    Bundle bundle = FhirHelper.bundleMeasureReport(measureReport, fhirServerStore, ctx, config.getFhirServerStore());

    logger.info("Bundle created for MeasureReport including " + bundle.getEntry().size() + " entries");

    String responseBody = ctx.newXmlParser().encodeResourceToString(bundle);
    response.setContentType("application/xml");
    response.setHeader("Content-Disposition", "attachment; filename=\"report.xml\"");

    InputStream is = new ByteArrayInputStream(responseBody.getBytes());
    IOUtils.copy(is, response.getOutputStream());
    response.flushBuffer();
  }
}
