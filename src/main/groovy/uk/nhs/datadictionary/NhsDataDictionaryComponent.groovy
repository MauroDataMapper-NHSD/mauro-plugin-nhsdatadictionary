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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.maurodata.dita.elements.langref.base.DitaMap
import org.maurodata.dita.elements.langref.base.Topic
import org.maurodata.dita.elements.langref.base.XRef
import org.maurodata.dita.helpers.HtmlHelper
import org.maurodata.dita.meta.SpaceSeparatedStringList
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.terminology.Term
import uk.nhs.datadictionary.publish.changePaper.ChangeAware
import uk.nhs.datadictionary.publish.structure.AliasesRow
import uk.nhs.datadictionary.publish.structure.AliasesSection
import uk.nhs.datadictionary.publish.structure.ChangeLogRow
import uk.nhs.datadictionary.publish.structure.ChangeLogSection
import uk.nhs.datadictionary.publish.structure.DescriptionSection
import uk.nhs.datadictionary.publish.structure.DictionaryItem
import uk.nhs.datadictionary.publish.structure.ItemLink
import uk.nhs.datadictionary.publish.structure.WhereUsedRow
import uk.nhs.datadictionary.publish.structure.WhereUsedSection
import uk.nhs.datadictionary.services.MauroPersistenceService
import uk.nhs.datadictionary.utils.DDHelperFunctions

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

@Slf4j
@CompileDynamic
abstract class NhsDataDictionaryComponent <T extends AdministeredItem >  implements ChangeAware {

    abstract String getStereotype()
    abstract String getStereotypeForPreview()
    abstract String getPluralStereotypeForWebsite()

    abstract String getMetadataNamespace()

    @JsonIgnore
    NhsDataDictionary dataDictionary

    @JsonIgnore
    T catalogueItem

    NhsDataDictionaryComponent(T catalogueItem = null, UUID branchId = null) {
        this.catalogueItem = catalogueItem
        if(catalogueItem) {
            this.catalogueItemId = catalogueItem.id
        }
        this.branchId = branchId
    }


    UUID catalogueItemId

    @JsonIgnore
    UUID branchId

    @JsonIgnore
    String catalogueItemModelId

    @JsonIgnore
    String catalogueItemParentId

    String getName() {
        catalogueItem.label
    }

    @JsonIgnore
    String definition = ""

    //String htmlDescription

    @JsonIgnore
    Map<String, String> otherProperties = [:]

    @JsonIgnore
    Map<NhsDataDictionaryComponent, String> whereUsed = [:]

    List<NhsDDChangeLog> changeLog = []

    @JsonIgnore
    String changeLogHeaderText = ""
    @JsonIgnore
    String changeLogFooterText = ""

    abstract String calculateShortDescription()

    boolean isRetired() {
        "true" == otherProperties["isRetired"]
    }

    boolean isPreparatory() {
        "true" == otherProperties["isPreparatory"]
    }

    boolean isActivePage() {
        !isRetired() && !isPreparatory()
    }

    @JsonIgnore
    String getUin() {
        otherProperties["uin"]
    }

    String getShortDescription() {
        if(otherProperties["shortDescription"] && isActivePage()){
            return otherProperties["shortDescription"]
        } else {
            return calculateShortDescription()
        }
    }

    @JsonIgnore
    String getTitleCaseName() {
        otherProperties["titleCaseName"]
    }

    void setShortDescription() {
        String shortDescription = calculateShortDescription()
        if(!shortDescription) {
            log.error("Null short description! ${name}")
            shortDescription = name
        }
        //shortDescription = shortDescription.replaceAll("\\\\r", " ")
        shortDescription = shortDescription.replaceAll("\\s+", " ")
        shortDescription = shortDescription.replaceAll("\\\\n", " ")
        shortDescription = shortDescription.replaceAll("\\\\r", " ")
        otherProperties["shortDescription"] = shortDescription
    }

