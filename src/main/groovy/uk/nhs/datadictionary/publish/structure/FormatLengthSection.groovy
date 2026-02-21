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
package uk.nhs.datadictionary.publish.structure

import groovy.xml.MarkupBuilder
import org.maurodata.dita.elements.langref.base.Body
import org.maurodata.dita.elements.langref.base.Div
import uk.nhs.datadictionary.NhsDDFormatLength
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget

class FormatLengthSection extends Section {
    final NhsDDFormatLength value
    final NhsDDFormatLength comparisonValue

    FormatLengthSection(DictionaryItem parent, NhsDDFormatLength value) {
        this(parent, value, null)
    }

    FormatLengthSection(DictionaryItem parent, NhsDDFormatLength value, NhsDDFormatLength comparisonValue) {
        super(parent, "formatLength", "Format / Length")

        this.value = value
        this.comparisonValue = comparisonValue
    }

    Section addFormatLengthSection(Section previous){
        FormatLengthSection previousSection = previous as FormatLengthSection

        NhsDDFormatLength currentValue = this.value
        NhsDDFormatLength previousValue = previousSection?.value ?: NhsDDFormatLength.EMPTY

        if (currentValue.empty()) {
            return null
        }

        new FormatLengthSection(this.parent, currentValue, previousValue)

    }


    @Override
    Section produceDiff(Section previous) {
        FormatLengthSection previousSection = previous as FormatLengthSection

        NhsDDFormatLength currentValue = this.value
        NhsDDFormatLength previousValue = previousSection?.value ?: NhsDDFormatLength.EMPTY

        if (currentValue.empty() && previousValue.empty()) {
            return null
        }

        boolean isNewItem = previous == null

        if (!isNewItem && currentValue.equals(previousValue)) {
            // Identical, nothing to show. If it's a new item though, always show this
            return null
        }

        new FormatLengthSection(this.parent, currentValue, previousValue)
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            if (context.target == PublishTarget.CHANGE_PAPER) {
                p {
                    b title
                }
            }

            if (this.comparisonValue && !this.value.equals(this.comparisonValue)) {
                if (!this.value.empty()) {
                    p NhsDDFormatLength.buildChangeDita(this.value, PublishHelper.getDiffCssClass(DiffStatus.NEW))
                }
                if (!this.comparisonValue.empty()) {
                    p NhsDDFormatLength.buildChangeDita(this.comparisonValue, PublishHelper.getDiffCssClass(DiffStatus.REMOVED))
                }
            }
            else {
                p NhsDDFormatLength.buildChangeDita(this.value)
            }
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            if (context.target == PublishTarget.CHANGE_PAPER) {
                p {
                    b title
                }
            }

            if (this.comparisonValue && !this.value.equals(this.comparisonValue)) {
                if (!this.value.empty()) {
                    p NhsDDFormatLength.buildChangeDita(this.value, PublishHelper.getDiffCssClass(DiffStatus.NEW))
                }
                if (!this.comparisonValue.empty()) {
                    p NhsDDFormatLength.buildChangeDita(this.comparisonValue, PublishHelper.getDiffCssClass(DiffStatus.REMOVED))
                }
            }
            else {
                p NhsDDFormatLength.buildChangeDita(this.value)
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String sectionOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_DIV : null
        String paragraphOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_PARAGRAPH : ""

        builder.div(class: sectionOutputClass) {
            if (context.target == PublishTarget.CHANGE_PAPER) {
                builder.p(class: paragraphOutputClass) {
                    b title
                }
            }

            if (this.comparisonValue && !this.value.equals(this.comparisonValue)) {
                String newOutputClass = "${paragraphOutputClass} ${PublishHelper.getDiffCssClass(DiffStatus.NEW)}".trim()
                String removedOutputClass = "${paragraphOutputClass} ${PublishHelper.getDiffCssClass(DiffStatus.REMOVED)}".trim()

                NhsDDFormatLength.writeChangeHtml(builder, this.value, newOutputClass)
                NhsDDFormatLength.writeChangeHtml(builder, this.comparisonValue, removedOutputClass)
            }
            else {
                NhsDDFormatLength.writeChangeHtml(builder, this.value, paragraphOutputClass)
            }
        }
    }
}
