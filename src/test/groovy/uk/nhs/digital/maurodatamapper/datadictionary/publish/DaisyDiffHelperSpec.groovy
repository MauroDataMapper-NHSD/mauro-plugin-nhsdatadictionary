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
package uk.nhs.digital.maurodatamapper.datadictionary.publish

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.eclipse.compare.rangedifferencer.RangeDifference
import spock.lang.Specification
import uk.nhs.datadictionary.publish.DaisyDiffHelper


@MicronautTest
class DaisyDiffHelperSpec extends Specification {


    void "should diff HTML with basic markup"() {
        given: "there is a source html content"
        String leftHtml = resolveFile("basic/left.txt")

        and: "there is a target html content"
        String rightHtml = resolveFile("basic/right.txt")

        when: "html is diffed"
        String diffHtml = DaisyDiffHelper.diff(leftHtml, rightHtml)

        then: "the expected html is returned"
        String expected = "<p>The name of an area of work.</p><p>An area of work is an area, function <span class=\"diff-html-removed\" id=\"removed-diff-0\" previous=\"first-diff\" changeId=\"removed-diff-0\" next=\"last-diff\">or specialty </span>where work activity takes place.</p>"
        verifyAll {
            diffHtml.trim() == expected.trim()
        }
    }

    void "should not diff HTML which includes tables"() {
        given: "there is a source html content"
        String leftHtml = resolveFile("complexHtmlTable/left.txt")

        and: "there is a target html content"
        String rightHtml = resolveFile("complexHtmlTable/right.txt")

        when: "html is checked"
        boolean leftContainsHtmlTable = DaisyDiffHelper.containsHtmlTable(leftHtml)
        boolean rightContainsHtmlTable = DaisyDiffHelper.containsHtmlTable(rightHtml)

        then: "the expected html is returned"
        verifyAll {
            leftContainsHtmlTable
            rightContainsHtmlTable
        }
    }

    void "should have no differences when HTML strings are identical"() {
        given: "there is a source html content"
        String leftHtml = resolveFile("htmlIdentical/left.txt")

        and: "there is a target html content"
        String rightHtml = resolveFile("htmlIdentical/right.txt")

        when: "differences are checked"
        RangeDifference[] differences = DaisyDiffHelper.calculateDifferences(leftHtml, rightHtml)

        then: "there are no differences"
        verifyAll {
            differences.size() == 0
        }
    }

    void "should have no differences when HTML strings contain whitespace"() {
        given: "there is a source html content"
        String leftHtml = resolveFile("htmlWithWhitespace/left.txt")

        and: "there is a target html content"
        String rightHtml = resolveFile("htmlWithWhitespace/right.txt")

        when: "differences are checked"
        RangeDifference[] differences = DaisyDiffHelper.calculateDifferences(leftHtml, rightHtml)

        then: "there are no differences"
        verifyAll {
            differences.size() == 0
        }
    }

    void "should have differences when HTML strings contain different content"() {
        given: "there is a source html content"
        String leftHtml = resolveFile("htmlDifferent/left.txt")

        and: "there is a target html content"
        String rightHtml = resolveFile("htmlDifferent/right.txt")

        when: "differences are checked"
        RangeDifference[] differences = DaisyDiffHelper.calculateDifferences(leftHtml, rightHtml)

        then: "there are differences"
        verifyAll {
            differences.size() > 0
        }
    }

    String resolveFile(String filename) {
        File file = new File(this.class.getClassLoader().getResource("html/" + filename).toURI())
        return new String(file.readBytes())
    }
}
