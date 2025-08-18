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
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget
import uk.nhs.datadictionary.publish.changePaper.Change

class ClassAttributeSection extends Section {
    final ClassAttributeTable table

    ClassAttributeSection(DictionaryItem parent, ClassAttributeTable table) {
        this(parent, "Attributes", table)
    }

    ClassAttributeSection(DictionaryItem parent, List<ClassAttributeRow> rows) {
        this(parent, new ClassAttributeTable(rows))
    }

    ClassAttributeSection(DictionaryItem parent, String title, ClassAttributeTable table) {
        super(parent, "attributes", title)

        this.table = table
    }

    @Override
    Section produceDiff(Section previous) {
        ClassAttributeSection previousSection = previous as ClassAttributeSection
        ClassAttributeTable diffTable = this.table.produceDiff(previousSection?.table) as ClassAttributeTable
        if (!diffTable) {
            return null
        }

        new ClassAttributeSection(this.parent, Change.CHANGED_ATTRIBUTES_TYPE, diffTable)
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        String paragraphOutputClass = context.target == PublishTarget.WEBSITE ? HtmlConstants.CSS_TOPIC_PARAGRAPH : ""

        if (table.empty) {
            builder.p(class: paragraphOutputClass) {
                mkp.yield("This class has no attributes")
            }
        }
        else {
            if (context.target == PublishTarget.CHANGE_PAPER) {
                builder.p(class: paragraphOutputClass) {
                    mkp.yield("Attributes of this Class are:")
                }
            }

            table.buildHtml(context, builder)
        }
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            if (table.empty) {
                p "This class has no attributes"
            }
            else {
                if (context.target == PublishTarget.CHANGE_PAPER) {
                    p "Attributes of this Class are:"
                }
                simpletable table.generateDita(context)
            }
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            if (table.empty) {
                p "This class has no attributes"
            }
            else {
                if (context.target == PublishTarget.CHANGE_PAPER) {
                    p "Attributes of this Class are:"
                }
                simpletable table.generateDita(context)
            }
        }
    }
}

class ClassAttributeTable extends StandardTable<ClassAttributeRow> {
    static final List<StandardColumn> COLUMNS = [
        new StandardColumn("Key", "1*", "5%"),
        new StandardColumn("Attribute Name", "9*", "95%"),
    ]

    ClassAttributeTable(List<ClassAttributeRow> rows) {
        super(COLUMNS, rows)
    }

    @Override
    protected StandardTable cloneWithRows(List<ClassAttributeRow> rows) {
        new ClassAttributeTable(rows)
    }
}

class ClassAttributeRow extends StandardRow {
    final boolean isKey
    final ItemLink attributeLink

    ClassAttributeRow(boolean isKey, ItemLink attributeLink) {
        this(isKey, attributeLink, DiffStatus.NONE)
    }

    ClassAttributeRow(boolean isKey, ItemLink attributeLink, DiffStatus diffStatus) {
        super(diffStatus)

        this.isKey = isKey
        this.attributeLink = attributeLink
    }

    @Override
    String getDiscriminator() {
        "${isKey ? 'key:' : ''}${attributeLink.name}"
    }

    @Override
    Strow generateDita(PublishContext context) {
        String outputClass = PublishHelper.getDiffCssClass(diffStatus)

        Strow.build() {
            stentry(outputClass: outputClass) {
                txt isKey ? 'Key' : ''
            }
            stentry(outputClass: outputClass) {
                xRef attributeLink.generateDita(context)
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

        builder.tr(class: rowCssClass) {
            builder.td(class: entryCssClass) {
                mkp.yield(isKey ? 'Key' : '')
            }
            builder.td(class: entryCssClass) {
                attributeLink.buildHtml(context, builder)
            }
        }
    }

    @Override
    protected StandardRow cloneWithDiff(DiffStatus diffStatus) {
        new ClassAttributeRow(this.isKey, this.attributeLink, diffStatus)
    }
}