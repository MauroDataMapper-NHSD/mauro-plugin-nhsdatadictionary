package uk.nhs.datadictionary.services.profiles

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.TermCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.CodeSetCacheableRepository

@Singleton
class MauroPersistenceService {

    @Inject
    TermCacheableRepository termCacheableRepository

    @Inject
    CodeSetCacheableRepository codeSetCacheableRepository

    @Inject
    DataTypeCacheableRepository dataTypeCacheableRepository

}
