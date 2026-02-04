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

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.domain.folder.Folder
import org.maurodata.security.AccessControlService
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDBusinessDefinition
import uk.nhs.datadictionary.NhsDDClass
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.NhsDDDataSetConstraint
import uk.nhs.datadictionary.NhsDDDataSetFolder
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.NhsDDSupportingInformation
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.api.NhsDataDictionaryApi
import uk.nhs.datadictionary.publish.changePaper.ChangePaperPreview
import uk.nhs.datadictionary.services.AttributeService
import uk.nhs.datadictionary.services.BusinessDefinitionService
import uk.nhs.datadictionary.services.ClassService
import uk.nhs.datadictionary.services.DataSetConstraintService
import uk.nhs.datadictionary.services.DataSetFolderService
import uk.nhs.datadictionary.services.DataSetService
import uk.nhs.datadictionary.services.ElementService
import uk.nhs.datadictionary.services.NhsDataDictionaryService
import uk.nhs.datadictionary.services.SupportingInformationService
import uk.nhs.datadictionary.utils.StereotypedCatalogueItem

import java.sql.DriverManager

@CompileStatic
@Controller()
@Secured(SecurityRule.IS_AUTHENTICATED)
@Slf4j
class NhsDataDictionaryController implements NhsDataDictionaryApi {

    @Inject ObjectMapper objectMapper

    @Inject NhsDataDictionaryService nhsDataDictionaryService

    @Inject ElementService elementService
    @Inject AttributeService attributeService
    @Inject ClassService classService
    @Inject BusinessDefinitionService businessDefinitionService
    @Inject SupportingInformationService supportingInformationService
    @Inject DataSetService dataSetService
    @Inject DataSetFolderService dataSetFolderService
    @Inject DataSetConstraintService dataSetConstraintService

    @Inject
    AccessControlService accessControlService


    @Get('/api/nhsdd/branches')
    List<Folder> branches() {
        nhsDataDictionaryService.branches()
    }

