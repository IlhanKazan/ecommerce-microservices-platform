package com.example.payment_service.iyzico.service.impl;

import com.example.payment_service.iyzico.entity.IyzicoTransaction;
import com.example.payment_service.iyzico.repository.IyzicoTransactionRepository;
import com.example.payment_service.iyzico.service.IyzicoTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IyzicoTransactionServiceImpl implements IyzicoTransactionService {

    private final IyzicoTransactionRepository iyzicoTransactionRepository;

    public IyzicoTransactionServiceImpl(IyzicoTransactionRepository iyzicoTransactionRepository){
        this.iyzicoTransactionRepository = iyzicoTransactionRepository;
    }


    // REQUIRES_NEW anotasyonu ile yeni bir transaction açarak mevcut transaction patlasa bile bu transaction sayesinde her turlu commitliyoruz logları
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IyzicoTransaction save(IyzicoTransaction iyzicoTransaction){
        return iyzicoTransactionRepository.save(iyzicoTransaction);
    }

}
