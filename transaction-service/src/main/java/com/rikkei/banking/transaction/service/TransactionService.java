package com.rikkei.banking.transaction.service;

import com.rikkei.banking.transaction.client.AccountServiceClient;
import com.rikkei.banking.transaction.dto.AccountResponse;
import com.rikkei.banking.transaction.dto.TransferRequest;
import com.rikkei.banking.transaction.dto.TransferResponse;
import com.rikkei.banking.transaction.exception.InvalidTransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final AccountServiceClient accountServiceClient;

    public TransactionService(AccountServiceClient accountServiceClient) {
        this.accountServiceClient = accountServiceClient;
    }

    public TransferResponse transfer(TransferRequest request) {
        log.info("Received transfer validation request: sourceAccountId={}, destinationAccountId={}, amount={}",
                request.sourceAccountId(), request.destinationAccountId(), request.amount());

        if (Objects.equals(request.sourceAccountId(), request.destinationAccountId())) {
            throw new InvalidTransferException("sourceAccountId and destinationAccountId must differ");
        }

        AccountResponse sourceAccount = accountServiceClient.getAccount(request.sourceAccountId());
        log.info("Source account verified: id={}, accountNumber={}, owner={}",
                sourceAccount.id(), sourceAccount.accountNumber(), sourceAccount.ownerName());

        return new TransferResponse(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                "SOURCE_ACCOUNT_VERIFIED",
                "Source account exists. Transfer execution is not implemented.");
    }
}
