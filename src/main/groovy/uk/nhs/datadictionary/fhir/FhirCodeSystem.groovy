/*
 * Copyright 2020-2025 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.nhs.datadictionary.fhir

import uk.nhs.datadictionary.NhsDDAttribute

class FhirCodeSystem implements FhirResource {

    // TODO - take these from system properties
    public static final String URL_PREFIX =  "https://maurotest.datadictionary.nhs.uk/code_system/union/"
    public static final String SYSTEM = "https://datadictionary.nhs.uk/V3"
    public static final String IDENTIFIER_PREFIX = "https://datadictionary.nhs.uk/data_dictionary/attributes/"

    String id
    String version
    String url
    String identifierSystem
    String identifierValue
    String name
    String title
    String status
    String experimental
    String date
    String description
    List<FhirConcept> concepts = []


    static FhirCodeSystem fromDDAttribute(NhsDDAttribute attribute, String versionId, String sharedPublishDate) {

        String adjustedName = attribute.name.toLowerCase().replace(" ", "_")
        String idAdjustedName = attribute.name.toLowerCase().replace(" ", "-")
        new FhirCodeSystem().tap {
            id = "MauroTest-NHS-Data_Dictionary-CS-" + idAdjustedName + "-" + versionId
            version = versionId
            url = "${URL_PREFIX}${adjustedName}"
            identifierSystem = SYSTEM
            identifierValue = "${IDENTIFIER_PREFIX}${adjustedName}"
            name = attribute.name.toUpperCase().replaceAll("[^A-Za-z0-9]", "_")
            title = attribute.getTitleCaseName()
            status = "active"
            experimental = "false"
            date = sharedPublishDate
            description = attribute.getShortDescription()
            attribute.codes
                .sort { it.code }
                .sort {it.webOrder }
                .each { attributeCode ->
                    FhirConcept concept = new FhirConcept().tap {
                        code = attributeCode.code
                        display = attributeCode.definition
                        definition = attributeCode.definition

                        properties.add(new FhirProperty(code: "Publish Date", valueDateTime: sharedPublishDate))
                        properties.add(new FhirProperty(code: "status", valueCode: attribute.isRetired()? "retired" : "active"))
                        properties.add(new FhirProperty(code: "Data Attribute", valueString: attributeCode.owningAttribute.getTitleCaseName()))
                        attributeCode.usedByElements.each {usedByElement ->
                            properties.add(new FhirProperty(code: "Data Element", valueString: usedByElement.getTitleCaseName()))
                        }
                    }
                concepts.add(concept)
                }
        }
    }


}
