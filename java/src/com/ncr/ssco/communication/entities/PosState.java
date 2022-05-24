package com.ncr.ssco.communication.entities;

/**
 * Created by stefanobertarello on 01/03/17.
 */
public enum PosState {
    Inactive("inactive"),
    Active ("active"),
    Busy ("busy");

    private final String name;

    PosState(String state) {
        name = state;
    }

    public boolean equals(String otherName) {
        return name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
}