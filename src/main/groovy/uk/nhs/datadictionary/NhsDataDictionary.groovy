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

import groovy.util.logging.Slf4j
import groovy.xml.XmlParser
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Prototype
import jakarta.inject.Inject
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataModelType
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.terminology.Terminology
import org.maurodata.exception.MauroApplicationException
import uk.nhs.datadictionary.services.AttributeService
import uk.nhs.datadictionary.services.BusinessDefinitionService
import uk.nhs.datadictionary.services.ClassService
import uk.nhs.datadictionary.services.DataDictionaryComponentService
import uk.nhs.datadictionary.services.DataSetConstraintService
import uk.nhs.datadictionary.services.DataSetFolderService
import uk.nhs.datadictionary.services.DataSetService
import uk.nhs.datadictionary.services.ElementService
import uk.nhs.datadictionary.services.NhsDataDictionaryService
import uk.nhs.datadictionary.services.SupportingInformationService

import java.time.Duration
import java.time.Instant
import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@Prototype
class NhsDataDictionary {

    @Inject
    NhsDataDictionaryService nhsDataDictionaryService

    @Inject
    ClassService classService

    @Inject
    ElementService elementService

    @Inject
    AttributeService attributeService

    @Inject
    BusinessDefinitionService businessDefinitionService

    @Inject
    DataSetService dataSetService

    @Inject
    DataSetConstraintService dataSetConstraintService

    @Inject
    SupportingInformationService supportingInformationService

    @Inject
    DataSetFolderService dataSetFolderService


    static final String WEBSITE_URL = "https://www.datadictionary.nhs.uk"

    static final String METADATA_NAMESPACE = "uk.nhs.datadictionary"
    static final String METADATA_DATASET_TABLE_NAMESPACE = METADATA_NAMESPACE + ".dataset.table"
    static final String METADATA_DATASET_ELEMENT_NAMESPACE = METADATA_NAMESPACE + ".dataset.element"
    static final String DEFAULT_PROFILE_NAMESPACE = "default.profile"

    static final String FOLDER_NAME = "NHS Data Dictionary"
    static final String ATTRIBUTE_TERMINOLOGIES_FOLDER_NAME = "Attribute Terminologies"
    static final String ELEMENT_CODESETS_FOLDER_NAME = "Data Element CodeSets"
    static final String ELEMENTS_MODEL_NAME = "Data Elements"
    static final String CLASSES_MODEL_NAME = "Classes and Attributes"
    static final String DATA_SETS_FOLDER_NAME = "Data Sets"
    static final String BUSINESS_DEFINITIONS_TERMINOLOGY_NAME = "NHS Business Definitions"
    static final String SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME = "Supporting Information"
    static final String DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME = "Data Set Constraints"

    // The following keys are used in DataSetParser and NhsDDDataSetClass and NhsDDDataSetElement and
    // must match the property names used in the dataSetTableProfile and dataSetElementProfile.
    static final String DATASET_TABLE_KEY_WEB_ORDER = "webOrder"
    static final String DATASET_TABLE_KEY_MRO = "mandation"
    static final String DATASET_TABLE_KEY_GROUP_REPEATS = "groupRepeats"
    static final String DATASET_TABLE_KEY_RULES = "rules"
    static final String DATASET_TABLE_KEY_MULTIPLICITY_TEXT = "multiplicity"
    static final String DATASET_TABLE_KEY_CHOICE = "choice"
    static final String DATASET_TABLE_KEY_AND = "and"
    static final String DATASET_TABLE_KEY_INCLUSIVE_OR = "inclusiveOr"
    static final String DATASET_TABLE_KEY_NOT_OPTION = "notOption"
    static final String DATASET_TABLE_KEY_ADDRESS_CHOICE = "addressChoice"
    static final String DATASET_TABLE_KEY_NAME_CHOICE = "nameChoice"
    static final String DATASET_TABLE_KEY_DATA_SET_REFERENCE = "dataSetReference"
    static final String DATASET_TABLE_KEY_DATA_SET_REFERENCE_TO = "dataSetReferenceTo"

