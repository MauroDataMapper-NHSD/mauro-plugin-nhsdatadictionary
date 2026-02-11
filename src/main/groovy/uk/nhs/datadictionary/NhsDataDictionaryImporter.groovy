package uk.nhs.datadictionary

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.domain.folder.Folder
import org.maurodata.plugin.importer.FolderImporterPlugin

@Slf4j
@Singleton
@CompileStatic
class NhsDataDictionaryImporter implements FolderImporterPlugin<DataDictionaryImportParameters> {

    @Inject
    @JsonIgnore
    ApplicationContext applicationContext

    @Override
    List<Folder> importDomain(DataDictionaryImportParameters params) {
        NhsDataDictionary nhsDataDictionary = applicationContext.getBean(NhsDataDictionary)
        nhsDataDictionary.buildFromXml(params)
        [nhsDataDictionary.generateFolder(params)]
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

    String displayName = 'NHS Data Dictionary XML Importer'
}
