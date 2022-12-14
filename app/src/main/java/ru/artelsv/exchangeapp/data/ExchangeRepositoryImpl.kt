package ru.artelsv.exchangeapp.data

import kotlinx.coroutines.flow.*
import ru.artelsv.exchangeapp.data.datasource.ExchangeDataSource
import ru.artelsv.exchangeapp.domain.model.BaseCurrency
import ru.artelsv.exchangeapp.domain.model.toModel
import ru.artelsv.exchangeapp.domain.model.toModelList
import ru.artelsv.exchangeapp.domain.repository.ExchangeRepository
import javax.inject.Inject
import javax.inject.Named

class ExchangeRepositoryImpl @Inject constructor(
    @Named("remote")
    private val remote: ExchangeDataSource,
    @Named("local")
    private val local: ExchangeDataSource
) : ExchangeRepository {

    private var lastList: List<BaseCurrency.Currency> = emptyList()

    override fun getList(
        currency: BaseCurrency.Currency?
    ): Flow<List<BaseCurrency.Currency>> {

        return when (currency) {
            null -> {
                if (lastList.isEmpty()) {
                    initList()
                } else {
                    favUpdateList()
                }
            }
            else -> {
                remote.getListByCurrency(currency, lastList).map { it.toModelList() }.combine(local.getFavouriteList()) { list, favList ->
                    val newList = arrayListOf<BaseCurrency.Currency>()
                    newList.addAll(list)

                    favList.forEach { item ->
                        val id = newList.indexOfFirst { it.name == item.name }
                        if (id != -1) {
                            newList[id].favourite = item.favourite
                        }
                    }

                    newList
                }
            }
        }
    }

    override fun getFavouriteList(): Flow<List<BaseCurrency.Currency>> =
        local.getFavouriteList()

    override suspend fun setFavourite(currency: BaseCurrency.Currency) {
        local.setFavourite(currency.toModel())
    }

    private fun initList() =
        remote.getList().map { it.toModelList() }.combine(local.getFavouriteList()) { list, favList ->
            val newList = arrayListOf<BaseCurrency.Currency>()
            newList.addAll(list)

            favList.forEach { item ->
                val id = newList.indexOfFirst { it.name == item.name }
                if (id != -1) {
                    newList[id].favourite = item.favourite
                }
            }

            lastList = newList
            newList
        }

    private fun favUpdateList() =
        local.getFavouriteList().map { favList ->
            val newList = arrayListOf<BaseCurrency.Currency>()
            newList.addAll(lastList)

            favList.forEach { item ->
                val id = newList.indexOfFirst { it.name == item.name && it.favourite != item.favourite}
                if (id != -1) {
                    newList[id].favourite = item.favourite
                }
            }

            newList
        }
}