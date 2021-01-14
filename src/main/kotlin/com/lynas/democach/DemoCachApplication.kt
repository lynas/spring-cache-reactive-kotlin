package com.lynas.democach

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.*
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@SpringBootApplication
@EnableCaching
class DemoCachApplication {

}
@Configuration
class AppConfig {

    @Bean
    fun cacheManager(): CacheManager {
        return CaffeineCacheManager("Customer")
            .apply {
                isAllowNullValues = false
                setCaffeine(Caffeine.newBuilder()
                    .maximumSize(100)
                    .expireAfterAccess(1, TimeUnit.HOURS)
                )
            }
    }
}

fun main(args: Array<String>) {
    runApplication<DemoCachApplication>(*args)
}

@Table
data class Customer(
    @Id
    val id: Long?,
    val name: String
)

interface CustomerRepository : ReactiveCrudRepository<Customer, Long> {

    @Query("select * from customer where name=?")
    suspend fun findCustomerByCustomerName(name: String) : Customer

}

@Service
@CacheConfig(cacheNames = ["Customer"])
class CustomerService(
    val customerRepository: CustomerRepository
){


//    @Cacheable
//    suspend fun findCustomerByName(name: String) : Customer {
//        return customerRepository.findCustomerByCustomerName(name)
//    }

    @Cacheable
    fun findCustomerByName(name: String) : Deferred<Customer> {
        return GlobalScope.async {
            customerRepository.findCustomerByCustomerName(name)
        }
    }

}


@RestController
@RequestMapping("/customer")
class CustomerController(
    val customerRepository: CustomerRepository,
    val customerService: CustomerService
) {

    @GetMapping
    suspend fun getAllCustomer() : Flow<Customer> = customerRepository.findAll().asFlow()

    @GetMapping("/save/save")
    suspend fun saveCustomer() : Customer?  = customerRepository.save(Customer(null,"Random ${System.currentTimeMillis()}")).awaitFirstOrNull()

    @GetMapping("/{name}")
    suspend fun getCustomerByName(@PathVariable name: String) : Customer = customerService.findCustomerByName(name).await()

}


