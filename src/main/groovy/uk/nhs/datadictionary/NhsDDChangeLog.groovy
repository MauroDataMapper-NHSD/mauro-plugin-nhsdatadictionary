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

class NhsDDChangeLog {
    String reference
    String referenceUrl
    String description
    String implementationDate

    NhsDDChangeLog() {
        // For testing only, set properties manually
    }

    NhsDDChangeLog(NhsDDBranch branch, String changeRequestUrl) {
        this.reference = branch.reference
        if (!this.reference || this.reference.empty) {
            this.reference = "n/a"
        }
        else {
            this.referenceUrl = changeRequestUrl.replace(NhsDataDictionary.CHANGE_REQUEST_NUMBER_TOKEN, this.reference)
        }

        this.description = branch.subject
        if (!this.description || this.description.empty) {
            this.description = ""
        }

        this.implementationDate = branch.effectiveDate
        if (!this.implementationDate || this.implementationDate.empty) {
            this.implementationDate = "Immediate"
        }
    }
}
