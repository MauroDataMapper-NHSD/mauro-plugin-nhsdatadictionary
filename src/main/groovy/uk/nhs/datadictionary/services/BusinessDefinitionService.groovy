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
package uk.nhs.datadictionary.services

import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.terminology.dto.TermDTORepository
import uk.nhs.datadictionary.NhsDDBusinessDefinition
import uk.nhs.datadictionary.NhsDataDictionary

@Slf4j
@Singleton
class BusinessDefinitionService extends DataDictionaryComponentService<Term, NhsDDBusinessDefinition> {

    @Inject
    TermCacheableRepository termCacheableRepository

    @Inject
    TermDTORepository termDTORepository

    BusinessDefinitionService() {
    }

    String getStereotype() {
        "businessDefinition"
    }

    @Override
    NhsDDBusinessDefinition show(UUID versionedFolderId, UUID id, NhsDataDictionaryService nhsDataDictionaryService) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary(versionedFolderId)
        Term businessDefinitionTerm = termDTORepository.findById(id)
        NhsDDBusinessDefinition businessDefinition = new NhsDDBusinessDefinition().fromMauroItem(dataDictionary, mauroPersistenceService, businessDefinitionTerm)
        businessDefinition.htmlDescription = convertLinksInDescription(versionedFolderId, businessDefinition.getDescription())
        businessDefinition.definition =
            convertLinksInDescription(versionedFolderId, businessDefinition.getDescription())
        return businessDefinition
    }

    @Override
    Set<Term> getAll(UUID versionedFolderId, NhsDataDictionaryService nhsDataDictionaryService, Boolean includeRetired = false) {
        Terminology busDefTerminology = nhsDataDictionaryService.getBusinessDefinitionTerminology(versionedFolderId)
        busDefTerminology.terms.findAll {term ->
            includeRetired || !catalogueItemIsRetired(term)
        }
    }

    void persistBusinessDefinitions(NhsDataDictionary dataDictionary, Folder dictionaryFolder) {

        Terminology terminology = new Terminology(
            label: NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME,
            folder: dictionaryFolder)
        dictionaryFolder.terminologies.add(terminology)
        TreeMap<String, Term> allTerms = new TreeMap<>()
        dataDictionary.businessDefinitions.each {name, businessDefinition ->

            // Either this is the first time we've seen a term...
            // .. or if we've already got one, we'll overwrite it with this one
            // (if this one isn't retired)
            if(!allTerms[name] || !businessDefinition.isRetired()) {

                Term term = new Term(
                    code: name,
                    label: name,
                    definition: name,
                    // Leave Url blank for now
                    // url: businessDefinition.otherProperties["ddUrl"].replaceAll(" ", "%20"),
                    description: businessDefinition.definition,
                    depth: 1,
                    terminology: terminology)

                addMetadataFromComponent(term, businessDefinition)

                allTerms[name] = term
            }
        }
        allTerms.values().each { term ->
            terminology.terms.add(term)
        }

    }

    NhsDDBusinessDefinition getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.businessDefinitions.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }

}
