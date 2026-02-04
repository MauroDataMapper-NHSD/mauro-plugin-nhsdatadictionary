package uk.nhs.datadictionary

import jakarta.inject.Singleton
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.terminology.Term
import uk.nhs.datadictionary.services.BusinessDefinitionService
import uk.nhs.datadictionary.services.ElementService

@Singleton
class NhsDataDictionaryComponentFactory {

    NhsDDElement createElement(DataElement dataElement, UUID branchId, ElementService elementService) {
        NhsDDElement retElement = new NhsDDElement(dataElement, branchId)
        retElement.dataDictionaryComponentService = elementService
        return retElement
    }


    NhsDDBusinessDefinition createBusinessDefinition(Term term, UUID branchId, BusinessDefinitionService businessDefinitionService) {
        NhsDDBusinessDefinition retBusinessDefinition = new NhsDDBusinessDefinition(term, branchId)
        retElement.dataDictionaryComponentService = elementService
        return retElement
    }


}