    static final String CHANGE_REQUEST_NUMBER_TOKEN = "{cr_number}"

    /**
     * This is the HTML link pattern to use when reading the XML ingest files
     */
    static Pattern pattern = Pattern.compile("<a[\\s]*(?:uin=\"[^\"]*\")?[\\s]*href=\"([^\"]*)\"[\\s]*(?:uin=\"[^\"]*\")?>([^<]*)</a>")

    String retiredItemText = ""
    String preparatoryItemText = ""
    String changeRequestUrl = ""
    String changeLogHeaderText = ""
    String changeLogFooterText = ""

    Map<String, String> workItemDetails = [:]


    Map<String, NhsDDAttribute> attributes = [:]
    Map<String, NhsDDElement> elements = [:]
    Map<String, NhsDDClass> classes = [:]
    Map<String, NhsDDDataSet> dataSets = [:]
    Map<List<String>, List<NhsDDDataSetFolder>> dataSetFolders = [:]
    Map<String, NhsDDBusinessDefinition> businessDefinitions = [:]
    Map<String, NhsDDSupportingInformation> supportingInformation = [:]
    Map<String, NhsDDDataSetConstraint> dataSetConstraints = [:]
    Map<String, NhsDDWebPage> webPages = [:]

    Map<String, NhsDDAttribute> attributesByUin = [:]
    Map<String, NhsDDClass> classesByUin = [:]
    Map<String, NhsDDWebPage> webPagesByUin = [:]
    Map<String, String> dataSetNamesByDDUrl = [:]

    // TODO:  Need to refactor this out sometime
    Map<String, DataElement> elementsByUrl = [:]

    Map<UUID, NhsDDCode> codesByCatalogueId = [:]
    Map<UUID, NhsDDClass> classesByCatalogueId = [:]
    Map<UUID, NhsDDElement> elementsByCatalogueId = [:]
    Map<UUID, NhsDDAttribute> attributesByCatalogueId = [:]

    Map<UUID, List<NhsDDCode>> attributeTerminologyCodes = [:]
    Map<UUID, List<NhsDDCode>> elementCodeSetCodes = [:]

    Map<String, NhsDataDictionaryComponent> pathLookup = [:]

    String folderName = FOLDER_NAME

    String branchName = "main"

    Folder containingVersionedFolder

    /**
     * Stores a map of every work item branch available (i.e. branches where changes are made), where the key is the branch name
     */
    Map<String, NhsDDBranch> workItemBranches = [:]

    Map<String, Map<String, NhsDataDictionaryComponent>> componentClasses = [
        "NhsDDAttribute": attributes,
        "NhsDDElement": elements,
        "NhsDDClass": classes,
        "NhsDDWebPage": webPages,
        "NhsDDBusinessDefinition": businessDefinitions,
        "NhsDDSupportingInformation": supportingInformation,
        "NhsDDDataSet": dataSets,
        "NhsDDDataSetConstraint": dataSetConstraints,
    ]

    List<NhsDataDictionaryComponent> getAllComponents() {
        (attributes.values() +
            elements.values() +
            classes.values() +
            dataSets.values() +
            businessDefinitions.values() +
            supportingInformation.values() +
            dataSetConstraints.values() +
            webPages.values() +
            dataSetFolders.values().flatten()) as List<NhsDataDictionaryComponent>
    }

    Map<String, List<NhsDataDictionaryComponent>> allComponentsByIndex(Boolean includeRetired = false) {
        componentsByIndex(getAllComponents(), includeRetired)
    }

