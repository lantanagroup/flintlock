package com.lantanagroup.link.api.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.lantanagroup.fhir.transform.FHIRTransformResult;
import com.lantanagroup.fhir.transform.FHIRTransformer;
import com.lantanagroup.link.Helper;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reportability Response Controller
 */
@RestController
public class PatientIdentifierController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(PatientIdentifierController.class);
    private final FhirContext ctx = FhirContext.forR4();


    private void receiveFHIR(Resource resource, HttpServletRequest request) throws Exception {
        logger.info("Storing patient identifiers");
        IGenericClient fhirStoreClient = this.getFhirStoreClient(null, request);
        MethodOutcome outcome = null;
        if (resource.hasId()) {
            resource.setId((String) null);
        }
        if (resource instanceof ListResource) {

            List<Identifier> identifierList = ((ListResource) resource).getIdentifier();
            Date datetime = ((ListResource) resource).getDate();
            // added some validation code
            if (datetime == null) {
                throw new Exception("Date is not passed in the file.");
            }
            if (identifierList.isEmpty()) {
                throw new Exception("Identifier is not present.");
            }
            String system = ((ListResource) resource).getIdentifier().get(0).getSystem();
            String value = ((ListResource) resource).getIdentifier().get(0).getValue();
            String date = Helper.getFhirDate(datetime);
            Bundle bundle = searchListByCodeData(system, value, date, fhirStoreClient);
            if (bundle.getEntry().size() == 0) {
                ((ListResource) resource).setDateElement(new DateTimeType(date));
                createResource(resource, fhirStoreClient);
            } else {
                ListResource existingList = (ListResource) bundle.getEntry().get(0).getResource();
                // filter out duplicates
                List<ListResource.ListEntryComponent> uniqueEntries = ((ListResource) resource).getEntry().parallelStream()
                        .filter(e -> {
                            String systemA = e.getItem().getIdentifier().getSystem();
                            String valueA = e.getItem().getIdentifier().getValue();

                            return !existingList.getEntry().stream().anyMatch(s -> systemA.equals(s.getItem().getIdentifier().getSystem()) && valueA.equals(s.getItem().getIdentifier().getValue()));
                        }).collect(Collectors.toList());

                // merge lists into existingList
                uniqueEntries.parallelStream().forEach(entry -> {
                    existingList.getEntry().add(entry);
                });
                ((ListResource) resource).setDateElement(new DateTimeType(date));
                updateResource(existingList, fhirStoreClient);
            }
        } else {
            createResource(resource, fhirStoreClient);
        }
    }

    private ListResource getListFromBundle(Bundle bundle) {
        ListResource list = new ListResource();
        String date = Helper.getFhirDate(bundle.getTimestamp());
        list.setDateElement(new DateTimeType(date));
        List<Identifier> identifierList = new ArrayList<>();
        identifierList.add(bundle.getIdentifier());
        list.setIdentifier(identifierList);
        (bundle.getEntry().parallelStream()).forEach(bundleEntry -> {
            if (bundleEntry.getResource().getResourceType().equals(ResourceType.Patient)) {
                Patient p = (Patient) bundleEntry.getResource();
                if (null != p.getIdentifier().get(0)) {
                    String system = p.getIdentifier().get(0).getSystem();
                    String value = p.getIdentifier().get(0).getValue();
                    ListResource.ListEntryComponent listEntry = new ListResource.ListEntryComponent();
                    Identifier patientIdentifier = new Identifier();
                    patientIdentifier.setSystemElement(new UriType(system));
                    patientIdentifier.setValueElement(new StringType(value));
                    Reference reference = new Reference();
                    reference.setIdentifier(patientIdentifier);
                    listEntry.setItem(reference);
                    list.addEntry(listEntry);
                }
            }
        });
        return list;
    }


    private Bundle searchListByCodeData(String system, String value, String date, IGenericClient fhirStoreClient) {
        return fhirStoreClient
                .search()
                .forResource(ListResource.class)
                .where(ListResource.IDENTIFIER.exactly().systemAndValues(system, value))
                .and(ListResource.DATE.exactly().day(date))
                .returnBundle(Bundle.class)
                .cacheControl(new CacheControlDirective().setNoCache(true))
                .execute();
    }

    @PostMapping(value = "api/fhir/Bundle", consumes = MediaType.APPLICATION_XML_VALUE)
    public void receiveFHIRXML(@RequestBody() String body, HttpServletRequest request) throws Exception {
        logger.debug("Receiving RR FHIR XML. Parsing...");

        Bundle bundle = this.ctx.newXmlParser().parseResource(Bundle.class, body);
        ListResource list = getListFromBundle(bundle);

        logger.debug("Done parsing. Storing RR FHIR XML...");

        this.receiveFHIR(list, request);
    }


    @PostMapping(value = "api/fhir/Bundle", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void receiveFHIRJSON(@RequestBody() String body, HttpServletRequest request) throws Exception {
        logger.debug("Receiving RR FHIR JSON. Parsing...");

        Bundle bundle = this.ctx.newJsonParser().parseResource(Bundle.class, body);
        ListResource list = getListFromBundle(bundle);

        logger.debug("Done parsing. Storing RR FHIR JSON...");

        this.receiveFHIR(list, request);
    }


    @PostMapping(value = "api/fhir/List", consumes = {MediaType.APPLICATION_XML_VALUE})
    public void getPatientIdentifierListXML(@RequestBody() String body, HttpServletRequest request) throws Exception {
        logger.debug("Receiving patient identifier FHIR List in XML");

        ListResource list = this.ctx.newXmlParser().parseResource(ListResource.class, body);
        this.receiveFHIR(list, request);
    }

    @PostMapping(value = "api/fhir/List", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void getPatientIdentifierListJSON(@RequestBody() String body, HttpServletRequest request) throws Exception {
        logger.debug("Receiving patient identifier FHIR List in JSON");

        Resource bundle = this.ctx.newJsonParser().parseResource(Bundle.class, body);
        this.receiveFHIR(bundle, request);
    }

    @PostMapping(value = "api/cda", consumes = MediaType.APPLICATION_XML_VALUE)
    public void receiveCDA(@RequestBody() String xml, HttpServletRequest request) throws Exception {
        FHIRTransformer transformer = new FHIRTransformer();

        logger.debug("Receiving RR CDA XML. Converting to FHIR4 XML...");

        FHIRTransformResult result = transformer.cdaToFhir4(xml);

        if (!result.isSuccess()) {
            logger.error("Failed to transform RR CDA XML to FHIR4 XML!");

            List<String> messages = result.getMessages().stream().map(m -> m.getMessage()).collect(Collectors.toList());

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, StringUtils.join(messages, "\r\n"));
        } else {
            logger.debug("Parsing FHIR XML Bundle...");

            Resource bundle = this.ctx.newXmlParser().parseResource(Bundle.class, result.getResult());

            logger.debug("Done parsing. Storing RR FHIR XML...");

            this.receiveFHIR(bundle, request);
        }
    }
}
