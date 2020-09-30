package com.ncr.ecommerce;

import com.ncr.ECommerceManager;
import org.apache.log4j.BasicConfigurator;

public class Test {

    public static void main(String args[]) {
        BasicConfigurator.configure();
        ECommerceManager.getInstance().checkForNewBasket("001");
    }
}