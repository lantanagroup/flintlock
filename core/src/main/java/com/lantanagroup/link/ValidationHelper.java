package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.r4.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.r4.hapi.validation.PrePopulatedValidationSupport;
import org.hl7.fhir.r4.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.List;

public class ValidationHelper {

  public static ValidationResult validateList(String listResource) throws Exception{
    FhirContext ctx = FhirContext.forR4();
    ctx.getResourceDefinition("List");

    // Ask the context for a validator
    FhirValidator validator = ctx.newValidator();

    InputStream resource = new ClassPathResource("StructureDefinition-nhsnlink-patient-list.xml").getInputStream();
    StructureDefinition listProfile = (StructureDefinition) ctx.newXmlParser().parseResource(resource);
    PrePopulatedValidationSupport valSupport = new PrePopulatedValidationSupport();
    valSupport.fetchAllStructureDefinitions(ctx);
    valSupport.addStructureDefinition(listProfile);

    ValidationSupportChain validationSupportChain = new ValidationSupportChain(
            new DefaultProfileValidationSupport(),
            valSupport
    );;
    validationSupportChain.fetchAllStructureDefinitions(ctx);
    //validationSupportChain.fetchAllConformanceResources(ctx);
    // Create a validation module and register it
    IValidatorModule module = new FhirInstanceValidator(validationSupportChain);
    validator.registerValidatorModule(module);

    return validator.validateWithResult(listResource);
  }
}
