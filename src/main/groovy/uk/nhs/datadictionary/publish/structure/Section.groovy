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
import org.maurodata.dita.elements.langref.base.Topic
import org.maurodata.dita.meta.DitaElement
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.PublishTarget

abstract class Section implements DitaAware<DitaElement>, HtmlAware, HtmlBuilder, DiffAware<Section, Section> {
    final DictionaryItem parent

    final String type
    final String title

    Section(DictionaryItem parent, String type, String title) {
        this.parent = parent
        this.type = type
        this.title = title
    }

    @Override
    DitaElement generateDita(PublishContext context) {
        // Generating the dita for a section is a bit annoying. This could either be a Topic or a Div, depending on
        // whether the output is for a website or a change paper. But the dita-dsl needs to know the parent object
        // to understand what child elements are supported, so we have to have two options for generating the same
        // contents
        if (context.target == PublishTarget.CHANGE_PAPER) {
            return generateDivDita(context)
        }

        String xrefId = "${parent.xrefId}_${type}"
        Topic.build(id: xrefId) {
            title title
            body generateBodyDita(context)
        }
    }

    protected abstract Body generateBodyDita(PublishContext context)

    protected abstract Div generateDivDita(PublishContext context)

    @Override
    String generateHtml(PublishContext context) {
        StringWriter writer = new StringWriter()
        MarkupBuilder builder = PublishHelper.createMarkupBuilder(writer, context.prettyPrintHtml)

        builder.div(class: HtmlConstants.CSS_TOPIC_BODY) {
            buildHtml(context, builder)
        }

        writer.toString()
    }
}