    abstract T newCatalogueItem(String name = null)

    void fromXml(def xml, NhsDataDictionary dataDictionary) {
        String label
        if(xml.name.size() > 0 && xml.name.text()) {
            label = xml.name[0].text().replace("_", " ")
        } else { // This should only apply for dataSetConstraints
            label = xml."class".name.text().replace("_", " ")
        }
        catalogueItem = newCatalogueItem(label)

        /*  We're doing capitalised items now
        if(xml.TitleCaseName.text()) {
            this.name = xml.TitleCaseName[0].text()
        } else { // This should only apply for dataSetConstraints
            this.name = xml."class".websitePageHeading.text()
        }*/

/*        String cleanedDefinition = xml.definition.text().
            replace("&amp;", "&").
            replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;\$1" ).
            replace("<", "&lt;").
            replace(">", "&gt;").
            replace("\u00a0", " ")
        definition = DDHelperFunctions.parseHtml(cleanedDefinition)
        definition = definition.replaceAll("\\s+", " ")
*/
        catalogueItem.description = (DDHelperFunctions.parseHtml(xml.definition[0])).replace("\u00a0", " ")

        NhsDataDictionary.METADATA_FIELD_MAPPING.entrySet().each {entry ->
            Node xmlValue = xml[entry.value][0]
            if((!xmlValue || xmlValue.text() == "") && xml."class"[entry.value]) {
                xmlValue = xml."class"[entry.value][0]
            }
            if(xmlValue && xmlValue.text() != "") {
                otherProperties[entry.key] = xmlValue.text()
            }
        }
    }

    @JsonIgnore
    boolean isValidXmlNode(def xmlNode) {
        return true
    }

    @JsonIgnore
    abstract String getXmlNodeName()

    void addWhereUsed(NhsDataDictionaryComponent component, String description) {
        whereUsed[component] = description
    }

    boolean hasNoAliases() {
        return getAliases().size() == 0
    }

    @JsonProperty("alsoKnownAs")
    Map<String, String> getAliases() {
        Map<String, String> aliases = [:]
        NhsDataDictionary.aliasFields.each {aliasKey, aliasValue ->

            String alias = otherProperties[aliasKey]
            if(alias) {
                aliases[aliasValue] = alias
            }
        }
        return aliases
    }

    @JsonIgnore
    Map<String, String> getUrlReplacements() {
        String ddUrl = this.otherProperties["ddUrl"]

        return [
            (ddUrl) : this.mauroPath
        ]
    }

    @JsonIgnore
    String getNameWithoutNonAlphaNumerics() {
        name.replaceAll("[^A-Za-z0-9- ]", "").replace(" ", "_")
    }

    @JsonIgnore
    abstract String getMauroPath()

    @JsonIgnore
    String getDitaKey() {
        String key = getStereotype().replace(" ", "_") + "_" + getNameWithoutNonAlphaNumerics()
        if(isRetired()) {
            key += "_retired"
        }
        key.toLowerCase()
    }

    String getDescription() {
        if(dataDictionary && isRetired()) {
            return dataDictionary.retiredItemText
        } else if(dataDictionary && isPreparatory()) {
            return dataDictionary.preparatoryItemText
        } else {
            return catalogueItem.description
        }
    }

    @JsonIgnore
    String getNameWithRetired() {
        if(isRetired()) {
            return this.name + " (Retired)"
        } else {
            return this.name
        }
    }

    @JsonIgnore
    String getDataDictionaryUrl() {
        String domain = NhsDataDictionary.WEBSITE_URL
        String stereotype = getPluralStereotypeForWebsite()
        String itemPage = "${getNameWithoutNonAlphaNumerics().toLowerCase()}.html"

        if (this.itemState == DictionaryItem.DictionaryItemState.RETIRED) {
            return "${domain}/${stereotype}/retired/${itemPage}"
        }

        return "${domain}/${stereotype}/${itemPage}"
    }

