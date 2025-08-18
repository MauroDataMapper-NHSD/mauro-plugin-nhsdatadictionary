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
package uk.nhs.datadictionary.publish.changePaper

class ChangePaperPreview {
    ChangePaperPreviewBackground background
    List<ChangePaperPreviewStereotype> stereotypes
}

class ChangePaperPreviewBackground {
    String reference
    String type
    String versionNo
    String subject
    String effectiveDate
    String reasonForChange
    String publicationDate
    String background
    String sponsor
    String contactDetails
}

class ChangePaperPreviewStereotype {
    String name
    List<ChangePaperPreviewItem> changes
}

class ChangePaperPreviewItem {
    String name
    String summary
    String htmlOutput
}