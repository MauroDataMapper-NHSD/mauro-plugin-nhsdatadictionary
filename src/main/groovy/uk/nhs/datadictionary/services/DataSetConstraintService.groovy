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
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import uk.nhs.datadictionary.NhsDDBusinessDefinition
import uk.nhs.datadictionary.NhsDDDataSetComponent
import uk.nhs.datadictionary.NhsDDDataSetConstraint
import uk.nhs.datadictionary.NhsDataDictionary

@Slf4j
@Singleton
class DataSetConstraintService extends DataDictionaryComponentService<Term, NhsDDDataSetConstraint> {

    @Inject
    TermCacheableRepository termCacheableRepository

    String getStereotype() {
        "xmlSchemaConstraint"
    }


    @Override
    NhsDDDataSetConstraint show(UUID versionedFolderId, UUID id, NhsDataDictionaryService nhsDataDictionaryService) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary(versionedFolderId)

        Term dataSetConstraintTerm = termCacheableRepository.findById(id)
        NhsDDDataSetConstraint dataSetConstraint = initialiseComponent(new NhsDDDataSetConstraint(), dataSetConstraintTerm, versionedFolderId, nhsDataDictionaryService)
        return dataSetConstraint
    }

    @Override
    Set<Term> getAll(UUID versionedFolderId, NhsDataDictionaryService nhsDataDictionaryService, Boolean includeRetired = false) {

        Terminology dataSetConstraintTerminology = nhsDataDictionaryService.getDataSetConstraintTerminology(versionedFolderId)

        List<Term> terms = termCacheableRepository.readAllByTerminologyIdIn([dataSetConstraintTerminology.id])
        Map<UUID, Term> termsMap = terms.collectEntries {
            [it.id, it]
        }
        List<Metadata> metadata = metadataCacheableRepository.findByMultiFacetAwareItemIdInAndNamespaceAndKey(terms.id, new NhsDDDataSetConstraint().getMetadataNamespace(), "isRetired")

        metadata.each {md ->
            termsMap[md.multiFacetAwareItemId].metadata.add(md)
        }
        return termsMap.values().findAll {term ->
            includeRetired || !catalogueItemIsRetired(term)
        } as Set<Term>

    }


    void persistDataSetConstraints(NhsDataDictionary dataDictionary, Folder dictionaryFolder) {

        Terminology terminology = new Terminology(
            label: NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME,
            folder: dictionaryFolder)
        dictionaryFolder.terminologies.add(terminology)
        TreeMap<String, Term> allTerms = new TreeMap<>()
        dataDictionary.dataSetConstraints.each {name, dataSetConstraint ->

            // Either this is the first time we've seen a term...
            // .. or if we've already got one, we'll overwrite it with this one
            // (if this one isn't retired)
            if(!allTerms[name] || !dataSetConstraint.isRetired()) {

                Term term = new Term(
                    code: name,
                    label: name,
                    definition: name,
                    // Leave Url blank for now
                    // url: businessDefinition.otherProperties["ddUrl"].replaceAll(" ", "%20"),
                    description: dataSetConstraint.description,
                    depth: 1,
                    terminology: terminology)

                addMetadataFromComponent(term, dataSetConstraint)

                allTerms[name] = term
            }
        }
        allTerms.values().each { term ->
            terminology.terms.add(term)
        }
    }

    NhsDDDataSetConstraint getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary) {
        nhsDataDictionary.dataSetConstraints.values().find {
            it.catalogueItem.id == catalogueItemId
        }
    }


}