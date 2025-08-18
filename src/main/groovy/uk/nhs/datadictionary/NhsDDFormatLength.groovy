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
package uk.nhs.datadictionary

import groovy.xml.MarkupBuilder
import org.maurodata.dita.elements.langref.base.P
import org.maurodata.dita.elements.langref.base.XRef
import uk.nhs.datadictionary.publish.changePaper.ChangeAware

class NhsDDFormatLength implements ChangeAware {
    String text
    XRef xref

    static final NhsDDFormatLength EMPTY = new NhsDDFormatLength()

    NhsDDFormatLength(NhsDDElement element) {
        this.text = element.formatLength
        this.xref = element.formatLinkXref
    }

    private NhsDDFormatLength() {
    }

    boolean empty() {
        !text && !xref
    }

    @Override
    boolean equals(Object obj) {
        def other = obj as NhsDDFormatLength
        if (!other) {
            return false
        }

        if (this.empty() && !other.empty()) {
            return false
        }

        if (!this.empty() && other.empty()) {
            return false
        }

        this.discriminator == other.discriminator
    }

    @Override
    String getDiscriminator() {
        if (xref) {
            return "See ${xref.toXmlString()}"
        }

        if (text) {
            return text
        }

        return ""
    }

    static void writeChangeHtml(MarkupBuilder markupBuilder, NhsDDFormatLength formatLength, String outputClass = null) {
        if (!formatLength.empty()) {
            markupBuilder.p(class: outputClass) {
                // TODO: not sure what to output for HTML here, but just for preview
                mkp.yield(formatLength.discriminator)
            }
        }
    }

    static P buildChangeDita(NhsDDFormatLength formatLength, String outputClass = null) {
        if (formatLength.xref) {
            return P.build(outputClass: outputClass) {
                txt "See"
                xRef formatLength.xref
            }
        }

        P.build(outputClass: outputClass) {
            txt formatLength.text
        }
    }
}
