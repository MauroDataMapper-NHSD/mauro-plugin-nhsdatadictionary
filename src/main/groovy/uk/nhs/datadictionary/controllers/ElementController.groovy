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
package uk.nhs.datadictionary.controllers

import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataElement
import uk.nhs.datadictionary.services.DataDictionaryComponentService
import uk.nhs.datadictionary.services.ElementService

@Controller()
@Secured(SecurityRule.IS_AUTHENTICATED)
@Slf4j

class ElementController extends DataDictionaryComponentController<DataElement>{

    @Inject ElementService elementService




    @Override
    String getParameterIdKey() {
        "elementId"
    }



    @Override
    DataDictionaryComponentService getService() {
        return elementService
    }
}