    Map<String, List<NhsDataDictionaryComponent>> componentsByIndex(Collection<NhsDataDictionaryComponent> components,
                                                                    Boolean includeRetired = false) {

        Map<String, List<NhsDataDictionaryComponent>> indexMap = [:]

        List<String> alphabet = ['0-9']
        alphabet.addAll('a'..'z')

        alphabet.each {alphIndex ->
            List<NhsDataDictionaryComponent> componentsByIndex = components.findAll {component ->
                (includeRetired || !component.isRetired()) &&
                ((alphIndex == '0-9' && Character.isDigit(component.name.charAt(0))) ||
                 component.name.toLowerCase().startsWith(alphIndex))
            }.sort {it.name.toLowerCase()}

            if(componentsByIndex.size() > 0) {
                indexMap[alphIndex.toUpperCase()] = componentsByIndex
            }
        }

        indexMap
    }

    Map<String, NhsDataDictionaryComponent> internalLinks = [:]

    void buildInternalLinks() {
        allComponents.each {eachComponent ->
            internalLinks[eachComponent.getMauroPath()] = eachComponent
        }
    }


    void buildFromXml(DataDictionaryImportParameters parameters) {
        Node xml = new XmlParser().parse(parameters.importFile.inputStream)

        log.info('Building new NHS Data Dictionary from XML')
        Instant startTime = Instant.now()

        // I'm not sure we do this any more?
        /* if (parameters.releaseDate) {
            folderName = "${FOLDER_NAME} (${releaseDate})"
        }
        */
        folderName = FOLDER_NAME

        branchName = parameters.newBranchName ?: "main"
        buildDataSetUrlMap(xml)

        componentClasses.each {componentClassName, map ->
            String componentNameWithPackage = "${NhsDataDictionary.packageName}.${componentClassName}"
            NhsDataDictionaryComponent dummyComponent = (NhsDataDictionaryComponent) Class.forName(componentNameWithPackage).getConstructor()
                .newInstance()
            log.info("Building ${componentClassName}...")
            Instant componentStartTime = Instant.now()
            if(parameters.isPublishableComponent(dummyComponent)) {
                xml[dummyComponent.getXmlNodeName()] /*.sort {it.name.text()} */.each {node ->
                    NhsDataDictionaryComponent component = (NhsDataDictionaryComponent) Class.forName(componentNameWithPackage).getConstructor()
                        .newInstance()
                    if (component.isValidXmlNode(node)) {
                        component.fromXml(node, this)
                        NhsDataDictionaryComponent existingItem = map[component.name]
                        if (!existingItem || existingItem.isRetired()) {
                            map[component.name] = component
                        }
                    }
                }
            }
            log.info("${componentClassName} built in ${Duration.between(componentStartTime, Instant.now()).toString()}")
        }
        Instant componentStartTime = Instant.now()
        if(!parameters.omitDataSetFolders) {
            processDataSetFolders()
        }
        log.info("Data Set Folders built in ${Duration.between(componentStartTime, Instant.now()).toString()}")
        componentStartTime = Instant.now()
        if(!parameters.omitClasses) {
            processClassLinks()
        }
        log.info("Class Links built in ${Duration.between(componentStartTime, Instant.now()).toString()}")
        componentStartTime = Instant.now()
        processLinksFromXml()
        log.info("Links processed in ${Duration.between(componentStartTime, Instant.now()).toString()}")

        log.info("Data Dictionary build from XML complete in ${Duration.between(startTime, Instant.now()).toString()}")

    }

