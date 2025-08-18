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
package uk.nhs.datadictionary.publish.structure.datasets.cds

import groovy.xml.MarkupBuilder
import org.maurodata.dita.elements.langref.base.Body
import org.maurodata.dita.elements.langref.base.Div
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.datasets.output.html.CDSDataSetToHtml
import uk.nhs.datadictionary.publish.PublishContext
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.Section

/**
 * Special legacy case for CDS Data Sets. These are too complicated to rebuild right now, so use existing DITA/HTML generation code.
 * Does not support comparisons in change papers though.
 */
class LegacyCdsDataSetSection extends Section {
    final NhsDDDataSet cdsDataSet


    LegacyCdsDataSetSection(DictionaryItem parent, NhsDDDataSet cdsDataSet) {
        super(parent, "specification", "Specification")

        this.cdsDataSet = cdsDataSet
    }

    @Override
    Section produceDiff(Section previous) {
        // Cannot compare CDS data sets yet - do not use
        null
    }

    @Override
    protected Body generateBodyDita(PublishContext context) {
        Body.build() {
            this.cdsDataSet.sortedDataSetClasses.each { dataSetClass ->
                div dataSetClass.outputCDSClassAsDita(this.cdsDataSet.dataDictionary)
            }
        }
    }

    @Override
    protected Div generateDivDita(PublishContext context) {
        Div.build() {
            this.cdsDataSet.sortedDataSetClasses.each { dataSetClass ->
                div dataSetClass.outputCDSClassAsDita(this.cdsDataSet.dataDictionary)
            }
        }
    }

    @Override
    void buildHtml(PublishContext context, MarkupBuilder builder) {
        if (this.cdsDataSet.dataSetClasses.empty) {
            return
        }

        CDSDataSetToHtml cdsHtmlBuilder = new CDSDataSetToHtml(builder)
        cdsHtmlBuilder.outputAsCdsHtml(this.cdsDataSet)
    }
}
