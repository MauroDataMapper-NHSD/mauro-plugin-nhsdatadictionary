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
import org.maurodata.dita.elements.langref.base.XRef
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.PublishHelper
import uk.nhs.datadictionary.publish.changePaper.ChangeAware

class ExternalLink implements DitaAware<XRef>, HtmlBuilder, ChangeAware{
    final String name
    final String url
    final String outputClass

    final DiffStatus diffStatus

    ExternalLink(String name, String url, String outputClass) {
        this(name, url, outputClass, DiffStatus.NONE)
    }

    ExternalLink(String name, String url, String outputClass, DiffStatus diffStatus) {
        this.name = name
        this.url = url
        this.outputClass = outputClass
        this.diffStatus = diffStatus
    }

    @Override
    String getDiscriminator() {
        "${name}_${url}"
    }

    @Override
    XRef generateDita(PublishContext context) {
        PublishHelper.buildExternalXRef(url, name, outputClass)
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        builder.a(class: outputClass, title: name, href: url) {
            mkp.yield(name)
        }
    }
}
