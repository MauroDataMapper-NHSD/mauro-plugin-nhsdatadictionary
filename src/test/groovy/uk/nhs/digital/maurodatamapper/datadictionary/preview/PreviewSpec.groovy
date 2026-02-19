package uk.nhs.digital.maurodatamapper.datadictionary.preview

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.maurodata.api.folder.FolderApi
import org.maurodata.domain.folder.Folder
import org.maurodata.plugin.importer.FileParameter
import org.maurodata.web.ListResponse
import spock.lang.Shared
import spock.lang.Specification
import uk.nhs.datadictionary.DataDictionaryImportParameters
import uk.nhs.datadictionary.NhsDDAttribute
import uk.nhs.datadictionary.NhsDataDictionaryImporter
import uk.nhs.datadictionary.NhsDataDictionaryWebsiteExporter
import uk.nhs.datadictionary.api.NhsDataDictionaryApi
import uk.nhs.datadictionary.utils.StereotypedCatalogueItem

@Slf4j
@MicronautTest(startApplication = true, environments = ['secured'])
@Property(name = "datasources.default.driver-class-name",
    value = "org.testcontainers.jdbc.ContainerDatabaseDriver")
@Property(name = "datasources.default.url",
    value = "jdbc:tc:postgresql:16-alpine:///db")
class PreviewSpec extends Specification {

    @Inject
    @Client('/')
    @Shared
    HttpClient client

    @Inject
    @Shared
    NhsDataDictionaryImporter nhsDataDictionaryImporter

    @Inject
    @Shared
    NhsDataDictionaryWebsiteExporter nhsDataDictionaryWebsiteExporter

    @Inject
    FolderApi folderApi

    @Inject NhsDataDictionaryApi nhsDataDictionaryAPI


    @Shared
    byte[] xmlBytes = new byte[60000000]

    @Shared
    DataDictionaryImportParameters dataDictionaryImportParameters

    @Shared UUID branchId

    @Inject ApplicationContext ctx

    void setup() {
        println "Active envs = ${ctx.environment.activeNames}"
        System.err.println(Runtime.getRuntime().maxMemory())
        this.class.getClassLoader().getResourceAsStream("datadictionary_november2025.xml").withStream {stream ->
            xmlBytes = stream.readAllBytes()
        }
        dataDictionaryImportParameters = new DataDictionaryImportParameters()
        dataDictionaryImportParameters.importFile = new FileParameter('datadictionary_november2025.xml', 'application/xml', xmlBytes)
        MultipartBody importRequest = MultipartBody.builder()
        //  .addPart('folderId', folderId.toString()) // Should now be optional
            .addPart('importFile', 'file.json', MediaType.APPLICATION_XML_TYPE, xmlBytes)
            .build()

        ListResponse<Folder> folders = folderApi.importModel(
            importRequest,
            nhsDataDictionaryImporter.namespace,
            nhsDataDictionaryImporter.name,
            nhsDataDictionaryImporter.version)

        branchId = folders.items.get(0).id
    }

    void "list all elements"() {
        when:
        List<StereotypedCatalogueItem> catalogueItems = nhsDataDictionaryAPI.allItemsIndex(branchId)

        then:
        catalogueItems.size() == 10107

        catalogueItems.count { sci ->
            (0..9).find {sci.name.startsWith(it.toString())}
        } == 6

        [a:690, b:386, c:1215, d:594, e:521, f:181, g:208, h:396, i:326, j:33, k:30, l:247, m:479,
        n:467, o: 599, p:1404, q:18, r:607, s:955, t:366, u:154, v:42, w:147, x:2, y:32, z:2].every {alpha, size ->
            System.err.println(alpha)
            System.err.println(catalogueItems.findAll {sci -> sci.name.toLowerCase().startsWith(alpha)}.sort {
                it.name.toLowerCase()
            }.collect {it.name})
            System.err.println(catalogueItems.findAll {sci -> sci.name.toLowerCase().startsWith(alpha)}.sort {
                it.name.toLowerCase()
            }.collect {it.name}.size())
            catalogueItems.findAll {sci -> sci.name.toLowerCase().startsWith(alpha)}.size() == size
        }

/*
    In 'C' the published version has CLINICAL TRIAL IDENTIFIER (e) and CHILDHOOD IMMUNISATION TYPE as both retired and non-retired
       There are two DISCHARGE LETTER ISSUED DATE (COMMUNITY CARE) (e)
       And two PATIENT ORGANISATION END DATE (a)
 */

        catalogueItems.every {
            !it.stereotype.isEmpty() &&
                !it.name.isEmpty() &&
                it.catalogueItemId

        }
    }

    void "Export website"() {
        when:
        HttpResponse<byte[]> response = folderApi.exportModel(branchId, nhsDataDictionaryWebsiteExporter.namespace, nhsDataDictionaryWebsiteExporter.name, nhsDataDictionaryWebsiteExporter.version)

        then:
        response.body()
    }