    Folder generateFolder(DataDictionaryImportParameters parameters) {

        Folder dictionaryFolder = new Folder(label: folderName, branchName: Model.DEFAULT_BRANCH_NAME)
        if(parameters.branchName && parameters.finalised == false){
            dictionaryFolder.finalised = false
            dictionaryFolder.branchName = parameters.branchName
        } else if (parameters.folderVersionNo && parameters.releaseDate) {
            dictionaryFolder.finalised = true
            dictionaryFolder.modelVersion = ModelVersion.from(parameters.folderVersionNo)
            dictionaryFolder.modelVersionTag = parameters.releaseDate
            dictionaryFolder.branchName = null
        }
        if(parameters.prevVersion) {
            VersionLink versionLink = new VersionLink()
            versionLink.versionLinkType = VersionLink.NEW_MODEL_VERSION_OF
            versionLink.target = new Folder(id: parameters.prevVersion)
            dictionaryFolder.versionLinks.add(versionLink)
        }


        nhsDataDictionaryService.defaultProfileMetadata().each { defaultMetadata ->
            dictionaryFolder.metadata(defaultMetadata)
        }


        Map<String, Terminology> attributeTerminologiesByName = [:]
        Map<String, DataClass> attributeClassesByUin = [:]
        Set<String> attributeUinIsKey = []

        if(!parameters.omitAttributes && !parameters.omitClasses) {

            DataModel classesDataModel =
                new DataModel(label: CLASSES_MODEL_NAME,
                              description: "NHS Data Dictionary Data Model (Classes and Attributes)",
                              dataModelType: DataModelType.DATA_STANDARD,
                              folder: dictionaryFolder)
            dictionaryFolder.dataModels.add(classesDataModel)
            classService.createClassesModel(this, classesDataModel, attributeClassesByUin, attributeUinIsKey)

            if(!parameters.omitAttributes) {
                attributeService.createAttributes(this, dictionaryFolder, classesDataModel, attributeTerminologiesByName, attributeClassesByUin, attributeUinIsKey)
            }
        }

        DataModel elementDataModel = null
        if (!parameters.omitElements) {
            elementDataModel =
                new DataModel(label: ELEMENTS_MODEL_NAME,
                              description: "NHS Data Dictionary Data Elements",
                              folder: dictionaryFolder,
                              dataModelType: DataModelType.DATA_STANDARD)
            dictionaryFolder.dataModels.add(elementDataModel)
            elementService.persistElements(this, dictionaryFolder, elementDataModel, attributeTerminologiesByName)
        }

        if(!parameters.omitBusinessDefinitions) {
            businessDefinitionService.persistBusinessDefinitions(this, dictionaryFolder)
        }

        if(!parameters.omitSupportingInformation) {
            supportingInformationService.persistSupportingInformation(this, dictionaryFolder)
        }

        if(!parameters.omitDataSetConstraints) {
            dataSetConstraintService.persistDataSetConstraints(this, dictionaryFolder)
        }

        if(!parameters.omitDataSetFolders) {
            dataSetFolderService.persistDataSetFolders(this, dictionaryFolder)
        }

        if(!parameters.omitDataSets) {
            dataSetService.persistDataSets(this, dictionaryFolder, elementDataModel)
        }

        dictionaryFolder
   }

    Map<String, String> introductionPageMap = [
            //"PLICS": "PLICS Data Set Overview",
            "PLICS Data Set": "Patient Level Information Costing System Integrated Data Set Introduction",
            "EPMA": "Electronic Prescribing and Medicines Administration Data Sets Introduction",
            "COSDS": "Cancer Outcomes and Services Data Set Introduction",
            "CDS V6-2": "Commissioning Data Set Version 6-2 Type List",
            "CDS V6-3": "Commissioning Data Set Version 6-3 Type List",
            //"Commissioning Data Sets": "Commissioning Data Sets (CDS) Introduction"
    ]

    void buildDataSetUrlMap(def xml) {
        xml.DDDataSet.each { it ->
            dataSetNamesByDDUrl[it.DD_URL.text()] = it.name[0].text().replace("_", " ")
        }
    }


