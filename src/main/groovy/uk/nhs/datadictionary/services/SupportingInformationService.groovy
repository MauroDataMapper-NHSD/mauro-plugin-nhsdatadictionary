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
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import uk.nhs.datadictionary.NhsDDSupportingInformation
import uk.nhs.datadictionary.NhsDataDictionary

@Slf4j
@Singleton
class SupportingInformationService extends DataDictionaryComponentService<Term, NhsDDSupportingInformation> {

    @Override
    NhsDDSupportingInformation show(UUID versionedFolderId, String id) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.newDataDictionary()
        dataDictionary.containingVersionedFolder = versionedFolderService.get(versionedFolderId)

        Term supportingInformationTerm = termService.get(id)
        NhsDDSupportingInformation supportingInformation = new NhsDDSupportingInformation().fromMauroItem(dataDictionary, mauroPersistenceService, supportingInformationTerm)
        supportingInformation.definition = convertLinksInDescription(versionedFolderId, supportingInformation.getDescription())
        return supportingInformation
    }

    @Override
    Set<Term> getAll(UUID versionedFolderId, boolean includeRetired = false) {
        Terminology supInfTerminology = nhsDataDictionaryService.getSupportingDefinitionTerminology(versionedFolderId)
        List<Term> terms = termService.findAllByTerminologyId(supInfTerminology.id)
        terms.findAll {term ->
            includeRetired || !catalogueItemIsRetired(term)
        }
    }


    void persistSupportingInformation(NhsDataDictionary dataDictionary, Folder dictionaryFolder) {

        Terminology terminology = new Terminology(
            label: NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME,
            folder: dictionaryFolder,
            branchName: dataDictionary.branchName)
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
                    description: supportingInformation.definition,
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
