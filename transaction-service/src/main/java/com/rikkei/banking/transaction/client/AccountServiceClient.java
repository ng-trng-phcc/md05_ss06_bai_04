package com.rikkei.banking.transaction.client;

import com.rikkei.banking.transaction.dto.AccountResponse;
import com.rikkei.banking.transaction.exception.SourceAccountNotFoundException;
import com.rikkei.banking.transaction.exception.UpstreamFailureException;
import com.rikkei.banking.transaction.exception.UpstreamServiceUnavailableException;
import com.rikkei.banking.transaction.exception.UpstreamTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;

@Component
public class AccountServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClient.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    private final RestTemplate restTemplate;
    private final String accountServiceBaseUrl;

    public AccountServiceClient(
            RestTemplate accountServiceRestTemplate,
            @Value("${account-service.base-url}") String accountServiceBaseUrl) {
        this.restTemplate = accountServiceRestTemplate;
        this.accountServiceBaseUrl = accountServiceBaseUrl;
    }

    public AccountResponse getAccount(Long accountId) {
        String requestUrl = accountServiceBaseUrl + "/api/accounts/" + accountId;

        HttpHeaders headers = new HttpHeaders();
        String requestId = MDC.get("requestId");
        if (requestId != null && !requestId.isBlank()) {
            headers.set(REQUEST_ID_HEADER, requestId);
        }

        try {
            log.info("Calling {} via Docker internal DNS", requestUrl);
            ResponseEntity<AccountResponse> response = restTemplate.exchange(
                    accountServiceBaseUrl + "/api/accounts/{id}",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    AccountResponse.class,
                    accountId);

            log.info("account-service returned HTTP {}", response.getStatusCode().value());

            AccountResponse body = response.getBody();
            if (body == null || body.id() == null) {
                throw new UpstreamFailureException("account-service returned an empty account payload");
            }
            return body;
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("account-service returned HTTP {} for account id={}",
                    ex.getStatusCode().value(), accountId);
            throw new SourceAccountNotFoundException(accountId);
        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            log.warn("account-service returned unexpected HTTP {} for account id={}", status, accountId);
            if (status >= 500) {
                throw new UpstreamFailureException("account-service failed with HTTP " + status);
            }
            throw new UpstreamFailureException("account-service rejected the request with HTTP " + status);
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                log.error("account-service request timed out for account id={}", accountId);
                throw new UpstreamTimeoutException("account-service timed out");
            }
            log.error("account-service is unreachable for account id={}: {}", accountId, ex.getMessage());
            throw new UpstreamServiceUnavailableException("account-service is unavailable");
        } catch (RestClientException ex) {
            log.error("Unexpected account-service client failure for account id={}", accountId, ex);
            throw new UpstreamFailureException("Unexpected account-service response");
        }
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