    void processDataSetFolders() {
        Set<List<String>> paths = []
        dataSets.values().each {dataSet ->
            List<String> newWebPath = []
            newWebPath.addAll(dataSet.getWebPath())
            newWebPath.removeLast()
            paths.add(newWebPath)
        }
        paths.add(["Commissioning Data Sets"])
        paths.add(["Retired"])
        paths.each {path ->

            NhsDDDataSetFolder dataSetFolder = new NhsDDDataSetFolder()
            dataSetFolder.catalogueItem = new Folder(label: path.last())
            dataSetFolder.folderPath.addAll(path)
            if(path.contains("Retired")) {
                dataSetFolder.addOtherProperties(["isRetired": "true"])
            } else {
                dataSetFolder.addOtherProperties(["isRetired": "false"])
            }

            String introductionPageName = introductionPageMap[dataSetFolder.name]
            if(!introductionPageName) {
                introductionPageName = dataSetFolder.name + " Introduction"
            }
            NhsDataDictionaryComponent introductoryWebPage = webPages[introductionPageName]?:supportingInformation[introductionPageName]
            if(!introductoryWebPage && !dataSetFolder.isRetired()) {
                log.error("Cannot find introductory web page: ${dataSetFolder.name}")
            } else {
                if(introductoryWebPage && !dataSetFolder.isRetired()) {
                    log.info("Introduction page found: " + introductoryWebPage.name)
                    dataSetFolder.addOtherProperties(introductoryWebPage.otherProperties)
                    dataSetFolder.catalogueItem.description = introductoryWebPage.catalogueItem.description
                }
            }

            if(dataSetFolders[path]) {
                dataSetFolders[path].add(dataSetFolder)
            } else {
                dataSetFolders[path] = [dataSetFolder]
            }
        }

    }

    void processClassLinks() {
        classes.each {name, clazz ->
            clazz.classLinks.each {classLink ->
                classLink.supplierClass = classesByUin[classLink.supplierUin]
                if(!classLink.supplierClass) {
                    log.error("Cannot match supplier class relationship: {} :: SupplierUin: {}\n{}", name, classLink.supplierUin, classLink.toString())
                }
                classLink.clientClass = classesByUin[classLink.clientUin]
                if(!classLink.clientClass) {
                    log.error("Cannot match client class relationship: {} ClientUin: {}\n{}", name, classLink.clientUin, classLink.toString())
                }
            }
        }
    }

    void processLinksFromXml() {
        Map<String, String> replacements = [:]
        getAllComponents().each {component ->
            replacements.putAll(component.getUrlReplacements())
        }
        Map<String, List<String>> unmatchedUrls = [:]
        getAllComponents().each {component ->
            component.catalogueItem.description = replaceUrls(component.catalogueItem.description, replacements, unmatchedUrls, component.name)
            if(component instanceof NhsDDElement) {

                ((NhsDDElement)component).codes.each {code ->
                    if(code.webPresentation) {
                        code.webPresentation = replaceUrls(code.webPresentation, replacements, unmatchedUrls, component.name)
                    }
                }
                String formatLink = ((NhsDDElement)component).otherProperties["formatLink"]
                if(formatLink && replacements[formatLink]) {
                    ((NhsDDElement)component).otherProperties["formatLink"] = replacements[formatLink]
                }
//                if(component.otherProperties["attributeText"] && component.otherProperties["attributeText"] != "") {
//                    component.otherProperties["attributeText"] = replaceUrls(component.otherProperties["attributeText"], replacements, unmatchedUrls, component.name)
//                }

            }
            if(component instanceof NhsDDAttribute) {
                ((NhsDDAttribute)component).codes.each {code ->
                    code.webPresentation = replaceUrls(code.webPresentation, replacements, unmatchedUrls, component.name)
                }
            }
        }
        log.warn("Unmatched Urls: {}", unmatchedUrls.size())
    }

    static String replaceUrls(String input, Map<String, String> replacements, Map<String, List<String>> unmatchedUrls, String componentName) {
        if(input) {
            Matcher matcher = pattern.matcher(input)
            while(matcher.find()) {
                String matchedUrl = replacements[matcher.group(1)]
                if(matchedUrl) {
                    String replacement = "<a href=\"${matchedUrl}\">${matcher.group(2).replaceAll("_"," ")}</a>"
                    input = input.replace(matcher.group(0), replacement)
                    log.trace("Replacing: " + matcher.group(0) + " with " + replacement)
                } else {
                    List<String> existingUnmatched = unmatchedUrls[componentName]
                    if(existingUnmatched) {
                        existingUnmatched.add(matcher.group(0))
                    } else {
                        unmatchedUrls[componentName] = [matcher.group(0)]
                    }
                }

            }
        }
        return input
    }