    void "Preview Attribute"() {
        when:
        List<StereotypedCatalogueItem> attributeList = nhsDataDictionaryAPI.indexAttributes(branchId, true)
        List<StereotypedCatalogueItem> businessDefinitionList = nhsDataDictionaryAPI.indexBusinessDefinitions(branchId, true)

        then:
        attributeList.size() == 2701
        System.err.println(businessDefinitionList.size())
        businessDefinitionList.size() == 1260

        when:
        UUID attributeId = attributeList.find { it.name == "ABDOMINAL X-RAY PERFORMED REASON"}.catalogueItemId
        UUID busDefId = businessDefinitionList.find { it.name == "Abdominal X-Ray"}.catalogueItemId

        Map<String, Object> response = client.toBlocking().retrieve(HttpRequest.GET("api/nhsdd/${branchId}/preview/attributes/${attributeId}"), Map)

        then:
        response.branchId == branchId.toString()
        response.name == "ABDOMINAL X-RAY PERFORMED REASON"
        response.stereotype == "Attribute"
        response.stereotypeForPreview == "attribute"
        response.nationalCodes.size() == 8
        response.retired == false
        response.preparatory == false
        response.shortDescription == "The reason why an Abdominal X-Ray was performed."
        response.dataElements.size() == 1
        response.dataElements[0].name == "ABDOMINAL X-RAY PERFORMED REASON"
        response.dataElements[0].stereotype == "Data Element"
        response.htmlDescription == "<p>The reason why an     <a class=\"businessDefinition\" href=\"#/preview/${branchId}/businessDefinition/${busDefId}\">Abdominal X-Ray</a> was performed.  </p>"
        response.alsoKnownAs.size() == 1
        response.alsoKnownAs['Plural'] == "ABDOMINAL X-RAY PERFORMED REASONS"
    }

    void "Preview Element"() {
        when:
        List<StereotypedCatalogueItem> elementList = nhsDataDictionaryAPI.indexElements(branchId, true)
        List<StereotypedCatalogueItem> attributeList = nhsDataDictionaryAPI.indexAttributes(branchId, true)
        List<StereotypedCatalogueItem> businessDefinitionList = nhsDataDictionaryAPI.indexBusinessDefinitions(branchId, true)
        List<StereotypedCatalogueItem> classList = nhsDataDictionaryAPI.indexClasses(branchId, true)

        then:
        System.err.println(elementList.size())
        elementList.size() == 5279
        //businessDefinitionList.size() == 1260

        when:
        UUID elementId = elementList.find { it.name == "ABBREVIATED MENTAL TEST SCORE"}.catalogueItemId
        UUID busDefId = businessDefinitionList.find { it.name == "Breast Cancer Care Spell"}.catalogueItemId
        UUID busDefId2 = businessDefinitionList.find { it.name == "Abbreviated Mental Test Score"}.catalogueItemId
        UUID classId = classList.find { it.name == "ASSESSMENT TOOL"}.catalogueItemId
        UUID attributeId = attributeList.find { it.name == "PERSON SCORE"}.catalogueItemId

        Map<String, Object> response = client.toBlocking().retrieve(HttpRequest.GET("api/nhsdd/${branchId}/preview/elements/${elementId}"), Map)
        then:
        response.branchId == branchId.toString()
        response.name == "ABBREVIATED MENTAL TEST SCORE"
        response.stereotype == "Data Element"
        response.stereotypeForPreview == "element"
        !response.nationalCodes
        response.retired == false
        response.preparatory == false
        response.shortDescription == "The score taken from an ASSESSMENT TOOL."
        response.formatLength == "max n2"
        response.allAttributesForElement.size() == 1
        response.allAttributesForElement[0].stereotype == "Attribute"
        response.allAttributesForElement[0].name == "PERSON SCORE"
        response.htmlDescription == "<a class=\"element\" href=\"#/preview/$branchId/element/$elementId\">ABBREVIATED MENTAL TEST SCORE</a> is the same as attribute <a class=\"attribute\" href=\"#/preview/$branchId/attribute/${response.allAttributesForElement[0].catalogueItemId}\">PERSON SCORE</a>. <p>    <a class=\"element\" href=\"#/preview/${branchId}/element/${elementId}\">ABBREVIATED MENTAL TEST SCORE</a> is recorded during a     <a class=\"businessDefinition\" href=\"#/preview/$branchId/businessDefinition/${busDefId}\">Breast Cancer Care Spell</a> where the     <a class=\"class\" href=\"#/preview/$branchId/class/$classId\">ASSESSMENT TOOL</a> is     <em>'      <a class=\"businessDefinition\" href=\"#/preview/$branchId/businessDefinition/${busDefId2}\">Abbreviated Mental Test Score</a>'    </em>.  </p>  <p>The score is in the range 0 to 10.</p>"
        response.alsoKnownAs.size() == 1
        response.alsoKnownAs['Plural'] == "ABBREVIATED MENTAL TEST SCORES"
    }




}
