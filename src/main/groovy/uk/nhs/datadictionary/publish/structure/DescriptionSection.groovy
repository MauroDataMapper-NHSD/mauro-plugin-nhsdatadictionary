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

import groovy.util.logging.Slf4j
import groovy.xml.MarkupBuilder
import org.maurodata.dita.elements.langref.base.Body
import org.maurodata.dita.elements.langref.base.Div
import org.maurodata.dita.helpers.HtmlHelper
import uk.nhs.datadictionary.publish.DaisyDiffHelper
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.publish.changePaper.Change

@Slf4j
class DescriptionSection extends Section {
    final String text
    final DiffStatus diffStatus
    final boolean includesComparison

    private static final CANNOT_COMPARE_TEXT = "Unable to show the changes between the Change Request and the NHS Data Model and Dictionary. These are the changes made in the Change Request."

    DescriptionSection(DictionaryItem parent, String text) {
        this(parent, "Description", text, DiffStatus.NONE, false)
    }

    DescriptionSection(DictionaryItem parent, String title, String text, DiffStatus diffStatus, boolean includesComparison) {
        super(parent, "description", title)

        this.text = text
            ? text.replace('<table', '<table class=\"table table-sm table-striped table-bordered\"')
            : ""

        this.diffStatus = diffStatus
        this.includesComparison = includesComparison
    }

    Section addDescriptionSection(){
        return new DescriptionSection(this.parent, this.text)
    }


    @Override
    Section produceDiff(Section previous) {
        DescriptionSection previousSection = previous as DescriptionSection
        boolean isNewItem = previousSection == null

        if (isNewItem) {
            return new DescriptionSection(this.parent, Change.NEW_TYPE, this.text, DiffStatus.NEW, false)
        }

        // To get accurate description comparison, we have to load all strings as HTML Dom trees and compare them. A simple
        // string comparison won't do anymore - because you could have superfluous whitespace between HTML tags that a string compare will
        // just consider "different" when semantically it isn't. _However_, this HTML comparison is *incredibly* slow across comparing
        // two dictionary branches now! If you want to test it faster, uncomment the line below and remember to put it back before you commit
        //if (description == previousComponent.description) { // DEBUG
        if (DaisyDiffHelper.calculateDifferences(this.text, previousSection.text).length == 0) {
            // These descriptions are the same - no change required
            return null
        }

        // For updated descriptions, there could be really complex HTML which really messes up the diff output,
        // particular when HTML tables merge cells together, the diff tags added back don't align correctly.
        // So just ignore this for now!
        boolean currentContainsHtmlTable = DaisyDiffHelper.containsHtmlTable(this.text)
        boolean previousContainsHtmlTable = DaisyDiffHelper.containsHtmlTable(previousSection.text)
        boolean canDiffContent = !currentContainsHtmlTable && !previousContainsHtmlTable

        String diffHtml = ""
        if (!canDiffContent) {
            log.warn("HTML in ${parent.stereotype} '${parent.name}' contains table, cannot produce diff")
            diffHtml = this.text
        }
        else {
            diffHtml = DaisyDiffHelper.diff(previousSection.text, this.text)

            if (!diffHtml) {
                log.warn("HTML in ${parent.stereotype} '${parent.name}' failed DaisyDiff comparison, showing Change Request text only")
                canDiffContent = false
                diffHtml = this.text
            }
        }

        String changeType = this.parent.state == DictionaryItem.DictionaryItemState.RETIRED && previousSection.parent.state != DictionaryItem.DictionaryItemState.RETIRED
            ? Change.RETIRED_TYPE
            : Change.UPDATED_DESCRIPTION_TYPE

        new DescriptionSection(this.parent, changeType, diffHtml, DiffStatus.MODIFIED, canDiffContent)
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        String modifiedText = context.replaceLinksInString(text)

        Body.build() {
            if (this.diffStatus == DiffStatus.MODIFIED && !this.includesComparison) {
                p(outputClass: HtmlConstants.CSS_INFO_MESSAGE_PARAGRAPH) {
                    txt CANNOT_COMPARE_TEXT
                }
            }

            div HtmlHelper.replaceHtmlWithDita(modifiedText)
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        String modifiedText = context.replaceLinksInString(text)

        String outputClass = PublishHelper.getDiffCssClass(diffStatus)
        Div.build(outputClass: outputClass) {
            if (this.diffStatus == DiffStatus.MODIFIED && !this.includesComparison) {
                p(outputClass: HtmlConstants.CSS_INFO_MESSAGE_PARAGRAPH) {
                    txt CANNOT_COMPARE_TEXT
                }
            }

            div HtmlHelper.replaceHtmlWithDita(modifiedText)
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String modifiedText = context.replaceLinksInString(text)

        String sectionOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_DIV : null
        String paragraphOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_PARAGRAPH : null

        builder.div(class: PublishHelper.combineCssClassWithDiffStatus(sectionOutputClass, this.diffStatus)) {
            if (this.diffStatus == DiffStatus.MODIFIED && !this.includesComparison) {
                builder.p(class: HtmlConstants.CSS_INFO_MESSAGE_PARAGRAPH) {
                    mkp.yield(CANNOT_COMPARE_TEXT)
                }
            }

            // TODO: prefix sentence - for Element page type (X is the same as Y)
            builder.p(class: paragraphOutputClass) {
                mkp.yieldUnescaped(modifiedText)
            }
        }
    }
}
