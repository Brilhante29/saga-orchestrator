package com.portfolio.saga

import com.portfolio.saga.domain.SagaOrchestrator
import com.portfolio.saga.domain.SagaStep
import com.portfolio.saga.domain.SagaStore
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.WebApplicationType
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SagaApplication {
    @Bean
    fun sagaOrchestrator(store: SagaStore, steps: List<SagaStep>) = SagaOrchestrator(store, steps)
}

fun main(args: Array<String>) {
    val application = SpringApplication(SagaApplication::class.java)
    if (args.contains("--benchmark")) {
        application.webApplicationType = WebApplicationType.NONE
    }
    application.run(*args)
}