    void getTermsForAttributes() {
        Folder terminologiesFolder = containingVersionedFolder.childFolders.find {it.label == ATTRIBUTE_TERMINOLOGIES_FOLDER_NAME}
        terminologiesFolder.childFolders.each {childFolder ->
            childFolder.terminologies.each {terminology ->
                attributeTerminologyCodes[terminology.id] =
                    terminology.terms.collect {term ->
                        new NhsDDCode(term)
                    }
            }
            if(childFolder.childFolders) {
                childFolder.childFolders.each {childChildFolder ->
                    childChildFolder.terminologies.each {terminology ->
                        attributeTerminologyCodes[terminology.id] =
                            terminology.terms.collect {term ->
                                new NhsDDCode(term)
                            }
                    }
                }
            }
        }
    }
    void getTermsForElements() {
        Folder codeSetsFolder = containingVersionedFolder.childFolders.find {it.label == ELEMENT_CODESETS_FOLDER_NAME}
        codeSetsFolder.childFolders.each {childFolder ->
            childFolder.codeSets.each {codeSet ->
                elementCodeSetCodes[codeSet.id] =
                    codeSet.terms.collect {term ->
                        new NhsDDCode(term)
                    }
            }
            if(childFolder.childFolders) {
                childFolder.childFolders.each {childChildFolder ->
                    childChildFolder.codeSets.each {terminology ->
                        elementCodeSetCodes[codeSet.id] =
                            terminology.terms.collect {term ->
                                new NhsDDCode(term)
                            }
                    }
                }
            }
        }
    }


    Map<String, Map<String, Number>> statistics() {
        ['Attributes': [
            'Total':  attributes.size(),
            'Preparatory': attributes.values().count {it.isPreparatory()},
            'Retired': attributes.values().count {it.isRetired()},
            ],
        'Data Elements': [
            'Total': elements.size(),
            'Preparatory': elements.values().count {it.isPreparatory()},
            'Retired': elements.values().count {it.isRetired()},
            ],
        'Classes': [
            'Total': classes.size(),
            'Preparatory': classes.values().count {it.isPreparatory()},
            'Retired': classes.values().count {it.isRetired()}
            ],
        'Data Sets': [
            'Total': dataSets.size(),
            'Preparatory': dataSets.values().count {it.isPreparatory()},
            'Retired': dataSets.values().count {it.isRetired()},
            ],
        'NHS Business Definitions': [
            'Total': businessDefinitions.size(),
            'Preparatory': businessDefinitions.values().count {it.isPreparatory()},
            'Retired': businessDefinitions.values().count {it.isRetired()},
            ],
        'Supporting Information': [
            'Total': supportingInformation.size(),
            'Preparatory': supportingInformation.values().count {it.isPreparatory()},
            'Retired': supportingInformation.values().count {it.isRetired()},
            ],
        'Data Set Constraints': [
            'Total': dataSetConstraints.size(),
            'Preparatory': dataSetConstraints.values().count {it.isPreparatory()},
            'Retired': dataSetConstraints.values().count {it.isRetired()},
            ]
        ]

    }

    String replaceLinksInString(String source) {
        if(!source || source.isEmpty()) {
            return source
        }
        Matcher matcher = DataDictionaryComponentService.pattern.matcher(source)
        while (matcher.find()) {
            NhsDataDictionaryComponent component = pathLookup[matcher.group(1)]

            if (component) {
                String ditaKey = component.ditaKey
                if(component instanceof NhsDDDataSetFolder) {
                    ditaKey = ditaKey + "_group_index"
                }
                String text = matcher.group(2).replaceAll("_"," ")
                String replacement = "<a class='${component.getOutputClass()}' href=\"${ditaKey}\">${text}</a>"
                source = source.replace(matcher.group(0), replacement)
            }
            else {
                log.trace("Cannot match component: ${matcher.group(1)}")
            }
        }

        return source

    }

