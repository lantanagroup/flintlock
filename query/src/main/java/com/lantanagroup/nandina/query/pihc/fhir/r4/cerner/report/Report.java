package com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.report;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.PatientData;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter.EncounterDateFilter;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.pihc.fhir.r4.cerner.scoop.EncounterScoop;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Report {

	protected static final Logger logger = LoggerFactory.getLogger(Report.class);
	protected List<PatientData> patientData;
	private FhirContext ctx;

	public Report(EncounterScoop scoop, FhirContext ctx) {
		this.ctx = ctx;
		initReport(scoop, null);
	}

	public Report(EncounterScoop scoop, List<Filter> filters, FhirContext ctx) {
		this.ctx = ctx;
		initReport(scoop, filters);
	}

	private void initReport(EncounterScoop scoop, List<Filter> filters) {
		if (scoop == null) return;
		if (filters == null) filters = new ArrayList<Filter>();
		if (scoop.getReportDate() != null) {
			// TODO: Need to create date filters for Condition, Procedure, etc.
			// and add them here in addition to the EncounterDateFilter
			// or maybe create an "EverythingDate" filter that checks all that stuff.
			EncounterDateFilter edf = new EncounterDateFilter(scoop.getReportDate());
			filters.add(edf);
		}

		this.patientData = new ArrayList<>();

		if (scoop.getPatientData() != null) {
			for (PatientData pd : scoop.getPatientData()) {
				logger.info("Checking patient " + pd.getPatient().getId());

				if (filters.size() == 0) {
					patientData.add(pd);
				} else {
					// this calls the runFilter() method on each of the filters and if they "allMatch" true then result is
					// true. They all have to return true for the result to be true.
					boolean result = filters.parallelStream().allMatch(f -> f.runFilter(pd) == true);

					if (result) {
						patientData.add(pd);
					} else {
						logger.info(pd.getPatient().getId() + " did not pass all filters ");
						//logger.info(pd.getBundleXml());
					}
				}
			}
		}
	}

	/**
	 * @return a byte array with the report data. By default it is a FHIR Bundle of
	 *         the report data after filtering. Override if you want something else
	 *         like a Zip file containing CSVs
	 * @throws IOException
	 */
	public byte[] getReportData() throws IOException {
		Bundle b = getReportBundle();
		String bundleStr = this.ctx.newXmlParser().encodeResourceToString(b);
		return bundleStr.getBytes();
	}

	/**
	 * @return a FHIR Bundle resource containing the PatientData resources that are
	 *         part of this report after every Filter has been run
	 */
	public Bundle getReportBundle() {
		Bundle b = new Bundle();
		b.setType(BundleType.COLLECTION);
		for (PatientData pd : patientData) {
			b.addEntry().setResource(pd.getBundle());
		}
		return b;
	}

	public int getReportCount() {
		return patientData.size();
	}

	public List<PatientData> getPatientData() {
		return patientData;
	}
}