    DitaMap generateMap() {
        DitaMap.build(
                id: getDitaKey()
        ) {
            title getNameWithRetired()
        }
    }

    @JsonIgnore
    LocalDate getToDate() {
        if(otherProperties["validTo"]) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(otherProperties["validTo"] as CharSequence, formatter);
        }
        return null
    }

    @JsonIgnore
    LocalDate getFromDate() {
        if(otherProperties["validFrom"]) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(otherProperties["validFrom"] as CharSequence, formatter);
        }
        return null
    }

    @JsonIgnore
    DictionaryItem.DictionaryItemState getItemState() {
        isRetired()
            ? DictionaryItem.DictionaryItemState.RETIRED
            : isPreparatory()
            ? DictionaryItem.DictionaryItemState.PREPARATORY
            : DictionaryItem.DictionaryItemState.ACTIVE
    }

    @JsonIgnore
    DictionaryItem getPublishStructure() {
        DictionaryItem dictionaryItem = new DictionaryItem(this, this.branchId)

        addDescriptionSection(dictionaryItem)

        if (itemState == DictionaryItem.DictionaryItemState.ACTIVE) {
            addAliasesSection(dictionaryItem)
            addWhereUsedSection(dictionaryItem)
        }

        addChangeLogSection(dictionaryItem)

        dictionaryItem
    }

    void addDescriptionSection(DictionaryItem dictionaryItem) {
        dictionaryItem.addSection(new DescriptionSection(dictionaryItem, description))
    }

    void addAliasesSection(DictionaryItem dictionaryItem) {
        if (aliases) {
            List<AliasesRow> aliasesRows = getAliases()
                .collect {context, alias -> new AliasesRow(context, alias)}

            dictionaryItem.addSection(new AliasesSection(dictionaryItem, aliasesRows))
        }
    }

    void addWhereUsedSection(DictionaryItem dictionaryItem) {
        if (whereUsed) {
            List<WhereUsedRow> whereUsedRows = whereUsed
                .findAll { it.key.itemState != DictionaryItem.DictionaryItemState.RETIRED }
                .sort { it.key.name }
                .collect { component, text ->
                    new WhereUsedRow(component.stereotype, ItemLink.create(component), text)
                }

            dictionaryItem.addSection(new WhereUsedSection(dictionaryItem, whereUsedRows))
        }
    }

    void addChangeLogSection(DictionaryItem dictionaryItem) {
        List<ChangeLogRow> changeLogRows = changeLog.collect {entry -> new ChangeLogRow(entry) }
        dictionaryItem.addSection(new ChangeLogSection(dictionaryItem, changeLogHeaderText, changeLogFooterText, changeLogRows))
    }

    @JsonIgnore
    List<Topic> getWebsiteTopics() {
        List<Topic> topics = []
        topics.add(descriptionTopic())
        if (isActivePage()) {
            if (getAliases()) {
                topics.add(aliasesTopic())
            }
            if (whereUsed) {
                topics.add(whereUsedTopic())
            }
        }
        topics.add(changeLogTopic())
        return topics
    }

    Topic generateTopic() {
        String titleOutputClass = getOutputClass()
        Topic.build(
            id: getDitaKey()
        ) {
            title (outputClass: titleOutputClass)  {
                text getNameWithRetired()
            }
            shortdesc getShortDescription()
            getWebsiteTopics().each {
                topic it
            }
        }
    }

    Topic descriptionTopic() {
        Topic.build (id: getDitaKey() + "_description") {
            title "Description"
            body {
                if(catalogueItem.description) {
                    div HtmlHelper.replaceHtmlWithDita(catalogueItem.description.replace('<table', '<table class=\"table-striped\"'))
                }
            }
        }
    }

    Topic whereUsedTopic() {
        Topic.build (id: getDitaKey() + "_whereUsed") {
            title "Where Used"
            body {
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*", "3*", "2*"]), outputClass: "table table-sm table-striped") {
                    stHead (outputClass: "thead-light") {
                        stentry "Type"
                        stentry "Link"
                        stentry "How used"
                    }
                    whereUsed
                        .findAll { !it.key.isRetired() }
                        .sort { it.key.name }
                        .each {component, text ->
                            strow {
                                stentry component.stereotype
                                stentry {
                                    xRef component.calculateXRef()
                                }
                                stentry text
                            }
                        }
                }
            }
        }
    }

    Topic aliasesTopic() {
        Topic.build (id: getDitaKey() + "_aliases") {
            title "Also Known As"
            body {
                p "This ${getStereotype()} is also known by these names:"
                simpletable(relColWidth: new SpaceSeparatedStringList (["1*","2*"]), outputClass: "table table-sm table-striped") {
                    stHead (outputClass: "thead-light") {
                        stentry "Context"
                        stentry "Alias"
                    }
                    getAliases().each {context, alias ->
                        strow {
                            stentry context
                            stentry alias
                        }
                    }
                }
            }
        }
    }

    Topic changeLogTopic() {
        Topic.build(id: getDitaKey() + "_changeLog") {
            title "Change Log"
            body {
                if (changeLog && !changeLog.empty && changeLogHeaderText) {
                    div HtmlHelper.replaceHtmlWithDita(changeLogHeaderText)
                }
                if (changeLog && !changeLog.empty) {
                    simpletable(relColWidth: new SpaceSeparatedStringList (["2*", "5*", "3*"]), outputClass: "table table-sm table-striped") {
                        stHead (outputClass: "thead-light") {
                            stentry "Change Request"
                            stentry "Change Request Description"
                            stentry "Implementation Date"
                        }
                        changeLog.each { entry ->
                            strow {
                                stentry {
                                    if (entry.referenceUrl) {
                                        xRef getExternalXRef(entry.referenceUrl, entry.reference)
                                    }
                                    else {
                                        txt entry.reference
                                    }
                                }
                                stentry entry.description
                                stentry entry.implementationDate
                            }
                        }
                    }
                }
                if (changeLogFooterText) {
                    div HtmlHelper.replaceHtmlWithDita(changeLogFooterText)
                }
            }
        }
    }

    XRef calculateXRef() {
        XRef.build(
            outputClass: getOutputClass(),
            keyRef: getDitaKey(),
            format: "html"
        ) {
            txt getNameWithRetired()
        }
    }

    @JsonIgnore
    XRef getExternalXRef(String url, String text) {
        XRef.build(
            scope: Scope.EXTERNAL,
            format: "html",
            href: url
        ) {
            txt text
        }
    }

    @JsonIgnore
    String getOutputClass() {
        String outputClass = getStereotypeForPreview()
        if(isRetired()) {
            outputClass += " retired"
        }

        return outputClass
    }

    void replaceLinksInDefinition(Map<String, NhsDataDictionaryComponent> pathLookup) {
        if(catalogueItem.description) {
            catalogueItem.description = replaceLinksInString(catalogueItem.description, pathLookup)
        }
    }

    String replaceLinksInString(String source, Map<String, NhsDataDictionaryComponent> pathLookup) {
        NhsDataDictionary.replaceLinksInStringAndUpdateWhereUsed(source, pathLookup, this)
    }

    static List<String> calculateSentences(String html) {
        Node xml = HtmlHelper.tidyAndConvertToNode(html)
        if(xml.children().find { childNode ->
            childNode instanceof String || childNode.name().toString().toLowerCase() == 'a' // An indicator that there are no paragraphs
        }) {
            return xml.text().split("\\.")
        }

        List<String> response = this.getNodeSentences(xml)

        response.removeAll {it.trim() == ""}
        return response
    }

    @JsonIgnore
    static List<String> getNodeSentences(String str) {
        return str.split("\\.")
    }

    @JsonIgnore
    static List<String> getNodeSentences(Node xml) {
        List<String> response = []
        xml.children().each { childNode ->
            if (childNode instanceof String) {
                response.add((String) childNode)
            } else {
                switch (childNode.name().toString().toLowerCase()) {
                    case 'img':
                    case 'br':
                        break
                    case 'ul':
                    case 'table':
                    case 'div':
                        childNode.children().each { child ->
                            response.addAll(getNodeSentences(child))
                        }
                        break
                    case 'p':
                    case 'span':
                    case 'strong':
                    default:
                        response.addAll(childNode.text().split("\\."))
                        break


                }
            }
        }
        return response
    }


    @JsonIgnore
    String getFirstSentence(String html = this.getDescription()) {
        getSentence(html, 0)
    }

    @JsonIgnore
    String getSentence(String html = this.getDescription(), int i) {
        if(!html) {
            return null
        }
        String sentence = calculateSentences(html)[i]
        if (!sentence) {
            return null
        }
        return tidyShortDescription(sentence).trim() + "."
    }

    String tidyShortDescription(String sentence) {
        if(!sentence) {
            return null
        }
        String response = sentence.replace("_", " ")
        response = response.replaceAll("\\s+", " ")
        return response
    }

    @JsonIgnore
    List<String> getWebPath() {
        if(otherProperties["baseUri"]) {
            // This is really for when we're ingesting
            List<String> path = []
            try {
                path.addAll(DDHelperFunctions.getPath(otherProperties["baseUri"], "Messages", ".txaClass20"))
            } catch (Exception e) {
                path.addAll(DDHelperFunctions.getPath(otherProperties["baseUri"], "Web_Site_Content", ".txaClass20"))
            }
            path.removeAll {it.equalsIgnoreCase("Data_Sets")}
            path.removeAll {it.equalsIgnoreCase("Content")}

            if(name.startsWith("CDS") && !(isRetired())) {
                path.add(0, "Commissioning Data Sets")
            }
            if (isRetired()) {
                path.add(0, "Retired")
            }
            path = path.collect {DDHelperFunctions.tidyLabel(it)}
            return path
        } else {
            // otherwise, get the path from the folder hierarchy -
            // TODO this with inheritance
            if(this instanceof NhsDDDataSet) {
                return ((NhsDDDataSet)this).path
            }
        }
    }

    /*
    Helper functions to resolve type checking issues in grails views
     */
    @JsonIgnore
    String getCatalogueItemIdAsString() {
        return catalogueItem.id.toString()
    }
    @JsonIgnore
    String getCatalogueItemDomainTypeAsString() {
        return catalogueItem.domainType.toString()
    }

    void updateWhereUsed() {

    }


    NhsDataDictionaryComponent<T> fromMauroItem(NhsDataDictionary dataDictionary, MauroPersistenceService mauroPersistenceService, T catalogueItem) {
        this.catalogueItem = catalogueItem
        this.dataDictionary = dataDictionary

        catalogueItemId = catalogueItem.id
        if(!branchId) {
            branchId = dataDictionary?.containingVersionedFolder?.id
        }

        // This is not obvious, but these parent/model IDs are required in the GSON views for the integrity checks - they are used for the direct
        // URLs to items in the Mauro UI
        if(catalogueItem instanceof DataClass) {
            catalogueItemParentId = ((DataClass)catalogueItem).parentDataClass?.id?.toString()
            catalogueItemModelId = ((DataClass)catalogueItem).dataModel?.id?.toString()
        }
        if(catalogueItem instanceof DataElement) {
            catalogueItemParentId = ((DataElement)catalogueItem).dataClass?.id?.toString()
            catalogueItemModelId = ((DataElement)catalogueItem).dataClass?.dataModel?.id?.toString()
        }
        if(catalogueItem instanceof DataModel) {
            catalogueItemModelId = catalogueItem.id.toString()
        }
        if(catalogueItem instanceof Term) {
            catalogueItemModelId = ((Term)catalogueItem).terminology?.id?.toString()
        }

        List<Metadata> metadata = catalogueItem.metadata.findAll {
            it.namespace == getMetadataNamespace() &&
            NhsDataDictionary.getAllMetadataKeys().contains(it.key)
        }


        NhsDataDictionary.getAllMetadataKeys().each {key ->
            otherProperties[key] = metadata.find {it.key == key}?.value
        }

        setNhsDataDictionaryComponentChangeLog(dataDictionary)
        return this
    }

    @JsonIgnore
    static Pattern CHANGE_LOG_BRANCH_NAME_PATTERN = Pattern.compile(/(?<=\$)(.*?(?='))/)

    void setNhsDataDictionaryComponentChangeLog(NhsDataDictionary dataDictionary) {
        changeLogHeaderText = dataDictionary?.changeLogHeaderText
        changeLogFooterText = dataDictionary?.changeLogFooterText

        List<Edit> mergeEdits = getMergeEditsForChangeLog()
        if (mergeEdits.empty) {
            return
        }

        Set<String> branchNames = mergeEdits
            .collect {edit -> edit.description.find(CHANGE_LOG_BRANCH_NAME_PATTERN) }
            .findAll { branchName -> branchName != null }
            .toSet()

        if (branchNames.empty) {
            return
        }

        changeLog = branchNames
            .findAll { branchName -> dataDictionary?.workItemBranches?.containsKey(branchName) }
            .collect { branchName ->
                NhsDDBranch branch = dataDictionary?.workItemBranches?.get(branchName)
                new NhsDDChangeLog(branch, dataDictionary?.changeRequestUrl)
            }
    }

    @JsonIgnore
    List<Edit> getMergeEditsForChangeLog() {
        catalogueItem.edits.findAll {
            it.title = EditType.MERGE
        }
        // Assume already loaded in from the database
        //editService.findAllByResourceAndTitle(component.catalogueItem.domainType, component.catalogueItem.id, EditTitle.MERGE)
    }
/*
    @JsonIgnore
    List<NhsDDCode> getCodesForTerms(List<Term> terms, NhsDataDictionary nhsDataDictionary) {
        List<NhsDDCode> codes = []
        // Assume facets already loaded from teh db
        //List<Metadata> allRelevantMetadata = Metadata
        //    .byMultiFacetAwareItemIdInList(terms.collect {it.id})
        //    .inList('key', ['publishDate', 'webOrder', 'webPresentation', 'isDefault', 'isRetired', 'retiredDate'])
        //    .list()
        codes.addAll(terms.collect {term ->
            NhsDDCode nhsDDCode = nhsDataDictionary.codesByCatalogueId[term.id]
            if(!nhsDDCode) {
                nhsDDCode = new NhsDDCode().tap {
                    code = term.code
                    definition = term.definition
                    publishDate = term.metadata.find {it.multiFacetAwareItemId == term.id && it.key == 'publishDate'}?.value
                    webOrder = Integer.parseInt(term.metadata.find {it.multiFacetAwareItemId == term.id && it.key == 'webOrder'}?.value ?: "0")
                    webPresentation = term.metadata.find {it.multiFacetAwareItemId == term.id && it.key == 'webPresentation'}?.value
                    isDefault = Boolean.valueOf(term.metadata.find {it.multiFacetAwareItemId == term.id && it.key == 'isDefault'}?.value ?: "false")
                    isRetired = Boolean.valueOf(term.metadata.find {it.multiFacetAwareItemId == term.id && it.key == 'isRetired'}?.value ?: "false")
                    retiredDate = term.metadata.find {it.multiFacetAwareItemId == term.id && it.key == 'retiredDate'}?.value
                    it.catalogueItem = term
                }
                nhsDataDictionary.codesByCatalogueId[term.id] = nhsDDCode
            }
            return nhsDDCode
        })
        return codes
    }
*/
    @JsonIgnore
    @Override
    String getDiscriminator() {
        name
    }

}