    static Set<String> getAllMetadataKeys() {
        Set<String> allKeys = []
        allKeys.addAll(METADATA_FIELD_MAPPING.keySet())

        // These fields are not part of the original ingest from xml but calculated subsequently.
        // Another approach would be to enumerate all the profile definitions from this plugin and list all their keys
        allKeys.addAll(["shortDescription","overview","linkedAttributes","noAliasesRequired","approvingOrganisation", "suppressFirstSentence"])
        allKeys.removeAll(KEYS_FOR_INGEST_ONLY)
        return allKeys
    }

    static Map<String, String> aliasFields = [
        "aliasAlsoKnownAs": "Also known as",
        "aliasFormerly": "Formerly",
        "aliasFullName": "Full name",
        "aliasIndexName": "Index name",
        "aliasODSXMLSchema": "ODS XML Schema",
        "aliasOid": "Object Identifier",
        "aliasPlural": "Plural",
        "aliasPossessive": "Possessive",
        "aliasReportHeading": "Report heading",
        "aliasSchema": "Schema",
        "aliasShortName": "Short name",
        "aliasSnomedCTRefsetId": "SNOMED CT Refset Id",
        "aliasSnomedCTRefsetName": "SNOMED CT Refset Name"
    ]

    static List<String> KEYS_FOR_INGEST_ONLY = [
            "uin",
            "baseUri",
            "ultimatePath",
            "ddUrl",
            "search",
            "baseVersion"
    ]

    static final Map<String, String> METADATA_FIELD_MAPPING = [
        // Aliases
        "aliasShortName": "aliasShortName",
        "aliasAlsoKnownAs": "aliasAlsoKnownAs",
        "aliasPlural": "aliasPlural",
        "aliasFormerly": "aliasFormerly",
        "aliasFullName": "aliasFullName",
        "aliasIndexName": "aliasIndexName",
        "aliasSchema": "aliasSchema",
        "name": "name",
        "titleCaseName": "TitleCaseName",
        "websitePageHeading": "websitePageHeading",

        // SNOMED
        "aliasSnomedCTRefsetId": "aliasSnomedCTRefsetId",
        "aliasSnomedCTRefsetName": "aliasSnomedCTRefsetName",
        "aliasSnomedCTSubsetName": "aliasSnomedCTSubsetName",
        "aliasSnomedCTSubsetOriginalId": "aliasSnomedCTSubsetOriginalId",

        // Publication Lifecycle
        "isPreparatory": "isPrepatory",
        "isRetired": "isRetired",
        "retiredDate": "retiredDate",
        "validFrom": "validFrom",
        "validTo": "validTo",

        // Legacy
        "uin": "uin",
        "baseUri": "base-uri",
        "ultimatePath": "ultimate-path",
        "ddUrl": "DD_URL",
        "search": "search",
        "baseVersion": "baseVersion",
        // "modFinal": "mod__final",
        // "modAbstract": "mod__abstract",

        // Encoding
        "formatLength": "format-length",
        "formatLink": "format-link",

            "isKey": "isKey",

        //"fhirItem": "FHIR_Item",
        //"definition": "definition",
        //"codeSystem": "code-system",
        //"link": "link",
        //"linkTarget": "link-target",
        //"property": "property",
        //"extends": "extends",
        //"type": "type",
        //"readOnly": "readOnly",
        //"clientRole": "clientRole",
        //"permittedNationalCodes": "permitted-national-codes",
        "navigationParent": "navigationParent",
        "explanatoryPage": "explanatoryPage",
        "class": "class",
        "doNotShowChangeLog": "doNotShowChangeLog",
        "node": "node",
        "referencedElement": "referencedElement",
        "participant": "participant",
        "supportingFile": "supportingFile",
        "isNavigationComponent": "isNavigationComponent",
        "isWebsiteIndex": "isWebsiteIndex",

        // Not sure about this one...
        // "extraFieldHead": "ExtraFieldhead",

        // do I need to parse these for history?
        //"defaultCode": "DefaultCode",
        //"nationalCodes": "national-codes",
        //"defaultCodes": "default-codes",
        //"valueSet": "value-set",
    ]



}
