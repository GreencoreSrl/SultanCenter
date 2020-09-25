package com.ncr.ssco.communication.entities.pos;

import com.ncr.ssco.communication.entities.TableElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Umberto on 16/05/2017.
 */
public class SscoTerminal {

    private SscoTransaction transaction;
    private SscoCashier cashier;
    private String idTransactionVoided;
    private String regNumber;
    private List<TableElement> departmentsTable = new ArrayList<TableElement>();

    public SscoTerminal(){
        this.cashier = new SscoCashier();
        this.transaction = new SscoTransaction();
    }

    public SscoTerminal(String reg){
        this.cashier = new SscoCashier();
        this.transaction = new SscoTransaction();
        this.regNumber = reg;
    }

    public SscoTerminal(SscoTransaction transaction, SscoCashier cashier) {
        this.transaction = transaction;
        this.cashier = cashier;
    }

    public String getIdTransactionVoided() {
        return idTransactionVoided;
    }

    public void setIdTransactionVoided(String idTransactionVoided) {
        this.idTransactionVoided = idTransactionVoided;
    }

    public void initTransaction(int id){
        this.transaction = new SscoTransaction(id);
    }

    public void voidTransaction(){
        transaction = new SscoTransaction();
        //this.transaction.setItems(null);
        //this.transaction = null;
    }

    public SscoTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(SscoTransaction transaction) {
        this.transaction = transaction;
    }

    public SscoCashier getCashier() {
        return cashier;
    }

    public void setCashier(SscoCashier cashier) {
        this.cashier = cashier;
    }

    public String getRegNumber() {
        return regNumber;
    }

    public void setRegNumber(String regNumber) {
        this.regNumber = regNumber;
    }

    public List<TableElement> getDepartmentsTable() {
        return departmentsTable;
    }

    public void setDepartmentsTable(List<TableElement> departmentsTable) {
        this.departmentsTable = departmentsTable;
    }
}
