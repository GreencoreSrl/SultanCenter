package com.ncr.ssco.communication.entities;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public enum TenderTypeEnum {
    Coupon("Coupon"),
    Cash ("Cash"),
    Credit ("Credit"),
    Debit ("Debit"),
    Loyalty ("Loyalty"),
    DebitSaving ("DebitSaving"),
    DebitChecking ("DebitChecking"),
    EBT ("EBT"),
    Foodstamps ("Foodstamps"),
    GiftCard ("GiftCard"),
    PIPGeneric ("PIPGeneric"),
    PIPCredit ("PIPCredit"),
    PIPDebit ("PIPDebit"),
    PIPDebitCashBack ("PIPDebitCashBack"),
    PIPLoyalty ("PIPLoyalty"),
    PIPDebitSaving ("PIPDebitSaving"),
    PIPDebitChecking ("PIPDebitChecking"),
    PIPEBT ("PIPEBT"),
    PIPFoodStamps ("PIPFoodStamps"),
    PIPGiftCard ("PIPGiftCard"),
    ChecK ("ChecK"),
    CashRap ("CashRap"),
    Voucher("Voucher"),
    AssistTender ("AssistTender"),
    CustomPayment ("CustomPayment"),
    UserDefined1 ("UserDefined1"),
    UserDefined2 ("UserDefined2"),
    UserDefined3 ("UserDefined3"),
    UserDefined4 ("UserDefined4"),
    UserDefined5 ("UserDefined5"),
    UserDefined6 ("UserDefined6"),
    UserDefined7 ("UserDefined7"),
    UserDefined8 ("UserDefined8"),
    UserDefined9 ("UserDefined9"),
    UserDefined10 ("UserDefined10"),
    UserDefined11 ("UserDefined11"),
    UserDefined12 ("UserDefined12"),
    UserDefined13 ("UserDefined13"),
    UserDefined14 ("UserDefined14"),
    UserDefined15 ("UserDefined15"),
    UserDefined16 ("UserDefined16"),
    UserDefined17 ("UserDefined17"),
    UserDefined18 ("UserDefined18"),
    UserDefined19 ("UserDefined19"),
    UserDefined20 ("UserDefined20"),
    UserDefined21 ("UserDefined21"),
    UserDefined22 ("UserDefined22"),
    UserDefined23 ("UserDefined23"),
    UserDefined24 ("UserDefined24"),
    UserDefined25 ("UserDefined25"),
    UserDefined26 ("UserDefined26"),
    None ("None");


    private final String type;

    TenderTypeEnum(String type) {
        this.type = type;
    }

    public boolean equals(String otherType) {
        return type.equals(otherType);
    }

    public String toString() {
        return this.type;
    }
}