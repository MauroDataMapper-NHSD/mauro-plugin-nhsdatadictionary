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
package uk.nhs.digital.maurodatamapper.datadictionary.integritychecks

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataElement
import spock.lang.Specification
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.integritychecks.AllItemsAreWithinValidDateRange

import java.time.LocalDate

@MicronautTest(startApplication = true, environments = ['secured'])
@Property(name = "datasources.default.driver-class-name",
    value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.url",
    value = "jdbc:tc:postgresql:16-alpine:///db")
class AllItemsAreWithinValidDateRangeSpec extends Specification {

    LocalDate testDateNow = LocalDate.of(2024, 12, 31)

    def "validateDateRange returns false for valid dates"() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute(new DataElement(), UUID.randomUUID())

        component.addOtherProperties(["validFrom": "3030-12-31", "validTo": "3040-01-01"])

        expect:
        //False means it has not been added to the errors list as it did not fail the check
        checker.validateDateRange(component, testDateNow) == false
    }

    def "validateDateRange returns true for todate set before fromdate "() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute(new DataElement(), UUID.randomUUID())
        component.addOtherProperties(["validFrom": "2030-12-31", "validTo": "2030-12-01"])

        expect:
        checker.validateDateRange(component, testDateNow) == true
    }

    def "validateDateRange returns true when todate is set and fromdate is not"() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute(new DataElement(), UUID.randomUUID())
        component.addOtherProperties(["validTo": "2030-12-01"])

        expect:
        checker.validateDateRange(component, testDateNow) == true
    }

    def "validateDateRange returns true when todate has expired"() {
        given:
        def checker = new AllItemsAreWithinValidDateRange()
        def component = new NhsDDAttribute(new DataElement(), UUID.randomUUID())
        component.addOtherProperties(["validFrom": "2020-12-31", "validTo": "2020-01-01"])

        expect:
        checker.validateDateRange(component, testDateNow) == true
    }
}