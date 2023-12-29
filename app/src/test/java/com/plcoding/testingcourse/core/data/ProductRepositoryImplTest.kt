package com.plcoding.testingcourse.core.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.plcoding.testingcourse.core.domain.LogParam
import com.plcoding.testingcourse.core.domain.ProductRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

internal class ProductRepositoryImplTest {

    private lateinit var repository: ProductRepositoryImpl
    private lateinit var productApi: ProductApi
    private lateinit var analyticsLogger: FirebaseAnalyticsLogger
    private lateinit var mockWebServer: MockWebServer

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        // Create a mock Retrofit service
        // but the base url actually does not connect to the internet, only in the local end
        productApi = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(mockWebServer.url("/"))
            .build()
            .create()

        analyticsLogger = mockk(relaxed = true)
        repository = ProductRepositoryImpl(productApi, analyticsLogger)
    }

    @Test
    fun `Response error, exception logger - MockWebServer` () = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(404)//Can mock more configurations in MockResponse() API if needed
        )

        val result = repository.purchaseProducts((listOf()))

        assertThat(result.isFailure).isTrue()

        verify {
            analyticsLogger.logEvent(
                "http_error",
                LogParam("code",404),
                LogParam("message", "Client Error")
            )
        }

    }


    @Test
    fun `Response error, exception is logged`() = runBlocking {
        every {analyticsLogger.logEvent(any(), any(), any())} answers {
            println("This is a log event")
        }


        coEvery {productApi.purchaseProducts(any())} throws mockk<HttpException> {
            every { code() } returns 404
            every { message() } returns "Test message"
        }
        val result = repository.purchaseProducts(listOf())

        assertThat(result.isFailure).isTrue()

        verify {
            analyticsLogger.logEvent(
                "http_error",
                LogParam("code",404),
                LogParam("message", "Test message")
            )
        }

        }
}






