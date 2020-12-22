package com.lantanagroup.nandina;

import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.cli.*;
import org.apache.commons.codec.binary.Base64;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FixMeasureBundle {
  private static final Logger logger = LoggerFactory.getLogger(FixMeasureBundle.class);

  public static void main(String[] args) throws IOException, ParseException {
    Options options = new Options();
    CommandLineParser parser = new DefaultParser();

    // Add options for CLI
    options.addOption("path", true, "Path to the bundle XML file");
    options.addOption("id", true, "ID that the Measure in the bundle should be set to");
    options.addOption("vsacUsername", true, "Username for VSAC authentication");
    options.addOption("vsacPassword", true, "Password for VSAC authentication");

    CommandLine cmd = parser.parse(options, args);
    String measureBundle = cmd.getOptionValue("path");
    String measureId = cmd.getOptionValue("id");
    String vsacUsername = cmd.getOptionValue("vsacUsername");
    String vsacPassword = cmd.getOptionValue("vsacPassword");

    FhirContext ctx = FhirContext.forR4();
    Path measureBundlePath = Paths.get(measureBundle);
    Bundle bundle = (Bundle) ctx.newXmlParser().parseResource(Files.readString(measureBundlePath));

    bundle.setId("Bundle-" + measureId);

    List<ValueSet> valueSets = new ArrayList<>();

    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().getMeta() != null) {
        entry.getResource().setMeta(null);
      }

      if (entry.getResource() instanceof Measure) {
        Measure measure = (Measure) entry.getResource();
        measure.setId(measureId);
        entry.getRequest().setUrl("Measure/" + measureId);
      } else if (entry.getResource() instanceof Library) {
        Library library = (Library) entry.getResource();

        for (RelatedArtifact related : library.getRelatedArtifact()) {
          if (related.getUrl() != null && related.getUrl().startsWith("http://cts.nlm.nih.gov/fhir/ValueSet")) {
            Optional<Bundle.BundleEntryComponent> foundEntry = bundle.getEntry().stream().filter(n -> {
              return n.getResource() instanceof ValueSet &&
                      ((ValueSet) n.getResource()).getUrl().equals(related.getUrl());
            }).findAny();
            Optional<ValueSet> foundDownloaded = valueSets.stream().filter(n -> n.getUrl().equals(related.getUrl())).findAny();

            if (foundEntry.isPresent() || foundDownloaded.isPresent()) {
              continue;
            }

            String relatedUrl = related.getUrl();
            relatedUrl = "https" + relatedUrl.substring(4);
            URL url = new URL(relatedUrl + "/$expand");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            String unencodedCredentials = vsacUsername + ":" + vsacPassword;
            byte[] encodedCredentials = Base64.encodeBase64(unencodedCredentials.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedCredentials);
            conn.setRequestProperty("Authorization", authHeaderValue);

            logger.info("Getting value set from VSAC " + url.toString());

            int status = conn.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
              content.append(inputLine);
            }
            in.close();

            if (content.length() > 0) {
              logger.info("Parsing value set...");

              ValueSet vs = (ValueSet) ctx.newXmlParser().parseResource(content.toString());
              valueSets.add(vs);

              logger.info("Retrieved value set " + url.toString());
            } else {
              logger.error("Value set not found. VSAC returned status code " + status);
            }
          }
        }
      }
    }

    valueSets.parallelStream().forEach(vs -> {
      bundle.addEntry()
              .setResource(vs)
              .getRequest()
              .setMethod(Bundle.HTTPVerb.PUT)
              .setUrl("ValueSet/" + vs.getIdElement().getIdPart());
    });

    String xml = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(bundle);
    Files.write(measureBundlePath, xml.getBytes(StandardCharsets.UTF_8));

    logger.info("Done.");
  }
}
