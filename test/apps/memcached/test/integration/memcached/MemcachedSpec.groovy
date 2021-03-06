package memcached

import com.odobo.grails.plugin.springsecurity.rest.token.storage.MemcachedTokenStorageService
import com.odobo.grails.plugin.springsecurity.rest.token.storage.TokenNotFoundException
import grails.test.spock.IntegrationSpec
import net.spy.memcached.MemcachedClient
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Unroll

class MemcachedSpec extends IntegrationSpec {

    MemcachedClient memcachedClient

    @Shared
    MemcachedTokenStorageService tokenStorageService

    @Shared
    Integer originalExpiration

    void setupSpec() {
        originalExpiration = tokenStorageService.expiration
    }

    void cleanupSpec() {
        tokenStorageService.expiration = originalExpiration
    }

    @Unroll
	void "Memcached connection works for storing #key's"() {

        when:
        memcachedClient.set(key, 3600, object)

        then:
        memcachedClient.get(key) == object

        where:
        key         | object
        'String'    | 'My cool string value'
        'Date'      | new Date()
	}

    @Issue("https://github.com/alvarosanchez/grails-spring-security-rest/issues/86")
    void "Objects stored expire after the expiration time"() {
        given:
        tokenStorageService.expiration = 1
        UserDetails principal = new User('username', 'password', [])
        String token = 'abcd'
        tokenStorageService.storeToken(token, principal)
        Thread.sleep(1000)

        when:
        tokenStorageService.loadUserByToken(token)

        then:
        thrown(TokenNotFoundException)
    }

    @Issue("https://github.com/alvarosanchez/grails-spring-security-rest/issues/86")
    void "Objects are refreshed when accessed"() {
        given:
        tokenStorageService.expiration = 2
        UserDetails principal = new User('username', 'password', [])
        String token = 'abcd' + System.currentTimeMillis()
        tokenStorageService.storeToken(token, principal)
        Thread.sleep(1000)

        when: "it is accessed within the expiration time"
        Object details = tokenStorageService.loadUserByToken(token)

        then: "it is found, and expiration time reset to 2 sencods"
        details

        when: "it is accessed after one second"
        Thread.sleep(1000)
        tokenStorageService.loadUserByToken(token)

        then: "is still found"
        notThrown(TokenNotFoundException)
    }
}
