package com.company.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.company.model.Bill;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    boolean existsByInvoiceNo(String invoiceNo);

    boolean existsByFileHash(String fileHash);

    List<Bill> findAllByOrderByIdDesc();

    List<Bill> findByVendorNameAndAmountAndBillDate(
        String vendorName, Double amount, String billDate);
}