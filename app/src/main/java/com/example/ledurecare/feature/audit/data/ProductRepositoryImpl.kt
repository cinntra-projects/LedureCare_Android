package com.example.ledurecare.feature.audit.data

import com.example.ledurecare.BuildConfig
import com.example.ledurecare.core.common.AppResult
import com.example.ledurecare.core.common.IoDispatcher
import com.example.ledurecare.core.common.safeCall
import com.example.ledurecare.core.network.BusinessPartnerServer
import com.example.ledurecare.core.network.findArray
import com.example.ledurecare.feature.audit.domain.Product
import com.example.ledurecare.feature.audit.domain.ProductRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/** One entry of `data` in the `item/bp_order_items` answer. */
@Serializable
data class BpItemDto(
    val ItemCode: String? = null,
    val ItemName: String? = null,
    val UnitPrice: String? = null,
    val Discount: String? = null,
    val HSN: String? = null,
    val UoS: String? = null,
    val U_GST: String? = null,
    val TaxCode: String? = null,
    val Status: String? = null,
)

/** Null for an entry without a code, or one the server marks inactive (`tNO`). */
internal fun BpItemDto.toProduct(): Product? {
    val code = ItemCode?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (Status.equals("tNO", ignoreCase = true)) return null
    return Product(
        code = code,
        name = ItemName?.trim().orEmpty().ifEmpty { code },
        unitPrice = UnitPrice?.takeIf { it.isNotBlank() },
        discount = Discount?.takeIf { it.isNotBlank() },
        hsn = HSN?.takeIf { it.isNotBlank() },
        uom = UoS?.takeIf { it.isNotBlank() },
        gst = U_GST?.takeIf { it.isNotBlank() },
        taxCode = TaxCode?.takeIf { it.isNotBlank() },
    )
}

internal fun JsonElement.toProducts(json: Json): List<Product> = findArray().mapNotNull { element ->
    runCatching { json.decodeFromJsonElement(BpItemDto.serializer(), element) }.getOrNull()?.toProduct()
}

const val PRODUCT_PAGE_SIZE = 20

class ProductRepositoryImpl @Inject constructor(
    private val api: ProductApi,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ProductRepository {

    override suspend fun search(cardCode: String, text: String): AppResult<List<Product>> =
        safeCall(io) {
            api.searchItems(
                BpItemsRequest(CardCode = cardCode, SearchText = text.takeIf { it.isNotBlank() }, PageNo = 1, MaxSize = PRODUCT_PAGE_SIZE)
            ).toProducts(json)
        }
}

/** Sample items for when `USE_STATIC_PRODUCTS` is on. */
@Singleton
class FakeProductRepository @Inject constructor() : ProductRepository {
    override suspend fun search(cardCode: String, text: String): AppResult<List<Product>> =
        AppResult.Success(
            SampleAuditCatalog.products.filter {
                it.code.contains(text, ignoreCase = true) || it.name.contains(text, ignoreCase = true)
            }.take(PRODUCT_PAGE_SIZE)
        )
}

@Module
@InstallIn(SingletonComponent::class)
object ProductModule {

    @Provides
    @Singleton
    fun provideProductApi(@BusinessPartnerServer retrofit: Retrofit): ProductApi = retrofit.create(ProductApi::class.java)

    @Provides
    @Singleton
    fun provideProductRepository(
        real: Provider<ProductRepositoryImpl>,
        fake: Provider<FakeProductRepository>,
    ): ProductRepository = if (BuildConfig.USE_STATIC_PRODUCTS) fake.get() else real.get()
}
