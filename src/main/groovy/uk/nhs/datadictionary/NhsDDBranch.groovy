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
package uk.nhs.datadictionary

import org.maurodata.domain.folder.Folder

class NhsDDBranch {
    static final String METADATA_NAMESPACE = 'uk.nhs.datadictionary.workItem'
    static final String METADATA_KEY_TYPE = 'type'
    static final String METADATA_KEY_REFERENCE = 'reference'
    static final String METADATA_KEY_SUBJECT = 'subject'
    static final String METADATA_KEY_EFFECTIVE_DATE = 'effectiveDate'

    String id
    String branchName
    String modelVersion
    String modelVersionLabel
    boolean finalised

    String type
    String reference
    String subject
    String effectiveDate

    NhsDDBranch(Folder versionedFolder) {
        this.id = versionedFolder.id
        this.branchName = versionedFolder.branchName
        this.modelVersion = versionedFolder.modelVersion
        this.modelVersionLabel = versionedFolder.modelVersionTag
        this.finalised = versionedFolder.finalised.booleanValue()

        this.type = versionedFolder.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY_TYPE)?.value ?: ''
        this.reference = versionedFolder.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY_REFERENCE)?.value ?: ''
        this.subject = versionedFolder.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY_SUBJECT)?.value ?: ''
        this.effectiveDate = versionedFolder.findMetadataByNamespaceAndKey(METADATA_NAMESPACE, METADATA_KEY_EFFECTIVE_DATE)?.value ?: ''
    }
}
