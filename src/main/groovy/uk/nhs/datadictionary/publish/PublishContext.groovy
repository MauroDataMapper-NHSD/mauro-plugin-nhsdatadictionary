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
package uk.nhs.datadictionary.publish

import groovy.util.logging.Slf4j
import uk.nhs.datadictionary.publish.structure.HtmlConstants

@Slf4j
class PublishContext {
    final PublishTarget target

    private ItemLinkScanner itemLinkScanner

    boolean prettyPrintHtml = true

    PublishContext(PublishTarget target) {
        this.target = target
    }

    void setItemLinkScanner(ItemLinkScanner itemLinkScanner) {
        this.itemLinkScanner = itemLinkScanner
    }

    String replaceLinksInString(String source) {
        if (!this.itemLinkScanner) {
            log.error("No itemLinkScanner set for ${this}")
            return source
        }

        this.itemLinkScanner.scanForItemLinks(source)
    }

    String getParagraphCssClass() {
        target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_PARAGRAPH : ""
    }

    String getRowCssClass() {
        target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_ROW : null
    }

    String getEntryCssClass() {
        target == PublishTarget.WEBSITE
            ? HtmlConstants.CSS_TABLE_ENTRY
            : null
    }

    String getEntryCssClass(String diffOutputClass) {
        target == PublishTarget.WEBSITE
            ? HtmlConstants.CSS_TABLE_ENTRY
            : !diffOutputClass.empty
            ? diffOutputClass
            : null
    }

    String getTableCssClass(String diffOutputClass) {
        target == PublishTarget.WEBSITE
            ? HtmlConstants.CSS_DATA_SET_TABLE
            : !diffOutputClass.empty
            ? diffOutputClass
            : null
    }

    String getTableHeadCssClass() {
        context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_HEAD : ""
    }
}
