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
import org.maurodata.dita.elements.langref.base.Row
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.datadictionary.publish.structure.DiffObjectAware
import uk.nhs.datadictionary.publish.structure.DiffStatus
import uk.nhs.datadictionary.publish.structure.DitaAware
import uk.nhs.datadictionary.publish.structure.HtmlBuilder
import uk.nhs.datadictionary.publish.structure.HtmlConstants

class OtherDataSetRow implements DitaAware<Row>, HtmlBuilder, ChangeAware, DiffObjectAware<OtherDataSetRow> {
    final String mandation
    final OtherDataSetCell cell

    final DiffStatus diffStatus

    OtherDataSetRow(String mandation, OtherDataSetCell cell) {
        this(mandation, cell, DiffStatus.NONE)
    }

    OtherDataSetRow(String mandation, OtherDataSetCell cell, DiffStatus diffStatus) {
        this.mandation = mandation
        this.cell = cell
        this.diffStatus = diffStatus
    }

    OtherDataSetRow cloneWithDiffStatus(DiffStatus diffStatus) {
        new OtherDataSetRow(this.mandation, this.cell, diffStatus)
    }

    @Override
    String getDiscriminator() {
        "row:mro:${mandation}-cell:${cell.discriminator}"
    }

    @Override
    Row generateDita(PublishContext context) {
        String outputClass = PublishHelper.getDiffCssClass(diffStatus)

        Row.build(outputClass: outputClass) {
            entry {
                p this.mandation
            }
            entry {
                this.cell.generateDita(context).each { paragraph ->
                    p paragraph
                }
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String rowCssClass = context.rowCssClass
        String entryCssClass = context.getEntryCssClass()

        builder.tr(class: PublishHelper.combineCssClassWithDiffStatus(rowCssClass, diffStatus)) {
            builder.td(class: PublishHelper.combineCssClasses(entryCssClass, HtmlConstants.CSS_HTML_ALIGN_CENTER)) {
                PublishHelper.buildHtmlParagraph(context, builder, this.mandation)
            }
            builder.td(class: entryCssClass) {
                this.cell.buildHtml(context, builder)
            }
        }
    }
}
