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
package uk.nhs.datadictionary.services

import com.fasterxml.jackson.annotation.JsonIgnore
import io.micronaut.context.ApplicationContext
import org.maurodata.api.model.ModelVersionedRefDTO
import org.maurodata.controller.folder.VersionedFolderController
import org.maurodata.domain.terminology.Term
import org.maurodata.iso11179.domain.MetadataBundle

import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.commons.io.FileUtils
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.datamodel.DataClassRepository
import org.maurodata.persistence.datamodel.DataElementRepository
import org.maurodata.persistence.datamodel.DataModelRepository
import org.maurodata.persistence.datamodel.DataTypeRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.folder.FolderRepository
import org.maurodata.persistence.terminology.TerminologyRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams
import uk.nhs.datadictionary.DataDictionaryImportParameters
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDDBranch
import uk.nhs.datadictionary.NhsDDBusinessDefinition
import uk.nhs.datadictionary.NhsDDClass
import uk.nhs.datadictionary.NhsDDClassRelationship
import uk.nhs.datadictionary.NhsDDDataSet
import uk.nhs.datadictionary.NhsDDDataSetConstraint
import uk.nhs.datadictionary.NhsDDDataSetFolder
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.NhsDDSupportingInformation
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.fhir.FhirBundle
import uk.nhs.datadictionary.fhir.FhirEntry
import uk.nhs.datadictionary.integritychecks.IntegrityCheck
import uk.nhs.datadictionary.integritychecks.IntegrityCheckError
import uk.nhs.datadictionary.publish.MauroCatalogueItemPathResolver
import uk.nhs.datadictionary.publish.changePaper.ChangePaperHtmlUtility
import uk.nhs.datadictionary.publish.changePaper.ChangePaperPdfUtility
import uk.nhs.datadictionary.publish.changePaper.ChangePaperPreview
import uk.nhs.datadictionary.services.profiles.DDWorkItemProfileProviderService
import uk.nhs.datadictionary.utils.StereotypedCatalogueItem

import java.lang.reflect.Parameter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Slf4j
@Singleton
class NhsDataDictionaryService {

    @Inject
    List<IntegrityCheck> integrityChecks

    static final Map<String, String> KNOWN_KEYS = [
        (API_PROPERTY_RETIRED_TEMPLATE): '<p>This item has been retired from the NHS Data Model and Dictionary.</p>' +
                            '<p>The last version of this item is available in the ?????? release of the NHS Data Model and Dictionary.</p>' +
                           '<p>Access to the last live version of this item can be obtained by emailing <a href=\"mailto:support.digitalservices@nhs' +
                           '.net\">support.digitalservices@nhs.net</a> with "NHS Data Model and Dictionary - Archive Request" in the email subject ' +
                            'line.</p>',
        (API_PROPERTY_PREPARATORY_TEMPLATE): '<p><b>This item is being used for development purposes and has not yet been approved.</b></p>',
        (API_PROPERTY_CHANGE_LOG_CHANGE_REQUEST_URL): "https://mauro.dataproducts.nhs.uk/changerequest/$NhsDataDictionary.CHANGE_REQUEST_NUMBER_TOKEN",
        (API_PROPERTY_CHANGE_LOG_HEADER_TEXT): "<p>Click on the links below to view the change requests this item is part of:</p>",
        (API_PROPERTY_CHANGE_LOG_FOOTER_TEXT): "<p>Click <a href='https://www.datadictionary.nhs.uk/archive'>here</a> to see the Change Log Information for changes before January 2025.</p>"
    ]

    static final String NHSDD_PROPERTY_CATEGORY = 'NHS Data Dictionary'
    static final String API_PROPERTY_RETIRED_TEMPLATE = 'retired.template'
    static final String API_PROPERTY_PREPARATORY_TEMPLATE = 'preparatory.template'
    static final String API_PROPERTY_CHANGE_LOG_CHANGE_REQUEST_URL = 'changelog.url.changerequest'
    static final String API_PROPERTY_CHANGE_LOG_HEADER_TEXT = 'changelog.headertext'
    static final String API_PROPERTY_CHANGE_LOG_FOOTER_TEXT = 'changelog.footertext'

    @Inject
    MauroPersistenceService mauroPersistenceService

    @Inject
    ItemCacheableRepository.ApiPropertyCacheableRepository apiPropertyCacheableRepository

