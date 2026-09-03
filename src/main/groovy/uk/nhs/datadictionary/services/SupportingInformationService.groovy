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
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDBusinessDefinition
import uk.nhs.datadictionary.NhsDDSupportingInformation
import uk.nhs.datadictionary.NhsDataDictionary

@Slf4j
@Singleton
class SupportingInformationService extends DataDictionaryComponentService<Term, NhsDDSupportingInformation> {

    @Inject
    TermCacheableRepository termCacheableRepository

    String getStereotype() {
        "supportingInformation"
    }


    @Override
    NhsDDSupportingInformation show(UUID versionedFolderId, UUID id, NhsDataDictionaryService nhsDataDictionaryService) {
        Term supportingInformationTerm = termCacheableRepository.findById(id)
        NhsDDSupportingInformation supportingInformation = initialiseComponent(new NhsDDSupportingInformation(), supportingInformationTerm, versionedFolderId, nhsDataDictionaryService)
        return supportingInformation
    }

    @Override
    Set<Term> getAll(UUID versionedFolderId, NhsDataDictionaryService nhsDataDictionaryService, Boolean includeRetired = false) {
        Terminology supInfTerminology = nhsDataDictionaryService.getSupportingInformationTerminology(versionedFolderId)
        List<Term> terms = termCacheableRepository.readAllByTerminologyIdIn([supInfTerminology.id])
        Map<UUID, Term> termsMap = terms.collectEntries {
            [it.id, it]
        }
        List<Metadata> metadata = metadataCacheableRepository.findByMultiFacetAwareItemIdInAndNamespaceAndKey(terms.id, new NhsDDSupportingInformation().getMetadataNamespace(), "isRetired")

        metadata.each {md ->
            termsMap[md.multiFacetAwareItemId].metadata.add(md)
        }
        return termsMap.values().findAll {term ->
            includeRetired || !catalogueItemIsRetired(term)
        } as Set<Term>
    }


    void persistSupportingInformation(NhsDataDictionary dataDictionary, Folder dictionaryFolder) {

        Terminology terminology = new Terminology(
            label: NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME,
            folder: dictionaryFolder)
        dictionaryFolder.terminologies.add(terminology)
        TreeMap<String, Term> allTerms = new TreeMap<>()
        dataDictionary.supportingInformation.each {name, supportingInformation ->

            // Either this is the first time we've seen a term...
            // .. or if we've already got one, we'll overwrite it with this one
            // (if this one isn't retired)
            if(!allTerms[name] || !supportingInformation.isRetired()) {

                Term term = new Term(
                    code: name,
                    label: name,
                    definition: name,
                    // Leave Url blank for now
                    // url: businessDefinition.otherProperties["ddUrl"].replaceAll(" ", "%20"),
                    description: supportingInformation.description,
                    depth: 1,
                    terminology: terminology)

                addMetadataFromComponent(term, supportingInformation)

                allTerms[name] = term
            }
        }
        allTerms.values().each { term ->
            terminology.terms.add(term)
        }
    }

    NhsDDSupportingInformation getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.supportingInformation.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }


}
