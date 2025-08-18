package uk.nhs.datadictionary

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.xml.XmlParser
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import org.maurodata.plugin.importer.FolderImporterPlugin
import uk.nhs.datadictionary.services.NhsDataDictionaryService

@Slf4j
@Singleton
@CompileStatic
class NhsDataDictionaryImporter implements FolderImporterPlugin<DataDictionaryImportParameters> {

    @Inject
    NhsDataDictionaryService nhsDataDictionaryService

    static XmlParser xmlParser = new XmlParser(false, false)

    @Override
    List<Folder> importDomain(DataDictionaryImportParameters params) {
        def xml = xmlParser.parse(params.importFile.getInputStream())
        [nhsDataDictionaryService.ingest(xml, params)]
    }

    @Override
    Boolean handlesContentType(String contentType) {
        return contentType == 'text/xml'
    }

    @Override
    Class<DataDictionaryImportParameters> importParametersClass() {
        return DataDictionaryImportParameters
    }

    @Override
    String getVersion() {
        "1.0.0"
    }
}
