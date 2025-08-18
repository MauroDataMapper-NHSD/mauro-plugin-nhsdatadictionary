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
package uk.nhs.datadictionary.publish.structure.datasets.other

import groovy.xml.MarkupBuilder
import org.maurodata.dita.elements.langref.base.Entry
import org.maurodata.dita.helpers.HtmlHelper
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.datadictionary.publish.structure.DiffAware
import uk.nhs.datadictionary.publish.structure.DiffObjectAware
import uk.nhs.datadictionary.publish.structure.DiffStatus
import uk.nhs.datadictionary.publish.structure.DitaAware
import uk.nhs.datadictionary.publish.structure.HtmlBuilder
import uk.nhs.datadictionary.publish.structure.HtmlConstants

enum OtherDataSetHeaderType {
    TABLE,
    GROUP
}

class OtherDataSetHeader
    implements
        DitaAware<Entry>,
        HtmlBuilder,
        ChangeAware,
        DiffAware<OtherDataSetHeader, OtherDataSetHeader>,
        DiffObjectAware<OtherDataSetHeader> {
    final OtherDataSetHeaderType type
    final String name
    final String description    // Optional
    final String previousDescription    // Optional, only used for diff comparisons

    final DiffStatus diffStatus

    OtherDataSetHeader(OtherDataSetHeaderType type, String name, String description) {
        this(type, name, description, null, DiffStatus.NONE)
    }

    OtherDataSetHeader(OtherDataSetHeaderType type, String name, String description, DiffStatus diffStatus) {
        this(type, name, description, null, diffStatus)
    }

    OtherDataSetHeader(
        OtherDataSetHeaderType type,
        String name,
        String description,
        String previousDescription,
        DiffStatus diffStatus) {
        this.type = type
        this.name = name
        this.description = description
        this.previousDescription = previousDescription
        this.diffStatus = diffStatus
    }

    @Override
    String getDiscriminator() {
        "header:${type}_${name}"
    }

    @Override
    OtherDataSetHeader cloneWithDiffStatus(DiffStatus diffStatus) {
        new OtherDataSetHeader(this.type, this.name, this.description, diffStatus)
    }

    @Override
    OtherDataSetHeader produceDiff(OtherDataSetHeader previous) {
        if (!previous) {
            return cloneWithDiffStatus(DiffStatus.NEW)
        }

        if (this.description != previous.description) {
            return new OtherDataSetHeader(this.type, this.name, this.description, previous.description, DiffStatus.MODIFIED)
        }

        // No change
        null
    }

    @Override
    Entry generateDita(PublishContext context) {
        String diffOutputStatus = PublishHelper.getDiffCssClass(diffStatus)

        String nameStart = type == OtherDataSetHeaderType.TABLE
            ? OtherDataSetTable.MANDATION_COLUMN.colId
            : OtherDataSetTable.DATA_ELEMENTS_COLUMN.colId

        Entry.build(namest: nameStart, nameend: OtherDataSetTable.DATA_ELEMENTS_COLUMN.colId, outputClass: diffOutputStatus) {
            b this.name
            if (this.description) {
                if (this.previousDescription && diffStatus == DiffStatus.MODIFIED) {
                    // Special case where we need to show the description has changed
                    div(outputClass: HtmlConstants.CSS_DIFF_NEW) {
                        div HtmlHelper.replaceHtmlWithDita(this.description)
                    }
                    div(outputClass: HtmlConstants.CSS_DIFF_DELETED) {
                        div HtmlHelper.replaceHtmlWithDita(this.previousDescription)
                    }
                }
                else {
                    div HtmlHelper.replaceHtmlWithDita(this.description)
                }
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String entryCssClass = context.target == PublishTarget.WEBSITE ? "${HtmlConstants.CSS_TABLE_ENTRY}" : ""

        if (type == OtherDataSetHeaderType.TABLE) {
            builder.th(colspan: 2, class: PublishHelper.combineCssClasses(entryCssClass, HtmlConstants.CSS_HTML_ALIGN_CENTER)) {
                b this.name
                buildHtmlDescriptionWithChange(context, builder)
            }
        }
        else {
            builder.td(class: entryCssClass) {
                b this.name
                buildHtmlDescriptionWithChange(context, builder)
            }
        }
    }

    void buildHtmlDescriptionWithChange(PublishContext context, MarkupBuilder builder) {
        if (this.description) {
            if (this.previousDescription && diffStatus == DiffStatus.MODIFIED) {
                // Special case where we need to show the description has changed
                builder.p(class: PublishHelper.combineCssClassWithDiffStatus(context.paragraphCssClass, DiffStatus.NEW)) {
                    mkp.yieldUnescaped(this.description)
                }
                builder.p(class: PublishHelper.combineCssClassWithDiffStatus(context.paragraphCssClass, DiffStatus.REMOVED)) {
                    mkp.yieldUnescaped(this.previousDescription)
                }
            }
            else {
                builder.p(class: context.paragraphCssClass) {
                    mkp.yieldUnescaped(this.description)
                }
            }
        }
    }
}
