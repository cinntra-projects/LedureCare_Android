package com.example.ledurecare.core.network

import javax.inject.Qualifier

/**
 * Marks the HTTP client and Retrofit for the business-partner server. It is a different server from the
 * main API, so it gets its own client that never adds the login token.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BusinessPartnerServer
