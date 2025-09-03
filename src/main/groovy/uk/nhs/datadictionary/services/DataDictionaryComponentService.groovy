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

import groovy.util.logging.Slf4j
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataModelService
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.domain.terminology.TerminologyService
import org.maurodata.exception.MauroApplicationException
import org.maurodata.persistence.cache.FacetCacheableRepository.MetadataCacheableRepository
import uk.nhs.datadictionary.NhsDDBranch
import uk.nhs.datadictionary.NhsDDChangeLog
import uk.nhs.datadictionary.NhsDDCode
import uk.nhs.datadictionary.NhsDDElement
import uk.nhs.datadictionary.NhsDataDictionary
import uk.nhs.datadictionary.NhsDataDictionaryComponent
import uk.nhs.datadictionary.utils.StereotypedCatalogueItem

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@Singleton
abstract class DataDictionaryComponentService<T extends AdministeredItem, D extends NhsDataDictionaryComponent> {

    @Inject
    MetadataCacheableRepository metadataCacheableRepository

    List<T> index(UUID versionedFolderId, boolean includeRetired = false) {
        (getAll(versionedFolderId, includeRetired) as List).sort {it.label}
    }

    abstract def show(UUID versionedFolderId, String id)

    abstract Set<T> getAll(UUID versionedFolderId, boolean includeRetired = false)

/*    Map<String, String> getAliases(T catalogueItem) {
        Map<String, String> aliases = [:]
        catalogueItem.aliases
        NhsDataDictionary.aliasFields.each {aliasField ->
            Metadata foundMd = catalogueItem.metadata.find {it.namespace == getMetadataNamespace() && it.key == aliasField.key}
            if (foundMd) {
                aliases[aliasField.value] = foundMd.value
            }
        }
        return aliases
    }
*/
    abstract D getByCatalogueItemId(UUID catalogueItemId, NhsDataDictionary nhsDataDictionary)

    List<StereotypedCatalogueItem> getWhereUsed(UUID versionedFolderId, String id) {
        NhsDataDictionary dataDictionary = nhsDataDictionaryService.buildDataDictionary(versionedFolderId)

        // Do a full check of every "where used" link type, same as the DITA generation. Only way to be sure that
        // every possible link is captured
        Map<String, NhsDataDictionaryComponent> pathLookup = [:]
        dataDictionary.allComponents.each { component ->
            pathLookup[component.getMauroPath()] = component
        }

        dataDictionary.allComponents.each {component ->
            component.replaceLinksInDefinition(pathLookup)
        }

        NhsDataDictionaryComponent component = getByCatalogueItemId(UUID.fromString(id), dataDictionary)
        component.updateWhereUsed()
        component.whereUsed
            .findAll { !it.key.isRetired() }
            .sort { it.key.name }
            .collect { item, text -> new StereotypedCatalogueItem(item, text) }
    }

    /**
     * This is the HTML link pattern to use for descriptions in Mauro. These could be ingested links, or links created
     * via the Mauro UI
     */
    static Pattern pattern = Pattern.compile("<a\\s+[^>]*?href=\"([^\"]+)\"[^>]*>(.*?)</a>")


    String convertLinksInDescription(UUID branchId, String description) {
        Folder versionedFolder = VersionedFolder.get(branchId)

        String newDescription = description
        log.debug(newDescription)
        Matcher matcher = pattern.matcher(description)
        while (matcher.find()) {
            if(matcher.group(1).startsWith('http') ||
               matcher.group(1).startsWith('mailto')
            ) {
                // ignore
            } else {
                //System.err.println(matcher.group(1))
                try {
                    String[] path = matcher.group(1).split("\\|")
                    AdministeredItem foundCatalogueItem = getByPath(versionedFolder, path)
                    if (foundCatalogueItem) {
                        String stereotype = getStereotypeByPath(path)
                        String catalogueId = foundCatalogueItem.id.toString()
                        String cssClass = stereotype
                        String replacementLink = """<a class="${cssClass}" href="#/preview/${branchId.toString()}/${stereotype}/${catalogueId}">${
                            matcher
                                .group(2)}</a>"""
                        newDescription = newDescription.replace(matcher.group(0), replacementLink)
                    } else {
                        System.err.println("Cannot find domain item: ${matcher.group(1)}")
                    }
                } catch(Exception e) {
                    System.err.println(e.message)
                    e.printStackTrace()
                    System.err.println(description)
                }

            }
        }
        return newDescription?.trim() ?: null
    }

