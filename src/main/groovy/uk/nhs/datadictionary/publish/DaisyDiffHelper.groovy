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

import groovy.util.logging.Slf4j
import org.eclipse.compare.internal.LCSSettings
import org.eclipse.compare.rangedifferencer.RangeDifference
import org.eclipse.compare.rangedifferencer.RangeDifferencer
import org.outerj.daisy.diff.helper.NekoHtmlParser
import org.outerj.daisy.diff.html.HTMLDiffer
import org.outerj.daisy.diff.html.HtmlSaxDiffOutput
import org.outerj.daisy.diff.html.TextNodeComparator
import org.outerj.daisy.diff.html.dom.DomTreeBuilder
import org.xml.sax.InputSource

import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.sax.TransformerHandler
import javax.xml.transform.stream.StreamResult

@Slf4j
class DaisyDiffHelper {

    static String normalizeHtmlForDiff(String input) {
        if (!input) {
            return ""
        }

        // Keep entities as text so DaisyDiff does not need to diff synthetic inline tags.
        input
            .replace('\r\n', '\n')
            .replace('\r', '\n')
            .replace('®', '&reg;')
            .replaceAll('\\s+', ' ')
    }

    static String diff(String first, String second) throws Exception {
        String normalizedFirst = normalizeHtmlForDiff(first)
        String normalizedSecond = normalizeHtmlForDiff(second)

        String initialDiff = tryDiff(normalizedFirst, normalizedSecond)
        if (initialDiff != null) {
            return initialDiff
        }

        // Retry once with compacted tag spacing for edge cases in anchor/tag serialization.
        String retryFirst = compactTagWhitespace(normalizedFirst)
        String retrySecond = compactTagWhitespace(normalizedSecond)
        String retryDiff = tryDiff(retryFirst, retrySecond)
        if (retryDiff != null) {
            log.warn("DaisyDiff comparison succeeded on retry after compacting tag whitespace")
            return retryDiff
        }

        return ""
    }

    private static String tryDiff(String leftHtml, String rightHtml) {
        try {
            StringWriter finalResult = new StringWriter()
            SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance()
            TransformerHandler result = tf.newTransformerHandler()
            result.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            result.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes")
            result.getTransformer().setOutputProperty(OutputKeys.METHOD, "html")
            result.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            //result.getTransformer().setOutputProperty(OutputKeys.ENCODING, TestHelper.ENCODING);
            result.setResult(new StreamResult(finalResult))

            Locale locale = Locale.getDefault()

            NekoHtmlParser cleaner = new NekoHtmlParser()

            InputSource oldSource = new InputSource(new StringReader(leftHtml))
            InputSource newSource = new InputSource(new StringReader(rightHtml))

            DomTreeBuilder oldHandler = new DomTreeBuilder()
            cleaner.parse(oldSource, oldHandler)
            TextNodeComparator leftComparator = new TextNodeComparator(oldHandler, locale)

            DomTreeBuilder newHandler = new DomTreeBuilder()
            cleaner.parse(newSource, newHandler)
            TextNodeComparator rightComparator = new TextNodeComparator(newHandler, locale)

            HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(result, "diff")

            HTMLDiffer differ = new HTMLDiffer(output)
            differ.diff(leftComparator, rightComparator)

            return finalResult.toString().replaceAll(" changes=\"[^\"]*\"", "")
        } catch(Throwable e) {
            log.warn("Failed DaisyDiff comparison", e)
            log.warn("Left HTML: {}", leftHtml)
            log.warn("Right HTML: {}", rightHtml)
            return ""
        }
    }

    private static String compactTagWhitespace(String input) {
        input.replaceAll(/<\s+/, "<")
            .replaceAll(/\s+>/, ">")
            .replaceAll(/\s{2,}/, " ")
    }

    static boolean containsHtmlTable(String source) {
        // Not elegant, but should work...
        source.contains("<table")
    }

    static RangeDifference[] calculateDifferences(String first, String second) {
        try {
            Locale locale = Locale.getDefault()

            NekoHtmlParser cleaner = new NekoHtmlParser()

            InputSource oldSource = new InputSource(new StringReader(normalizeHtmlForDiff(first)))
            InputSource newSource = new InputSource(new StringReader(normalizeHtmlForDiff(second)))

            DomTreeBuilder oldHandler = new DomTreeBuilder()
            cleaner.parse(oldSource, oldHandler)
            TextNodeComparator leftComparator = new TextNodeComparator(oldHandler, locale)

            DomTreeBuilder newHandler = new DomTreeBuilder()
            cleaner.parse(newSource, newHandler)
            TextNodeComparator rightComparator = new TextNodeComparator(newHandler, locale)

            LCSSettings settings = new LCSSettings()
            settings.setUseGreedyMethod(false)
            // settings.setPowLimit(1.5);
            // settings.setTooLong(100000*100000);

            RangeDifference[] differences = RangeDifferencer.findDifferences(settings, leftComparator, rightComparator)
            differences
        } catch (Throwable e) {
            log.warn("Failed DaisyDiff difference calculation", e)
            [new RangeDifference(RangeDifference.CHANGE, 0, 1, 0, 1)] as RangeDifference[]
        }
    }
}