    @Inject
    FolderRepository folderRepository

    @Inject
    TerminologyRepository terminologyRepository

    @Inject
    AdministeredItemCacheableRepository.TermCacheableRepository termCacheableRepository

    @Inject
    DataElementRepository dataElementRepository

    @Inject
    DataTypeRepository dataTypeRepository

    @Inject
    DataClassRepository dataClassRepository

    @Inject
    MetadataRepository metadataRepository


    @Inject
    DataModelRepository dataModelRepository

    @Inject
    DataSetService dataSetService

    @Inject
    ClassService classService

    @Inject
    ElementService elementService

    @Inject
    AttributeService attributeService

    @Inject
    BusinessDefinitionService businessDefinitionService

    @Inject
    SupportingInformationService supportingInformationService

    @Inject
    DataSetConstraintService dataSetConstraintService

    @Inject
    DataSetFolderService dataSetFolderService

    @Inject
    DDWorkItemProfileProviderService ddWorkItemProfileProviderService

    @Inject
    ContentsService contentsService

    @Inject VersionedFolderController versionedFolderController

    @Inject
    ApplicationContext applicationContext

    @Inject ChangePaperHtmlUtility changePaperHtmlUtility


    List<Folder> branches(/*UserSecurityPolicyManager userSecurityPolicyManager */) {
        folderRepository.readAll().findAll {
            it.label.startsWith("NHS Data Dictionary")
        }

/*
        Folder oldestAncestor = versionedFolderService.findOldestAncestor(versionedFolders[0])

        // List<VersionTreeModel>
        Map versionTreeModelList = versionedFolderService.buildModelVersionTree(
            oldestAncestor,
            null,
            null,
            true,
            false,
            userSecurityPolicyManager)
        return versionTreeModelList
*/
    }
    ListResponse<StereotypedCatalogueItem> allItems(UUID versionedFolderId, String prefix = "", PaginationParams paginationParams = new PaginationParams()){
        // Going to need to build this list quicker than building the whole contents
        List<StereotypedCatalogueItem> response = []
        Folder versionedFolder = folderRepository.readById(versionedFolderId)
        long timestamp = System.currentTimeMillis()
        List<Terminology> terminologies = terminologyRepository.readAllByFolderIdIn([versionedFolderId])
        System.err.println("1: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()
        Terminology supportingInformationTerminology = terminologies
            .find {it.label == NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}
        List<Term> terms = termCacheableRepository.readAllByTerminologyIdIn([supportingInformationTerminology.id])
        System.err.println("2: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()
        response.addAll(terms.collect {new StereotypedCatalogueItem(it, supportingInformationService.stereotype)})

        Terminology businessDefinitionTerminology = terminologies
            .find {it.label == NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}
        terms = termCacheableRepository.readAllByTerminologyIdIn([businessDefinitionTerminology.id])
        System.err.println("3: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()

        response.addAll(terms.collect {new StereotypedCatalogueItem(it, businessDefinitionService.stereotype)})

        Terminology xmlSchemaConstraintTerminology = terminologies
            .find {it.label == NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}
        terms = termCacheableRepository.readAllByTerminologyIdIn([xmlSchemaConstraintTerminology.id])
        System.err.println("4: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()

        response.addAll(terms.collect {new StereotypedCatalogueItem(it, dataSetConstraintService.stereotype)})

        List<DataModel> dataModels = dataModelRepository.findAllByFolderId(versionedFolderId)
        System.err.println("5: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()
        DataModel elementsDataModel = dataModels.find {it.label == NhsDataDictionary.ELEMENTS_MODEL_NAME}
        List<DataElement> elementDataElements = dataElementRepository.readAllByDataClassDataModelIdInAndLabelContains([elementsDataModel.id], prefix?:"")
        System.err.println("6: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()

        response.addAll(elementDataElements.collect {new StereotypedCatalogueItem(it, elementService.stereotype)})

        DataModel classesDataModel = dataModels.find {it.label == NhsDataDictionary.CLASSES_MODEL_NAME}
        List<DataElement> attributeDataElements = dataElementRepository.readAllByDataClassDataModelIdInAndLabelContains ([classesDataModel.id], prefix?:"")
        System.err.println("7: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()

        List<DataType> dataTypes = dataTypeRepository.readAllByDataModel(classesDataModel)
        Map<UUID, DataType> dataTypeMap  = dataTypes.collectEntries {[(it.id): it]}

        response.addAll(attributeDataElements.findAll {dataElement ->
            dataTypeMap[dataElement.dataType.id].dataTypeKind != DataType.DataTypeKind.REFERENCE_TYPE
        }.collect {new StereotypedCatalogueItem(it, attributeService.stereotype)})

        List<DataClass> classDataClasses = dataClassRepository.readAllByDataModel (classesDataModel)
        System.err.println("8: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()

        response.addAll(classDataClasses.collect {new StereotypedCatalogueItem(it, classService.stereotype)})

        List<Folder> allDataSetFolders = []
        List<Folder> nextFolders = []
        do {
            if(nextFolders.isEmpty()) {
                nextFolders = folderRepository.readAllByFolderIdIn([versionedFolderId])
            } else {
                nextFolders = folderRepository.readAllByFolderIdIn(nextFolders.id)
            }
            allDataSetFolders.addAll(nextFolders)
        } while(!nextFolders.isEmpty())
        response.addAll(nextFolders.collect {new StereotypedCatalogueItem(it, dataSetFolderService.stereotype)})

        System.err.println("10: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()

        List<DataModel> dataSets = dataModelRepository.readAllByFolderIdIn(allDataSetFolders.id)
        response.addAll(dataSets.collect {new StereotypedCatalogueItem(it, dataSetService.stereotype)})

        System.err.println("11: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()


        paginationParams.max = paginationParams.max ?: 100

        ListResponse<StereotypedCatalogueItem> listResponse = ListResponse.from(
            response
            .findAll {!prefix || it.name.toLowerCase().contains(prefix.toLowerCase())}
            .sort {it.name.toLowerCase()}, paginationParams) as ListResponse<StereotypedCatalogueItem>

        // speed up the response time
        listResponse.items.each {it.description = null}

        System.err.println("12: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()

        Map<UUID, StereotypedCatalogueItem> itemResponseMap = listResponse.items.collectEntries {[(it.catalogueItemId): it]}

        Set<Metadata> metadata = metadataRepository.readAllByMultiFacetAwareItemIdIn(listResponse.items.catalogueItemId)

        System.err.println("13: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()

        metadata.each {md ->
            if(md.key == 'isRetired' && md.value == 'true') {
                itemResponseMap[md.multiFacetAwareItemId].retired = true
            }
        }

        System.err.println("14: ${System.currentTimeMillis() - timestamp}")
        timestamp = System.currentTimeMillis()

        return listResponse

    }



    Map<IntegrityCheck, List<IntegrityCheckError>> integrityChecks(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)

        integrityChecks.findAll {it.enabled}
            .collectEntries {integrityCheck ->
                [integrityCheck, integrityCheck.runCheck(dataDictionary).sort {it.component.name }]
            }
    }


    NhsDataDictionary buildDataDictionary(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = newDataDictionary(versionedFolderId)
        Folder contentsFolder = (Folder) folderRepository.loadWithContent(versionedFolderId)
        dataDictionary.containingVersionedFolder = contentsFolder

        buildWorkItemDetails(dataDictionary.containingVersionedFolder, dataDictionary)

        Terminology busDefTerminology = contentsFolder.terminologies.find {it.label == NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME }
        Terminology supDefTerminology = contentsFolder.terminologies.find {it.label == NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}
        Terminology dataSetConstraintsTerminology = contentsFolder.terminologies.find {it.label == NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}

        Folder dataSetsFolder = contentsFolder.childFolders.find {it.label == NhsDataDictionary.DATA_SETS_FOLDER_NAME}

        DataModel classesModel = contentsFolder.dataModels.find {it.label == NhsDataDictionary.CLASSES_MODEL_NAME}

        DataModel elementsModel = contentsFolder.dataModels.find {it.label == NhsDataDictionary.ELEMENTS_MODEL_NAME}

        if(classesModel) {
            dataDictionary.getTermsForAttributes()
            addAttributesToDictionary(classesModel, dataDictionary)
            addClassesToDictionary(classesModel, dataDictionary)
        } else {
            log.error("No classes model found")
        }
        if(elementsModel) {
            dataDictionary.getTermsForElements()
            addElementsToDictionary(elementsModel, dataDictionary)
        } else {
            log.error("No elements model found")
        }
        if(dataSetsFolder) {
            addDataSetFoldersToDictionary(dataSetsFolder, dataDictionary)
            addDataSetsToDictionary(dataSetsFolder, dataDictionary)
        } else {
            log.error("No datasets folder found")
        }
        if(busDefTerminology) {
            addBusDefsToDictionary(busDefTerminology, dataDictionary)
        } else {
            log.error("No business definitions terminology found")
        }
        if(supDefTerminology) {
            addSupDefsToDictionary(supDefTerminology, dataDictionary)
        } else {
            log.info("No supporting definitions terminology found")
        }
        if(dataSetConstraintsTerminology) {
            addDataSetConstraintsToDictionary(dataSetConstraintsTerminology, dataDictionary)
        } else {
            log.error("No dataset constraints terminology found")
        }

        dataDictionary.buildInternalLinks()

        return dataDictionary
    }

    Terminology getBusinessDefinitionTerminology(UUID versionedFolderId) {
        List<Terminology> terminologies = terminologyRepository.findAllByFolderId(versionedFolderId)
        terminologies.find {it.label == NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}
    }

    Terminology getSupportingInformationTerminology(UUID versionedFolderId) {
        List<Terminology> terminologies = terminologyRepository.findAllByFolderId(versionedFolderId)
        terminologies.find {it.label == NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}
    }

    Terminology getDataSetConstraintTerminology(UUID versionedFolderId) {
        List<Terminology> terminologies = terminologyRepository.findAllByFolderId(versionedFolderId)
        terminologies.find {it.label == NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}
    }

    DataModel getElementsModel(UUID versionedFolderId) {
        List<DataModel> dataModels = dataModelRepository.findAllByFolderId(versionedFolderId)
        dataModels.find {it.label == NhsDataDictionary.ELEMENTS_MODEL_NAME}
    }

    DataModel getClassesModel(UUID versionedFolderId) {
        List<DataModel> dataModels = dataModelRepository.findAllByFolderId(versionedFolderId)
        dataModels.find {it.label == NhsDataDictionary.CLASSES_MODEL_NAME}
    }

    Folder getDataSetsFolder(UUID versionedFolderId) {
        List<Folder> childFolders = folderRepository.findAllByFolderId(versionedFolderId)
        childFolders.find {it.label == NhsDataDictionary.DATA_SETS_FOLDER_NAME}
    }

    void addAttributesToDictionary(DataModel classesModel, NhsDataDictionary dataDictionary) {
        Set<DataElement> attributeElements = classesModel.dataElements.findAll {
            !(it.dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE)
        }
        dataDictionary.attributes =
            attributeElements.collectEntries {ci ->
                 [ci.label, new NhsDDAttribute(ci).fromMauroItem(dataDictionary, mauroPersistenceService, ci)]
            }
        dataDictionary.attributes.values().each { ddAttribute ->
            dataDictionary.attributesByCatalogueId[ddAttribute.getCatalogueItem().id] = ddAttribute
        }
    }

    void addElementsToDictionary(DataModel elementsModel, NhsDataDictionary dataDictionary) {
        Set<DataElement> elementElements = elementsModel.dataElements

        dataDictionary.elements = elementElements.collectEntries {ci ->
                [ci.label, new NhsDDElement(ci).fromMauroItem(dataDictionary, mauroPersistenceService, ci)]
            }
        dataDictionary.elements.values().each { ddElement ->
            dataDictionary.elementsByCatalogueId[ddElement.getCatalogueItem().id] = ddElement
        }
    }

    void addClassesToDictionary(DataModel classesModel, NhsDataDictionary dataDictionary) {
        //DataClass classesClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.DATA_CLASSES_CLASS_NAME}
        //DataClass retiredClassesClass = classesClass.dataClasses.find {it.label == "Retired"}
        Set<DataClass> classClasses = classesModel.dataClasses.collect() as Set
        classClasses.addAll(classClasses.find {it.label == "Retired"}.dataClasses)
        classClasses.removeAll {it.label == "Retired"}
        dataDictionary.classes = classClasses.collectEntries {ci ->
            [ci.label, new NhsDDClass(ci).fromMauroItem(dataDictionary, mauroPersistenceService, ci)]
        }
        dataDictionary.classes.values().each { ddClass ->
            dataDictionary.classesByCatalogueId[ddClass.getCatalogueItem().id] = ddClass
        }

        // Now link associations
        dataDictionary.classes.values().each { dataClass ->
            ((DataClass)dataClass.catalogueItem).dataElements.each { dataElement ->
                if(dataElement.dataType.dataTypeKind == DataType.DataTypeKind.REFERENCE_TYPE) {
                    DataClass referencedClass = ((DataType)dataElement.dataType).referenceClass
                    dataClass.classRelationships.add(new NhsDDClassRelationship(dataElement, dataDictionary.classes[referencedClass.label]))
                } else {
                    NhsDDAttribute attribute = dataDictionary.attributesByCatalogueId[dataElement.id]
                    if (attribute) {
                        attribute.parentClass = dataClass
                        if (attribute.otherProperties['isKey']) {
                            dataClass.keyAttributes.add(attribute)
                        } else {
                            dataClass.otherAttributes.add(attribute)
                        }
                    }
                    else {
                        log.warn("Cannot find link association for NhsDDAttribute based on Mauro data element '$dataElement.label' [$dataElement.id]")
                    }
                }
            }
            ((DataClass)dataClass.catalogueItem).extendsDataClasses.each { extendedDataClass ->
                dataClass.extendsClasses.add(dataDictionary.classesByCatalogueId[extendedDataClass.id])
            }

            // TODO:  Sort key and non-key attributes here...
            //((DataClass)dataClass.catalogueItem).dataElements.each { dataElement ->
            //}
        }

    }


    void addDataSetsToDictionary(Folder dataSetsFolder, NhsDataDictionary dataDictionary) {
        Map<List<String>, Set<DataModel>> dataSetModelMap = dataSetService.getAllDataSets([], dataSetsFolder, true)
        dataSetModelMap.each {path, dataModels ->
            dataModels.each { dataModel ->
                NhsDDDataSet dataSet = new NhsDDDataSet(dataModel).fromMauroItem(dataDictionary, mauroPersistenceService, dataModel)
                dataSet.path.addAll(path)
                dataDictionary.dataSets[dataModel.label] = dataSet
                List<String> folderPath = []
                folderPath.addAll(path)
                String parentFolderName = folderPath.removeLast()
                NhsDDDataSetFolder folder = dataDictionary.dataSetFolders[folderPath].find {it.name == parentFolderName }
                folder.dataSets.add(dataSet)
            }
        }
    }

    void addDataSetFoldersToDictionary(Folder dataSetsFolder, NhsDataDictionary dataDictionary) {
        Map<List<String>, Set<Folder>> dataSetFolders = dataSetFolderService.getAllFolders([], dataSetsFolder)

        dataSetFolders.each { path, folders ->
            folders.each {folder ->
                NhsDDDataSetFolder dataSetFolder = new NhsDDDataSetFolder(folder).fromMauroItem(dataDictionary, mauroPersistenceService, folder)
                dataSetFolder.setPath(path)
                if(dataDictionary.dataSetFolders[path]) {
                    dataDictionary.dataSetFolders[path].add(dataSetFolder)
                } else {
                    dataDictionary.dataSetFolders[path] = [dataSetFolder]
                }
            }
        }

        dataDictionary.dataSetFolders.each { path, dataSetFoldersAtPath ->
            dataSetFoldersAtPath.each { dataSetFolder ->
                List<String> childPath = []
                childPath.addAll(path)
                childPath.add(dataSetFolder.name)

                dataDictionary.dataSetFolders[childPath].each {childDataSetFolder ->
                    dataSetFolder.childFolders.add(childDataSetFolder)
                }
            }

        }
    }


    void addBusDefsToDictionary(Terminology busDefsTerminology, NhsDataDictionary dataDictionary) {
        dataDictionary.businessDefinitions =
            busDefsTerminology.terms.collectEntries {ci ->
                [ci.label, new NhsDDBusinessDefinition(ci).fromMauroItem(dataDictionary, mauroPersistenceService, ci)]
            }
    }

    void addSupDefsToDictionary(Terminology supDefsTerminology, NhsDataDictionary dataDictionary) {
        dataDictionary.supportingInformation =
            supDefsTerminology.terms.collectEntries {ci ->
                [ci.label, new NhsDDSupportingInformation(ci).fromMauroItem(dataDictionary, mauroPersistenceService, ci)]
            }
        }

    void addDataSetConstraintsToDictionary(Terminology dataSetConstraintsTerminology, NhsDataDictionary dataDictionary) {
        dataDictionary.dataSetConstraints =
            dataSetConstraintsTerminology.terms.collectEntries {ci ->
                [ci.label, new NhsDDDataSetConstraint(ci).fromMauroItem(dataDictionary, mauroPersistenceService, ci)]
            }
    }

/*
    Set<DataClass> getDataClasses(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(NhsDataDictionary.CORE_MODEL_NAME)

        DataClass classesClass = coreModel.dataClasses.find {it.label == NhsDataDictionary.DATA_CLASSES_CLASS_NAME}

        Set<DataClass> classClasses = []
        classClasses.addAll(classesClass.dataClasses.findAll {it.label != "Retired"})
        if (includeRetired) {
            DataClass retiredClassesClass = classesClass.dataClasses.find {it.label == "Retired"}
            classClasses.addAll(retiredClassesClass.dataClasses)
        }

        return classClasses

    }

    Set<DataElement> getDataElements(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(NhsDataDictionary.ELEMENTS_MODEL_NAME)

        return coreModel.getAllDataElements().findAll{
            includeRetired || it.parent.label == "Retired"
        }
    }

    Set<DataElement> getAttributes(includeRetired = false) {
        DataModel coreModel = dataModelService.findCurrentMainBranchByLabel(NhsDataDictionary.CLASSES_MODEL_NAME)

        return coreModel.getAllDataElements().findAll{
            includeRetired || it.parent.label == "Retired"
        }

    }
*/

    List<StereotypedCatalogueItem> allItemsIndex(UUID versionedFolderId) {
        List<StereotypedCatalogueItem> allItems = []
        [elementService, dataSetService, classService, attributeService, businessDefinitionService, supportingInformationService, dataSetConstraintService]
            .each {service ->
                allItems.addAll(service.index(versionedFolderId, this, true))
        }
        return allItems.sort {it.name.toLowerCase()}
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }


    void deleteOriginalFolder(String coreFolderName) {
        Folder originalFolder = folderRepository.readAll().find {
            it.label == coreFolderName
        }
        if(originalFolder) {
            folderRepository.delete(originalFolder)
        }
    }

    FhirBundle codeSystemValidationBundle(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)
        String publishDate = Instant.now().toString()
        List<FhirEntry> entries = dataDictionary.attributes.values()
            .findAll { attribute ->
                !attribute.isRetired() &&
                    attribute.codes.size() > 0
            }
            .sort { it.name }
            .collect {attribute ->
                new FhirEntry( requestUrl: 'CodeSystem/$validate',
                               resource: FhirCodeSystem.fromDDAttribute(attribute, "1.0.0", publishDate))
            }

        return new FhirBundle(type: "transaction", entries: entries)
    }

    FhirBundle valueSetValidationBundle(UUID versionedFolderId) {
        NhsDataDictionary dataDictionary = buildDataDictionary(versionedFolderId)
        String publishDate = Instant.now().toString()
        List<FhirEntry> entries = dataDictionary.elements.values()
            .findAll { element ->
                !element.isRetired() &&
                    element.codes.size() > 0
            }
            .sort { it.name }
            .collect { element ->
                new FhirEntry( requestUrl: 'ValueSet/$validate',
                               resource: FhirValueSet.fromDDElement(element, "1.0.0", publishDate))
            }
        return new FhirBundle(type: "transaction", entries: entries)
    }

    String iso11179(UUID versionedFolderId) {
        Folder thisDictionary = versionedFolderService.get(versionedFolderId)
        NhsDataDictionary thisDataDictionary = buildDataDictionary(thisDictionary.id)

        MetadataBundle metadataBundle = ISO11179Helper.generateMetadataBundle(thisDataDictionary)

        return MarshalHelper.marshallMetadataBundleToString(metadataBundle)
    }


    ChangePaperPreview previewChangePaper(UUID versionedFolderId, boolean includeDataSets = false) {
        NhsDataDictionary thisDataDictionary = buildDataDictionary(versionedFolderId)
        ModelVersionedRefDTO modelVersionedRefDTO = versionedFolderController.latestFinalisedModel(versionedFolderId)
        NhsDataDictionary previousDataDictionary = buildDataDictionary(modelVersionedRefDTO.id)

        MauroCatalogueItemPathResolver pathResolver = applicationContext.createBean(MauroCatalogueItemPathResolver)
        pathResolver.setVersionedFolderId(versionedFolderId)


        ChangePaperPreview preview = changePaperHtmlUtility.generateChangePaper(
            pathResolver,
            thisDataDictionary,
            previousDataDictionary,
            includeDataSets)

        preview
    }

    byte[] generateChangePaper(UUID versionedFolderId, boolean includeDataSets = false, boolean isTest = false) {

        NhsDataDictionary thisDataDictionary = buildDataDictionary(versionedFolderId)
        ModelVersionedRefDTO modelVersionedRefDTO = versionedFolderController.latestFinalisedModel(versionedFolderId)
        NhsDataDictionary previousDataDictionary = buildDataDictionary(modelVersionedRefDTO.id)

        Path outputPath = getTestOutputPath()
        if(!isTest) {
            outputPath = Files.createTempDirectory('changePaper')
        }
        return ChangePaperPdfUtility.generateChangePaper(thisDataDictionary, previousDataDictionary, outputPath, includeDataSets)
    }

/*
    File generateWebsite(UUID versionedFolderId, DataDictionaryImportParameters parameters) {
        Folder thisDictionary = versionedFolderService.get(versionedFolderId)
        NhsDataDictionary thisDataDictionary = buildDataDictionary(thisDictionary.id)
        thisDataDictionary.branchName = thisDictionary.finalised ? thisDictionary.modelVersionTag : thisDictionary.branchName

        //Path outputPath = Files.createTempDirectory('website')

        Path outputPath = getTestOutputPath()

        return WebsiteUtility.generateWebsite(thisDataDictionary, outputPath, parameters)
    }
*/
/*
    def shortDescriptions(UUID versionedFolderId) {
        Folder thisDictionary = versionedFolderService.get(versionedFolderId)
        NhsDataDictionary thisDataDictionary = buildDataDictionary(thisDictionary.id)

        List<String> response = []
        thisDataDictionary.getAllComponents().sort { it.name.toLowerCase()}.each { component ->
            String shortDescription = component.otherProperties["shortDescription"]
            response.add(""" "${component.getStereotype()}", "${component.getNameWithRetired()}", "${shortDescription}" """)
        }
        return response
    }
*/

    NhsDataDictionary newDataDictionary(UUID versionedFolderId) {
        NhsDataDictionary nhsDataDictionary = new NhsDataDictionary()
        setApiProperties(nhsDataDictionary)
        loadBranchInformation(nhsDataDictionary)
        nhsDataDictionary.containingVersionedFolder = folderRepository.findById(versionedFolderId)
        return nhsDataDictionary
    }

    void setApiProperties(NhsDataDictionary dataDictionary) {

        apiPropertyCacheableRepository.findAll().find {
            it.category == NHSDD_PROPERTY_CATEGORY
        }.each {apiProperty ->
            if(apiProperty.key == API_PROPERTY_RETIRED_TEMPLATE) {
                dataDictionary.retiredItemText = apiProperty.value
            }
            if(apiProperty.key == API_PROPERTY_PREPARATORY_TEMPLATE) {
                dataDictionary.preparatoryItemText = apiProperty.value
            }
            if (apiProperty.key == API_PROPERTY_CHANGE_LOG_CHANGE_REQUEST_URL) {
                dataDictionary.changeRequestUrl = apiProperty.value
            }
            if (apiProperty.key == API_PROPERTY_CHANGE_LOG_HEADER_TEXT) {
                dataDictionary.changeLogHeaderText = apiProperty.value
            }
            if (apiProperty.key == API_PROPERTY_CHANGE_LOG_FOOTER_TEXT) {
                dataDictionary.changeLogFooterText = apiProperty.value
            }
        }
        if(!dataDictionary.preparatoryItemText) {
            dataDictionary.preparatoryItemText = KNOWN_KEYS[API_PROPERTY_PREPARATORY_TEMPLATE]
        }
        if(!dataDictionary.retiredItemText) {
            dataDictionary.retiredItemText = KNOWN_KEYS[API_PROPERTY_RETIRED_TEMPLATE]
        }
        if (!dataDictionary.changeRequestUrl) {
            dataDictionary.changeRequestUrl = KNOWN_KEYS[API_PROPERTY_CHANGE_LOG_CHANGE_REQUEST_URL]
        }
        if (!dataDictionary.changeLogHeaderText) {
            dataDictionary.changeLogHeaderText = KNOWN_KEYS[API_PROPERTY_CHANGE_LOG_HEADER_TEXT]
        }
        if (!dataDictionary.changeLogFooterText) {
            dataDictionary.changeLogFooterText = KNOWN_KEYS[API_PROPERTY_CHANGE_LOG_FOOTER_TEXT]
        }
    }

    void loadBranchInformation(NhsDataDictionary dataDictionary) {

        List<Folder> dictionaryFolders = folderRepository.readAll().findAll {it.label.startsWith("NHS Data Dictionary") }

        if (dictionaryFolders.empty) {
            return
        }

        dictionaryFolders
            .collect {versionedFolder -> new NhsDDBranch(versionedFolder) }
            .findAll {branch -> !branch.finalised && branch.branchName }
            .each { branch ->
                dataDictionary.workItemBranches[branch.branchName] = branch
            }
    }

    def diff(UUID versionedFolderId) {
        Folder thisDictionary = versionedFolderService.get(versionedFolderId)

        Folder previousVersion = versionedFolderService.getFinalisedParent(thisDictionary)

        // Load the things into memory
        //NhsDataDictionary thisDataDictionary = buildDataDictionary(versionedFolderId)
        //NhsDataDictionary previousDataDictionary = buildDataDictionary(versionedFolderId)
        //ObjectDiff objectDiff = versionedFolderService.getDiffForVersionedFolders(thisDictionary, previousVersion)

        log.info('---------- Starting merge diff ----------')
        long start = System.currentTimeMillis()
        // TODO - fix this
        // MergeDiff<Folder> objectDiff = versionedFolderService.getMergeDiffForVersionedFolders(thisDictionary, previousVersion)
        log.info('Merge Diff took {}', Utils.timeTaken(start))

        return objectDiff
    }

    void buildWorkItemDetails(Folder thisVersionedFolder, NhsDataDictionary dataDictionary) {
        dataDictionary.workItemDetails =
            thisVersionedFolder.metadata.findAll {md ->
                md.namespace == 'uk.nhs.datadictionary.workItem' // ddWorkItemProfileProviderService.metadataNamespace
            }.collectEntries {md ->
                [md.key, md.value]
            }
    }

    static List<Metadata> defaultProfileMetadata() {
        return [
            new Metadata(namespace: NhsDataDictionary.DEFAULT_PROFILE_NAMESPACE,
                         key: 'namespace',
                         value: DDWorkItemProfileProviderService.getPackageName().toString()),
            new Metadata(namespace: NhsDataDictionary.DEFAULT_PROFILE_NAMESPACE,
                         key: 'name',
                         value: DDWorkItemProfileProviderService.getSimpleName()),
            new Metadata(namespace: NhsDataDictionary.DEFAULT_PROFILE_NAMESPACE,
                         key: 'version',
                         value: '1.0.0')
        ]
    }
/*
    void validateAndSaveModel(DataModel dataModel) {
        long startTime = System.currentTimeMillis()
        log.debug('Validating [{}] model', dataModel.label)
        dataModelService.validate(dataModel)
        long endTime = System.currentTimeMillis()
        log.info('Validate [{}] model complete in {}', dataModel.label, Utils.getTimeString(endTime - startTime))
        startTime = endTime
        if (dataModel.hasErrors()) {
            throw new Exception('DMSXX', 'Model is invalid', dataModel.errors)
        }

        dataModelService.saveModelWithContent(dataModel, 1000)
        endTime = System.currentTimeMillis()
        log.info('Saved [{}] model complete in {}', dataModel.label, Utils.getTimeString(endTime - startTime))
    }
*/

    /**
     * Prepare and get a test directory name under $TEMP - replaces the use of
     * the user desktop which might not be present on a server.
     */
    static Path getTestOutputPath() {
        File ditaTestDir = new File(System.getProperty("java.io.tmpdir"), "ditaTest")

        if (ditaTestDir.exists()) {
            FileUtils.deleteDirectory(ditaTestDir)
        }
        ditaTestDir.mkdirs()

        return ditaTestDir.toPath()
    }
}