    String replaceLinksInShortDescription(String input) {
        String output = input
        Matcher matcher = pattern.matcher(input)
        while (matcher.find()) {
            output = output.replace(matcher.group(0), matcher.group(1))
        }
        return output
    }

    @Deprecated
    AdministeredItem getByPath(Folder versionedFolder, String[] path) {
        if (path[0] == "te:${NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByFolderIdAndLabel(versionedFolder.id, NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if (t) {
                return t
            }
        }
        if (path[0] == "te:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByFolderIdAndLabel(versionedFolder.id, NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if (t) {
                return t
            }
        }
        if (path[0] == "te:${NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}") {
            Terminology terminology = terminologyService.findByFolderIdAndLabel(versionedFolder.id, NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME)
            String termLabel = path[1].replace("tm:", "")
            Term t = terminology.findTermByCode(termLabel)
            if (t) {
                return t
            }
        }
        if (path[0] == "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}".toString()) {
            DataModel dm = dataModelService.findByFolderIdAndLabel(versionedFolder.id, NhsDataDictionary.CLASSES_MODEL_NAME)
            if (path[1] == "dc:Retired") {
                DataClass dc1 = dataClassService.findByDataModelIdAndLabel(dm.id, "Retired")
                DataClass dc2 = dataClassService.findByParentAndLabel(dc2, path[2].replace("dc:", ""))
                return dc2
            } else {
                DataClass dc1 = dataClassService.findByDataModelIdAndLabel(dm.id, path[1].replace("dc:", ""))
                if(path.length > 2 && path[2].startsWith("de")) {
                    DataElement de = dataElementService.findByParentAndLabel(dc1, path[2].replace("de:", ""))
                    return de
                } else {
                    return dc1
                }
            }
        } else if (path[0] == "dm:${NhsDataDictionary.ELEMENTS_MODEL_NAME}") {
            DataModel dm = dataModelService.findByFolderIdAndLabel(versionedFolder.id, NhsDataDictionary.ELEMENTS_MODEL_NAME)
            if (path[1] == "dc:Retired") {
                DataClass dc1 = dataClassService.findByDataModelIdAndLabel(dm.id, "Retired")
                DataElement de = dataElementService.findByParentAndLabel(dc1, path[2].replace("de:", ""))
                return de
            } else {
                DataClass dc = dataClassService.findByDataModelIdAndLabel(dm.id, path[1].replace("dc:", ""))
                DataElement de = dataElementService.findByParentAndLabel(dc, path[2].replace("de:", ""))
                return de
            }
        }

        if (path.length == 1 && path[0].startsWith("dm:")) {
            DataModel dm = dataModelService.findByLabel(path[0].replace("dm:", ""))
            return dm
        }

        return null
    }

    static String getStereotypeByPath(String[] path) {
        if (path[0] == "te:${NhsDataDictionary.BUSINESS_DEFINITIONS_TERMINOLOGY_NAME}") {
            return "businessDefinition"
        }
        if (path[0] == "te:${NhsDataDictionary.SUPPORTING_DEFINITIONS_TERMINOLOGY_NAME}") {
            return "supportingInformation"
        }
        if (path[0] == "te:${NhsDataDictionary.DATA_SET_CONSTRAINTS_TERMINOLOGY_NAME}") {
            return "dataSetConstraint"
        }
        if (path[0] == "dm:${NhsDataDictionary.CLASSES_MODEL_NAME}") {
            if(path[path.size() -1].startsWith("de:")) {
                return "attribute"
            }
            return "class"
        }
        if (path[0] == "dm:${NhsDataDictionary.ELEMENTS_MODEL_NAME}") {
            return "element"
        }

        if (path.length == 1 && path[0].startsWith("dm:")) {
            return "dataSet"
        }
        return null
    }


    Map<String, String> attributeMetadata = [
        // Aliases
        "aliasShortName"               : "aliasShortName",
        "aliasAlsoKnownAs"             : "aliasAlsoKnownAs",
        "aliasPlural"                  : "aliasPlural",
        "aliasFormerly"                : "aliasFormerly",
        "aliasFullName"                : "aliasFullName",
        "aliasIndexName"               : "aliasIndexName",
        "aliasSchema"                  : "aliasSchema",
        "name"                         : "name",
        "titleCaseName"                : "TitleCaseName",
        "websitePageHeading"           : "websitePageHeading",

        // SNOMED
        "aliasSnomedCTRefsetId"        : "aliasSnomedCTRefsetId",
        "aliasSnomedCTRefsetName"      : "aliasSnomedCTRefsetName",
        "aliasSnomedCTSubsetName"      : "aliasSnomedCTSubsetName",
        "aliasSnomedCTSubsetOriginalId": "aliasSnomedCTSubsetOriginalId",

        // Publication Lifecycle
        "isPreparatory"                : "isPrepatory",
        "isRetired"                    : "isRetired",
        "retiredDate"                  : "retiredDate",

        // Legacy
        "uin"                          : "uin",
        "baseUri"                      : "base-uri",
        "ultimatePath"                 : "ultimate-path",
        "ddUrl"                        : "DD_URL",
        "search"                       : "search",
        "baseVersion"                  : "baseVersion",
        // "modFinal": "mod__final",
        // "modAbstract": "mod__abstract",

        // Encoding
        "formatLength"                 : "format-length",
        "formatLink"                   : "format-link",


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
        "navigationParent"             : "navigationParent",
        "explanatoryPage"              : "explanatoryPage",
        "class"                        : "class",
        "doNotShowChangeLog"           : "doNotShowChangeLog",
        "node"                         : "node",
        "referencedElement"            : "referencedElement",
        "participant"                  : "participant",
        "supportingFile"               : "supportingFile",
        "isNavigationComponent"        : "isNavigationComponent",
        "isWebsiteIndex"               : "isWebsiteIndex",

        // Not sure about this one...
        // "extraFieldHead": "ExtraFieldhead",

        // do I need to parse these for history?
        //"defaultCode": "DefaultCode",
        //"nationalCodes": "national-codes",
        //"defaultCodes": "default-codes",
        //"valueSet": "value-set",
    ]

    void addMetadataFromComponent(Item domainObject, NhsDataDictionaryComponent component) {
        component.otherProperties.each {key, value ->
            if(!NhsDataDictionary.KEYS_FOR_INGEST_ONLY.contains(key))
            addToMetadata(domainObject, component.getMetadataNamespace(), key, value)
        }
    }


    void addToMetadata(Item domainObject, String namespace, String key, String value) {
        if (domainObject && value) {
            if(domainObject.metadata.find {
                it.key == key
            }) {
                throw new Exception("Duplicate metadata for $key on object of type ${domainObject.domainType}")
            } else {
                domainObject.metadata.add(new Metadata(namespace: namespace,
                                                        key: key,
                                                        value: value))
            }
        }
    }


    boolean catalogueItemIsRetired(AdministeredItem catalogueItem) {
        // Maybe should check for namespace here as well?
        catalogueItem.metadata.find {
            it.name == 'isRetired' && it.value == 'true'
        }
    }


    List<NhsDDCode> getCodesForTerms(List<Term> terms, NhsDataDictionary nhsDataDictionary) {
        List<NhsDDCode> codes = []
        List<Metadata> allRelevantMetadata = Metadata
            .byMultiFacetAwareItemIdInList(terms.collect {it.id})
            .inList('key', ['publishDate', 'webOrder', 'webPresentation', 'isDefault', 'isRetired', 'retiredDate'])
            .list()
        codes.addAll(terms.collect {term ->
            NhsDDCode nhsDDCode = nhsDataDictionary.codesByCatalogueId[term.id]
            if(!nhsDDCode) {
                nhsDDCode = new NhsDDCode().tap {
                    code = term.code
                    definition = term.definition
                    publishDate = allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'publishDate'}?.value
                    webOrder = Integer.parseInt(allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'webOrder'}?.value ?: "0")
                    webPresentation = allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'webPresentation'}?.value
                    isDefault = Boolean.valueOf(allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'isDefault'}?.value ?: "false")
                    isRetired = Boolean.valueOf(allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'isRetired'}?.value ?: "false")
                    retiredDate = allRelevantMetadata.find {it.multiFacetAwareItemId == term.id && it.key == 'retiredDate'}?.value
                    catalogueItem = term
                }
                nhsDataDictionary.codesByCatalogueId[term.id] = nhsDDCode
            }
            return nhsDDCode
        })
        return codes
    }

    Folder getFolderAtPath(Folder sourceFolder, List<String> path) {
        if (path.size() == 0) {
            return sourceFolder
        } else {
            String nextFolderName = path.remove(0)
            Folder nextFolder = sourceFolder.childFolders.find {it.label == nextFolderName}
            if (!nextFolder) {
                nextFolder = new Folder(label: nextFolderName)
                sourceFolder.childFolders.add(nextFolder)
            }
            return getFolderAtPath(nextFolder, path)

        }

    }

}
