package uk.nhs.datadictionary.services

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.CodeSetCacheableRepository
import org.maurodata.persistence.terminology.TerminologyRepository

@Singleton
class MauroPersistenceService {

    @Inject
    TermCacheableRepository termCacheableRepository

    @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassCacheableRepository

    @Inject
    CodeSetCacheableRepository codeSetCacheableRepository

    @Inject
    DataTypeCacheableRepository dataTypeCacheableRepository

    @Inject ModelCacheableRepository.TerminologyCacheableRepository terminologyCacheableRepository
    @Inject TerminologyRepository terminologyRepository
    @Inject ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository
    @Inject AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementCacheableRepository


}
