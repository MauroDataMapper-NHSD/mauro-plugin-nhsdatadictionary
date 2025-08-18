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
import org.maurodata.dita.elements.langref.base.Strow
import org.maurodata.dita.helpers.HtmlHelper
import uk.nhs.datadictionary.NhsDDChangeLog
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget

class ChangeLogSection extends Section {
    final String headerText
    final String footerText
    final ChangeLogTable table

    ChangeLogSection(DictionaryItem parent, String headerText, String footerText, ChangeLogTable table) {
        super(parent, "changeLog", "Change Log")

        this.headerText = headerText
        this.footerText = footerText
        this.table = table
    }

    ChangeLogSection(DictionaryItem parent, String headerText, String footerText, List<ChangeLogRow> rows) {
        this(parent, headerText, footerText, new ChangeLogTable(rows))
    }

    @Override
    Section produceDiff(Section previous) {
        // Do not compare this section
        return null
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            if (headerText && !table.empty) {
                div HtmlHelper.replaceHtmlWithDita(headerText)
            }

            if (!table.empty) {
                simpletable table.generateDita(context)
            }

            if (footerText) {
                div HtmlHelper.replaceHtmlWithDita(footerText)
            }
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            if (headerText && !table.empty) {
                div HtmlHelper.replaceHtmlWithDita(headerText)
            }

            if (!table.empty) {
                simpletable table.generateDita(context)
            }

            if (footerText) {
                div HtmlHelper.replaceHtmlWithDita(footerText)
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String bodyOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_BODY : ""

        if (headerText && !table.empty) {
            builder.div(class: bodyOutputClass) {
                mkp.yieldUnescaped(headerText)
            }
        }

        if (!table.empty) {
            table.buildHtml(context, builder)
        }

        if (footerText) {
            builder.div(class: bodyOutputClass) {
                mkp.yieldUnescaped(footerText)
            }
        }
    }
}

class ChangeLogTable extends StandardTable<ChangeLogRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Change Request", "2*", "20%"),
        new StandardColumn("Change Request Description", "5*", "55%"),
        new StandardColumn("Implementation Date", "3*", "25%"),
    ]

    ChangeLogTable(List<ChangeLogRow> rows) {
        super(COLUMNS, rows)
    }

    @Override
    protected StandardTable<ChangeLogRow> cloneWithRows(List<ChangeLogRow> rows) {
        new ChangeLogTable(rows)
    }
}

class ChangeLogRow extends StandardRow {
    final NhsDDChangeLog entry

    ChangeLogRow(NhsDDChangeLog entry) {
        this.entry = entry
    }

    @Override
    String getDiscriminator() {
        // No change detection required
        return null
    }

    @Override
    Strow generateDita(PublishContext context) {
        Strow.build() {
            stentry {
                if (entry.referenceUrl) {
                    xRef PublishHelper.buildExternalXRef(entry.referenceUrl, entry.reference)
                }
                else {
                    txt entry.reference
                }
            }
            stentry entry.description
            stentry entry.implementationDate
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        builder.tr(class: HtmlConstants.CSS_TABLE_STROW) {
            builder.td(class: HtmlConstants.CSS_TABLE_STENTRY) {
                if (entry.referenceUrl) {
                    builder.a(href: entry.referenceUrl) {
                        mkp.yield(entry.reference)
                    }
                }
                else {
                    mkp.yield(entry.reference)
                }
            }
            builder.td(class: HtmlConstants.CSS_TABLE_STENTRY) {
                mkp.yield(entry.description)
            }
            builder.td(class: HtmlConstants.CSS_TABLE_STENTRY) {
                mkp.yield(entry.implementationDate)
            }
        }
    }

    @Override
    protected StandardRow cloneWithDiff(DiffStatus diffStatus) {
        // No change detection required
        return null
    }
}