    @Get('/api/nhsdd/{dictionaryId}/publish/changePaper')
    HttpResponse<StreamedFile> generateChangePaper(UUID dictionaryId, @Nullable @QueryValue Boolean includeDataSets) {
        File f = nhsDataDictionaryService.generateChangePaper(dictionaryId, includeDataSets)
        return HttpResponse.ok(new StreamedFile(new ByteArrayInputStream(f.readBytes()), MediaType.ZIP_TYPE))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"${f.name}\"")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ZIP)
            .header("Access-Control-Expose-Headers", "Content-Disposition, Content-Length")
    }


    @Get('/api/nhsdd/{dictionaryId}/statistics')
    Map statistics(UUID dictionaryId) {
        nhsDataDictionaryService.buildDataDictionary(dictionaryId).statistics()
    }

    @Get('/api/nhsdd/{dictionaryId}/integrityChecks')
    List<LinkedHashMap<String, Object>> integrityChecks(UUID dictionaryId) {
        nhsDataDictionaryService.integrityChecks(dictionaryId).collect {integrityCheck, errors ->
            [
                checkName: integrityCheck.name,
                description: integrityCheck.description,
                errors: errors.collect {error -> [
                    type: error.component.getStereotype(),
                    label: error.component.name,
                    id: error.component.catalogueItem.id.toString(),
                    domainType: error.component.catalogueItem.domainType.toString(),
                    parentId: error.component.catalogueItem.parent.id,
                    modelId: error.component.catalogueItemParentId,
                    details: error.details
                ]}
            ]
        }
    }

    @Get('api/nhsdd/{dictionaryId}/preview/changePaper')
    ChangePaperPreview previewChangePaper(UUID dictionaryId, @Nullable @QueryValue Boolean includeDataSets) {
        nhsDataDictionaryService.previewChangePaper(dictionaryId, includeDataSets)

    }


    @Get('api/nhsdd/{dictionaryId}/preview/elements')
    List<StereotypedCatalogueItem> indexElements(UUID dictionaryId, @Nullable @QueryValue Boolean includeDeleted) {
        elementService.index(dictionaryId, nhsDataDictionaryService, includeDeleted)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/elements/{elementId}')
    NhsDDElement showElement(UUID dictionaryId, UUID elementId) {
        elementService.show(dictionaryId, elementId, nhsDataDictionaryService)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/elements/{elementId}/whereUsed')
    List<Map<String, Object>> elementWhereUsed(UUID dictionaryId, UUID elementId) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(dictionaryId)
        elementService.getWhereUsed(dataDictionary, elementId)
    }


    @Get('api/nhsdd/{dictionaryId}/preview/attributes')
    List<StereotypedCatalogueItem> indexAttributes(UUID dictionaryId, @Nullable @QueryValue Boolean includeDeleted) {
        attributeService.index(dictionaryId, nhsDataDictionaryService, includeDeleted)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/attributes/{attributeId}')
    NhsDDAttribute showAttribute(UUID dictionaryId, UUID attributeId) {
        attributeService.show(dictionaryId, attributeId, nhsDataDictionaryService)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/attributes/{attributeId}/whereUsed')
    List<Map<String, Object>> attributeWhereUsed(UUID dictionaryId, UUID attributeId) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(dictionaryId)
        attributeService.getWhereUsed(dataDictionary, attributeId)
    }


    @Get('api/nhsdd/{dictionaryId}/preview/classes')
    List<StereotypedCatalogueItem> indexClasses(UUID dictionaryId, @Nullable @QueryValue Boolean includeDeleted) {
        classService.index(dictionaryId, nhsDataDictionaryService, includeDeleted)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/classes/{classId}')
    NhsDDClass showClass(UUID dictionaryId, UUID classId) {
        classService.show(dictionaryId, classId, nhsDataDictionaryService)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/classes/{classId}/whereUsed')
    List<Map<String, Object>> classWhereUsed(UUID dictionaryId, UUID classId) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(dictionaryId)
        classService.getWhereUsed(dataDictionary, classId)
    }


    @Get('api/nhsdd/{dictionaryId}/preview/businessDefinitions')
    List<StereotypedCatalogueItem> indexBusinessDefinitions(UUID dictionaryId, @Nullable @QueryValue Boolean includeDeleted) {
        businessDefinitionService.index(dictionaryId, nhsDataDictionaryService, includeDeleted)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/businessDefinitions/{businessDefinitionId}')
    NhsDDBusinessDefinition showBusinessDefinition(UUID dictionaryId, UUID businessDefinitionId) {
        businessDefinitionService.show(dictionaryId, businessDefinitionId, nhsDataDictionaryService)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/businessDefinitions/{businessDefinitionId}/whereUsed')
    List<Map<String, Object>> businessDefinitionWhereUsed(UUID dictionaryId, UUID businessDefinitionId) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(dictionaryId)
        businessDefinitionService.getWhereUsed(dataDictionary, businessDefinitionId)
    }


    @Get('api/nhsdd/{dictionaryId}/preview/supportingInformation')
    List<StereotypedCatalogueItem> indexSupportingInformation(UUID dictionaryId, @Nullable @QueryValue Boolean includeDeleted) {
        supportingInformationService.index(dictionaryId, nhsDataDictionaryService, includeDeleted)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/supportingInformation/{supportingInformationId}')
    NhsDDSupportingInformation showSupportingInformation(UUID dictionaryId, UUID supportingInformationId) {
        supportingInformationService.show(dictionaryId, supportingInformationId, nhsDataDictionaryService)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/supportingInformation/{supportingInformationId}/whereUsed')
    List<Map<String, Object>> supportingInformationWhereUsed(UUID dictionaryId, UUID supportingInformationId) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(dictionaryId)
        supportingInformationService.getWhereUsed(dataDictionary, supportingInformationId)
    }


    @Get('api/nhsdd/{dictionaryId}/preview/dataSetConstraints')
    List<StereotypedCatalogueItem> indexDataSetConstraints(UUID dictionaryId, @Nullable @QueryValue Boolean includeDeleted) {
        dataSetConstraintService.index(dictionaryId, nhsDataDictionaryService, includeDeleted)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/dataSetConstraints/{dataSetConstraintId}')
    NhsDDDataSetConstraint showDataSetConstraint(UUID dictionaryId, UUID dataSetConstraintId) {
        dataSetConstraintService.show(dictionaryId, dataSetConstraintId, nhsDataDictionaryService)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/dataSetConstraints/{dataSetConstraintId}/whereUsed')
    List<Map<String, Object>> dataSetConstraintWhereUsed(UUID dictionaryId, UUID dataSetConstraintId) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(dictionaryId)
        dataSetConstraintService.getWhereUsed(dataDictionary, dataSetConstraintId)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/dataSets')
    List<StereotypedCatalogueItem> indexDataSets(UUID dictionaryId, @Nullable @QueryValue Boolean includeDeleted) {
        dataSetService.index(dictionaryId, nhsDataDictionaryService, includeDeleted)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/dataSets/{dataSetId}')
    NhsDDDataSet showDataSet(UUID dictionaryId, UUID dataSetId) {
        dataSetService.show(dictionaryId, dataSetId, nhsDataDictionaryService)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/dataSets/{dataSetId}/whereUsed')
    List<Map<String, Object>> dataSetWhereUsed(UUID dictionaryId, UUID dataSetId) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(dictionaryId)
        dataSetService.getWhereUsed(dataDictionary, dataSetId)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/dataSetFolders/root')
    NhsDDDataSetFolder indexDataSetFolders(UUID dictionaryId) {
        dataSetFolderService.show(dictionaryId, null, nhsDataDictionaryService)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/dataSetFolders/{dataSetFolderId}')
    NhsDDDataSetFolder indexDataSetFolders(UUID dictionaryId, UUID dataSetFolderId) {
        dataSetFolderService.show(dictionaryId, dataSetFolderId, nhsDataDictionaryService)
    }

    @Get('api/nhsdd/{dictionaryId}/preview/dataSetFolders/{dataSetFolderId}/whereUsed')
    List<Map<String, Object>> dataSetFolderWhereUsed(UUID dictionaryId, UUID dataSetFolderId) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(dictionaryId)
        dataSetFolderService.getWhereUsed(dataDictionary, dataSetFolderId)
    }


    @Get('api/nhsdd/{dictionaryId}/preview/allItemsIndex')
    List<StereotypedCatalogueItem> allItemsIndex(UUID dictionaryId) {
        nhsDataDictionaryService.allItemsIndex(dictionaryId)
    }

    @Get('api/admin/status')
    Map status() {
        accessControlService.checkAuthenticated()
        List<Map<String, Serializable>> databaseDrivers = DriverManager.getDrivers().asIterator().collect { driver ->
                return [
                    "class"  : driver.getClass().canonicalName,
                    "version": "${driver.majorVersion}.${driver.minorVersion}",
                ] as Map<String, Serializable>
            }

        return [
            'Mauro Data Mapper Version'       : '0.0.3-SNAPSHOT',
            'Java Version'                    : System.getProperty('java.version'),
            'Java Vendor'                     : System.getProperty('java.vendor'),
            'OS Name'                         : System.getProperty('os.name'),
            'OS Version'                      : System.getProperty('os.version'),
            'OS Architecture'                 : System.getProperty('os.arch'),
            'Driver Manager Drivers Available': databaseDrivers
        ]
    }

        /*
            @Transactional
            def newVersion() {
                log.debug("Creating a new version...")
                CatalogueUser currentUser = getCurrentUser()
                long startTime = System.currentTimeMillis()
                UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
                UUID newVersionedFolderId = nhsDataDictionaryService.newVersion(currentUser, versionedFolderId)
                log.debug(Utils.timeTaken(startTime))
                respond([newVersionedFolderId.toString()])
            }
        */
/*
    def previewChangePaper() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        boolean includeDataSets = params.boolean('includeDataSets') ?: false
        respond nhsDataDictionaryService.previewChangePaper(versionedFolderId, includeDataSets)
    }



    def integrityChecks() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond integrityChecks: nhsDataDictionaryService.integrityChecks(versionedFolderId)
    }

    def allItemsIndex() {
        respond nhsDataDictionaryService.allItemsIndex(UUID.fromString(params.versionedFolderId))
    }


    // Publication endpoints - generate documents

    def generateWebsite() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)

        DataDictionaryImportParameters parameters = new DataDictionaryImportParameters()
        //.fromParameters(params)
        File file = nhsDataDictionaryService.generateWebsite(versionedFolderId, publishOptions)
        header 'Access-Control-Expose-Headers', 'Content-Disposition'
        render(file: file, fileName: file.name, contentType: "application/zip")
    }


    def generateChangePaper() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        File file = nhsDataDictionaryService.generateChangePaper(versionedFolderId,
                                                                 params.boolean('dataSets')?: false,
                                                                 params.boolean('test')?: false)

        header 'Access-Control-Expose-Headers', 'Content-Disposition'
        render(file: file, fileName: file.name, contentType: "application/zip")
    }

    def codeSystemValidateBundle() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond nhsDataDictionaryService.codeSystemValidationBundle(versionedFolderId)
    }

    def valueSetValidateBundle() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond nhsDataDictionaryService.valueSetValidationBundle(versionedFolderId)
    }

    def shortDescriptions() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond nhsDataDictionaryService.shortDescriptions(versionedFolderId)
    }


    def diff() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        respond(nhsDataDictionaryService.diff(versionedFolderId))

    }

    def iso11179() {
        UUID versionedFolderId = UUID.fromString(params.versionedFolderId)
        String response = nhsDataDictionaryService.iso11179(versionedFolderId)
        render (text: response, contentType: "text/xml", encoding: "UTF-8")
    }
*/

}