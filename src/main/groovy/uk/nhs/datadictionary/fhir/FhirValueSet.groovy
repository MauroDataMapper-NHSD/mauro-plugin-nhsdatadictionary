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
import uk.nhs.datadictionary.NhsDDElement

class FhirValueSet implements FhirResource {

    // TODO - take these from system properties
    public static final String URL_PREFIX =  "https://maurotest.datadictionary.nhs.uk/valueSet/"
    public static final String SYSTEM = "https://datadictionary.nhs.uk/V3"
    public static final String IDENTIFIER_PREFIX = "https://datadictionary.nhs.uk/data_dictionary/data_field_notes/"

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
    List<FhirInclude> includes


    static FhirValueSet fromDDElement(NhsDDElement element, String versionId, String sharedPublishDate) {
        String adjustedName = element.name.toLowerCase().replace(" ", "_")
        String idAdjustedName = element.name.toLowerCase().replace(" ", "-")
        new FhirValueSet().tap {
            id = "MauroTest-NHS-Data_Dictionary-VS-" + idAdjustedName + "-" + versionId
            version = versionId
            url = "${URL_PREFIX}${adjustedName}"
            identifierSystem = SYSTEM
            identifierValue = "${IDENTIFIER_PREFIX}${adjustedName}"
            name = element.name.toUpperCase().replaceAll("[^A-Za-z0-9]", "_")
            title = element.getTitleCaseName()
            status = "active"
            experimental = "false"
            date = sharedPublishDate
            description = element.getShortDescription()

            Set<NhsDDAttribute> codesUsedByAttributes = element.codes.collect {code ->
                code.owningAttribute
            } as Set

            includes = codesUsedByAttributes.collect {attribute ->
                new FhirInclude().tap {
                    system = FhirCodeSystem.URL_PREFIX + attribute.name.toLowerCase().replace(" ", "_")
                    version = versionId
                    concepts = element.codes
                        .findAll {code -> code.owningAttribute == attribute}
                        .sort {it.code}
                        .collect {elementCode ->
                            new FhirConcept().tap {
                                code = elementCode.code
                                display = elementCode.definition
                                definition = elementCode.definition

                                properties.add(new FhirProperty(code: "Publish Date", valueDateTime: sharedPublishDate))
                                properties.add(new FhirProperty(code: "status", valueCode: attribute.isRetired()? "retired" : "active"))

                            }

                        }
                }
            }
        }
    }


}
