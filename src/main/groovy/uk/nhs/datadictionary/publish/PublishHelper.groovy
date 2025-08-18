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
package uk.nhs.datadictionary.publish

import groovy.xml.MarkupBuilder
import org.maurodata.dita.elements.langref.base.XRef
import org.maurodata.dita.enums.Scope
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.DiffStatus
import uk.nhs.datadictionary.publish.structure.HtmlConstants

class PublishHelper {
    static MarkupBuilder createMarkupBuilder(StringWriter writer, boolean prettyPrint = true) {
        MarkupBuilder builder = prettyPrint
            ? new MarkupBuilder(writer)
            : new MarkupBuilder(new IndentPrinter(writer, "", false))

        builder.omitEmptyAttributes = true
        builder.omitNullAttributes = true
        builder.doubleQuotes = true
        builder
    }

    static String createOfficialName(String name, DictionaryItem.DictionaryItemState state) {
        if (state == DictionaryItem.DictionaryItemState.RETIRED) {
            return "$name (Retired)"
        }

        name
    }

    static String createItemCssClass(String stereotype, DictionaryItem.DictionaryItemState state) {
        if (state == DictionaryItem.DictionaryItemState.RETIRED) {
            return "$stereotype retired"
        }

        stereotype
    }

    static String createXrefId(String stereotype, String name, DictionaryItem.DictionaryItemState state) {
        String encodedName = replaceNonAlphaNumerics(name)
        String retiredSuffix = state == DictionaryItem.DictionaryItemState.RETIRED ? "_retired" : ""
        String key = "${stereotype}_${encodedName}${retiredSuffix}".replace(" ", "_").toLowerCase()
        key
    }

    static String getDiffCssClass(DiffStatus status) {
        switch (status) {
            case DiffStatus.NEW:
                return HtmlConstants.CSS_DIFF_NEW
            case DiffStatus.REMOVED:
                return HtmlConstants.CSS_DIFF_DELETED
            default:
                return ""
        }
    }

    static String combineCssClassWithDiffStatus(String cssClass, DiffStatus diffStatus) {
        String diffOutputClass = getDiffCssClass(diffStatus)

        if (!cssClass || cssClass.empty) {
            return diffOutputClass
        }

        "${cssClass} ${diffOutputClass}".trim()
    }

    static String combineCssClasses(String... args) {
        args
            .findAll { val -> val != null && !val.empty }
            .join(" ")
            .trim()
    }

    static void buildHtmlParagraph(PublishContext context, MarkupBuilder builder, String text) {
        if (!text) {
            return
        }

        builder.p(class: context.paragraphCssClass) {
            mkp.yield(text)
        }
    }

    static XRef buildExternalXRef(String url, String text, String outputClass = null) {
        XRef.build(
            scope: Scope.EXTERNAL,
            format: "html",
            href: url,
            outputClass: outputClass
        ) {
            txt text
        }
    }

    private static String replaceNonAlphaNumerics(String value) {
        if (!value) {
            return ""
        }

        value.replaceAll("[^A-Za-z0-9- ]", "")
    }
}
