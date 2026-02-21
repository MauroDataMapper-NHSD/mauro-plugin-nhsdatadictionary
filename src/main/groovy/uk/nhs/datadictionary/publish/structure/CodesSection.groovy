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
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.publish.changePaper.Change

class CodesSection extends Section {
    static final String NATIONAL_CODES_TYPE = "nationalCodes"
    static final String DEFAULT_CODES_TYPE = "defaultCodes"

    final CodesTable table

    CodesSection(DictionaryItem parent, String type, String title, CodesTable table) {
        super(parent, type, title)

        this.table = table
    }

    CodesSection(DictionaryItem parent, String type, String title,  List<CodesRow> rows) {
        this(parent, type, title, new CodesTable(rows))
    }

    static CodesSection createNationalCodes(DictionaryItem parent, List<CodesRow> rows) {
        new CodesSection(parent, NATIONAL_CODES_TYPE, "National Codes", rows)
    }

    static CodesSection createDefaultCodes(DictionaryItem parent, List<CodesRow> rows) {
        new CodesSection(parent, DEFAULT_CODES_TYPE, "Default Codes", rows)
    }

    Section  addDefaultCodeSection(){
        if(this.table == null){
            return null
        }
        new CodesSection(parent, DEFAULT_CODES_TYPE, "Default Codes", this.table)

    }


    @Override
    Section produceDiff(Section previous) {
        CodesSection previousSection = previous as CodesSection
        CodesTable diffTable = this.table.produceDiff(previousSection?.table) as CodesTable
        if (!diffTable) {
            return null
        }

        String diffTitle = this.type == NATIONAL_CODES_TYPE ? Change.NATIONAL_CODES_TYPE : Change.DEFAULT_CODES_TYPE
        new CodesSection(this.parent, this.type, diffTitle, diffTable)
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String paragraphOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_PARAGRAPH : ""

        if (context.target == PublishTarget.CHANGE_PAPER) {
            builder.p(class: paragraphOutputClass) {
                b title
            }
        }

        table.buildHtml(context, builder)
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            if (context.target == PublishTarget.CHANGE_PAPER) {
                p {
                    b title
                }
            }
            simpletable table.generateDita(context)
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
            simpletable table.generateDita(context)
        }
    }
}

class CodesTable extends StandardTable<CodesRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Code", "1*", "20%"),
        new StandardColumn("Description", "4*", "80%"),
    ]

    CodesTable(List<CodesRow> rows) {
        super(COLUMNS, rows)
    }

    @Override
    protected StandardTable<CodesRow> cloneWithRows(List<CodesRow> rows) {
        new CodesTable(rows)
    }
}

class CodesRow extends StandardRow {
    final String code
    final String description
    final boolean isHtml

    CodesRow(String code, String description, boolean isHtml) {
        this(code, description, isHtml, DiffStatus.NONE)
    }

    CodesRow(String code, String description, boolean isHtml, DiffStatus diffStatus) {
        super(diffStatus)

        this.code = code
        this.description = description
        this.isHtml = isHtml
    }

    @Override
    String getDiscriminator() {
        "${code}: ${description}"
    }

    @Override
    Strow generateDita(PublishContext context) {
        String outputClass = PublishHelper.getDiffCssClass(diffStatus)

        String modifiedDescription = isHtml ? context.replaceLinksInString(description) : description

        Strow.build() {
            stentry(outputClass: outputClass) {
                txt this.code
            }
            stentry(outputClass: outputClass) {
                if (isHtml) {
                    div HtmlHelper.replaceHtmlWithDita(modifiedDescription)
                }
                else {
                    txt modifiedDescription
                }
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String diffOutputClass = PublishHelper.getDiffCssClass(diffStatus)

        String rowCssClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TABLE_STROW : null
        String entryCssClass = context.target == PublishTarget.WEBSITE
            ? HtmlConstants.CSS_TABLE_STENTRY
            : !diffOutputClass.empty
            ? diffOutputClass
            : null

        String modifiedDescription = isHtml ? context.replaceLinksInString(description) : description

        builder.tr(class: rowCssClass) {
            builder.td(class: entryCssClass) {
                mkp.yield(code)
            }
            builder.td(class: entryCssClass) {
                this.isHtml ? mkp.yieldUnescaped(modifiedDescription) : mkp.yield(modifiedDescription)
            }
        }
    }

    @Override
    protected StandardRow cloneWithDiff(DiffStatus diffStatus) {
        new CodesRow(this.code, this.description, this.isHtml, diffStatus)
    }
}