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
package uk.nhs.datadictionary.publish.structure.datasets

import org.maurodata.dita.elements.langref.base.Table
import uk.nhs.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.datadictionary.publish.structure.DiffAware
import uk.nhs.datadictionary.publish.structure.DiffHierarchyAware
import uk.nhs.datadictionary.publish.structure.DiffObjectAware
import uk.nhs.datadictionary.publish.structure.DitaAware
import uk.nhs.datadictionary.publish.structure.HtmlBuilder


interface DataSetTable
    extends
        DitaAware<Table>,
        HtmlBuilder,
        DiffAware<DataSetTable, DataSetTable>,
        ChangeAware,
        DiffObjectAware<DataSetTable>,
        DiffHierarchyAware {
}